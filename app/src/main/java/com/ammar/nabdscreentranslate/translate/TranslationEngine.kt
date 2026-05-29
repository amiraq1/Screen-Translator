package com.ammar.nabdscreentranslate.translate

interface TranslationEngine {
    suspend fun translate(text: String, sourceLang: String, targetLang: String): TranslationResult
    suspend fun downloadModel(langCode: String): TranslationResult
    suspend fun isModelDownloaded(langCode: String): Boolean
    fun close()
}
