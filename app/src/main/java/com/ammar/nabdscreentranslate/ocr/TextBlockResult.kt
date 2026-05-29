package com.ammar.nabdscreentranslate.ocr

import android.graphics.Rect

data class TextBlockResult(
    val text: String,
    val boundingBox: Rect?,
    val confidence: Float? = null
)
