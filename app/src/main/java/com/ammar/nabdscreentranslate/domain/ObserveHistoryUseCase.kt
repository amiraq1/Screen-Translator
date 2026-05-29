package com.ammar.nabdscreentranslate.domain

import com.ammar.nabdscreentranslate.data.TranslationHistoryDao
import com.ammar.nabdscreentranslate.data.TranslationHistoryEntity
import kotlinx.coroutines.flow.Flow

class ObserveHistoryUseCase(
    private val dao: TranslationHistoryDao
) {
    fun observeAll(): Flow<List<TranslationHistoryEntity>> = dao.observeAll()

    fun search(query: String): Flow<List<TranslationHistoryEntity>> = dao.search(query)

    suspend fun delete(entity: TranslationHistoryEntity) = dao.delete(entity)

    suspend fun deleteAll() = dao.deleteAll()
}
