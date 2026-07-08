package luzzr.xi.data.repository

import android.content.Context
import android.util.Log
import luzzr.xi.core.network.ApiProvider
import luzzr.xi.core.network.NetworkCheck
import luzzr.xi.core.datastore.SettingsDataStore
import luzzr.xi.core.datastore.AppSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.flowOn
import com.google.gson.Gson
import luzzr.xi.core.network.OpenAiApi
import luzzr.xi.domain.model.AppError
import luzzr.xi.domain.model.UiText
import luzzr.xi.domain.model.ChatStreamChunk
import luzzr.xi.domain.model.Usage

abstract class ApiRepository(
    protected val context: Context,
    protected val settingsDataStore: SettingsDataStore,
    private val apiProvider: ApiProvider,
    private val networkCheck: NetworkCheck
) {
    private data class ApiConfig(
        val api: OpenAiApi? = null,
        val baseUrl: String? = null,
        val apiKey: String = "",
        val proxyEnabled: Boolean = false,
        val proxyHost: String = "",
        val proxyPort: Int = 0,
        val providerId: String = "xiaomi_mimo",
        val settings: luzzr.xi.core.datastore.AppSettings? = null
    )

    @Volatile private var apiConfig: ApiConfig = ApiConfig()

    private val apiMutex = Mutex()

    companion object {
        private val streamGson = Gson()
    }

    protected suspend fun getApi(): OpenAiApi = apiMutex.withLock {
        val s = settingsDataStore.settings.first()
        val cached = apiConfig
        if (cached.settings != null && cached.api != null &&
            cached.settings.apiBaseUrl == s.apiBaseUrl && cached.settings.apiKey == s.apiKey &&
            cached.settings.proxyEnabled == s.proxyEnabled && cached.settings.proxyHost == s.proxyHost && cached.settings.proxyPort == s.proxyPort &&
            cached.settings.providerId == s.providerId
        ) {
            return cached.api
        }
        val config = if (cached.api == null ||
            cached.baseUrl != s.apiBaseUrl || cached.apiKey != s.apiKey ||
            cached.proxyEnabled != s.proxyEnabled || cached.proxyHost != s.proxyHost || cached.proxyPort != s.proxyPort ||
            cached.providerId != s.providerId
        ) {
            val provider = luzzr.xi.core.provider.ProviderRegistry.getProvider(s.providerId)
            val newApi = apiProvider.createApi(
                providerConfig = provider,
                baseUrlOverride = s.apiBaseUrl,
                apiKey = s.apiKey,
                proxyEnabled = s.proxyEnabled,
                proxyHost = s.proxyHost,
                proxyPort = s.proxyPort
            )
            ApiConfig(
                api = newApi,
                baseUrl = s.apiBaseUrl,
                apiKey = s.apiKey,
                proxyEnabled = s.proxyEnabled,
                proxyHost = s.proxyHost,
                proxyPort = s.proxyPort,
                providerId = s.providerId,
                settings = s
            )
        } else {
            cached.copy(settings = s)
        }
        apiConfig = config
        return config.api!!
    }

    suspend fun testConnection(): luzzr.xi.domain.model.ModelListResponse {
        val s = settingsDataStore.settings.first()
        if (s.apiKey.isBlank()) {
            throw AppError.ConfigError(UiText.StringResource(luzzr.xi.R.string.error_empty_key))
        }
        val provider = luzzr.xi.core.provider.ProviderRegistry.getProvider(s.providerId)
        if (!provider.supportsModelListing) {
            return luzzr.xi.domain.model.ModelListResponse(data = emptyList())
        }
        return getApi().listModels()
    }

    protected suspend fun <T> callWithRetry(
        maxRetries: Int = 2,
        preloadedSettings: AppSettings? = null,
        block: suspend () -> Result<T>
    ): Result<T> = withContext(Dispatchers.IO) {
        val s = preloadedSettings ?: run {
            if (!networkCheck.isNetworkAvailable()) {
                return@withContext Result.failure(AppError.NetworkError())
            }
            settingsDataStore.settings.first().also { settings ->
                if (settings.apiKey.isBlank()) {
                    return@withContext Result.failure(AppError.ConfigError(UiText.StringResource(luzzr.xi.R.string.error_empty_key)))
                }
            }
        }

        if (!networkCheck.isNetworkAvailable()) {
            return@withContext Result.failure(AppError.NetworkError())
        }

        if (s.apiKey.isBlank()) {
            return@withContext Result.failure(AppError.ConfigError(UiText.StringResource(luzzr.xi.R.string.error_empty_key)))
        }

        var lastError: Exception? = null

        repeat(maxRetries + 1) { attempt ->
            try {
                return@withContext block()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (e is AppError.ConfigError) return@withContext Result.failure(e)
                lastError = e
                Log.w("ApiRepository", "Call attempt $attempt/${maxRetries + 1} failed", e)

                if (e is retrofit2.HttpException) {
                    val code = e.code()
                    if (code in 400..499 && code != 429) {
                        return@withContext Result.failure(AppError.ApiError(UiText.DynamicString("HTTP $code: ${e.message()}"), code, s.providerId))
                    }
                }

                if (attempt < maxRetries) {
                    val backoff = 1000L * (1 shl attempt)
                    delay(backoff)
                }
            }
        }

        return@withContext Result.failure(AppError.NetworkError(lastError))
    }

    protected fun streamWithRetry(
        maxRetries: Int = 2,
        preloadedSettings: AppSettings? = null,
        block: suspend () -> okhttp3.ResponseBody
    ): Flow<Result<ChatStreamChunk>> = flow {
        val s = preloadedSettings ?: run {
            if (!networkCheck.isNetworkAvailable()) {
                emit(Result.failure(AppError.NetworkError()))
                return@flow
            }
            settingsDataStore.settings.first().also { settings ->
                if (settings.apiKey.isBlank()) {
                    emit(Result.failure(AppError.ConfigError(UiText.StringResource(luzzr.xi.R.string.error_empty_key))))
                    return@flow
                }
            }
        }

        if (!networkCheck.isNetworkAvailable()) {
            emit(Result.failure(AppError.NetworkError()))
            return@flow
        }

        if (s.apiKey.isBlank()) {
            emit(Result.failure(AppError.ConfigError(UiText.StringResource(luzzr.xi.R.string.error_empty_key))))
            return@flow
        }

        val gson = streamGson
        var lastError: Exception? = null

        repeat(maxRetries + 1) { attempt ->
            try {
                val responseBody = block()
                try {
                    val reader = responseBody.byteStream().bufferedReader()
                    reader.use {
                        var line = it.readLine()
                        while (line != null) {
                            kotlinx.coroutines.currentCoroutineContext().ensureActive()
                            if (line.startsWith("data: ")) {
                                val data = line.removePrefix("data: ").trim()
                                if (data == "[DONE]") {
                                    break
                                }
                                try {
                                    val chunk = gson.fromJson(data, ChatStreamChunk::class.java)
                                    emit(Result.success(chunk))
                                } catch (e: Exception) {
                                    Log.w("ApiRepository", "Failed to parse SSE chunk", e)
                                }
                            }
                            line = it.readLine()
                        }
                    }
                } finally {
                    responseBody.close()
                }
                return@flow
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (e is AppError.ConfigError) {
                    emit(Result.failure(e))
                    return@flow
                }
                lastError = e
                Log.w("ApiRepository", "Stream attempt $attempt/${maxRetries + 1} failed", e)

                if (e is retrofit2.HttpException) {
                    val code = e.code()
                    if (code in 400..499 && code != 429) {
                        emit(Result.failure(AppError.ApiError(UiText.DynamicString("HTTP $code: ${e.message()}"), code, s.providerId)))
                        return@flow
                    }
                }

                if (attempt < maxRetries) {
                    val backoff = 1000L * (1 shl attempt)
                    delay(backoff)
                }
            }
        }
        
        emit(Result.failure(AppError.NetworkError(lastError)))
    }.flowOn(Dispatchers.IO)

}
