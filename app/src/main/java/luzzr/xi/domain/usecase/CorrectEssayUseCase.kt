package luzzr.xi.domain.usecase

import android.net.Uri
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

    suspend fun correctFromImage(uri: Uri?, effort: String?): Result<CorrectionResult> {
        if (uri == null) return Result.failure(EssayError.ImageNull)
        return essayRepo.correctFromImage(uri, effort)
    }

    suspend fun correctFromPdf(uri: Uri?, effort: String?): Result<CorrectionResult> {
        if (uri == null) return Result.failure(EssayError.PdfNull)
        return essayRepo.correctFromPdf(uri, effort)
    }
}