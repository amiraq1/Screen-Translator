package com.ammar.nabdscreentranslate.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ammar.nabdscreentranslate.data.AppDatabase
import com.ammar.nabdscreentranslate.data.TranslationHistoryEntity
import com.ammar.nabdscreentranslate.domain.ObserveHistoryUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).translationHistoryDao()
    private val observeHistoryUseCase = ObserveHistoryUseCase(dao)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val historyItems: StateFlow<List<TranslationHistoryEntity>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                observeHistoryUseCase.observeAll()
            } else {
                observeHistoryUseCase.search(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun deleteItem(entity: TranslationHistoryEntity) {
        viewModelScope.launch {
            observeHistoryUseCase.delete(entity)
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            observeHistoryUseCase.deleteAll()
        }
    }
}
