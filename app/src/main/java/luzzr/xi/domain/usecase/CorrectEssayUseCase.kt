package luzzr.xi.domain.usecase

import luzzr.xi.domain.model.CorrectionResult
import luzzr.xi.domain.repository.EssayGateway
import luzzr.xi.domain.repository.HistoryGateway
import luzzr.xi.domain.model.EssayError
import javax.inject.Inject

class CorrectEssayUseCase @Inject constructor(
    private val essayRepo: EssayGateway,
    private val historyGateway: HistoryGateway
) {
    suspend fun correctFromText(text: String, effort: String?): Result<CorrectionResult> {
        if (text.trim().isEmpty()) return Result.failure(EssayError.TextEmpty)
        val result = essayRepo.correctFromText(text.trim(), effort)
        result.onSuccess { correction ->
            runCatching {
                historyGateway.saveEssay(
                    originalText = text.trim(),
                    result = correction,
                    inputMode = "text",
                    thinkingLevel = effort
                )
            }
        }
        return result
    }

    suspend fun correctFromImage(uriString: String?, effort: String?): Result<CorrectionResult> {
        if (uriString.isNullOrEmpty()) return Result.failure(EssayError.ImageNull)
        val result = essayRepo.correctFromImage(uriString, effort)
        result.onSuccess { correction ->
            runCatching {
                historyGateway.saveEssay(
                    originalText = null,
                    result = correction,
                    inputMode = "image",
                    thinkingLevel = effort
                )
            }
        }
        return result
    }

    suspend fun correctFromPdf(uriString: String?, effort: String?): Result<CorrectionResult> {
        if (uriString.isNullOrEmpty()) return Result.failure(EssayError.PdfNull)
        val result = essayRepo.correctFromPdf(uriString, effort)
        result.onSuccess { correction ->
            runCatching {
                historyGateway.saveEssay(
                    originalText = null,
                    result = correction,
                    inputMode = "pdf",
                    thinkingLevel = effort
                )
            }
        }
        return result
    }
}
