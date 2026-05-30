package com.ammar.nabdscreentranslate.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ammar.nabdscreentranslate.data.SettingsDataStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val overlayOpacity: Float = 0.95f,
    val saveHistory: Boolean = true,
    val darkMode: Boolean = true,
    val vibrateOnTranslate: Boolean = true,
    val lightBgBehindTranslation: Boolean = false,
    val sourceLang: String = "auto",
    val targetLang: String = "ar"
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsDataStore = SettingsDataStore(application)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsDataStore.overlayOpacity,
        settingsDataStore.saveHistory,
        settingsDataStore.darkMode,
        settingsDataStore.vibrateOnTranslate,
        settingsDataStore.sourceLang,
    ) { opacity, saveHistory, darkMode, vibrate, sourceLang ->
        SettingsUiState(
            overlayOpacity = opacity,
            saveHistory = saveHistory,
            darkMode = darkMode,
            vibrateOnTranslate = vibrate,
            sourceLang = sourceLang,
            targetLang = "ar" // Will be combined separately
        )
    }.combine(settingsDataStore.targetLang) { state, targetLang ->
        state.copy(targetLang = targetLang)
    }.combine(settingsDataStore.lightBackgroundBehindTranslation) { state, lightBg ->
        state.copy(lightBgBehindTranslation = lightBg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setLightBgBehindTranslation(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setLightBackgroundBehindTranslation(enabled) }
    }

    fun setOverlayOpacity(opacity: Float) {
        viewModelScope.launch { settingsDataStore.setOverlayOpacity(opacity) }
    }

    fun setSaveHistory(save: Boolean) {
        viewModelScope.launch { settingsDataStore.setSaveHistory(save) }
    }

    fun setDarkMode(dark: Boolean) {
        viewModelScope.launch { settingsDataStore.setDarkMode(dark) }
    }

    fun setVibrateOnTranslate(vibrate: Boolean) {
        viewModelScope.launch { settingsDataStore.setVibrateOnTranslate(vibrate) }
    }
}
