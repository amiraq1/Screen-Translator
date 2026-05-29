package com.ammar.nabdscreentranslate.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ammar.nabdscreentranslate.data.SettingsDataStore
import com.ammar.nabdscreentranslate.translate.MlKitTranslationEngine
import com.ammar.nabdscreentranslate.translate.TranslationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class HomeUiState(
    val sourceLang: String = "auto",
    val targetLang: String = "ar",
    val isFloatingActive: Boolean = false,
    val isDownloadingModel: Boolean = false,
    val downloadMessage: String? = null,
    val hasOverlayPermission: Boolean = false,
    val hasMediaProjectionPermission: Boolean = false
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsDataStore = SettingsDataStore(application)
    private val translationEngine = MlKitTranslationEngine()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val sourceLang = settingsDataStore.sourceLang.first()
            val targetLang = settingsDataStore.targetLang.first()
            _uiState.value = _uiState.value.copy(
                sourceLang = sourceLang,
                targetLang = targetLang
            )
        }
    }

    fun setSourceLang(lang: String) {
        _uiState.value = _uiState.value.copy(sourceLang = lang)
        viewModelScope.launch { settingsDataStore.setSourceLang(lang) }
    }

    fun setTargetLang(lang: String) {
        _uiState.value = _uiState.value.copy(targetLang = lang)
        viewModelScope.launch { settingsDataStore.setTargetLang(lang) }
    }

    fun setFloatingActive(active: Boolean) {
        _uiState.value = _uiState.value.copy(isFloatingActive = active)
    }

    fun setOverlayPermission(granted: Boolean) {
        _uiState.value = _uiState.value.copy(hasOverlayPermission = granted)
    }

    fun downloadModels() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDownloadingModel = true, downloadMessage = null)

            val targetLang = _uiState.value.targetLang
            val sourceLang = _uiState.value.sourceLang

            // Download target language model
            val targetResult = translationEngine.downloadModel(targetLang)

            // Download source language model if not auto
            if (sourceLang != "auto") {
                translationEngine.downloadModel(sourceLang)
            }

            val message = when (targetResult) {
                is TranslationResult.Success -> "تم تحميل النماذج بنجاح ✓"
                is TranslationResult.Error -> "فشل التحميل: ${targetResult.message}"
                else -> "حدث خطأ غير متوقع"
            }

            _uiState.value = _uiState.value.copy(
                isDownloadingModel = false,
                downloadMessage = message
            )
        }
    }

    fun clearDownloadMessage() {
        _uiState.value = _uiState.value.copy(downloadMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        translationEngine.close()
    }
}
