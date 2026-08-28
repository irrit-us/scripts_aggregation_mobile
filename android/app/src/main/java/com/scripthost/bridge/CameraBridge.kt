package com.scripthost.bridge

import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import com.eclipsesource.v8.V8
import com.eclipsesource.v8.V8Array
import com.eclipsesource.v8.V8Function
import com.eclipsesource.v8.V8Object
import com.scripthost.engine.ScriptBridge
import com.scripthost.models.Permission
import com.scripthost.security.PermissionManager
import com.scripthost.ui.CameraCaptureController

/**
 * Camera Bridge - Exposes photo capture to scripts.
 *
 * Requires the CAMERA script permission. Capture runs through
 * [CameraCaptureController] (system camera app); the one-shot JS callback is
 * retained as a twin and invoked on the engine's V8 thread (J2V8 runtimes
 * are not thread-safe).
 */
class CameraBridge(
    private val context: Context,
    private val permissionManager: PermissionManager,
    private val scriptId: String,
    private val captureController: CameraCaptureController
) : ScriptBridge {

    private var runtime: V8? = null

    /**
     * Handler of the engine's V8 thread, captured in [register] (which the
     * engine invokes on that thread). The capture result arrives on the main
     * thread and is re-marshaled onto it.
     */
    private var engineHandler: Handler? = null

    /** Retained callback twin for the capture in flight, if any. */
    private var pendingCallback: V8Function? = null

    override fun register(runtime: V8) {
        this.runtime = runtime
        // register() is invoked on the engine's V8 thread
        engineHandler = Handler(Looper.myLooper() ?: Looper.getMainLooper())

        val cameraObject = V8Object(runtime)
        runtime.add("Camera", cameraObject)
        cameraObject.registerJavaMethod(this, "isAvailable", "isAvailable", emptyArray())
        cameraObject.registerJavaMethod(this, "takePhoto", "takePhoto",
            arrayOf(V8Function::class.java))
        cameraObject.release()
    }

    override fun unregister() {
        pendingCallback?.let { if (!it.isReleased) it.release() }
        pendingCallback = null
        runtime = null
    }

    /**
     * Whether the device has any camera.
     */
    @Suppress("unused")
    fun isAvailable(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    /**
     * Take a photo; the callback receives (base64, error) — exactly one of
     * the two is null.
     */
    @Suppress("unused")
    fun takePhoto(callback: V8Function) {
        if (!permissionManager.hasScriptPermission(scriptId, Permission.CAMERA)) {
            // The parameter handle is owned by J2V8; invoke it but do not release it
            deliverResult(callback, null, "Permission denied: CAMERA", releaseAfter = false)
            return
        }

        // J2V8 releases parameter handles when this method returns; retain a
        // twin until the capture completes.
        val retained = callback.twin()
        pendingCallback = retained
        captureController.takePhoto { base64, error ->
            // Capture completes on the main thread; V8 must only be touched
            // from the engine's V8 thread.
            onEngineThread {
                pendingCallback = null
                deliverResult(retained, base64, error, releaseAfter = true)
            }
        }
    }

    private fun deliverResult(
        callback: V8Function,
        base64: String?,
        error: String?,
        releaseAfter: Boolean
    ) {
        val runtime = this.runtime ?: return
        if (callback.isReleased) return
        val params = V8Array(runtime)
        if (error != null) {
            params.pushNull() // base64
            params.push(error) // error
        } else {
            params.push(base64 ?: "") // base64
            params.pushNull() // error
        }
        callback.call(runtime, params)
        params.release()
        if (releaseAfter && !callback.isReleased) callback.release()
    }

    /**
     * Run [block] on the engine's V8 thread. This is a no-op when already on
     * it. See SystemBridge.onEngineThread.
     */
    private fun onEngineThread(block: () -> Unit) {
        val handler = engineHandler
        if (handler == null || Looper.myLooper() == handler.looper) {
            block()
        } else {
            handler.post(block)
        }
    }
}
