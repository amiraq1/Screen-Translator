package com.ammar.nabdscreentranslate.translate

import com.google.mlkit.nl.translate.TranslateLanguage

object LanguageMapper {

    data class LanguageInfo(
        val code: String,
        val mlKitCode: String,
        val displayName: String,
        val nativeName: String
    )

    val supportedLanguages = listOf(
        LanguageInfo("auto", "", "Auto Detect", "تلقائي"),
        LanguageInfo("en", TranslateLanguage.ENGLISH, "English", "الإنجليزية"),
        LanguageInfo("ar", TranslateLanguage.ARABIC, "Arabic", "العربية"),
        LanguageInfo("tr", TranslateLanguage.TURKISH, "Turkish", "التركية"),
        LanguageInfo("zh", TranslateLanguage.CHINESE, "Chinese", "الصينية"),
        LanguageInfo("ja", TranslateLanguage.JAPANESE, "Japanese", "اليابانية"),
        LanguageInfo("ko", TranslateLanguage.KOREAN, "Korean", "الكورية"),
        LanguageInfo("fr", TranslateLanguage.FRENCH, "French", "الفرنسية"),
        LanguageInfo("de", TranslateLanguage.GERMAN, "German", "الألمانية"),
    )

    val targetLanguages = supportedLanguages.filter { it.code != "auto" }

    fun getMlKitCode(code: String): String? {
        return supportedLanguages.find { it.code == code }?.mlKitCode?.takeIf { it.isNotEmpty() }
    }

    fun getDisplayName(code: String): String {
        return supportedLanguages.find { it.code == code }?.nativeName ?: code
    }
}
