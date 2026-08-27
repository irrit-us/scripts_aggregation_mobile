package com.scripthost.engine

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import com.eclipsesource.v8.V8
import com.eclipsesource.v8.V8Function
import com.eclipsesource.v8.V8Object
import com.scripthost.config.AppSettings
import com.scripthost.models.ScriptContext
import com.scripthost.models.ScriptState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * JavaScript engine implementation using J2V8.
 * Provides sandboxed script execution with resource monitoring.
 *
 * Threading model — J2V8 runtimes are not thread-safe and enforce that every
 * V8/V8Value touch happens on ONE thread. This engine owns a dedicated
 * [HandlerThread] ("v8-engine") and confines ALL of it there: runtime
 * creation, bridge registration, `executeScript`, timer dispatch, and
 * teardown. Consequences:
 *
 *  - The main thread never blocks on script execution. [execute] posts the
 *    blocking native call to the engine thread and suspends until it
 *    completes, so a hung script (`while(true){}`) cannot ANR the app.
 *  - JS→Java bridge methods are invoked ON the engine thread; bridges
 *    marshal view mutations to the main thread (see UIBridge.onUiThread) and
 *    Java→JS callbacks (view events, sensors, HTTP/SSH completions, dialogs)
 *    back onto the engine thread.
 *  - Timer callbacks fire when the engine thread is free — i.e. after the
 *    currently running script yields — matching JS run-to-completion
 *    semantics.
 *
 * Interruption: `V8.terminateExecution()` (J2V8 6.2.1) is thread-safe and is
 * THE way to interrupt `executeScript`; [stop] and the 30-second watchdog use
 * it. Known J2V8 limitation: a script stuck in a NON-interruptible native
 * operation still keeps the engine thread busy; teardown is queued and the UI
 * stays responsive, but the thread leaks until the process dies. A hard
 * interrupt is impossible without killing the process.
 */
class JavaScriptEngine(private val context: Context) : ScriptEngine {

    /** Dedicated V8 thread; every V8/V8Value access runs on its Looper. */
    private val engineThread = HandlerThread("v8-engine").apply { start() }
    private val engineHandler = Handler(engineThread.looper)
    private val engineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /** Created on the engine thread (J2V8 thread affinity). */
    private lateinit var runtime: V8
    private val released = AtomicBoolean(false)

    // Resource limits: execution watchdog timeout from app settings
    // (Settings -> app section; default 30 seconds)
    private val maxExecutionTimeMs =
        AppSettings(context).engineTimeoutSeconds.coerceAtLeast(1) * 1000L

    // Active script contexts
    private val activeContexts = ConcurrentHashMap<String, ScriptContext>()

    // Bridge registry (written on the caller's thread, read on the engine thread)
    private val bridges = CopyOnWriteArrayList<ScriptBridge>()

    // Timer bookkeeping so scripts can clear timers and the engine can clean up
    private val timerIdGenerator = AtomicInteger(0)
    private val activeTimers = ConcurrentHashMap<Int, Runnable>()
    private val activeTimerCallbacks = ConcurrentHashMap<Int, V8Object>()

    // Debug mode (Settings -> App -> Debug mode): when enabled, script console
    // messages are mirrored to Logcat. Read once at engine setup; toggling it
    // applies to the next script run.
    private val debugConsole = AppSettings(context).debugMode

    init {
        engineHandler.post {
            runtime = V8.createV8Runtime()
            setupGlobalEnvironment()
        }
    }

    /**
     * Setup global JavaScript environment with console and timer APIs.
     * Runs on the engine thread (first queued task, so [runtime] is
     * initialized before any later task observes it).
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
     * Register a bridge to expose native functionality. The actual
     * registration is marshaled to the engine thread.
     */
    override fun registerBridge(bridge: ScriptBridge) {
        bridges.add(bridge)
        engineHandler.post {
            if (!released.get() && ::runtime.isInitialized) {
                bridge.register(runtime)
            }
        }
    }

