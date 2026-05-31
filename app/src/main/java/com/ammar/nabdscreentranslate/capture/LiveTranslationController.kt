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
import kotlinx.coroutines.*

/**
 * Controls the Live Translation loop.
 *
 * Keeps a persistent MediaProjection + VirtualDisplay alive for the duration
 * of the live session, capturing frames at a configurable interval.
 *
 * Key behaviors:
 * - Single MediaProjection consent per session
 * - Compares OCR text hash to skip unchanged content
 * - Stops after repeated errors
 * - Does not overlap captures (waits for previous to finish)
 */
class LiveTranslationController(
    private val context: Context
) {
    companion object {
        private const val TAG = "NabdScreenTranslate"
        private const val MAX_CONSECUTIVE_ERRORS = 3
    }

    var isRunning: Boolean = false
        private set

    private var loopJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Persistent capture resources
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var projectionCallback: MediaProjection.Callback? = null
    private val handler = Handler(Looper.getMainLooper())

    // State
    private var lastOcrTextHash: Int = 0
    private var consecutiveErrors: Int = 0
    private var isCapturing: Boolean = false

    // Screen metrics
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0
    private var screenDensity: Int = 0

    /** Callback for each successful translation tick. */
    var onTranslationReady: (suspend (Bitmap) -> Unit)? = null

    /** Callback when live mode stops (error or manual). */
    var onStopped: ((reason: String) -> Unit)? = null

    /** Callback to check if text changed. Returns hash of OCR text. */
    var onOcrHashComputed: ((hash: Int) -> Boolean)? = null

    /**
     * Starts the live translation loop.
     * Requires MediaProjection consent to already be stored in MediaProjectionHolder.
     */
    fun start(intervalMs: Long) {
        if (isRunning) {
            Log.d(TAG, "Live: already running, ignoring start")
            return
        }
        if (!MediaProjectionHolder.hasPermission()) {
            Log.e(TAG, "Live: no MediaProjection permission")
            onStopped?.invoke("لم يتم منح صلاحية التقاط الشاشة")
            return
        }

        Log.d(TAG, "Live mode started (interval=${intervalMs}ms)")
        isRunning = true
        consecutiveErrors = 0
        lastOcrTextHash = 0

        // Get screen metrics
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
        screenDensity = metrics.densityDpi

        // Setup persistent projection
        if (!setupProjection()) {
            isRunning = false
            onStopped?.invoke("تعذر إنشاء جلسة التقاط الشاشة")
            return
        }

        // Start the loop
        loopJob = scope.launch {
            while (isActive && isRunning) {
                if (!isCapturing) {
                    performLiveTick()
                }
                delay(intervalMs)
            }
        }
    }

    /**
     * Stops the live translation loop and releases all resources.
     */
    fun stop() {
        if (!isRunning) return
        Log.d(TAG, "Live mode stopped")
        isRunning = false
        loopJob?.cancel()
        loopJob = null
        releaseProjection()
        lastOcrTextHash = 0
        consecutiveErrors = 0
    }

    /**
     * Resets the text hash so the next tick will always translate.
     */
    fun resetHash() {
        lastOcrTextHash = 0
    }

    /**
     * Checks if the given OCR text hash is different from the last one.
     * Updates the stored hash if different.
     * Returns true if text changed (should translate).
     */
    fun hasTextChanged(ocrText: String): Boolean {
        val newHash = ocrText.hashCode()
        if (newHash == lastOcrTextHash) {
            Log.d(TAG, "Live: unchanged text, skipping")
            return false
        }
        lastOcrTextHash = newHash
        Log.d(TAG, "Live: text changed, translating")
        return true
    }

    private fun setupProjection(): Boolean {
        return try {
            val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = projectionManager.getMediaProjection(
                MediaProjectionHolder.resultCode,
                MediaProjectionHolder.resultData!!
            )
            MediaProjectionHolder.setProjection(projection)

            projectionCallback = object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.d(TAG, "Live: MediaProjection stopped externally")
                    if (isRunning) {
                        isRunning = false
                        loopJob?.cancel()
                        onStopped?.invoke("تم إيقاف جلسة التقاط الشاشة")
                    }
                }
            }
            projection!!.registerCallback(projectionCallback!!, handler)

            // Create persistent ImageReader
            imageReader = ImageReader.newInstance(
                screenWidth, screenHeight, PixelFormat.RGBA_8888, 2
            )

            // Create persistent VirtualDisplay
            virtualDisplay = projection!!.createVirtualDisplay(
                "NabdLiveCapture",
                screenWidth, screenHeight, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader!!.surface,
                null, handler
            )

            Log.d(TAG, "Live: persistent projection setup complete (${screenWidth}x${screenHeight})")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Live: failed to setup projection: ${e.message}", e)
            releaseProjection()
            false
        }
    }

    private suspend fun performLiveTick() {
        isCapturing = true
        Log.d(TAG, "Live capture tick")

        try {
            val bitmap = captureFrame()
            if (bitmap != null) {
                consecutiveErrors = 0
                onTranslationReady?.invoke(bitmap)
            } else {
                handleError("No frame captured")
            }
        } catch (e: Exception) {
            handleError(e.message ?: "Unknown error")
        } finally {
            isCapturing = false
        }
    }

    private suspend fun captureFrame(): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val image: Image? = imageReader?.acquireLatestImage()
            if (image != null) {
                val bitmap = imageToBitmap(image)
                image.close()
                bitmap
            } else {
                // No new frame available yet, wait a bit and retry once
                delay(100)
                val retryImage = imageReader?.acquireLatestImage()
                if (retryImage != null) {
                    val bitmap = imageToBitmap(retryImage)
                    retryImage.close()
                    bitmap
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Live: frame capture error: ${e.message}")
            null
        }
    }

    private fun imageToBitmap(image: Image): Bitmap {
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * screenWidth

        val bitmap = Bitmap.createBitmap(
            screenWidth + rowPadding / pixelStride,
            screenHeight,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)

        return if (rowPadding > 0) {
            val cropped = Bitmap.createBitmap(bitmap, 0, 0, screenWidth, screenHeight)
            if (cropped !== bitmap) bitmap.recycle()
            cropped
        } else {
            bitmap
        }
    }

    private fun handleError(message: String) {
        consecutiveErrors++
        Log.e(TAG, "Live error count: $consecutiveErrors — $message")
        if (consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) {
            Log.e(TAG, "Live stopped after repeated errors")
            stop()
            onStopped?.invoke("تم إيقاف الترجمة الفورية بسبب أخطاء متكررة")
        }
    }

    private fun releaseProjection() {
        try {
            imageReader?.setOnImageAvailableListener(null, null)
        } catch (_: Exception) {}
        try { virtualDisplay?.release() } catch (_: Exception) {}
        virtualDisplay = null
        try { imageReader?.close() } catch (_: Exception) {}
        imageReader = null
        try {
            projectionCallback?.let { projection?.unregisterCallback(it) }
        } catch (_: Exception) {}
        projectionCallback = null
        try { projection?.stop() } catch (_: Exception) {}
        projection = null
        MediaProjectionHolder.invalidateAfterUse()
        Log.d(TAG, "Live: projection resources released")
    }
}
