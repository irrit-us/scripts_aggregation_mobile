package com.scripthost.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Camera capture controller - takes a photo via the system camera app
 * (ActivityResultContracts.TakePicture) and delivers it as a JPEG Base64
 * string (Base64.NO_WRAP, no data: prefix), downscaled so the longest side
 * is at most [MAX_DIM] px, JPEG quality [JPEG_QUALITY].
 *
 * The activity-result contract is registered in [init], so this controller
 * MUST be constructed before the fragment is CREATED (ScriptRuntimeFragment
 * initializes it as a property). Only one capture may be in flight.
 */
class CameraCaptureController(private val fragment: Fragment) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var pendingFile: File? = null
    private var pendingResult: ((base64: String?, error: String?) -> Unit)? = null

    private val takePicture: ActivityResultLauncher<Uri>

    init {
        takePicture = fragment.registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            onCaptureResult(success)
        }
    }

    /** True while a capture is in flight. */
    val isCapturing: Boolean
        get() = pendingResult != null

    /**
     * Launch the camera and deliver the outcome to [onResult] on the main
     * thread: (base64, null) on success, (null, error) on cancel/failure.
     * A second call while busy fails immediately with
     * "Capture already in progress".
     */
    fun takePhoto(onResult: (base64: String?, error: String?) -> Unit) {
        if (pendingResult != null) {
            onResult(null, "Capture already in progress")
            return
        }
        val context = fragment.context
        if (context == null) {
            onResult(null, "Fragment not attached")
            return
        }
        val file = try {
            val dir = File(context.cacheDir, "camera")
            dir.mkdirs()
            File.createTempFile("photo_", ".jpg", dir)
        } catch (e: Exception) {
            onResult(null, e.message ?: "Failed to prepare camera output")
            return
        }
        val uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            file.delete()
            onResult(null, e.message ?: "Failed to prepare camera output")
            return
        }
        pendingFile = file
        pendingResult = onResult
        try {
            takePicture.launch(uri)
        } catch (e: Exception) {
            pendingFile = null
            pendingResult = null
            file.delete()
            onResult(null, e.message ?: "Failed to launch camera")
        }
    }

    private fun onCaptureResult(success: Boolean) {
        val callback = pendingResult ?: return
        val file = pendingFile
        pendingResult = null
        pendingFile = null
        if (!success || file == null) {
            file?.delete()
            callback(null, "Canceled")
            return
        }
        // Decode/compress off the main thread; deliver on the main thread.
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    encodePhoto(file, MAX_DIM, JPEG_QUALITY) to null
                } catch (e: Exception) {
                    null to (e.message ?: "Failed to process photo")
                } finally {
                    file.delete()
                }
            }
            callback(result.first, result.second)
        }
    }

    companion object {
        internal const val MAX_DIM = 1280
        internal const val JPEG_QUALITY = 85

        /**
         * Decode [file] (downsampling the decode), scale so the longest side
         * is at most [maxDim], and return it as a JPEG ([quality]) encoded
         * with Base64.NO_WRAP (no data: prefix).
         */
        internal fun encodePhoto(file: File, maxDim: Int, quality: Int): String {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            require(bounds.outWidth > 0 && bounds.outHeight > 0) {
                "Failed to decode captured photo"
            }

            // Decode at a reduced resolution close to the target size so
            // full-resolution photos do not blow up the heap
            var sampleSize = 1
            val longest = max(bounds.outWidth, bounds.outHeight)
            while (longest / (sampleSize * 2) >= maxDim) sampleSize *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = BitmapFactory.decodeFile(file.absolutePath, options)
                ?: throw IllegalStateException("Failed to decode captured photo")

            val (dstW, dstH) = scaledDimensions(decoded.width, decoded.height, maxDim)
            val output = if (dstW != decoded.width || dstH != decoded.height) {
                Bitmap.createScaledBitmap(decoded, dstW, dstH, true)
            } else {
                decoded
            }
            val stream = ByteArrayOutputStream()
            output.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
        }

        /**
         * Target dimensions with the longest side scaled down to [maxDim],
         * aspect ratio preserved. Images already within the limit are
         * returned unchanged. Pure math so it is JVM-testable.
         */
        internal fun scaledDimensions(srcW: Int, srcH: Int, maxDim: Int): IntArray {
            require(srcW > 0 && srcH > 0) { "Invalid source dimensions ${srcW}x${srcH}" }
            val longest = max(srcW, srcH)
            if (longest <= maxDim) return intArrayOf(srcW, srcH)
            val scale = maxDim.toDouble() / longest
            return intArrayOf(
                max(1, (srcW * scale).roundToInt()),
                max(1, (srcH * scale).roundToInt())
            )
        }
    }
}
