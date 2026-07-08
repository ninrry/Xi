package luzzr.xi.domain.model

enum class ModelDownloadState {
    IDLE, DOWNLOADING, COMPLETED, FAILED
}

data class ModelDownloadProgress(
    val state: ModelDownloadState = ModelDownloadState.IDLE,
    val progress: Float = 0f,
    val bytesReceived: Long = 0L,
    val totalBytes: Long = 0L,
    val message: String = ""
)
