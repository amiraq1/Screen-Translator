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
        Log.d(TAG, "OCR input: ${bitmap.width}x${bitmap.height} (${bitmap.byteCount / 1024}KB)")

        return when (currentSourceLang) {
            "ar" -> {
                Log.d(TAG, "Selected OCR engine: Arabic (source=ar)")
                timedRecognize("Arabic") { arabicEngine.recognizeText(bitmap) }
                    ?: fallbackMlKit(bitmap)
            }
            "auto" -> {
                Log.d(TAG, "Selected OCR engine: Hybrid (source=auto)")
                recognizeHybrid(bitmap)
            }
            else -> {
                Log.d(TAG, "Selected OCR engine: MLKit (source=$currentSourceLang)")
                timedRecognize("MLKit") { mlKitEngine.recognizeText(bitmap) }
                    ?: emptyList()
            }
        }
    }

    private suspend fun fallbackMlKit(bitmap: Bitmap): List<TextBlockResult> {
        Log.d(TAG, "Arabic OCR failed, trying MLKit as fallback")
        return try {
            mlKitEngine.recognizeText(bitmap)
        } catch (e: Exception) {
            Log.e(TAG, "MLKit fallback also failed: ${e.message}")
            emptyList()
        }
    }

    /**
     * Hybrid strategy for "auto" mode:
     * 1. Try ML Kit first (fast, good for Latin scripts)
     * 2. If result is empty or very short, try Arabic OCR as fallback
     * 3. Check if ML Kit result contains Arabic characters — if so, prefer Arabic engine
     */
    private suspend fun recognizeHybrid(bitmap: Bitmap): List<TextBlockResult> {
        // Step 1: Try ML Kit
        val mlKitStart = System.currentTimeMillis()
        val mlKitResults = try {
            mlKitEngine.recognizeText(bitmap)
        } catch (e: Exception) {
            Log.w(TAG, "MLKit OCR failed in hybrid mode: ${e.message}")
            emptyList()
        }
        val mlKitDuration = System.currentTimeMillis() - mlKitStart

        val totalChars = mlKitResults.sumOf { it.text.length }
        val mlKitText = mlKitResults.joinToString(" ") { it.text }
        val avgConfidence = mlKitResults.mapNotNull { it.confidence }.average().let {
            if (it.isNaN()) 0.0 else it
        }

        Log.d(TAG, "MLKit OCR: ${mlKitResults.size} blocks, $totalChars chars, " +
                "confidence=${(avgConfidence * 100).toInt()}%, duration=${mlKitDuration}ms")

        // Check if ML Kit detected Arabic characters (it can't read them but may detect blocks)
        val hasArabicChars = mlKitText.any { it in '\u0600'..'\u06FF' || it in '\u0750'..'\u077F' }

        // If ML Kit found reasonable non-Arabic text, use it
        if (mlKitResults.isNotEmpty() && totalChars >= MIN_CHARS_THRESHOLD && !hasArabicChars) {
            Log.d(TAG, "Hybrid result: MLKit ✓ ($totalChars chars, ${mlKitDuration}ms)")
            return mlKitResults
        }

        // Step 2: ML Kit found little/no text or detected Arabic — try Arabic OCR
        val reason = when {
            hasArabicChars -> "detected Arabic characters"
            totalChars < MIN_CHARS_THRESHOLD -> "insufficient text ($totalChars chars)"
            else -> "empty result"
        }
        Log.d(TAG, "Hybrid: Fallback to Arabic OCR — $reason")

        val arabicStart = System.currentTimeMillis()
        val arabicResults = try {
            arabicEngine.recognizeText(bitmap)
        } catch (e: Exception) {
            Log.w(TAG, "Arabic OCR fallback failed: ${e.message}")
            emptyList()
        }
        val arabicDuration = System.currentTimeMillis() - arabicStart

        val arabicChars = arabicResults.sumOf { it.text.length }
        val arabicConfidence = arabicResults.mapNotNull { it.confidence }.average().let {
            if (it.isNaN()) 0.0 else it
        }

        Log.d(TAG, "Arabic OCR: ${arabicResults.size} blocks, $arabicChars chars, " +
                "confidence=${(arabicConfidence * 100).toInt()}%, duration=${arabicDuration}ms")

        // Return whichever found more text
        return if (arabicChars > totalChars) {
            Log.d(TAG, "Hybrid result: Arabic ✓ ($arabicChars chars, total=${mlKitDuration + arabicDuration}ms)")
            arabicResults
        } else if (mlKitResults.isNotEmpty()) {
            Log.d(TAG, "Hybrid result: MLKit ✓ (kept, $totalChars chars >= $arabicChars chars)")
            mlKitResults
        } else {
            Log.d(TAG, "Hybrid result: No text found (total=${mlKitDuration + arabicDuration}ms)")
            arabicResults
        }
    }

    /**
     * Timed wrapper for OCR recognition with logging.
     * Returns null if the engine throws an exception.
     */
    private suspend fun timedRecognize(
        engineName: String,
        block: suspend () -> List<TextBlockResult>
    ): List<TextBlockResult>? {
        val start = System.currentTimeMillis()
        return try {
            val results = block()
            val duration = System.currentTimeMillis() - start
            val chars = results.sumOf { it.text.length }
            val confidence = results.mapNotNull { it.confidence }.average().let {
                if (it.isNaN()) 0.0 else it
            }
            Log.d(TAG, "$engineName OCR: ${results.size} blocks, $chars chars, " +
                    "confidence=${(confidence * 100).toInt()}%, duration=${duration}ms")
            results
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - start
            Log.e(TAG, "$engineName OCR failed after ${duration}ms: ${e.message}")
            null
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
