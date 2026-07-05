package luzzr.xi.domain.model

sealed class AppError(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
    class NetworkError(cause: Throwable? = null) : AppError("Network Error", cause)
    class ApiError(message: String, val code: Int? = null) : AppError(message)
    class ConfigError(message: String) : AppError(message)
    class EmptyResultError(message: String = "Result is empty") : AppError(message)
    class UnknownError(cause: Throwable) : AppError("Unknown error: ${cause.message}", cause)
    class ParseError(message: String, val rawResponse: String? = null) : AppError(message)
}
