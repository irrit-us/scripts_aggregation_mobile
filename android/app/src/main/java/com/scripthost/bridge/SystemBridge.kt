package com.scripthost.bridge

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.eclipsesource.v8.V8
import com.eclipsesource.v8.V8Array
import com.eclipsesource.v8.V8Function
import com.eclipsesource.v8.V8Object
import com.scripthost.engine.ScriptBridge
import com.scripthost.models.Permission
import com.scripthost.security.PermissionManager
import kotlinx.coroutines.*
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * System Bridge - Exposes system functionality to scripts
 * Includes network, storage, sensors, and device APIs
 */
class SystemBridge(
    private val context: Context,
    private val permissionManager: PermissionManager
) : ScriptBridge {

    private var runtime: V8? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    override fun register(runtime: V8) {
        this.runtime = runtime

        // Network API
        val networkObject = V8Object(runtime)
        runtime.add("Network", networkObject)
        networkObject.registerJavaMethod(this, "httpGet", "get",
            arrayOf(String::class.java, V8Function::class.java))
        networkObject.registerJavaMethod(this, "httpPost", "post",
            arrayOf(String::class.java, String::class.java, V8Function::class.java))
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
        runtime = null
    }

    // Network API

    /**
     * HTTP GET request
     */
    @Suppress("unused")
    fun httpGet(url: String, callback: V8Function) {
        if (!permissionManager.hasPermission(Permission.INTERNET)) {
            invokeCallback(callback, null, "Permission denied: INTERNET")
            return
        }

        scope.launch {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                val response = if (responseCode == 200) {
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
     * HTTP POST request
     */
    @Suppress("unused")
    fun httpPost(url: String, body: String, callback: V8Function) {
        if (!permissionManager.hasPermission(Permission.INTERNET)) {
            invokeCallback(callback, null, "Permission denied: INTERNET")
            return
        }

        scope.launch {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                connection.outputStream.use { it.write(body.toByteArray()) }

                val responseCode = connection.responseCode
                val response = if (responseCode == 200) {
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
     * Read file from app's private storage
     */
    @Suppress("unused")
    fun readFile(filename: String): String? {
        if (!permissionManager.hasPermission(Permission.READ_STORAGE)) {
            return null
        }

        return try {
            val file = File(context.filesDir, filename)
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
        if (!permissionManager.hasPermission(Permission.WRITE_STORAGE)) {
            return false
        }

        return try {
            val file = File(context.filesDir, filename)
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
        if (!permissionManager.hasPermission(Permission.WRITE_STORAGE)) {
            return false
        }

        return try {
            val file = File(context.filesDir, filename)
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
        if (!permissionManager.hasPermission(Permission.READ_STORAGE)) {
            return null
        }

        return try {
            val dir = File(context.filesDir, directory)
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

    private var sensorListener: SensorEventListener? = null

    /**
     * Get accelerometer data
     */
    @Suppress("unused")
    fun getAccelerometer(callback: V8Function) {
        if (!permissionManager.hasPermission(Permission.ACCELEROMETER)) {
            return
        }

        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return

        sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val data = V8Object(runtime)
                data.add("x", event.values[0].toDouble())
                data.add("y", event.values[1].toDouble())
                data.add("z", event.values[2].toDouble())

                val params = V8Array(runtime).push(data)
                callback.call(runtime, params)

                data.release()
                params.release()
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(
            sensorListener,
            sensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    /**
     * Get gyroscope data
     */
    @Suppress("unused")
    fun getGyroscope(callback: V8Function) {
        if (!permissionManager.hasPermission(Permission.GYROSCOPE)) {
            return
        }

        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) ?: return

        sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val data = V8Object(runtime)
                data.add("x", event.values[0].toDouble())
                data.add("y", event.values[1].toDouble())
                data.add("z", event.values[2].toDouble())

                val params = V8Array(runtime).push(data)
                callback.call(runtime, params)

                data.release()
                params.release()
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(
            sensorListener,
            sensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    /**
     * Stop sensor listening
     */
    @Suppress("unused")
    fun stopSensor() {
        sensorListener?.let {
            sensorManager.unregisterListener(it)
            sensorListener = null
        }
    }

    // Device API

    /**
     * Vibrate device
     */
    @Suppress("unused")
    fun vibrate(durationMs: Int) {
        if (!permissionManager.hasPermission(Permission.VIBRATE)) {
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
        val info = V8Object(runtime)
        info.add("manufacturer", Build.MANUFACTURER)
        info.add("model", Build.MODEL)
        info.add("androidVersion", Build.VERSION.RELEASE)
        info.add("sdkVersion", Build.VERSION.SDK_INT)
        return info
    }

    // Helper methods

    private fun invokeCallback(callback: V8Function, data: String?, error: String?) {
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
