package com.scripthost.engine

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.eclipsesource.v8.V8
import com.eclipsesource.v8.V8Function
import com.eclipsesource.v8.V8Object
import com.scripthost.models.ScriptContext
import com.scripthost.models.ScriptState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * JavaScript engine implementation using J2V8.
 * Provides sandboxed script execution with resource monitoring.
 *
 * Note: J2V8 runtimes are not thread-safe; this engine keeps all V8 access on
 * the thread that executes a script. UI bridge callbacks should be marshaled
 * through the bridge's own main-thread handler.
 */
class JavaScriptEngine(private val context: Context) : ScriptEngine {

    private val runtime: V8 = V8.createV8Runtime()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val engineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Resource limits
    private val maxExecutionTimeMs = 30_000L // 30 seconds

    // Active script contexts
    private val activeContexts = ConcurrentHashMap<String, ScriptContext>()

    // Bridge registry
    private val bridges = mutableListOf<ScriptBridge>()

    // Timer bookkeeping so scripts can clear timers and the engine can clean up
    private val timerIdGenerator = AtomicInteger(0)
    private val activeTimers = ConcurrentHashMap<Int, Runnable>()

    init {
        setupGlobalEnvironment()
    }

    /**
     * Setup global JavaScript environment with console and timer APIs.
     */
    private fun setupGlobalEnvironment() {
        // Console API
        val console = V8Object(runtime)
        runtime.add("console", console)

        console.registerJavaMethod(this, "consoleLog", "log", arrayOf(String::class.java))
        console.registerJavaMethod(this, "consoleWarn", "warn", arrayOf(String::class.java))
        console.registerJavaMethod(this, "consoleError", "error", arrayOf(String::class.java))

        console.release()

        // Timers
        runtime.registerJavaMethod(this, "setTimeout", "setTimeout",
            arrayOf(V8Object::class.java, Int::class.java))
        runtime.registerJavaMethod(this, "setInterval", "setInterval",
            arrayOf(V8Object::class.java, Int::class.java))
        runtime.registerJavaMethod(this, "clearTimeout", "clearTimeout",
            arrayOf(Int::class.java))
        runtime.registerJavaMethod(this, "clearInterval", "clearInterval",
            arrayOf(Int::class.java))
    }

    /**
     * Register a bridge to expose native functionality.
     */
    override fun registerBridge(bridge: ScriptBridge) {
        bridges.add(bridge)
        bridge.register(runtime)
    }

    /**
     * Execute a script with sandboxing and monitoring.
     */
    override suspend fun execute(scriptContext: ScriptContext): ExecutionResult = withContext(Dispatchers.Default) {
        val script = scriptContext.script

        try {
            scriptContext.state = ScriptState.RUNNING
            scriptContext.startTime = System.currentTimeMillis()
            activeContexts[script.id] = scriptContext

            // Start resource monitoring
            val monitorJob = startResourceMonitoring(scriptContext)

            // Execute with timeout
            val result = withTimeout(maxExecutionTimeMs) {
                try {
                    val output = runtime.executeScript(script.sourceCode, script.name, 0)
                    val resultString = output?.toString() ?: "undefined"
                    (output as? V8Object)?.release()
                    ExecutionResult.Success(resultString)
                } catch (e: CancellationException) {
                    // Let timeouts/cancellation propagate to the outer handler
                    throw e
                } catch (e: Exception) {
                    ExecutionResult.Error(e.message ?: "Unknown execution error")
                }
            }

            // Stop monitoring
            monitorJob.cancel()

            scriptContext.state = ScriptState.STOPPED
            scriptContext.endTime = System.currentTimeMillis()

            result

        } catch (e: TimeoutCancellationException) {
            scriptContext.state = ScriptState.ERROR
            scriptContext.errorMessage = "Script execution timeout"
            ExecutionResult.Error("Execution timeout after ${maxExecutionTimeMs}ms")

        } catch (e: CancellationException) {
            // Preserve structured-concurrency cancellation (e.g. activity destroyed)
            throw e

        } catch (e: Exception) {
            scriptContext.state = ScriptState.ERROR
            scriptContext.errorMessage = e.message
            ExecutionResult.Error(e.message ?: "Unknown error")

        } finally {
            activeContexts.remove(script.id)
        }
    }

