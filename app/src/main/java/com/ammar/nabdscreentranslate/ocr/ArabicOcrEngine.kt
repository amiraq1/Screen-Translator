package com.ammar.nabdscreentranslate.ocr

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.ammar.nabdscreentranslate.core.utils.BitmapUtils
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Arabic OCR engine using Tesseract4Android.
 * Supports Arabic script recognition offline using traineddata files.
 * The Arabic traineddata is bundled in assets and extracted on first use.
 */
class ArabicOcrEngine(private val context: Context) : OcrEngine {

    private var tessApi: TessBaseAPI? = null
    private var isInitialized = false
    private val dataPath: String by lazy {
        File(context.filesDir, "tesseract").absolutePath
    }

    /**
     * Initialize Tesseract with Arabic language data.
     * Extracts traineddata from assets if not already present.
     */
    private suspend fun ensureInitialized() {
        if (isInitialized && tessApi != null) return

        withContext(Dispatchers.IO) {
            extractTrainedData()

            tessApi = TessBaseAPI().also { api ->
                val success = api.init(dataPath, LANG_ARABIC)
                if (!success) {
                    api.recycle()
                    tessApi = null
                    throw IllegalStateException("فشل تهيئة محرك OCR العربي")
                }
                // Set page segmentation mode for better Arabic text detection
                api.pageSegMode = TessBaseAPI.PageSegMode.PSM_AUTO
                isInitialized = true
                Log.d(TAG, "Arabic OCR engine initialized successfully")
            }
        }
    }

    private fun extractTrainedData() {
        val tessdataDir = File(dataPath, "tessdata")
        val trainedDataFile = File(tessdataDir, "${LANG_ARABIC}.traineddata")

        if (trainedDataFile.exists()) {
            Log.d(TAG, "Arabic traineddata already exists: ${trainedDataFile.length()} bytes")
            return
        }

        tessdataDir.mkdirs()
        Log.d(TAG, "Extracting Arabic traineddata from assets...")

        try {
            context.assets.open("tessdata/${LANG_ARABIC}.traineddata").use { input ->
                FileOutputStream(trainedDataFile).use { output ->
                    input.copyTo(output)
                }
            }
            Log.d(TAG, "Arabic traineddata extracted: ${trainedDataFile.length()} bytes")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract Arabic traineddata: ${e.message}", e)
            throw IllegalStateException("تعذر تحميل بيانات اللغة العربية: ${e.message}")
        }
    }

    override suspend fun recognizeText(bitmap: Bitmap): List<TextBlockResult> {
        Log.d(TAG, "Arabic OCR started (input: ${bitmap.width}x${bitmap.height})")

        ensureInitialized()

        return withContext(Dispatchers.IO) {
            val api = tessApi ?: throw IllegalStateException("Arabic OCR not initialized")

            // Preprocess bitmap for better Arabic recognition
            val processedBitmap = BitmapUtils.prepareForArabicOcr(bitmap)
            Log.d(TAG, "Arabic OCR preprocessed: ${processedBitmap.width}x${processedBitmap.height}")

            try {
                api.setImage(processedBitmap)
                val text = api.utF8Text ?: ""
                val confidence = api.meanConfidence()

                // Recycle processed bitmap (not the original)
                if (processedBitmap !== bitmap && !processedBitmap.isRecycled) {
                    processedBitmap.recycle()
                }

                if (text.isBlank()) {
                    Log.d(TAG, "Arabic OCR completed - no text found")
                    return@withContext emptyList()
                }

                // Clean up OCR output: remove excessive whitespace, fix common artifacts
                val cleanedText = cleanArabicOcrOutput(text)

                // Parse text into blocks (split by double newlines or paragraphs)
                val blocks = cleanedText.split(Regex("\\n{2,}"))
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .map { blockText ->
                        TextBlockResult(
                            text = blockText,
                            boundingBox = null,
                            confidence = confidence / 100f
                        )
                    }

                val totalChars = blocks.sumOf { it.text.length }
                Log.d(TAG, "Arabic OCR completed - found ${blocks.size} text blocks ($totalChars chars), confidence: $confidence%")
                blocks
            } catch (e: Exception) {
                Log.e(TAG, "Arabic OCR failed: ${e.message}", e)
                // Recycle processed bitmap on error
                if (processedBitmap !== bitmap && !processedBitmap.isRecycled) {
                    processedBitmap.recycle()
                }
                throw e
            } finally {
                api.clear()
            }
        }
    }

    /**
     * Clean up common Tesseract Arabic OCR artifacts:
     * - Remove lines that are only punctuation/symbols
     * - Collapse multiple spaces
     * - Remove isolated single characters that are likely noise
     */
    private fun cleanArabicOcrOutput(text: String): String {
        return text.lines()
            .map { line -> line.replace(Regex("\\s{2,}"), " ").trim() }
            .filter { line ->
                // Keep lines that have at least 2 Arabic characters or meaningful content
                line.length >= 2 && (
                    line.count { it in '\u0600'..'\u06FF' || it in '\u0750'..'\u077F' } >= 2 ||
                    line.count { it.isLetterOrDigit() } >= 3
                )
            }
            .joinToString("\n")
    }

    override fun close() {
        try {
            tessApi?.recycle()
            tessApi = null
            isInitialized = false
            Log.d(TAG, "Arabic OCR engine closed")
        } catch (e: Exception) {
            Log.w(TAG, "Error closing Arabic OCR: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "NabdScreenTranslate"
        private const val LANG_ARABIC = "ara"
    }
}
