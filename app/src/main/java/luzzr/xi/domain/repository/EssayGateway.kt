package luzzr.xi.domain.repository

import android.net.Uri
import luzzr.xi.domain.model.CorrectionResult

interface EssayGateway {
    suspend fun correctFromText(text: String, reasoningEffort: String?): Result<CorrectionResult>
    suspend fun correctFromImage(imageUri: Uri, reasoningEffort: String?): Result<CorrectionResult>
    suspend fun correctFromPdf(pdfUri: Uri, reasoningEffort: String?): Result<CorrectionResult>
}
