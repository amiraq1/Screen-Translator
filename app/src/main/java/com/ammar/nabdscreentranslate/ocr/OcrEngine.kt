package com.ammar.nabdscreentranslate.ocr

import android.graphics.Bitmap

interface OcrEngine {
    suspend fun recognizeText(bitmap: Bitmap): List<TextBlockResult>
    fun close()
}
