package luzzr.xi.domain.usecase
import luzzr.xi.domain.model.ThinkingLevel

import android.net.Uri
import luzzr.xi.data.repository.CorrectionResult
import luzzr.xi.data.repository.EssayRepository
import luzzr.xi.data.repository.MediaProcessor
import javax.inject.Inject

class CorrectEssayUseCase @Inject constructor(
    private val essayRepo: EssayRepository,
    private val mediaProcessor: MediaProcessor
) {
    suspend fun correctFromText(text: String, effort: String?): Result<CorrectionResult> {
        if (text.trim().isEmpty()) return Result.failure(IllegalArgumentException("TEXT_EMPTY"))
        return essayRepo.correctEssay(text.trim(), effort)
    }

    suspend fun correctFromImage(uri: Uri?, effort: String?): Result<CorrectionResult> {
        if (uri == null) return Result.failure(IllegalArgumentException("IMAGE_NULL"))
        return essayRepo.correctEssayFromImage(uri, effort)
    }

    suspend fun correctFromPdf(uri: Uri?, effort: String?): Result<CorrectionResult> {
        if (uri == null) return Result.failure(IllegalArgumentException("PDF_NULL"))
        val base64Pages = mediaProcessor.renderPdfPagesAsBase64(uri)
        if (base64Pages.isEmpty()) return Result.failure(IllegalArgumentException("PDF_UNREADABLE"))
        return essayRepo.correctEssayFromBase64Images(base64Pages, effort)
    }
}