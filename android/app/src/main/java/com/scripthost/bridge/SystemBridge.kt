package com.scripthost.bridge

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.eclipsesource.v8.JavaCallback
import com.eclipsesource.v8.V8
import com.eclipsesource.v8.V8Array
import com.eclipsesource.v8.V8Function
import com.eclipsesource.v8.V8Object
import com.scripthost.engine.ScriptBridge
import com.scripthost.models.Permission
import com.scripthost.security.PermissionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * System Bridge - Exposes system functionality to scripts
 * Includes network, storage, sensors, and device APIs.
 *
 * J2V8 runtimes are not thread-safe. All V8 access (callback invocation,
 * V8Object/V8Array creation) happens on the main thread; network work runs on
 * the IO dispatcher and sensor events are marshaled through the main handler.
 */
class SystemBridge(
    private val context: Context,
    private val permissionManager: PermissionManager,
    private val scriptId: String
) : ScriptBridge {

    private var runtime: V8? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    override fun register(runtime: V8) {
        this.runtime = runtime

        // Network API
        val networkObject = V8Object(runtime)
        runtime.add("Network", networkObject)
        // Supports get(url, callback) and get(url, headers, callback)
        networkObject.registerJavaMethod(JavaCallback { _, parameters ->
            dispatchHttpGet(parameters)
            null
        }, "get")
        // Supports post(url, body, callback) and post(url, headers, body, callback)
        networkObject.registerJavaMethod(JavaCallback { _, parameters ->
            dispatchHttpPost(parameters)
            null
        }, "post")
        networkObject.release()

        // Storage API
        val storageObject = V8Object(runtime)
        runtime.add("Storage", storageObject)
        storageObject.registerJavaMethod(this, "readFile", "readFile",
            arrayOf(String::class.java))
        storageObject.registerJavaMethod(this, "writeFile", "writeFile",
            arrayOf(String::class.java, String::class.java))
        storageObject.registerJavaMethod(this, "deleteFile", "deleteFile",
            arrayOf(String::class.java))
        storageObject.registerJavaMethod(this, "listFiles", "listFiles",
            arrayOf(String::class.java))
        storageObject.release()

        // Sensor API
        val sensorObject = V8Object(runtime)
        runtime.add("Sensor", sensorObject)
        sensorObject.registerJavaMethod(this, "getAccelerometer", "getAccelerometer",
            arrayOf(V8Function::class.java))
        sensorObject.registerJavaMethod(this, "getGyroscope", "getGyroscope",
            arrayOf(V8Function::class.java))
        sensorObject.registerJavaMethod(this, "stopSensor", "stop", emptyArray())
        sensorObject.release()

        // Device API
        val deviceObject = V8Object(runtime)
        runtime.add("Device", deviceObject)
        deviceObject.registerJavaMethod(this, "vibrate", "vibrate",
            arrayOf(Int::class.java))
        deviceObject.registerJavaMethod(this, "getDeviceInfo", "getInfo", emptyArray())
        deviceObject.release()
    }

    override fun unregister() {
        scope.cancel()
        stopSensor()
        runtime = null
    }

    // Network API

    /**
     * Dispatch a `Network.get` call based on the number of JS arguments:
     * `get(url, callback)` or `get(url, headers, callback)`.
     */
    private fun dispatchHttpGet(parameters: V8Array) {
        try {
            if (parameters.length() < 2) {
                invokeCallback(parameters, "Network.get requires a URL and a callback")
                return
            }
            val callback = extractCallback(parameters)
            if (callback == null) {
                invokeCallback(parameters, "Network.get requires a callback function")
                return
            }
            val headers = if (parameters.length() >= 3) {
                readHeaders(parameters.getObject(1))
            } else {
                null
            }
            httpGet(parameters.getString(0), headers, callback)
        } catch (e: Exception) {
            invokeCallback(parameters, "Invalid Network.get arguments: ${e.message}")
        }
    }

    /**
     * Dispatch a `Network.post` call based on the number of JS arguments:
     * `post(url, body, callback)` or `post(url, headers, body, callback)`.
     */
    private fun dispatchHttpPost(parameters: V8Array) {
        try {
            if (parameters.length() < 3) {
                invokeCallback(parameters, "Network.post requires a URL, a body, and a callback")
                return
            }
            val callback = extractCallback(parameters)
            if (callback == null) {
                invokeCallback(parameters, "Network.post requires a callback function")
                return
            }
            val hasHeaders = parameters.length() >= 4
            val headers = if (hasHeaders) {
                readHeaders(parameters.getObject(1))
            } else {
                null
            }
            val body = if (hasHeaders) {
                parameters.getString(2)
            } else {
                parameters.getString(1)
            }
            httpPost(parameters.getString(0), headers, body, callback)
        } catch (e: Exception) {
            invokeCallback(parameters, "Invalid Network.post arguments: ${e.message}")
        }
    }

    /**
     * HTTP GET request with optional headers (e.g. Authorization).
     */
    fun httpGet(url: String, headers: Map<String, String>?, callback: V8Function) {
        if (!permissionManager.hasScriptPermission(scriptId, Permission.INTERNET)) {
            invokeCallback(callback, null, "Permission denied: INTERNET")
            return
        }

        scope.launch {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                headers?.forEach { (name, value) -> connection.setRequestProperty(name, value) }

                val responseCode = connection.responseCode
                val response = if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    null
                }

                connection.disconnect()

                withContext(Dispatchers.Main) {
                    invokeCallback(callback, response, if (response == null) "HTTP $responseCode" else null)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    invokeCallback(callback, null, e.message ?: "Network error")
                }
            }
        }
    }

    /**
     * HTTP POST request with optional headers (e.g. Authorization).
     */
    fun httpPost(url: String, headers: Map<String, String>?, body: String, callback: V8Function) {
        if (!permissionManager.hasScriptPermission(scriptId, Permission.INTERNET)) {
            invokeCallback(callback, null, "Permission denied: INTERNET")
            return
        }

        scope.launch {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                headers?.forEach { (name, value) -> connection.setRequestProperty(name, value) }
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                connection.outputStream.use { it.write(body.toByteArray()) }

                val responseCode = connection.responseCode
                val response = if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    null
                }

                connection.disconnect()

                withContext(Dispatchers.Main) {
                    invokeCallback(callback, response, if (response == null) "HTTP $responseCode" else null)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    invokeCallback(callback, null, e.message ?: "Network error")
                }
            }
        }
    }

    // Storage API

    /**
     * Resolve [name] against the script-dedicated storage subdirectory of the
     * app's private files directory, rejecting any path that escapes it (e.g.
     * `../foo` or absolute paths). Returns null for out-of-bounds paths so
     * callers fail closed like a permission denial.
     */
    private fun confinedFile(name: String): File? {
        return try {
            val base = File(context.filesDir, "script_storage")
            base.mkdirs()
            val canonicalBase = base.canonicalFile
            val file = File(canonicalBase, name).canonicalFile
            if (file == canonicalBase || file.path.startsWith(canonicalBase.path + File.separator)) file else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Read file from app's private storage
     */
    @Suppress("unused")
    fun readFile(filename: String): String? {
        if (!permissionManager.hasScriptPermission(scriptId, Permission.READ_STORAGE)) {
            return null
        }

        return try {
            val file = confinedFile(filename) ?: return null
            if (file.exists()) file.readText() else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Write file to app's private storage
     */
    @Suppress("unused")
    fun writeFile(filename: String, content: String): Boolean {
        if (!permissionManager.hasScriptPermission(scriptId, Permission.WRITE_STORAGE)) {
            return false
        }

        return try {
            val file = confinedFile(filename) ?: return false
            file.writeText(content)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Delete file from app's private storage
     */
    @Suppress("unused")
    fun deleteFile(filename: String): Boolean {
        if (!permissionManager.hasScriptPermission(scriptId, Permission.WRITE_STORAGE)) {
            return false
        }

        return try {
            val file = confinedFile(filename) ?: return false
            file.delete()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * List files in directory
     */
    @Suppress("unused")
    fun listFiles(directory: String): V8Array? {
        if (!permissionManager.hasScriptPermission(scriptId, Permission.READ_STORAGE)) {
            return null
        }
        val runtime = this.runtime ?: return null

        return try {
            val dir = confinedFile(directory) ?: return null
            val files = dir.listFiles() ?: emptyArray()

            val array = V8Array(runtime)
            files.forEach { file ->
                array.push(file.name)
            }
            array
        } catch (e: Exception) {
            null
        }
    }

    // Sensor API

    private val sensorListeners = mutableMapOf<Int, SensorEventListener>()

    /**
     * Get accelerometer data
     */
    @Suppress("unused")
    fun getAccelerometer(callback: V8Function) {
        if (!permissionManager.hasScriptPermission(scriptId, Permission.ACCELEROMETER)) {
            return
        }
        startSensor(Sensor.TYPE_ACCELEROMETER, callback)
    }

    /**
     * Get gyroscope data
     */
    @Suppress("unused")
    fun getGyroscope(callback: V8Function) {
        if (!permissionManager.hasScriptPermission(scriptId, Permission.GYROSCOPE)) {
            return
        }
        startSensor(Sensor.TYPE_GYROSCOPE, callback)
    }

    private fun startSensor(sensorType: Int, callback: V8Function) {
        val sensor = sensorManager.getDefaultSensor(sensorType) ?: return

        // Replace any existing listener for this sensor type so it is not leaked
        sensorListeners.remove(sensorType)?.let { sensorManager.unregisterListener(it) }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // Copy the values first: SensorEvent buffers are reused by the system.
                val x = event.values[0].toDouble()
                val y = event.values[1].toDouble()
                val z = event.values[2].toDouble()

                // Sensor callbacks arrive on the sensor thread; V8 must only be
                // touched from the main thread.
                mainHandler.post {
                    val runtime = this@SystemBridge.runtime ?: return@post
                    if (callback.isReleased) return@post

                    val data = V8Object(runtime)
                    data.add("x", x)
                    data.add("y", y)
                    data.add("z", z)

                    val params = V8Array(runtime).push(data)
                    callback.call(runtime, params)

                    data.release()
                    params.release()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorListeners[sensorType] = listener
        sensorManager.registerListener(
            listener,
            sensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    /**
     * Stop all sensor listening
     */
    @Suppress("unused")
    fun stopSensor() {
        sensorListeners.values.forEach { sensorManager.unregisterListener(it) }
        sensorListeners.clear()
    }

    // Device API

    /**
     * Vibrate device
     */
    @Suppress("unused")
    fun vibrate(durationMs: Int) {
        if (!permissionManager.hasScriptPermission(scriptId, Permission.VIBRATE)) {
            return
        }

        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs.toLong())
        }
    }

    /**
     * Get device information
     */
    @Suppress("unused")
    fun getDeviceInfo(): V8Object {
        val runtime = this.runtime ?: throw IllegalStateException("Runtime not initialized")
        val info = V8Object(runtime)
        info.add("manufacturer", Build.MANUFACTURER)
        info.add("model", Build.MODEL)
        info.add("androidVersion", Build.VERSION.RELEASE)
        info.add("sdkVersion", Build.VERSION.SDK_INT)
        return info
    }

    // Helper methods

    /**
     * Extract the trailing V8Function argument, or null when missing.
     */
    private fun extractCallback(parameters: V8Array): V8Function? {
        val last = parameters.length() - 1
        if (last < 0) return null
        return parameters.getObject(last) as? V8Function
    }

    /**
     * Convert a JS header object into a [Map] of header name to value.
     */
    private fun readHeaders(headersObject: V8Object): Map<String, String> {
        val headers = linkedMapOf<String, String>()
        for (key in headersObject.getKeys()) {
            headers[key] = headersObject.getString(key)
        }
        return headers
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
    }
}
