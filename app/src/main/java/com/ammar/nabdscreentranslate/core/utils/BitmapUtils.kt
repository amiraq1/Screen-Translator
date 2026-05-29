package com.ammar.nabdscreentranslate.core.utils

import android.graphics.Bitmap
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
