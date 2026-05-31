package com.ammar.nabdscreentranslate.domain

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import com.ammar.nabdscreentranslate.core.utils.BitmapUtils
import com.ammar.nabdscreentranslate.ocr.OcrEngine
import com.ammar.nabdscreentranslate.ocr.TextBlockResult
import com.ammar.nabdscreentranslate.overlay.BubblePrioritizer
import com.ammar.nabdscreentranslate.overlay.NoiseFilter
import com.ammar.nabdscreentranslate.overlay.TextBlockGrouper
import com.ammar.nabdscreentranslate.translate.TranslationEngine
import com.ammar.nabdscreentranslate.translate.TranslationResult
import com.ammar.nabdscreentranslate.translate.ArabicTextPolisher
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
    val translatedText: String,
    /** Overflow groups that didn't make it to inline bubbles (for bottom sheet). */
    val overflowBlocks: List<InPlaceBlock> = emptyList()
) {
    /** True if at least one block has positioning info usable for in-place rendering. */
    val hasPositions: Boolean get() = blocks.any { it.boundingBox != null }
}

class TranslateScreenUseCase(
    private val ocrEngine: OcrEngine,
    private val translationEngine: TranslationEngine
) {
    companion object {
        private const val TAG = "NabdScreenTranslate"
    }

    private val noiseFilter = NoiseFilter()
    private val grouper = TextBlockGrouper()
    private val arabicPolisher = ArabicTextPolisher()

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
     * Pipeline (when declutter enabled):
     * 1. OCR → raw text blocks
     * 2. Noise filtering → remove short/irrelevant/UI text
     * 3. Grouping → merge nearby blocks into paragraphs
     * 4. Prioritization → select top MAX_INLINE_BUBBLES groups
     * 5. Translation → translate each GROUP (not each block) once
     * 6. Return inline + overflow blocks
     *
     * @param screenWidth  full screen width in pixels (overlay coordinate space)
     * @param screenHeight full screen height in pixels
     * @param declutterEnabled whether to apply grouping/filtering (from settings)
     */
    suspend fun executeInPlace(
        bitmap: Bitmap,
        sourceLang: String,
        targetLang: String,
        screenWidth: Int,
        screenHeight: Int,
        region: Rect? = null,
        declutterEnabled: Boolean = true,
        polishArabicEnabled: Boolean = true
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

        Log.d(TAG, "Pipeline: raw blocks count = ${textBlocks.size}")

        // Scale factor: OCR bounding boxes are in preparedBitmap space.
        // Map them to the cropped-region space, then to full screen space.
        val regionWidth = region?.width() ?: screenWidth
        val regionHeight = region?.height() ?: screenHeight
        val scaleX = regionWidth.toFloat() / preparedBitmap.width
        val scaleY = regionHeight.toFloat() / preparedBitmap.height
        val offsetX = region?.left ?: 0
        val offsetY = region?.top ?: 0

        // Scale bounding boxes to screen coordinates for all blocks
        val scaledBlocks = textBlocks.map { block ->
            val screenBox = block.boundingBox?.let { b ->
                Rect(
                    (b.left * scaleX).toInt() + offsetX,
                    (b.top * scaleY).toInt() + offsetY,
                    (b.right * scaleX).toInt() + offsetX,
                    (b.bottom * scaleY).toInt() + offsetY
                )
            }
            TextBlockResult(block.text, screenBox, block.confidence)
        }

        if (!declutterEnabled) {
            // Legacy behavior: translate each block individually
            return executeInPlaceLegacy(scaledBlocks, sourceLang, targetLang, polishArabicEnabled)
        }

        // ── PHASE 2: Noise filtering ──
        val filterResult = noiseFilter.filter(scaledBlocks)
        val filteredBlocks = filterResult.passed
        Log.d(TAG, "Pipeline: filtered blocks count = ${filteredBlocks.size}, skipped noise = ${filterResult.skippedNoiseCount}")

        if (filteredBlocks.isEmpty()) {
            return Result.failure(Exception("لم يتم العثور على نص واضح في الشاشة."))
        }

        // ── PHASE 3: Grouping ──
        val groups = grouper.group(filteredBlocks)
        // Filter out groups that are themselves noise after merging
        val validGroups = groups.filter { !noiseFilter.isGroupNoise(it.mergedText) }
        Log.d(TAG, "Pipeline: grouped blocks count = ${validGroups.size}")

        if (validGroups.isEmpty()) {
            return Result.failure(Exception("لم يتم العثور على نص واضح في الشاشة."))
        }

        // ── PHASE 4: Prioritization ──
        val prioritizer = BubblePrioritizer(screenWidth, screenHeight)
        val prioritized = prioritizer.prioritize(validGroups)
        Log.d(TAG, "Pipeline: inline bubbles = ${prioritized.inlineBubbles.size}, overflow = ${prioritized.overflowGroups.size}")

        // ── PHASE 5: Translation (per group, in parallel) ──
        val inlineBlocks: List<InPlaceBlock> = coroutineScope {
            prioritized.inlineBubbles.map { group ->
                async {
                    val translated = when (val r = translationEngine.translate(group.mergedText, sourceLang, targetLang)) {
                        is TranslationResult.Success -> polishIfEnabled(r.translatedText, targetLang, polishArabicEnabled)
                        else -> group.mergedText
                    }
                    InPlaceBlock(
                        originalText = group.mergedText,
                        translatedText = translated,
                        boundingBox = group.mergedBoundingBox
                    )
                }
            }.awaitAll()
        }

        val overflowBlocks: List<InPlaceBlock> = coroutineScope {
            prioritized.overflowGroups.map { group ->
                async {
                    val translated = when (val r = translationEngine.translate(group.mergedText, sourceLang, targetLang)) {
                        is TranslationResult.Success -> polishIfEnabled(r.translatedText, targetLang, polishArabicEnabled)
                        else -> group.mergedText
                    }
                    InPlaceBlock(
                        originalText = group.mergedText,
                        translatedText = translated,
                        boundingBox = group.mergedBoundingBox
                    )
                }
            }.awaitAll()
        }

        val allBlocks = inlineBlocks + overflowBlocks
        val fullOriginal = allBlocks.joinToString("\n") { it.originalText }
        val fullTranslated = allBlocks.joinToString("\n") { it.translatedText }

        Log.d(TAG, "Pipeline: inline bubbles shown = ${inlineBlocks.size}, sheet items = ${overflowBlocks.size}")

        return Result.success(
            InPlaceTranslationResult(
                blocks = inlineBlocks,
                originalText = fullOriginal,
                translatedText = fullTranslated,
                overflowBlocks = overflowBlocks
            )
        )
    }

    /**
     * Legacy per-block translation without grouping/filtering.
     * Used when declutter mode is OFF.
     */
    private suspend fun executeInPlaceLegacy(
        scaledBlocks: List<TextBlockResult>,
        sourceLang: String,
        targetLang: String,
        polishArabicEnabled: Boolean = true
    ): Result<InPlaceTranslationResult> {
        val translatedBlocks: List<InPlaceBlock> = coroutineScope {
            scaledBlocks.map { block ->
                async {
                    val translated = when (val r = translationEngine.translate(block.text, sourceLang, targetLang)) {
                        is TranslationResult.Success -> polishIfEnabled(r.translatedText, targetLang, polishArabicEnabled)
                        else -> block.text
                    }
                    InPlaceBlock(
                        originalText = block.text,
                        translatedText = translated,
                        boundingBox = block.boundingBox
                    )
                }
            }.awaitAll()
        }

        val fullOriginal = scaledBlocks.joinToString("\n") { it.text }
        val fullTranslated = translatedBlocks.joinToString("\n") { it.translatedText }

        return Result.success(
            InPlaceTranslationResult(
                blocks = translatedBlocks,
                originalText = fullOriginal,
                translatedText = fullTranslated
            )
        )
    }

    /**
     * Applies Arabic text polishing if enabled and target language is Arabic.
     */
    private fun polishIfEnabled(text: String, targetLang: String, enabled: Boolean): String {
        if (!enabled) {
            Log.d(TAG, "ArabicPolish: polish skipped (disabled)")
            return text
        }
        // Only polish if target is Arabic
        if (targetLang != "ar") return text

        val result = arabicPolisher.polish(text)
        return result.text
    }
}
