package luzzr.xi.domain.repository

import luzzr.xi.domain.model.CorrectionResult

interface EssayGateway {
    suspend fun correctFromText(text: String, reasoningEffort: String?): Result<CorrectionResult>
    suspend fun correctFromImage(imageUriString: String, reasoningEffort: String?): Result<CorrectionResult>
    suspend fun correctFromPdf(pdfUriString: String, reasoningEffort: String?): Result<CorrectionResult>
}
