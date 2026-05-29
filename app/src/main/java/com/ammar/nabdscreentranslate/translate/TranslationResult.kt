package com.ammar.nabdscreentranslate.translate

sealed class TranslationResult {
    data class Success(val translatedText: String) : TranslationResult()
    data class Error(val message: String, val type: ErrorType = ErrorType.UNKNOWN) : TranslationResult()
    data object ModelNotDownloaded : TranslationResult()
    data object EmptyText : TranslationResult()

    enum class ErrorType {
        MODEL_NOT_DOWNLOADED,
        NETWORK_ERROR,
        LANGUAGE_NOT_SUPPORTED,
        UNKNOWN
    }
}
