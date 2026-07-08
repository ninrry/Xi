package luzzr.xi.domain.usecase

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.CancellationException
import luzzr.xi.domain.repository.TranslationGateway
import luzzr.xi.domain.repository.LocalTranslationGateway
import luzzr.xi.domain.model.TranslationEngine
import luzzr.xi.domain.model.TranslationResult
import javax.inject.Inject

@OptIn(FlowPreview::class)
class TranslateUseCase @Inject constructor(
    private val translationRepo: TranslationGateway,
    private val mlKitTranslator: LocalTranslationGateway
) {
    operator fun invoke(
        text: String,
        sourceLang: String,
        targetLang: String,
        engine: TranslationEngine,
        thinkingLevelId: String?
    ): Flow<Result<TranslationResult>> {
        if (engine == TranslationEngine.MLKIT) {
            return flow {
                try {
                    withTimeout(30_000L) {
                        val result = mlKitTranslator.translate(
                            text = text,
                            sourceLang = sourceLang,
                            targetLang = targetLang
                        ).map { TranslationResult(translation = it) }
                        emit(result)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: TimeoutCancellationException) {
                    emit(Result.failure(e))
                }
            }
        } else {
            return translationRepo.streamTranslate(
                text = text,
                sourceLang = sourceLang,
                targetLang = targetLang,
                reasoningEffort = thinkingLevelId
            )
        }
    }
}