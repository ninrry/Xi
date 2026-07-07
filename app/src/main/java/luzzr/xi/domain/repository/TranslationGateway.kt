package luzzr.xi.domain.repository

import kotlinx.coroutines.flow.Flow
import luzzr.xi.domain.model.TranslationResult

interface TranslationGateway {
    suspend fun translate(
        text: String,
        sourceLang: String = "English",
        targetLang: String = "Chinese",
        reasoningEffort: String? = null
    ): Result<TranslationResult>

    fun streamTranslate(
        text: String,
        sourceLang: String = "English",
        targetLang: String = "Chinese",
        reasoningEffort: String? = null
    ): Flow<Result<TranslationResult>>
}
