package com.scripthost.engine

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.eclipsesource.v8.V8
import com.eclipsesource.v8.V8Array
import com.eclipsesource.v8.V8Object
import com.scripthost.models.Script
import com.scripthost.models.ScriptContext
import com.scripthost.models.ScriptState
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * JavaScript engine implementation using J2V8
 * Provides sandboxed script execution with resource monitoring
 */
class JavaScriptEngine(private val context: Context) : ScriptEngine {

    private val runtime: V8 = V8.createV8Runtime()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val engineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Resource limits
    private val maxExecutionTimeMs = 30_000L // 30 seconds
    private val maxMemoryBytes = 50 * 1024 * 1024L // 50 MB

    // Active script contexts
    private val activeContexts = ConcurrentHashMap<String, ScriptContext>()

    // Bridge registry
    private val bridges = mutableListOf<ScriptBridge>()

    init {
        setupGlobalEnvironment()
    }

    /**
     * Setup global JavaScript environment with console and basic APIs
     */
    private fun setupGlobalEnvironment() {
        // Console API
        val console = V8Object(runtime)
        runtime.add("console", console)

        console.registerJavaMethod(this, "consoleLog", "log", arrayOf(String::class.java))
        console.registerJavaMethod(this, "consoleWarn", "warn", arrayOf(String::class.java))
        console.registerJavaMethod(this, "consoleError", "error", arrayOf(String::class.java))

        console.release()

        // setTimeout/setInterval (simplified)
        runtime.registerJavaMethod(this, "setTimeout", "setTimeout",
            arrayOf(V8Object::class.java, Int::class.java))
        runtime.registerJavaMethod(this, "setInterval", "setInterval",
            arrayOf(V8Object::class.java, Int::class.java))
    }

    /**
     * Register a bridge to expose native functionality
     */
    override fun registerBridge(bridge: ScriptBridge) {
        bridges.add(bridge)
        bridge.register(runtime)
    }

    /**
     * Execute a script with sandboxing and monitoring
     */
    override suspend fun execute(scriptContext: ScriptContext): ExecutionResult = withContext(Dispatchers.Default) {
        val script = scriptContext.script

        try {
            // Update state
            scriptContext.state = ScriptState.RUNNING
            activeContexts[script.id] = scriptContext

            // Start resource monitoring
            val monitorJob = startResourceMonitoring(scriptContext)

            // Execute with timeout
            val result = withTimeout(maxExecutionTimeMs) {
                try {
                    val output = runtime.executeScript(script.sourceCode, script.name, 0)
                    val resultString = output?.toString() ?: "undefined"
                    output?.release()
                    ExecutionResult.Success(resultString)
                } catch (e: Exception) {
                    ExecutionResult.Error(e.message ?: "Unknown execution error")
                }
            }

            // Stop monitoring
            monitorJob.cancel()

            // Update state
            scriptContext.state = ScriptState.STOPPED
            scriptContext.endTime = System.currentTimeMillis()

            result

        } catch (e: TimeoutCancellationException) {
            scriptContext.state = ScriptState.ERROR
            scriptContext.errorMessage = "Script execution timeout"
            ExecutionResult.Error("Execution timeout after ${maxExecutionTimeMs}ms")

        } catch (e: Exception) {
            scriptContext.state = ScriptState.ERROR
            scriptContext.errorMessage = e.message
            ExecutionResult.Error(e.message ?: "Unknown error")

        } finally {
            activeContexts.remove(script.id)
        }
    }

    /**
     * Stop a running script
     */
    override fun stop(scriptId: String) {
        activeContexts[scriptId]?.let { context ->
            context.state = ScriptState.STOPPED
            context.endTime = System.currentTimeMillis()
            activeContexts.remove(scriptId)

            // Terminate V8 execution (requires creating new runtime)
            // For production, implement proper script isolation per context
        }
    }

    /**
     * Monitor script resource usage
     */
    private fun startResourceMonitoring(scriptContext: ScriptContext): Job {
        return engineScope.launch {
            while (isActive && scriptContext.state == ScriptState.RUNNING) {
                delay(1000) // Check every second

                // Check memory usage
                val heapStatistics = runtime.getHeapStatistics()
                val usedMemory = heapStatistics.usedHeapSize()
                heapStatistics.release()

                if (usedMemory > maxMemoryBytes) {
                    scriptContext.state = ScriptState.ERROR
                    scriptContext.errorMessage = "Memory limit exceeded"
                    stop(scriptContext.script.id)
                    break
                }

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
     * Console.log implementation
     */
    @Suppress("unused")
    fun consoleLog(message: String) {
        println("[Script] $message")
    }

    /**
     * Console.warn implementation
     */
    @Suppress("unused")
    fun consoleWarn(message: String) {
        println("[Script WARN] $message")
    }

    /**
     * Console.error implementation
     */
    @Suppress("unused")
    fun consoleError(message: String) {
        System.err.println("[Script ERROR] $message")
    }

    /**
     * setTimeout implementation
     */
    @Suppress("unused")
    fun setTimeout(callback: V8Object, delay: Int): Int {
        mainHandler.postDelayed({
            if (!callback.isReleased) {
                callback.executeVoidFunction()
            }
        }, delay.toLong())
        return 0 // Return timer ID (simplified)
    }

    /**
     * setInterval implementation
     */
    @Suppress("unused")
    fun setInterval(callback: V8Object, interval: Int): Int {
        // Simplified implementation
        mainHandler.post(object : Runnable {
            override fun run() {
                if (!callback.isReleased) {
                    callback.executeVoidFunction()
                    mainHandler.postDelayed(this, interval.toLong())
                }
            }
        })
        return 0 // Return timer ID (simplified)
    }

    /**
     * Evaluate JavaScript expression
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
     * Release resources
     */
    override fun release() {
        engineScope.cancel()
        bridges.forEach { it.unregister() }
        bridges.clear()
        activeContexts.clear()
        runtime.release()
    }
}

/**
 * Script engine interface
 */
interface ScriptEngine {
    suspend fun execute(scriptContext: ScriptContext): ExecutionResult
    fun stop(scriptId: String)
    fun evaluate(expression: String): Any?
    fun registerBridge(bridge: ScriptBridge)
    fun release()
}

/**
 * Script execution result
 */
sealed class ExecutionResult {
    data class Success(val output: String) : ExecutionResult()
    data class Error(val message: String) : ExecutionResult()
}

/**
 * Bridge interface for exposing native APIs to scripts
 */
interface ScriptBridge {
    fun register(runtime: V8)
    fun unregister()
}
