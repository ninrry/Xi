package luzzr.xi.data.repository

import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import luzzr.xi.data.local.HistoryDao
import luzzr.xi.data.local.HistoryRecordEntity
import luzzr.xi.domain.model.CorrectionResult
import luzzr.xi.domain.model.HistoryRecord
import luzzr.xi.domain.model.HistorySource
import luzzr.xi.domain.model.HistoryType
import luzzr.xi.domain.model.TranslationResult
import luzzr.xi.domain.repository.HistoryGateway
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val historyDao: HistoryDao,
    private val gson: Gson
) : HistoryGateway {

    companion object {
        private const val MAX_RECORDS = 500
    }

    override fun observeAll(): Flow<List<HistoryRecord>> =
        historyDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeByType(type: String): Flow<List<HistoryRecord>> =
        historyDao.observeByType(type).map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: Long): HistoryRecord? =
        historyDao.getById(id)?.toDomain()

    override suspend fun saveTranslation(
        inputText: String,
        result: TranslationResult,
        sourceLang: String,
        targetLang: String,
        engine: String,
        thinkingLevel: String?,
        source: String
    ) {
        val translation = result.translation?.trim().orEmpty()
        if (translation.isEmpty()) return

        historyDao.insert(
            HistoryRecordEntity(
                type = HistoryType.TRANSLATE,
                createdAt = System.currentTimeMillis(),
                source = source,
                inputText = inputText,
                outputText = translation,
                sourceLang = sourceLang,
                targetLang = targetLang,
                engine = engine,
                thinkingLevel = thinkingLevel,
                detectedLanguage = result.detectedLanguage,
                alternativesJson = result.alternatives?.takeIf { it.isNotEmpty() }?.let { gson.toJson(it) },
                promptTokens = result.usage?.promptTokens,
                completionTokens = result.usage?.completionTokens,
                totalTokens = result.usage?.totalTokens
            )
        )
        historyDao.trimTo(MAX_RECORDS)
    }

    override suspend fun saveEssay(
        originalText: String?,
        result: CorrectionResult,
        inputMode: String,
        thinkingLevel: String?,
        source: String
    ) {
        val score = result.score
        historyDao.insert(
            HistoryRecordEntity(
                type = HistoryType.ESSAY,
                createdAt = System.currentTimeMillis(),
                source = source.ifBlank { HistorySource.APP },
                inputText = originalText,
                outputText = result.correctedEssay,
                originalEssay = originalText,
                thinkingLevel = thinkingLevel,
                inputMode = inputMode,
                scoreGrammar = score?.grammar,
                scoreVocab = score?.vocabulary,
                scoreStructure = score?.structure,
                scoreStyle = score?.style,
                scoreTotal = score?.total,
                grade = score?.grade,
                grammarErrorsJson = result.grammarErrors.takeIf { it.isNotEmpty() }?.let { gson.toJson(it) },
                vocabularyJson = result.vocabulary.takeIf { it.isNotEmpty() }?.let { gson.toJson(it) },
                structureJson = result.structure?.let { gson.toJson(it) },
                styleJson = result.style?.let { gson.toJson(it) },
                writingTipsJson = result.writingTips.takeIf { it.isNotEmpty() }?.let { gson.toJson(it) },
                promptTokens = result.usage?.promptTokens,
                completionTokens = result.usage?.completionTokens,
                totalTokens = result.usage?.totalTokens
            )
        )
        historyDao.trimTo(MAX_RECORDS)
    }

    override suspend fun countTranslateThisWeek(): Int =
        historyDao.countByTypeSince(HistoryType.TRANSLATE, weekStartMs())

    override suspend fun countEssayThisWeek(): Int =
        historyDao.countByTypeSince(HistoryType.ESSAY, weekStartMs())

    override suspend fun latestEssayScore(): Int? =
        historyDao.latestByType(HistoryType.ESSAY)?.scoreTotal

    override suspend fun deleteAll() = historyDao.deleteAll()

    override suspend fun deleteById(id: Long) = historyDao.deleteById(id)

    private fun HistoryRecordEntity.toDomain() = HistoryRecord(
        id = id,
        type = type,
        createdAt = createdAt,
        source = source,
        inputText = inputText,
        outputText = outputText,
        sourceLang = sourceLang,
        targetLang = targetLang,
        engine = engine,
        thinkingLevel = thinkingLevel,
        model = model,
        providerId = providerId,
        detectedLanguage = detectedLanguage,
        alternativesJson = alternativesJson,
        inputMode = inputMode,
        scoreGrammar = scoreGrammar,
        scoreVocab = scoreVocab,
        scoreStructure = scoreStructure,
        scoreStyle = scoreStyle,
        scoreTotal = scoreTotal,
        grade = grade,
        grammarErrorsJson = grammarErrorsJson,
        vocabularyJson = vocabularyJson,
        structureJson = structureJson,
        styleJson = styleJson,
        writingTipsJson = writingTipsJson,
        originalEssay = originalEssay,
        promptTokens = promptTokens,
        completionTokens = completionTokens,
        totalTokens = totalTokens
    )

    private fun weekStartMs(): Long {
        val calendar = java.util.Calendar.getInstance().apply {
            firstDayOfWeek = java.util.Calendar.MONDAY
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
            set(java.util.Calendar.DAY_OF_WEEK, firstDayOfWeek)
        }
        return calendar.timeInMillis
    }
}
