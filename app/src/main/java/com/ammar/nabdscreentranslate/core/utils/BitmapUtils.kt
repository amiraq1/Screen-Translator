package com.ammar.nabdscreentranslate.core.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect

object BitmapUtils {

    private const val MAX_OCR_WIDTH = 1920
    private const val MAX_OCR_HEIGHT = 1080

    /**
     * Downscale bitmap if too large for OCR processing
     */
    fun prepareForOcr(bitmap: Bitmap): Bitmap {
        if (bitmap.width <= MAX_OCR_WIDTH && bitmap.height <= MAX_OCR_HEIGHT) {
            return bitmap
        }

        val scale = minOf(
            MAX_OCR_WIDTH.toFloat() / bitmap.width,
            MAX_OCR_HEIGHT.toFloat() / bitmap.height
        )

        val newWidth = (bitmap.width * scale).toInt()
        val newHeight = (bitmap.height * scale).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Prepare bitmap specifically for Arabic OCR (Tesseract).
     * Applies: grayscale + contrast boost + upscale for small text.
     * Tesseract works best with high-contrast grayscale images at ~300 DPI equivalent.
     * Memory-safe: caps output at 4096x4096 to prevent OOM.
     */
    fun prepareForArabicOcr(bitmap: Bitmap): Bitmap {
        // Step 1: Upscale if text might be too small
        // Tesseract needs ~30px character height minimum for Arabic
        val scaleFactor = when {
            bitmap.height < 800 -> 2.0f
            bitmap.height < 1200 -> 1.5f
            else -> 1.0f
        }

        // Cap maximum dimensions to prevent OOM
        val maxDim = 4096
        val cappedScale = if (scaleFactor > 1.0f) {
            val targetW = (bitmap.width * scaleFactor).toInt()
            val targetH = (bitmap.height * scaleFactor).toInt()
            if (targetW > maxDim || targetH > maxDim) {
                minOf(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height)
            } else {
                scaleFactor
            }
        } else {
            scaleFactor
        }

        val scaled = if (cappedScale > 1.0f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * cappedScale).toInt(),
                (bitmap.height * cappedScale).toInt(),
                true
            )
        } else {
            bitmap
        }

        // Step 2: Convert to grayscale with contrast enhancement
        val grayscale = Bitmap.createBitmap(scaled.width, scaled.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(grayscale)
        val paint = Paint()

        // Grayscale + contrast boost (1.5x contrast, slight brightness reduction)
        val contrastMatrix = ColorMatrix().apply {
            setSaturation(0f) // Grayscale
        }
        val contrastBoost = ColorMatrix(floatArrayOf(
            1.4f, 0f, 0f, 0f, -30f,  // Red
            0f, 1.4f, 0f, 0f, -30f,  // Green
            0f, 0f, 1.4f, 0f, -30f,  // Blue
            0f, 0f, 0f, 1f, 0f       // Alpha
        ))
        contrastMatrix.postConcat(contrastBoost)

        paint.colorFilter = ColorMatrixColorFilter(contrastMatrix)
        canvas.drawBitmap(scaled, 0f, 0f, paint)

        // Recycle intermediate if we created it
        if (scaled !== bitmap) {
            scaled.recycle()
        }

        return grayscale
    }

    /**
     * Crop bitmap to a specific region
     */
    fun cropRegion(bitmap: Bitmap, region: Rect): Bitmap {
        val safeLeft = region.left.coerceIn(0, bitmap.width - 1)
        val safeTop = region.top.coerceIn(0, bitmap.height - 1)
        val safeRight = region.right.coerceIn(safeLeft + 1, bitmap.width)
        val safeBottom = region.bottom.coerceIn(safeTop + 1, bitmap.height)

        return Bitmap.createBitmap(
            bitmap,
            safeLeft,
            safeTop,
            safeRight - safeLeft,
            safeBottom - safeTop
        )
    }
}
