package com.ammar.nabdscreentranslate.domain

import com.ammar.nabdscreentranslate.data.TranslationHistoryDao
import com.ammar.nabdscreentranslate.data.TranslationHistoryEntity

class SaveTranslationUseCase(
    private val dao: TranslationHistoryDao
) {
    suspend fun execute(
        sourceText: String,
        translatedText: String,
        sourceLang: String,
        targetLang: String,
        appName: String? = null
    ) {
        val entity = TranslationHistoryEntity(
            sourceText = sourceText,
            translatedText = translatedText,
            sourceLang = sourceLang,
            targetLang = targetLang,
            appName = appName
        )
        dao.insert(entity)
    }
}
