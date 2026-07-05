package luzzr.xi.data.repository

import android.content.Context
import android.util.Log
import luzzr.xi.core.network.ApiProvider
import luzzr.xi.core.network.NetworkCheck
import luzzr.xi.core.datastore.SettingsDataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import luzzr.xi.core.network.OpenAiApi
import luzzr.xi.domain.model.AppError

/**
 * Base repository providing shared API access, caching, retry, and error handling.
 * Eliminates getApi() + retry loop duplication across concrete repositories.
 */
abstract class ApiRepository(
    protected val context: Context,
    protected val settingsDataStore: SettingsDataStore
) {
    private var api: OpenAiApi? = null
    private var cachedBaseUrl: String? = null
    private var cachedApiKey: String = ""
    private var cachedProxyEnabled: Boolean = false
    private var cachedProxyHost: String = ""
    private var cachedProxyPort: Int = 0

    protected suspend fun getApi(): OpenAiApi {
        val s = settingsDataStore.settings.first()
        if (api == null || cachedBaseUrl != s.apiBaseUrl || cachedApiKey != s.apiKey ||
            cachedProxyEnabled != s.proxyEnabled || cachedProxyHost != s.proxyHost || cachedProxyPort != s.proxyPort
        ) {
            api = ApiProvider.createApi(
                baseUrl = s.apiBaseUrl,
                apiKey = s.apiKey,
                proxyEnabled = s.proxyEnabled,
                proxyHost = s.proxyHost,
                proxyPort = s.proxyPort
            )
            cachedBaseUrl = s.apiBaseUrl
            cachedApiKey = s.apiKey
            cachedProxyEnabled = s.proxyEnabled
            cachedProxyHost = s.proxyHost
            cachedProxyPort = s.proxyPort
        }
        return api ?: throw AppError.ConfigError("Failed to create API client")
    }

    protected suspend fun <T> callWithRetry(
        maxRetries: Int = 2,
        block: suspend () -> Result<T>
    ): Result<T> {
        if (!NetworkCheck.isNetworkAvailable(context)) {
            return Result.failure(AppError.NetworkError())
        }

        val s = settingsDataStore.settings.first()
        if (s.apiKey.isBlank()) {
            return Result.failure(AppError.ConfigError(context.getString(luzzr.xi.R.string.error_empty_key)))
        }

        var lastError: Exception? = null

        repeat(maxRetries + 1) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                lastError = e
                Log.w("ApiRepository", "Call attempt $attempt/${maxRetries + 1} failed", e)
                if (attempt < maxRetries) {
                    delay(1000L * (attempt + 1))
                }
            }
        }

        return Result.failure(AppError.NetworkError(lastError))
    }
}
