package com.ammar.nabdscreentranslate.translate

import android.util.Log

/**
 * Post-translation Arabic text polisher.
 * Cleans up ML Kit translation output to produce more natural, readable Arabic.
 *
 * Applied after translation and before overlay display / history save.
 * Does NOT modify the translation engine itself.
 */
class ArabicTextPolisher {

    companion object {
        private const val TAG = "NabdScreenTranslate"

        /** Product names that should never be altered. */
        private val PRESERVED_NAMES = setOf(
            "ChatGPT", "Google", "Wikipedia", "Android", "Kotlin",
            "GitHub", "YouTube", "ML Kit", "Tesseract", "iPhone",
            "WhatsApp", "Instagram", "Facebook", "Twitter", "Telegram",
            "OpenAI", "Microsoft", "Apple", "Samsung", "Linux",
            "Python", "JavaScript", "TypeScript", "Flutter", "React"
        )

        /** Common Arabic corrections map. */
        private val CORRECTIONS = mapOf(
            "الذكاء الإصطناعي" to "الذكاء الاصطناعي",
            "الإصطناعي" to "الاصطناعي",
            "الأنكليزي" to "الإنجليزي",
            "الإنكليزي" to "الإنجليزي",
            "الأنكليزية" to "الإنجليزية",
            "الإنكليزية" to "الإنجليزية",
            "انكليزي" to "إنجليزي",
            "انجليزي" to "إنجليزي",
            "انكليزية" to "إنجليزية",
            "جوجل" to "Google",
            "شات جي بي تي" to "ChatGPT",
            "تشات جي بي تي" to "ChatGPT",
            "شات جيبيتي" to "ChatGPT",
            "ويكيبديا" to "ويكيبيديا",
            "اندرويد" to "Android",
            "أندرويد" to "Android",
            "يوتيوب" to "YouTube",
            "جيت هاب" to "GitHub",
            "جيتهاب" to "GitHub",
            "انقر" to "اضغط",
            "أنقر" to "اضغط",
            "إنقر" to "اضغط",
            "اسطر" to "أسطر",
            "اربعه" to "أربعة",
            "ثلاثه" to "ثلاثة",
            "مهمه" to "مهمة",
            "كبيره" to "كبيرة",
            "صغيره" to "صغيرة",
            "جديده" to "جديدة",
            "قديمه" to "قديمة",
            "مميزه" to "مميزة",
            "رائعه" to "رائعة",
            "جميله" to "جميلة",
            "طويله" to "طويلة",
            "قصيره" to "قصيرة"
        )

        /** Style improvements: verbose → concise. */
        private val STYLE_REPLACEMENTS = listOf(
            Pair("قم بكتابة", "اكتب"),
            Pair("قم بالكتابة", "اكتب"),
            Pair("قم بالضغط", "اضغط"),
            Pair("قم بإدخال", "أدخل"),
            Pair("قم بفتح", "افتح"),
            Pair("قم بإغلاق", "أغلق"),
            Pair("قم بحذف", "احذف"),
            Pair("قم بنسخ", "انسخ"),
            Pair("قم بلصق", "الصق"),
            Pair("قم بتحديد", "حدد"),
            Pair("قم بتحميل", "حمّل"),
            Pair("قم بتنزيل", "نزّل"),
            Pair("قم بإرسال", "أرسل"),
            Pair("قم بمشاركة", "شارك"),
            Pair("من فضلك قم بـ", "يرجى "),
            Pair("من فضلك قم ب", "يرجى "),
            Pair("يرجى منك أن تقوم بـ", "يرجى "),
            Pair("يرجى منك أن تقوم ب", "يرجى "),
            Pair("النص الذي هو", "النص"),
            Pair("الذي هو عبارة عن", "وهو"),
            Pair("هذا هو عبارة عن", "هذا"),
            Pair("وذلك من أجل أن", "لكي"),
            Pair("وذلك من أجل", "من أجل")
        )

        /** Punctuation that should have no space before it. */
        private val PUNCT_NO_SPACE_BEFORE = charArrayOf('،', '.', '؟', '!', ':', '؛', ')', ']', '}')

        /** Punctuation that should have a space after it (if followed by a letter). */
        private val PUNCT_SPACE_AFTER = charArrayOf('،', '.', '؟', '!', ':', '؛')
    }

    data class PolishResult(
        val text: String,
        val correctionsCount: Int
    )

    /**
     * Main entry point: polishes Arabic text.
     * Returns the polished text and the number of corrections applied.
     */
    fun polish(text: String): PolishResult {
        if (text.isBlank()) return PolishResult(text, 0)

        // Short text (1-2 words): minimal processing
        val wordCount = text.trim().split("\\s+".toRegex()).size
        if (wordCount <= 2) {
            val result = applyCorrections(text)
            return PolishResult(result.first.trim(), result.second)
        }

        var current = text
        var totalCorrections = 0

        // 1. Apply dictionary corrections
        val (corrected, corrCount) = applyCorrections(current)
        current = corrected
        totalCorrections += corrCount

        // 2. Apply style improvements
        val (styled, styleCount) = polishStyle(current)
        current = styled
        totalCorrections += styleCount

        // 3. Apply contextual grammar rules
        val (grammared, grammarCount) = applyGrammarRules(current)
        current = grammared
        totalCorrections += grammarCount

        // 4. Fix punctuation
        current = fixPunctuation(current)

        // 5. Clean spacing
        current = cleanSpacing(current)

        // 6. Clean artifacts
        current = cleanArtifacts(current)

        // 7. Remove unnecessary comma before prepositions
        current = fixCommaBeforePreposition(current)

        // 8. Fix sentence start/end (only for longer text)
        if (wordCount > 5) {
            current = fixSentenceBoundaries(current)
        }

        val finalText = current.trim()

        Log.d(TAG, "ArabicPolish: before=${text.length} chars, after=${finalText.length} chars, corrections=$totalCorrections")
        return PolishResult(finalText, totalCorrections)
    }

