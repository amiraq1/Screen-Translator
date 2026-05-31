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
    val displayMode: String = SettingsDataStore.DISPLAY_MODE_OVERLAY,
    val sourceLang: String = "auto",
    val targetLang: String = "ar",
    val declutterOverlay: Boolean = true,
    val polishArabic: Boolean = true,
    val liveTranslation: Boolean = false,
    val liveInterval: String = SettingsDataStore.LIVE_INTERVAL_BALANCED,
    val liveOnlyOnChange: Boolean = true
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
    }.combine(settingsDataStore.displayMode) { state, mode ->
        state.copy(displayMode = mode)
    }.combine(settingsDataStore.declutterOverlay) { state, declutter ->
        state.copy(declutterOverlay = declutter)
    }.combine(settingsDataStore.polishArabic) { state, polish ->
        state.copy(polishArabic = polish)
    }.combine(settingsDataStore.liveTranslation) { state, live ->
        state.copy(liveTranslation = live)
    }.combine(settingsDataStore.liveInterval) { state, interval ->
        state.copy(liveInterval = interval)
    }.combine(settingsDataStore.liveOnlyOnChange) { state, onlyOnChange ->
        state.copy(liveOnlyOnChange = onlyOnChange)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setDisplayMode(mode: String) {
        viewModelScope.launch { settingsDataStore.setDisplayMode(mode) }
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

    fun setDeclutterOverlay(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setDeclutterOverlay(enabled) }
    }

    fun setPolishArabic(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setPolishArabic(enabled) }
    }

    fun setLiveTranslation(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setLiveTranslation(enabled) }
    }

    fun setLiveInterval(interval: String) {
        viewModelScope.launch { settingsDataStore.setLiveInterval(interval) }
    }

    fun setLiveOnlyOnChange(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setLiveOnlyOnChange(enabled) }
    }
}
