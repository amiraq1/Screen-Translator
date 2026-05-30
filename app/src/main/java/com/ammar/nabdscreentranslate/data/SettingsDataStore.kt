package com.ammar.nabdscreentranslate.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val SOURCE_LANG = stringPreferencesKey("source_lang")
        val TARGET_LANG = stringPreferencesKey("target_lang")
        val OVERLAY_OPACITY = floatPreferencesKey("overlay_opacity")
        val SAVE_HISTORY = booleanPreferencesKey("save_history")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val VIBRATE_ON_TRANSLATE = booleanPreferencesKey("vibrate_on_translate")
        val LIGHT_BG_BEHIND_TRANSLATION = booleanPreferencesKey("light_bg_behind_translation")
    }

    val sourceLang: Flow<String> = context.dataStore.data.map { it[Keys.SOURCE_LANG] ?: "auto" }
    val targetLang: Flow<String> = context.dataStore.data.map { it[Keys.TARGET_LANG] ?: "ar" }
    val overlayOpacity: Flow<Float> = context.dataStore.data.map { it[Keys.OVERLAY_OPACITY] ?: 0.95f }
    val saveHistory: Flow<Boolean> = context.dataStore.data.map { it[Keys.SAVE_HISTORY] ?: true }
    val darkMode: Flow<Boolean> = context.dataStore.data.map { it[Keys.DARK_MODE] ?: true }
    val vibrateOnTranslate: Flow<Boolean> = context.dataStore.data.map { it[Keys.VIBRATE_ON_TRANSLATE] ?: true }

    /** Whether to draw a faint translucent background behind in-place translated text. Default OFF (cardless). */
    val lightBackgroundBehindTranslation: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.LIGHT_BG_BEHIND_TRANSLATION] ?: false }

    suspend fun setSourceLang(lang: String) {
        context.dataStore.edit { it[Keys.SOURCE_LANG] = lang }
    }

    suspend fun setTargetLang(lang: String) {
        context.dataStore.edit { it[Keys.TARGET_LANG] = lang }
    }

    suspend fun setOverlayOpacity(opacity: Float) {
        context.dataStore.edit { it[Keys.OVERLAY_OPACITY] = opacity }
    }

    suspend fun setSaveHistory(save: Boolean) {
        context.dataStore.edit { it[Keys.SAVE_HISTORY] = save }
    }

    suspend fun setDarkMode(dark: Boolean) {
        context.dataStore.edit { it[Keys.DARK_MODE] = dark }
    }

    suspend fun setVibrateOnTranslate(vibrate: Boolean) {
        context.dataStore.edit { it[Keys.VIBRATE_ON_TRANSLATE] = vibrate }
    }

    suspend fun setLightBackgroundBehindTranslation(enabled: Boolean) {
        context.dataStore.edit { it[Keys.LIGHT_BG_BEHIND_TRANSLATION] = enabled }
    }
}
