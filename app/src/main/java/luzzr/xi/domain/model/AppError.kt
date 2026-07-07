package luzzr.xi.domain.model

sealed class AppError(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
    class NetworkError(cause: Throwable? = null) : AppError("Network Error", cause)
    class ApiError(message: String, val code: Int? = null, val providerId: String? = null) : AppError(message)
    class ConfigError(message: String) : AppError(message)
    class EmptyResultError(message: String = "Result is empty") : AppError(message)
    class UnknownError(cause: Throwable) : AppError("Unknown error: ${cause.message}", cause)
    class ParseError(message: String, val rawResponse: String? = null) : AppError(message)

    fun toUiText(): UiText = when (this) {
        is NetworkError -> UiText.StringResource(luzzr.xi.R.string.error_network)
        is ApiError -> {
            val provider = luzzr.xi.core.provider.ProviderRegistry.getProvider(providerId ?: "custom")
            UiText.StringResource(luzzr.xi.R.string.error_api_with_provider, arrayOf(provider.providerName, message ?: "Unknown API error"))
        }
        is ConfigError -> UiText.DynamicString(message ?: "Configuration error")
        is EmptyResultError -> UiText.StringResource(luzzr.xi.R.string.error_result_empty)
        is ParseError -> UiText.StringResource(luzzr.xi.R.string.error_parse_failed)
        is UnknownError -> UiText.StringResource(luzzr.xi.R.string.error_unknown)
    }
}