    /**
     * Execute a script with sandboxing and monitoring.
     *
     * The blocking native `executeScript` runs on the engine thread; this
     * coroutine merely awaits the outcome, so [withTimeout] genuinely fires
     * and the calling thread (typically main) stays responsive. On timeout,
     * [terminateExecution] interrupts the native call.
     */
    override suspend fun execute(scriptContext: ScriptContext): ExecutionResult {
        val script = scriptContext.script

        scriptContext.state = ScriptState.RUNNING
        scriptContext.startTime = System.currentTimeMillis()
        activeContexts[script.id] = scriptContext

        if (released.get()) {
            activeContexts.remove(script.id)
            return ExecutionResult.Error("Engine already released")
        }

        // Start resource monitoring
        val monitorJob = startResourceMonitoring(scriptContext)

        val deferred = CompletableDeferred<ExecutionResult>()
        engineHandler.post {
            try {
                val output = runtime.executeScript(script.sourceCode, script.name, 0)
                val resultString = output?.toString() ?: "undefined"
                (output as? V8Object)?.release()
                deferred.complete(ExecutionResult.Success(resultString))
            } catch (e: Exception) {
                deferred.complete(ExecutionResult.Error(e.message ?: "Unknown execution error"))
            }
        }

        return try {
            val result = withTimeout(maxExecutionTimeMs) { deferred.await() }

            scriptContext.state = ScriptState.STOPPED
            scriptContext.endTime = System.currentTimeMillis()
            result

        } catch (e: TimeoutCancellationException) {
            // Cooperative cancellation cannot interrupt a blocking native
            // call; terminateExecution makes executeScript throw on the
            // engine thread so the engine can be reused/torn down.
            terminateExecution()
            scriptContext.state = ScriptState.ERROR
            scriptContext.errorMessage = "Script execution timeout"
            ExecutionResult.Error("Execution timeout after ${maxExecutionTimeMs}ms")

        } finally {
            monitorJob.cancel()
            activeContexts.remove(script.id)
        }
    }

    /**
     * Stop a running script. Cooperative: timers are cancelled and an
     * in-flight `executeScript` is interrupted via [terminateExecution].
     * Safe to call from any thread.
     */
    override fun stop(scriptId: String) {
        activeContexts[scriptId]?.let { context ->
            context.state = ScriptState.STOPPED
            context.endTime = System.currentTimeMillis()
            activeContexts.remove(scriptId)

            // Pending timers are cancelled so callbacks cannot outlive the script.
            clearAllTimers()
            terminateExecution()
        }
    }

