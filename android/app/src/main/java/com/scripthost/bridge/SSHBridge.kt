package com.scripthost.bridge

import android.os.Handler
import android.os.Looper
import com.eclipsesource.v8.JavaCallback
import com.eclipsesource.v8.V8
import com.eclipsesource.v8.V8Array
import com.eclipsesource.v8.V8Function
import com.eclipsesource.v8.V8Object
import com.scripthost.engine.ScriptBridge
import com.scripthost.models.Permission
import com.scripthost.security.PermissionManager
import com.scripthost.ssh.SSHSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * SSH Bridge - Exposes SSH connections to scripts (e.g. remote tmux control).
 *
 * Like the other bridges, all V8 access happens on the main thread while
 * blocking SSH I/O runs on the IO dispatcher. At most one session is kept;
 * a new `SSH.connect` replaces the previous one.
 */
class SSHBridge(
    private val permissionManager: PermissionManager,
    private val scriptId: String
) : ScriptBridge {

    private var runtime: V8? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val sessionManager = SSHSessionManager()

    /**
     * Handler of the engine's V8 thread, captured in [register] (which the
     * engine invokes on that thread). Async connect/exec completions are
     * re-marshaled onto it.
     */
    private var engineHandler: Handler? = null

    override fun register(runtime: V8) {
        this.runtime = runtime
        // register() is invoked on the engine's V8 thread
        engineHandler = Handler(Looper.myLooper() ?: Looper.getMainLooper())

        // SSH API
        val sshObject = V8Object(runtime)
        runtime.add("SSH", sshObject)
        // connect(host, port, username, password, callback)
        sshObject.registerJavaMethod(JavaCallback { _, parameters ->
            dispatchConnect(parameters)
            null
        }, "connect")
        // exec(command, callback)
        sshObject.registerJavaMethod(JavaCallback { _, parameters ->
            dispatchExec(parameters)
            null
        }, "exec")
        // disconnect()
        sshObject.registerJavaMethod(JavaCallback { _, _ ->
            disconnect()
            null
        }, "disconnect")
        sshObject.release()
    }

    override fun unregister() {
        scope.cancel()
        sessionManager.disconnect()
        runtime = null
    }

    // SSH API

    /**
     * Dispatch an `SSH.connect` call:
     * `connect(host, port, username, password, callback)`.
     */
    private fun dispatchConnect(parameters: V8Array) {
        try {
            if (parameters.length() < 5) {
                invokeCallback(parameters, "SSH.connect requires host, port, username, password, and a callback")
                return
            }
            val callback = extractCallback(parameters)
            if (callback == null) {
                invokeCallback(parameters, "SSH.connect requires a callback function")
                return
            }
            connect(
                parameters.getString(0),
                parameters.getInteger(1),
                parameters.getString(2),
                parameters.getString(3),
                callback
            )
        } catch (e: Exception) {
            invokeCallback(parameters, "Invalid SSH.connect arguments: ${e.message}")
        }
    }

    /**
     * Dispatch an `SSH.exec` call: `exec(command, callback)`.
     */
    private fun dispatchExec(parameters: V8Array) {
        try {
            if (parameters.length() < 2) {
                invokeCallback(parameters, "SSH.exec requires a command and a callback")
                return
            }
            val callback = extractCallback(parameters)
            if (callback == null) {
                invokeCallback(parameters, "SSH.exec requires a callback function")
                return
            }
            exec(parameters.getString(0), callback)
        } catch (e: Exception) {
            invokeCallback(parameters, "Invalid SSH.exec arguments: ${e.message}")
        }
    }

    /**
     * Open an SSH session. The callback receives a single argument:
     * null on success, or the error message on failure.
     */
    fun connect(host: String, port: Int, username: String, password: String, callback: V8Function) {
        if (!permissionManager.hasScriptPermission(scriptId, Permission.SSH)) {
            invokeError(callback, "Permission denied: SSH")
            return
        }

        scope.launch {
            try {
                sessionManager.connect(host, port, username, password)
                onEngineThread {
                    invokeError(callback, null)
                }
            } catch (e: Exception) {
                onEngineThread {
                    invokeError(callback, e.message ?: "SSH connect failed")
                }
            }
        }
    }

    /**
     * Run a command on the active session. The callback receives
     * (output, error) following the usual two-argument convention.
     */
    fun exec(command: String, callback: V8Function) {
        if (!permissionManager.hasScriptPermission(scriptId, Permission.SSH)) {
            invokeCallback(callback, null, "Permission denied: SSH")
            return
        }

        scope.launch {
            try {
                val output = sessionManager.exec(command)
                onEngineThread {
                    invokeCallback(callback, output, null)
                }
            } catch (e: Exception) {
                onEngineThread {
                    invokeCallback(callback, null, e.message ?: "SSH exec failed")
                }
            }
        }
    }

    /**
     * Close the active session, if any. Safe to call when not connected.
     */
    fun disconnect() {
        sessionManager.disconnect()
    }

    // Helper methods

    /**
     * Run [block] on the engine's V8 thread. This is a no-op when already on
     * it. Async SSH completions arrive off it and MUST be re-marshaled:
     * J2V8 throws on foreign-thread access.
     */
    private fun onEngineThread(block: () -> Unit) {
        val handler = engineHandler
        if (handler == null || Looper.myLooper() == handler.looper) {
            block()
        } else {
            handler.post(block)
        }
    }

    /**
     * Extract the trailing V8Function argument, or null when missing.
     */
    private fun extractCallback(parameters: V8Array): V8Function? {
        val last = parameters.length() - 1
        if (last < 0) return null
        return parameters.getObject(last) as? V8Function
    }

    /**
     * Report an argument error through the callback that was passed in.
     */
    private fun invokeCallback(parameters: V8Array, error: String) {
        val callback = extractCallback(parameters) ?: return
        invokeCallback(callback, null, error)
    }

    private fun invokeCallback(callback: V8Function, data: String?, error: String?) {
        val runtime = this.runtime ?: return
        val params = V8Array(runtime)

        if (error != null) {
            params.pushNull() // data
            params.push(error) // error
        } else {
            params.push(data ?: "") // data
            params.pushNull() // error
        }

        callback.call(runtime, params)
        params.release()
        // One-shot async callback: release the handle obtained by extractCallback
        callback.release()
    }

    /**
     * Invoke a single-argument (error) callback, as used by `SSH.connect`.
     */
    private fun invokeError(callback: V8Function, error: String?) {
        val runtime = this.runtime ?: return
        val params = V8Array(runtime)

        if (error != null) {
            params.push(error)
        } else {
            params.pushNull()
        }

        callback.call(runtime, params)
        params.release()
        // One-shot async callback: release the handle obtained by extractCallback
        callback.release()
    }
}
