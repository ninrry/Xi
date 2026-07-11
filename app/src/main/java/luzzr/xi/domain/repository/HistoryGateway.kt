package luzzr.xi.domain.repository

import kotlinx.coroutines.flow.Flow
import luzzr.xi.domain.model.CorrectionResult
import luzzr.xi.domain.model.HistoryRecord
import luzzr.xi.domain.model.TranslationResult

interface HistoryGateway {
    fun observeAll(): Flow<List<HistoryRecord>>
    fun observeByType(type: String): Flow<List<HistoryRecord>>
    suspend fun getById(id: Long): HistoryRecord?
    suspend fun saveTranslation(
        inputText: String,
        result: TranslationResult,
        sourceLang: String,
        targetLang: String,
        engine: String,
        thinkingLevel: String?,
        source: String
    )
    suspend fun saveEssay(
        originalText: String?,
        result: CorrectionResult,
        inputMode: String,
        thinkingLevel: String?,
        source: String = "app"
    )
    suspend fun countTranslateThisWeek(): Int
    suspend fun countEssayThisWeek(): Int
    suspend fun latestEssayScore(): Int?
    suspend fun deleteAll()
    suspend fun deleteById(id: Long)
}