    /**
     * Interrupt an in-flight `executeScript`. `V8.terminateExecution()` is
     * explicitly thread-safe (it is V8's interrupt mechanism and performs no
     * thread-affinity check), so this can be called from any thread. When no
     * script is executing the request is consumed by the next execution —
     * which is fine here because [stop]/timeout always precede teardown.
     */
    private fun terminateExecution() {
        if (!::runtime.isInitialized || runtime.isReleased) return
        try {
            runtime.terminateExecution()
        } catch (e: Exception) {
            // Runtime already released or terminated; nothing to interrupt
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
        if (debugConsole) {
            println("[Script] $message")
            Log.d(TAG_SCRIPT_CONSOLE, message)
        }
    }

    /**
     * Console.warn implementation.
     */
    @Suppress("unused")
    fun consoleWarn(message: String) {
        if (debugConsole) {
            println("[Script WARN] $message")
            Log.d(TAG_SCRIPT_CONSOLE, "[WARN] $message")
        }
    }

    /**
     * Console.error implementation.
     */
    @Suppress("unused")
    fun consoleError(message: String) {
        if (debugConsole) {
            System.err.println("[Script ERROR] $message")
            Log.d(TAG_SCRIPT_CONSOLE, "[ERROR] $message")
        }
    }

    /**
     * setTimeout implementation. Returns a timer ID usable with clearTimeout.
     * Runs on the engine thread (JS call); the callback is dispatched on the
     * engine thread once the current execution yields.
     */
    @Suppress("unused")
    fun setTimeout(callback: V8Object, delay: Int): Int {
        if (callback.isReleased) return 0

        // J2V8 releases parameter handles when this method returns; retain a
        // twin so the callback survives until the timer fires or is cleared.
        val retained = callback.twin()
        val timerId = timerIdGenerator.incrementAndGet()
        val runnable = Runnable {
            activeTimers.remove(timerId)
            activeTimerCallbacks.remove(timerId)
            try {
                (retained as? V8Function)?.call(runtime, null)
            } finally {
                // One-shot: release the twin right after it fired
                if (!retained.isReleased) retained.release()
            }
        }
        activeTimers[timerId] = runnable
        activeTimerCallbacks[timerId] = retained
        engineHandler.postDelayed(runnable, delay.coerceAtLeast(0).toLong())
        return timerId
    }

    /**
     * setInterval implementation. Returns a timer ID usable with clearInterval.
     */
    @Suppress("unused")
    fun setInterval(callback: V8Object, interval: Int): Int {
        if (callback.isReleased) return 0

        // Retained twin; released when the interval is cleared or the engine
        // is released — NOT after individual fires.
        val retained = callback.twin()
        val timerId = timerIdGenerator.incrementAndGet()
        val runnable = object : Runnable {
            override fun run() {
                // Timer was cleared; stop re-scheduling
                if (activeTimers[timerId] !== this) return

                (retained as? V8Function)?.call(runtime, null)
                engineHandler.postDelayed(this, interval.coerceAtLeast(0).toLong())
            }
        }
        activeTimers[timerId] = runnable
        activeTimerCallbacks[timerId] = retained
        engineHandler.post(runnable)
        return timerId
    }

    /**
     * clearTimeout implementation.
     */
    @Suppress("unused")
    fun clearTimeout(timerId: Int) {
        activeTimers.remove(timerId)?.let { engineHandler.removeCallbacks(it) }
        activeTimerCallbacks.remove(timerId)?.let { if (!it.isReleased) it.release() }
    }

    /**
     * clearInterval implementation.
     */
    @Suppress("unused")
    fun clearInterval(timerId: Int) {
        clearTimeout(timerId)
    }

    private fun clearAllTimers() {
        activeTimers.forEach { (_, runnable) -> engineHandler.removeCallbacks(runnable) }
        activeTimers.clear()
        val callbacks = activeTimerCallbacks.values.toList()
        activeTimerCallbacks.clear()
        if (callbacks.isEmpty()) return
        // V8 handles may only be released on the engine thread; stop() can
        // arrive from the resource-monitor coroutine on a background thread.
        val releaseAll = Runnable {
            callbacks.forEach { if (!it.isReleased) it.release() }
        }
        if (Looper.myLooper() == engineThread.looper) {
            releaseAll.run()
        } else {
            engineHandler.post(releaseAll)
        }
    }

    /**
     * Release resources. All V8 teardown is marshaled to the engine thread
     * and this method never blocks waiting for it: a script stuck in a
     * non-interruptible native operation keeps the thread busy, and blocking
     * the main thread here would recreate the ANR this design removes.
     */
    override fun release() {
        if (!released.compareAndSet(false, true)) return
        engineScope.cancel()
        clearAllTimers()
        terminateExecution() // unblock a hung script so teardown can run
        engineHandler.post {
            bridges.forEach { it.unregister() }
            bridges.clear()
            activeContexts.clear()
            if (::runtime.isInitialized && !runtime.isReleased) {
                runtime.release()
            }
            engineThread.quitSafely()
        }
    }

    companion object {
        private const val TAG_SCRIPT_CONSOLE = "ScriptConsole"
    }
}

/**
 * Script engine interface.
 */
interface ScriptEngine {
    suspend fun execute(scriptContext: ScriptContext): ExecutionResult
    fun stop(scriptId: String)
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
 * Bridge interface for exposing native APIs to scripts. [register] and
 * [unregister] are invoked on the engine's V8 thread.
 */
interface ScriptBridge {
    fun register(runtime: V8)
    fun unregister()
}
