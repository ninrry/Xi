package luzzr.xi.domain.model

sealed class AppError(
    open val uiText: UiText,
    cause: Throwable? = null
) : Exception(cause) {
    class NetworkError(cause: Throwable? = null) : AppError(UiText.StringResource(luzzr.xi.R.string.error_network), cause)
    class ApiError(uiText: UiText, val code: Int? = null, val providerId: String? = null) : AppError(uiText)
    class ConfigError(uiText: UiText) : AppError(uiText)
    class EmptyResultError(uiText: UiText = UiText.StringResource(luzzr.xi.R.string.error_result_empty)) : AppError(uiText)
    class UnknownError(cause: Throwable) : AppError(UiText.StringResource(luzzr.xi.R.string.error_unknown), cause)
    class GenericError(uiText: UiText, cause: Throwable? = null) : AppError(uiText)
    class ParseError(uiText: UiText, val rawResponse: String? = null) : AppError(uiText)

    fun toUiText(): UiText = uiText
}
