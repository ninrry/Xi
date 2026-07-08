package luzzr.xi.domain.repository

import luzzr.xi.domain.model.ModelDownloadProgress

interface MlKitModelGateway {
    fun addProgressListener(listener: (String, ModelDownloadProgress) -> Unit)
    fun removeProgressListener(listener: (String, ModelDownloadProgress) -> Unit)
    suspend fun downloadModel(
        sourceLang: String,
        targetLang: String,
        sourceCode: String?,
        targetCode: String?
    ): Result<Unit>
    fun cancelDownload()
    suspend fun isModelDownloaded(
        sourceLang: String,
        targetLang: String,
        sourceCode: String,
        targetCode: String
    ): Boolean
}
