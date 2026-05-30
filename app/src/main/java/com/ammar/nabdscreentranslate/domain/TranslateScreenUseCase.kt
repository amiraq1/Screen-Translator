package com.ammar.nabdscreentranslate.domain

import android.graphics.Bitmap
import android.graphics.Rect
import com.ammar.nabdscreentranslate.core.utils.BitmapUtils
import com.ammar.nabdscreentranslate.ocr.OcrEngine
import com.ammar.nabdscreentranslate.ocr.TextBlockResult
import com.ammar.nabdscreentranslate.translate.TranslationEngine
import com.ammar.nabdscreentranslate.translate.TranslationResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

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

/** A translated block positioned in screen-pixel coordinates for in-place overlay. */
data class InPlaceBlock(
    val originalText: String,
    val translatedText: String,
    val boundingBox: Rect?
)

data class InPlaceTranslationResult(
    val blocks: List<InPlaceBlock>,
    val originalText: String,
    val translatedText: String
) {
    /** True if at least one block has positioning info usable for in-place rendering. */
    val hasPositions: Boolean get() = blocks.any { it.boundingBox != null }
}

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

    /**
     * Translates the screen and maps each text block's bounding box into
     * screen-pixel coordinates for in-place (Google Lens style) overlay rendering.
     *
     * Each block is translated individually (in parallel) so it can be drawn
     * over its original location. Bounding boxes from OCR are in the downscaled
     * OCR-bitmap space, so they are scaled back to the full screen space.
     *
     * @param screenWidth  full screen width in pixels (overlay coordinate space)
     * @param screenHeight full screen height in pixels
     */
    suspend fun executeInPlace(
        bitmap: Bitmap,
        sourceLang: String,
        targetLang: String,
        screenWidth: Int,
        screenHeight: Int,
        region: Rect? = null
    ): Result<InPlaceTranslationResult> {
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

        // Scale factor: OCR bounding boxes are in preparedBitmap space.
        // Map them to the cropped-region space, then to full screen space.
        val regionWidth = region?.width() ?: screenWidth
        val regionHeight = region?.height() ?: screenHeight
        val scaleX = regionWidth.toFloat() / preparedBitmap.width
        val scaleY = regionHeight.toFloat() / preparedBitmap.height
        val offsetX = region?.left ?: 0
        val offsetY = region?.top ?: 0

        // Translate each block individually, in parallel for speed.
        val translatedBlocks: List<InPlaceBlock> = coroutineScope {
            textBlocks.map { block ->
                async {
                    val translated = when (val r = translationEngine.translate(block.text, sourceLang, targetLang)) {
                        is TranslationResult.Success -> r.translatedText
                        else -> block.text // fall back to original on failure
                    }
                    val screenBox = block.boundingBox?.let { b ->
                        Rect(
                            (b.left * scaleX).toInt() + offsetX,
                            (b.top * scaleY).toInt() + offsetY,
                            (b.right * scaleX).toInt() + offsetX,
                            (b.bottom * scaleY).toInt() + offsetY
                        )
                    }
                    InPlaceBlock(
                        originalText = block.text,
                        translatedText = translated,
                        boundingBox = screenBox
                    )
                }
            }.awaitAll()
        }

        val fullOriginal = textBlocks.joinToString("\n") { it.text }
        val fullTranslated = translatedBlocks.joinToString("\n") { it.translatedText }

        return Result.success(
            InPlaceTranslationResult(
                blocks = translatedBlocks,
                originalText = fullOriginal,
                translatedText = fullTranslated
            )
        )
    }
}
