package com.ammar.nabdscreentranslate.domain

import android.graphics.Bitmap
import android.graphics.Rect
import com.ammar.nabdscreentranslate.core.utils.BitmapUtils
import com.ammar.nabdscreentranslate.ocr.OcrEngine
import com.ammar.nabdscreentranslate.ocr.TextBlockResult
import com.ammar.nabdscreentranslate.translate.TranslationEngine
import com.ammar.nabdscreentranslate.translate.TranslationResult

data class ScreenTranslationResult(
    val originalText: String,
    val translatedText: String,
    val blocks: List<TranslatedBlock>
)

data class TranslatedBlock(
    val originalText: String,
    val translatedText: String,
    val boundingBox: Rect?
)

class TranslateScreenUseCase(
    private val ocrEngine: OcrEngine,
    private val translationEngine: TranslationEngine
) {

    suspend fun execute(
        bitmap: Bitmap,
        sourceLang: String,
        targetLang: String,
        region: Rect? = null
    ): TranslationResult {
        // Crop to region if specified
        val targetBitmap = if (region != null) {
            BitmapUtils.cropRegion(bitmap, region)
        } else {
            bitmap
        }

        // Prepare bitmap for OCR
        val preparedBitmap = BitmapUtils.prepareForOcr(targetBitmap)

        // Run OCR
        val textBlocks: List<TextBlockResult>
        try {
            textBlocks = ocrEngine.recognizeText(preparedBitmap)
        } catch (e: Exception) {
            return TranslationResult.Error(
                "فشل استخراج النص: ${e.message}",
                TranslationResult.ErrorType.UNKNOWN
            )
        }

        if (textBlocks.isEmpty()) {
            return TranslationResult.EmptyText
        }

        // Combine all text
        val fullText = textBlocks.joinToString("\n") { it.text }

        // Translate
        val result = translationEngine.translate(fullText, sourceLang, targetLang)

        return result
    }

    suspend fun executeWithBlocks(
        bitmap: Bitmap,
        sourceLang: String,
        targetLang: String,
        region: Rect? = null
    ): Result<ScreenTranslationResult> {
        val targetBitmap = if (region != null) {
            BitmapUtils.cropRegion(bitmap, region)
        } else {
            bitmap
        }

        val preparedBitmap = BitmapUtils.prepareForOcr(targetBitmap)

        val textBlocks: List<TextBlockResult>
        try {
            textBlocks = ocrEngine.recognizeText(preparedBitmap)
        } catch (e: Exception) {
            return Result.failure(e)
        }

        if (textBlocks.isEmpty()) {
            return Result.failure(Exception("لم يتم العثور على نص واضح في الشاشة."))
        }

        val fullText = textBlocks.joinToString("\n") { it.text }
        val translationResult = translationEngine.translate(fullText, sourceLang, targetLang)

        return when (translationResult) {
            is TranslationResult.Success -> {
                val translatedLines = translationResult.translatedText.split("\n")
                val blocks: List<TranslatedBlock> = textBlocks.mapIndexed { index, block ->
                    TranslatedBlock(
                        originalText = block.text,
                        translatedText = translatedLines.getOrElse(index) { translationResult.translatedText },
                        boundingBox = block.boundingBox
                    )
                }
                Result.success(
                    ScreenTranslationResult(
                        originalText = fullText,
                        translatedText = translationResult.translatedText,
                        blocks = blocks
                    )
                )
            }
            is TranslationResult.Error -> Result.failure(Exception(translationResult.message))
            TranslationResult.ModelNotDownloaded -> Result.failure(Exception("تعذرت الترجمة. تأكد من تحميل نموذج اللغة وحاول مرة أخرى."))
            TranslationResult.EmptyText -> Result.failure(Exception("لم يتم العثور على نص واضح في الشاشة."))
        }
    }
}
