package com.ammar.nabdscreentranslate.overlay

import android.util.Log
import com.ammar.nabdscreentranslate.ocr.TextBlockResult

/**
 * Filters out noise text blocks that would produce unhelpful translation bubbles.
 *
 * Filters:
 * - Text shorter than MIN_CHARS characters
 * - Text that is only numbers
 * - Text that is only symbols/punctuation
 * - Text with low OCR confidence
 * - Text matching the UI blacklist (common UI labels)
 *
 * A block is NOT filtered if it's part of a larger group (handled at group level).
 */
class NoiseFilter {

    companion object {
        private const val TAG = "NabdScreenTranslate"
        private const val MIN_CHARS = 4
        private const val MIN_CONFIDENCE = 0.4f

        /**
         * UI labels to ignore when they appear as standalone blocks.
         * These are common interface elements that don't need translation.
         */
        val UI_BLACKLIST = setOf(
            // Arabic UI elements
            "chatgpt", "نسخ", "حفظ", "إخفاء", "التفكير", "إرسال",
            // English UI elements
            "share", "copy", "like", "comment", "reply", "thinking",
            "send", "save", "hide", "close", "cancel", "ok", "yes", "no",
            "menu", "back", "next", "done", "edit", "delete", "more",
            "search", "home", "settings", "notifications",
            // Common app labels
            "gpt", "ai", "bot"
        )

        private val ONLY_NUMBERS = Regex("^[\\d٠-٩.,/%:]+$")
        private val ONLY_SYMBOLS = Regex("^[^\\p{L}\\p{N}]+$")
    }

    data class FilterResult(
        val passed: List<TextBlockResult>,
        val rejected: List<TextBlockResult>,
        val skippedNoiseCount: Int
    )

    /**
     * Filters individual blocks before grouping.
     * Returns blocks that pass the noise filter.
     */
    fun filter(blocks: List<TextBlockResult>): FilterResult {
        val passed = mutableListOf<TextBlockResult>()
        val rejected = mutableListOf<TextBlockResult>()
        var noiseCount = 0

        for (block in blocks) {
            val reason = getRejectReason(block)
            if (reason != null) {
                rejected.add(block)
                noiseCount++
                Log.d(TAG, "NoiseFilter: rejected '${block.text.take(30)}' — $reason")
            } else {
                passed.add(block)
            }
        }

        Log.d(TAG, "NoiseFilter: ${blocks.size} blocks → ${passed.size} passed, $noiseCount noise")
        return FilterResult(passed, rejected, noiseCount)
    }

    /**
     * Filters a merged group text. Used after grouping to check if the
     * entire group is just noise (e.g., a group of only short UI labels).
     */
    fun isGroupNoise(mergedText: String): Boolean {
        val trimmed = mergedText.trim()
        if (trimmed.length < MIN_CHARS) return true
        if (ONLY_NUMBERS.matches(trimmed)) return true
        if (ONLY_SYMBOLS.matches(trimmed)) return true
        // Don't apply blacklist to merged groups — they contain multiple words
        return false
    }

    private fun getRejectReason(block: TextBlockResult): String? {
        val text = block.text.trim()

        // Too short
        if (text.length < MIN_CHARS) return "too_short (${text.length} chars)"

        // Only numbers
        if (ONLY_NUMBERS.matches(text)) return "only_numbers"

        // Only symbols
        if (ONLY_SYMBOLS.matches(text)) return "only_symbols"

        // Low confidence
        block.confidence?.let { conf ->
            if (conf < MIN_CONFIDENCE) return "low_confidence ($conf)"
        }

        // UI blacklist — case-insensitive match for standalone words
        if (text.lowercase() in UI_BLACKLIST) return "ui_blacklist"

        return null
    }
}