    /**
     * Applies dictionary corrections.
     */
    private fun applyCorrections(text: String): Pair<String, Int> {
        var result = text
        var count = 0

        for ((wrong, correct) in CORRECTIONS) {
            if (result.contains(wrong)) {
                result = result.replace(wrong, correct)
                count++
            }
        }

        return Pair(result, count)
    }

    /**
     * Applies style improvements for more natural Arabic.
     */
    fun polishStyle(text: String): Pair<String, Int> {
        var result = text
        var count = 0

        for ((verbose, concise) in STYLE_REPLACEMENTS) {
            if (result.contains(verbose)) {
                result = result.replace(verbose, concise)
                count++
            }
        }

        return Pair(result, count)
    }

    /**
     * Fixes Arabic punctuation:
     * - Replace ? with ؟ in Arabic context
     * - Replace , with ، in Arabic context
     * - Remove repeated punctuation
     */
    private fun fixPunctuation(text: String): String {
        var result = text

        // Replace Western punctuation with Arabic equivalents when surrounded by Arabic
        result = result.replace(Regex("(\\p{InArabic})\\s*\\?"), "$1؟")
        result = result.replace(Regex("(\\p{InArabic})\\s*,\\s*(\\p{InArabic})"), "$1، $2")

        // Remove repeated punctuation (؟؟ → ؟, !! → !)
        result = result.replace(Regex("([؟?!.،؛:])\\1+"), "$1")

        return result
    }

    /**
     * Cleans up spacing issues.
     */
    private fun cleanSpacing(text: String): String {
        var result = text

        // Remove multiple spaces
        result = result.replace(Regex(" {2,}"), " ")

        // Remove space before punctuation
        for (p in PUNCT_NO_SPACE_BEFORE) {
            result = result.replace(" $p", "$p")
        }

        // Add space after punctuation if followed directly by a letter
        for (p in PUNCT_SPACE_AFTER) {
            result = result.replace(Regex("\\$p(\\p{L})"), "$p $1")
        }

        // Remove leading/trailing spaces per line
        result = result.lines().joinToString("\n") { it.trim() }

        return result
    }

    /**
     * Removes OCR/translation artifacts.
     */
    private fun cleanArtifacts(text: String): String {
        var result = text

        // Remove isolated special characters that don't belong
        result = result.replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]"), "")

        // Remove empty lines (more than one consecutive newline)
        result = result.replace(Regex("\n{3,}"), "\n\n")

        // Remove lines that are only punctuation/symbols
        result = result.lines()
            .filter { line -> line.isBlank() || line.any { it.isLetterOrDigit() } }
            .joinToString("\n")

        return result
    }

    /**
     * Fixes sentence boundaries:
     * - Trim leading whitespace/lowercase issues
     * - Add period at end of long text if missing punctuation
     */
    private fun fixSentenceBoundaries(text: String): String {
        var result = text.trim()

        // If text doesn't end with punctuation and is long enough, add period
        if (result.isNotEmpty()) {
            val lastChar = result.last()
            val endsWithPunct = lastChar in charArrayOf('.', '؟', '!', '،', '؛', ':', '…')
            if (!endsWithPunct && result.length > 20) {
                result = "$result."
            }
        }

        return result
    }

    /**
     * Applies contextual grammar rules:
     * - "اكتب مقال إنجليزي قصير" → "اكتب مقالًا إنجليزيًا قصيرًا"
     */
    private fun applyGrammarRules(text: String): Pair<String, Int> {
        var result = text
        var count = 0

        // "اكتب مقال" pattern → add tanween for accusative (مفعول به)
        if (result.contains("اكتب مقال")) {
            result = result.replace("اكتب مقال إنجليزي قصير", "اكتب مقالًا إنجليزيًا قصيرًا")
            result = result.replace("اكتب مقال عربي قصير", "اكتب مقالًا عربيًا قصيرًا")
            result = result.replace("اكتب مقال قصير", "اكتب مقالًا قصيرًا")
            result = result.replace("اكتب مقال طويل", "اكتب مقالًا طويلًا")
            // Generic: "اكتب مقال" without specific adjective
            if (result.contains("اكتب مقال") && !result.contains("اكتب مقالًا")) {
                result = result.replace("اكتب مقال", "اكتب مقالًا")
            }
            count++
        }

        return Pair(result, count)
    }

    /**
     * Removes unnecessary comma before prepositions like من، بـ، في، على
     * when preceded by an adjective (e.g., "قصيرًا، من" → "قصيرًا من")
     */
    private fun fixCommaBeforePreposition(text: String): String {
        var result = text
        // Remove comma before من/بـ/في/على when it follows an adjective or noun
        result = result.replace(Regex("([ًٌٍَُِّْا-ي])،\\s*(من|بـ|في|على|إلى)\\s"), "$1 $2 ")
        // Also handle without tanween
        result = result.replace(Regex("(قصير|طويل|جديد|قديم|كبير|صغير)،\\s*(من|بـ|في|على|إلى)"), "$1 $2")
        return result
    }
}
