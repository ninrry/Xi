package luzzr.xi.domain.usecase
import luzzr.xi.domain.model.ThinkingLevel

import luzzr.xi.domain.model.TranslationEngine
import luzzr.xi.data.repository.MlKitTranslator
import luzzr.xi.data.repository.TranslationRepository
import javax.inject.Inject
import kotlinx.coroutines.withTimeout

class TranslateUseCase @Inject constructor(
    private val translationRepo: TranslationRepository,
    private val mlKitTranslator: MlKitTranslator
) {
    suspend operator fun invoke(
        text: String,
        sourceLang: String,
        targetLang: String,
        engine: TranslationEngine,
        thinkingLevelId: String?
    ): Result<String> {
        return try {
            withTimeout(30_000L) {
                if (engine == TranslationEngine.MLKIT) {
                    mlKitTranslator.translate(
                        text = text,
                        sourceLang = sourceLang,
                        targetLang = targetLang
                    )
                } else {
                    translationRepo.translate(
                        text = text,
                        sourceLang = sourceLang,
                        targetLang = targetLang,
                        reasoningEffort = thinkingLevelId
                    )
                }
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Result.failure(e)
        }
    }
}