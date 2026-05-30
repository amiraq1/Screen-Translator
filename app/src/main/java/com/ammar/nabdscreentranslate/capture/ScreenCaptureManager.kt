package com.ammar.nabdscreentranslate.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Captures a single screenshot using MediaProjection API.
 * Each capture creates a fresh VirtualDisplay and cleans up after.
 * Does NOT store bitmaps - caller is responsible for the returned Bitmap.
 */
class ScreenCaptureManager(private val context: Context) {

    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var projection: MediaProjection? = null
    private var projectionCallback: MediaProjection.Callback? = null

    /**
     * Captures the current screen and returns a Bitmap.
     * Throws if MediaProjection permission is not available.
     * Caller must NOT recycle the bitmap until done with OCR/Translation.
     */
    suspend fun captureScreen(): Bitmap {
        if (!MediaProjectionHolder.hasPermission()) {
            throw IllegalStateException("لم يتم منح صلاحية التقاط الشاشة")
        }

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)

        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        Log.d(TAG, "Starting screen capture: ${width}x${height} @ ${density}dpi")

        return withTimeout(5000L) {
            captureScreenInternal(width, height, density)
        }
    }

    private suspend fun captureScreenInternal(
        width: Int,
        height: Int,
        density: Int
    ): Bitmap = suspendCancellableCoroutine { continuation ->
        var resumed = false

        fun safeResume(bitmap: Bitmap) {
            if (!resumed) {
                resumed = true
                continuation.resume(bitmap)
            }
        }

        fun safeResumeWithException(e: Exception) {
            if (!resumed) {
                resumed = true
                continuation.resumeWithException(e)
            }
        }

        try {
            val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

            // Create MediaProjection from stored consent
            projection = projectionManager.getMediaProjection(
                MediaProjectionHolder.resultCode,
                MediaProjectionHolder.resultData!!
            )
            MediaProjectionHolder.setProjection(projection)

            val handler = Handler(Looper.getMainLooper())
            projectionCallback = object : MediaProjection.Callback() {
                override fun onStop() {
                    cleanup()
                }
            }
            projection!!.registerCallback(projectionCallback!!, handler)

            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

            virtualDisplay = projection!!.createVirtualDisplay(
                "NabdScreenCapture",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader!!.surface,
                null, handler
            )

            // Use OnImageAvailableListener for reliable frame capture
            imageReader!!.setOnImageAvailableListener({ reader ->
                if (resumed) return@setOnImageAvailableListener

                try {
                    val image: Image? = reader.acquireLatestImage()
                    if (image != null) {
                        val bitmap = imageToBitmap(image, width, height)
                        image.close()
                        cleanup()
                        Log.d(TAG, "Screen capture successful: ${bitmap.width}x${bitmap.height}")
                        safeResume(bitmap)
                    }
                } catch (e: Exception) {
                    cleanup()
                    Log.e(TAG, "Error in image listener: ${e.message}", e)
                    safeResumeWithException(e)
                }
            }, handler)

            // Fallback: if no image arrives within 2.5 seconds, try once more then fail
            handler.postDelayed({
                if (!resumed) {
                    try {
                        val image = imageReader?.acquireLatestImage()
                        if (image != null) {
                            val bitmap = imageToBitmap(image, width, height)
                            image.close()
                            cleanup()
                            Log.d(TAG, "Screen capture successful (fallback)")
                            safeResume(bitmap)
                        } else {
                            cleanup()
                            Log.e(TAG, "Screen capture timeout - no image available")
                            safeResumeWithException(
                                RuntimeException("تعذر التقاط الشاشة. حاول مرة أخرى.")
                            )
                        }
                    } catch (e: Exception) {
                        cleanup()
                        Log.e(TAG, "Screen capture fallback error: ${e.message}", e)
                        safeResumeWithException(e)
                    }
                }
            }, 2500)

            continuation.invokeOnCancellation {
                if (!resumed) {
                    resumed = true
                    cleanup()
                }
            }
        } catch (e: Exception) {
            cleanup()
            Log.e(TAG, "Screen capture setup error: ${e.message}", e)
            safeResumeWithException(e)
        }
    }

    private fun imageToBitmap(image: Image, width: Int, height: Int): Bitmap {
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * width

        val bitmap = Bitmap.createBitmap(
            width + rowPadding / pixelStride,
            height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)

        // Crop to actual screen size if there's row padding
        return if (rowPadding > 0) {
            val cropped = Bitmap.createBitmap(bitmap, 0, 0, width, height)
            if (cropped !== bitmap) bitmap.recycle()
            cropped
        } else {
            bitmap
        }
    }

    private fun cleanup() {
        Log.d(TAG, "ScreenCaptureManager: cleaning up resources")
        try {
            imageReader?.setOnImageAvailableListener(null, null)
        } catch (e: Exception) {
            Log.w(TAG, "Error clearing ImageReader listener: ${e.message}")
        }
        try {
            virtualDisplay?.release()
            Log.d(TAG, "VirtualDisplay released")
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing VirtualDisplay: ${e.message}")
        }
        virtualDisplay = null
        try {
            imageReader?.close()
            Log.d(TAG, "ImageReader closed")
        } catch (e: Exception) {
            Log.w(TAG, "Error closing ImageReader: ${e.message}")
        }
        imageReader = null
        // Stop projection - on Android 14+ the token is single-use anyway
        try {
            projectionCallback?.let { projection?.unregisterCallback(it) }
            projectionCallback = null
            projection?.stop()
            Log.d(TAG, "MediaProjection stopped")
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping projection: ${e.message}")
        }
        projection = null
        // Invalidate stored consent since projection was consumed
        MediaProjectionHolder.invalidateAfterUse()
    }

    companion object {
        private const val TAG = "NabdScreenTranslate"
    }
}