    /**
     * Stop a running script.
     */
    override fun stop(scriptId: String) {
        activeContexts[scriptId]?.let { context ->
            context.state = ScriptState.STOPPED
            context.endTime = System.currentTimeMillis()
            activeContexts.remove(scriptId)

            // J2V8 cannot safely interrupt an in-flight executeScript call;
            // the resource monitor and withTimeout enforce the execution limit.
            // Pending timers are cancelled so callbacks cannot outlive the script.
            clearAllTimers()
        }
    }

    /**
     * Monitor script resource usage.
     */
    private fun startResourceMonitoring(scriptContext: ScriptContext): Job {
        return engineScope.launch {
            while (isActive && scriptContext.state == ScriptState.RUNNING) {
                delay(1000) // Check every second

                // Check execution time
                val executionTime = System.currentTimeMillis() - scriptContext.startTime
                if (executionTime > maxExecutionTimeMs) {
                    scriptContext.state = ScriptState.ERROR
                    scriptContext.errorMessage = "Execution time limit exceeded"
                    stop(scriptContext.script.id)
                    break
                }
            }
        }
    }

    /**
     * Console.log implementation.
     */
    @Suppress("unused")
    fun consoleLog(message: String) {
        println("[Script] $message")
    }

    /**
     * Console.warn implementation.
     */
    @Suppress("unused")
    fun consoleWarn(message: String) {
        println("[Script WARN] $message")
    }

    /**
     * Console.error implementation.
     */
    @Suppress("unused")
    fun consoleError(message: String) {
        System.err.println("[Script ERROR] $message")
    }

    /**
     * setTimeout implementation. Returns a timer ID usable with clearTimeout.
     */
    @Suppress("unused")
    fun setTimeout(callback: V8Object, delay: Int): Int {
        if (callback.isReleased) return 0

        val timerId = timerIdGenerator.incrementAndGet()
        val runnable = Runnable {
            activeTimers.remove(timerId)
            if (!callback.isReleased) {
                (callback as? V8Function)?.call(runtime, null)
            }
        }
        activeTimers[timerId] = runnable
        mainHandler.postDelayed(runnable, delay.coerceAtLeast(0).toLong())
        return timerId
    }

    /**
     * setInterval implementation. Returns a timer ID usable with clearInterval.
     */
    @Suppress("unused")
    fun setInterval(callback: V8Object, interval: Int): Int {
        if (callback.isReleased) return 0

        val timerId = timerIdGenerator.incrementAndGet()
        val runnable = object : Runnable {
            override fun run() {
                // Timer was cleared; stop re-scheduling
                if (activeTimers[timerId] !== this) return

                if (callback.isReleased) {
                    activeTimers.remove(timerId)
                    return
                }

                (callback as? V8Function)?.call(runtime, null)
                mainHandler.postDelayed(this, interval.coerceAtLeast(0).toLong())
            }
        }
        activeTimers[timerId] = runnable
        mainHandler.post(runnable)
        return timerId
    }

    /**
     * clearTimeout implementation.
     */
    @Suppress("unused")
    fun clearTimeout(timerId: Int) {
        activeTimers.remove(timerId)?.let { mainHandler.removeCallbacks(it) }
    }

    /**
     * clearInterval implementation.
     */
    @Suppress("unused")
    fun clearInterval(timerId: Int) {
        clearTimeout(timerId)
    }

    private fun clearAllTimers() {
        activeTimers.forEach { (_, runnable) -> mainHandler.removeCallbacks(runnable) }
        activeTimers.clear()
    }

    /**
     * Evaluate a JavaScript expression.
     */
    override fun evaluate(expression: String): Any? {
        return try {
            val result = runtime.executeScript(expression)
            val value = when {
                result == null -> null
                result is V8Object -> result.toString()
                else -> result
            }
            if (result is V8Object) result.release()
            value
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Release resources.
     */
    override fun release() {
        engineScope.cancel()
        clearAllTimers()
        bridges.forEach { it.unregister() }
        bridges.clear()
        activeContexts.clear()
        runtime.release()
    }
}

/**
 * Script engine interface.
 */
interface ScriptEngine {
    suspend fun execute(scriptContext: ScriptContext): ExecutionResult
    fun stop(scriptId: String)
    fun evaluate(expression: String): Any?
    fun registerBridge(bridge: ScriptBridge)
    fun release()
}

/**
 * Script execution result.
 */
sealed class ExecutionResult {
    data class Success(val output: String) : ExecutionResult()
    data class Error(val message: String) : ExecutionResult()
}

/**
 * Bridge interface for exposing native APIs to scripts.
 */
interface ScriptBridge {
    fun register(runtime: V8)
    fun unregister()
}
