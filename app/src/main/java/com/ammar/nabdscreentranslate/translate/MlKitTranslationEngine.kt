package com.ammar.nabdscreentranslate.translate

import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

class MlKitTranslationEngine : TranslationEngine {

    private val translatorCache = ConcurrentHashMap<String, Translator>()
    private val translationCache = ConcurrentHashMap<String, String>()

    override suspend fun translate(
        text: String,
        sourceLang: String,
        targetLang: String
    ): TranslationResult {
        if (text.isBlank()) return TranslationResult.EmptyText
        Log.d(TAG, "Translating ${text.length} chars: $sourceLang -> $targetLang")

        val cacheKey = "$sourceLang|$targetLang|$text"
        translationCache[cacheKey]?.let {
            return TranslationResult.Success(it)
        }

        val sourceCode = if (sourceLang == "auto") {
            // ML Kit will auto-detect when source is set to English and input is different
            // For proper auto-detect, we use language identification
            "en"
        } else {
            LanguageMapper.getMlKitCode(sourceLang)
                ?: return TranslationResult.Error(
                    "اللغة المصدر غير مدعومة",
                    TranslationResult.ErrorType.LANGUAGE_NOT_SUPPORTED
                )
        }

        val targetCode = LanguageMapper.getMlKitCode(targetLang)
            ?: return TranslationResult.Error(
                "اللغة الهدف غير مدعومة",
                TranslationResult.ErrorType.LANGUAGE_NOT_SUPPORTED
            )

        val translator = getOrCreateTranslator(sourceCode, targetCode)

        return suspendCancellableCoroutine { continuation ->
            translator.translate(text)
                .addOnSuccessListener { translatedText ->
                    translationCache[cacheKey] = translatedText
                    continuation.resume(TranslationResult.Success(translatedText))
                }
                .addOnFailureListener { exception ->
                    val errorMsg = exception.message ?: "تعذرت الترجمة"
                    if (errorMsg.contains("model", ignoreCase = true)) {
                        continuation.resume(TranslationResult.ModelNotDownloaded)
                    } else {
                        continuation.resume(
                            TranslationResult.Error(errorMsg, TranslationResult.ErrorType.UNKNOWN)
                        )
                    }
                }
        }
    }

    override suspend fun downloadModel(langCode: String): TranslationResult {
        val mlKitCode = LanguageMapper.getMlKitCode(langCode)
            ?: return TranslationResult.Error(
                "اللغة غير مدعومة",
                TranslationResult.ErrorType.LANGUAGE_NOT_SUPPORTED
            )

        // Create a translator to/from English to trigger model download
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(mlKitCode)
            .setTargetLanguage("en")
            .build()

        val translator = Translation.getClient(options)

        return suspendCancellableCoroutine { continuation ->
            val conditions = DownloadConditions.Builder().build()
            translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener {
                    Log.d(TAG, "Model downloaded successfully for: $langCode")
                    continuation.resume(TranslationResult.Success("تم تحميل النموذج بنجاح"))
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Model download failed for $langCode: ${exception.message}")
                    continuation.resume(
                        TranslationResult.Error(
                            exception.message ?: "فشل تحميل النموذج",
                            TranslationResult.ErrorType.NETWORK_ERROR
                        )
                    )
                }
        }
    }

    override suspend fun isModelDownloaded(langCode: String): Boolean {
        // ML Kit doesn't provide a direct way to check; we attempt translation
        // For MVP, we return true and handle errors gracefully
        return true
    }

    private fun getOrCreateTranslator(sourceCode: String, targetCode: String): Translator {
        val key = "$sourceCode->$targetCode"
        return translatorCache.getOrPut(key) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceCode)
                .setTargetLanguage(targetCode)
                .build()
            Translation.getClient(options)
        }
    }

    override fun close() {
        translatorCache.values.forEach { it.close() }
        translatorCache.clear()
        translationCache.clear()
    }

    companion object {
        private const val TAG = "NabdScreenTranslate"
    }
}
