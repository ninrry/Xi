package luzzr.xi.domain.usecase

import luzzr.xi.domain.model.CorrectionResult
import luzzr.xi.domain.repository.EssayGateway
import luzzr.xi.domain.model.EssayError
import javax.inject.Inject

class CorrectEssayUseCase @Inject constructor(
    private val essayRepo: EssayGateway
) {
    suspend fun correctFromText(text: String, effort: String?): Result<CorrectionResult> {
        if (text.trim().isEmpty()) return Result.failure(EssayError.TextEmpty)
        return essayRepo.correctFromText(text.trim(), effort)
    }

    suspend fun correctFromImage(uriString: String?, effort: String?): Result<CorrectionResult> {
        if (uriString.isNullOrEmpty()) return Result.failure(EssayError.ImageNull)
        return essayRepo.correctFromImage(uriString, effort)
    }

    suspend fun correctFromPdf(uriString: String?, effort: String?): Result<CorrectionResult> {
        if (uriString.isNullOrEmpty()) return Result.failure(EssayError.PdfNull)
        return essayRepo.correctFromPdf(uriString, effort)
    }
}