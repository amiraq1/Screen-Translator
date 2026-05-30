package com.ammar.nabdscreentranslate.ocr

import android.content.Context
import android.graphics.Bitmap
import android.util.Log

/**
 * Hybrid OCR engine that intelligently selects between ML Kit (Latin) and
 * Tesseract (Arabic) based on the source language setting.
 *
 * Strategy:
 * - source = "ar" → Use ArabicOcrEngine directly
 * - source = "auto" → Try ML Kit first; if result is empty/poor, fallback to Arabic
 * - source = any other → Use MlKitOcrEngine (Latin scripts)
 */
class HybridOcrEngine(context: Context) : OcrEngine {

    private val mlKitEngine = MlKitOcrEngine()
    private val arabicEngine = ArabicOcrEngine(context)

    private var currentSourceLang: String = "auto"

    /**
     * Set the source language to determine which OCR engine to use.
     * Call this before recognizeText().
     */
    fun setSourceLanguage(lang: String) {
        currentSourceLang = lang
        Log.d(TAG, "HybridOCR source language set to: $lang")
    }

    override suspend fun recognizeText(bitmap: Bitmap): List<TextBlockResult> {
        return when (currentSourceLang) {
            "ar" -> {
                Log.d(TAG, "Selected OCR engine: Arabic (source=ar)")
                recognizeArabic(bitmap)
            }
            "auto" -> {
                Log.d(TAG, "Selected OCR engine: Hybrid (source=auto)")
                recognizeHybrid(bitmap)
            }
            else -> {
                Log.d(TAG, "Selected OCR engine: MLKit (source=$currentSourceLang)")
                recognizeWithMlKit(bitmap)
            }
        }
    }

    private suspend fun recognizeArabic(bitmap: Bitmap): List<TextBlockResult> {
        return try {
            arabicEngine.recognizeText(bitmap)
        } catch (e: Exception) {
            Log.e(TAG, "Arabic OCR failed: ${e.message}, trying MLKit as fallback")
            // Fallback to ML Kit if Arabic engine fails
            mlKitEngine.recognizeText(bitmap)
        }
    }

    private suspend fun recognizeWithMlKit(bitmap: Bitmap): List<TextBlockResult> {
        return mlKitEngine.recognizeText(bitmap)
    }

    /**
     * Hybrid strategy for "auto" mode:
     * 1. Try ML Kit first (fast, good for Latin scripts)
     * 2. If result is empty or very short, try Arabic OCR as fallback
     * 3. Check if ML Kit result contains Arabic characters — if so, prefer Arabic engine
     */
    private suspend fun recognizeHybrid(bitmap: Bitmap): List<TextBlockResult> {
        // Step 1: Try ML Kit
        val mlKitResults = try {
            mlKitEngine.recognizeText(bitmap)
        } catch (e: Exception) {
            Log.w(TAG, "MLKit OCR failed in hybrid mode: ${e.message}")
            emptyList()
        }

        val totalChars = mlKitResults.sumOf { it.text.length }
        val mlKitText = mlKitResults.joinToString(" ") { it.text }

        // Check if ML Kit detected Arabic characters (it can't read them but may detect blocks)
        val hasArabicChars = mlKitText.any { it in '\u0600'..'\u06FF' || it in '\u0750'..'\u077F' }

        // If ML Kit found reasonable non-Arabic text, use it
        if (mlKitResults.isNotEmpty() && totalChars >= MIN_CHARS_THRESHOLD && !hasArabicChars) {
            Log.d(TAG, "Hybrid: MLKit found sufficient text ($totalChars chars, ${mlKitResults.size} blocks)")
            return mlKitResults
        }

        // Step 2: ML Kit found little/no text or detected Arabic — try Arabic OCR
        val reason = when {
            hasArabicChars -> "detected Arabic characters"
            totalChars < MIN_CHARS_THRESHOLD -> "insufficient text ($totalChars chars)"
            else -> "empty result"
        }
        Log.d(TAG, "Hybrid: Fallback to Arabic OCR — $reason")

        val arabicResults = try {
            arabicEngine.recognizeText(bitmap)
        } catch (e: Exception) {
            Log.w(TAG, "Arabic OCR fallback failed: ${e.message}")
            emptyList()
        }

        val arabicChars = arabicResults.sumOf { it.text.length }

        // Return whichever found more text
        return if (arabicChars > totalChars) {
            Log.d(TAG, "Hybrid: Using Arabic OCR result ($arabicChars chars > $totalChars chars)")
            arabicResults
        } else if (mlKitResults.isNotEmpty()) {
            Log.d(TAG, "Hybrid: Keeping MLKit result ($totalChars chars >= $arabicChars chars)")
            mlKitResults
        } else {
            Log.d(TAG, "Hybrid: No text found by either engine")
            arabicResults // Return whatever we have (could be empty)
        }
    }

    override fun close() {
        mlKitEngine.close()
        arabicEngine.close()
        Log.d(TAG, "HybridOcrEngine closed")
    }

    companion object {
        private const val TAG = "NabdScreenTranslate"
        // Minimum characters to consider ML Kit result as "sufficient"
        private const val MIN_CHARS_THRESHOLD = 10
    }
}
