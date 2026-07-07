package luzzr.xi.data.repository

import android.content.Context
import android.util.Log
import luzzr.xi.core.network.ApiProvider
import luzzr.xi.core.network.NetworkCheck
import luzzr.xi.core.datastore.SettingsDataStore
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
import luzzr.xi.domain.model.ChatStreamChunk
import luzzr.xi.domain.model.Usage

abstract class ApiRepository(
    protected val context: Context,
    protected val settingsDataStore: SettingsDataStore,
    private val apiProvider: ApiProvider,
    private val networkCheck: NetworkCheck
) {
    @Volatile private var api: OpenAiApi? = null
    @Volatile private var cachedBaseUrl: String? = null
    @Volatile private var cachedApiKey: String = ""
    @Volatile private var cachedProxyEnabled: Boolean = false
    @Volatile private var cachedProxyHost: String = ""
    @Volatile private var cachedProxyPort: Int = 0
    @Volatile private var cachedProviderId: String = "xiaomi_mimo"
    @Volatile private var settingsSnapshot: luzzr.xi.core.datastore.AppSettings? = null

    private val apiMutex = Mutex()

    companion object {
        private val streamGson = Gson()
    }

    protected suspend fun getApi(): OpenAiApi = apiMutex.withLock {
        val s = settingsDataStore.settings.first()
        val cached = settingsSnapshot
        if (cached != null && api != null &&
            cached.apiBaseUrl == s.apiBaseUrl && cached.apiKey == s.apiKey &&
            cached.proxyEnabled == s.proxyEnabled && cached.proxyHost == s.proxyHost && cached.proxyPort == s.proxyPort &&
            cached.providerId == s.providerId
        ) {
            return api ?: throw AppError.ConfigError("Failed to create API client")
        }
        settingsSnapshot = s
        if (api == null || cachedBaseUrl != s.apiBaseUrl || cachedApiKey != s.apiKey ||
            cachedProxyEnabled != s.proxyEnabled || cachedProxyHost != s.proxyHost || cachedProxyPort != s.proxyPort ||
            cachedProviderId != s.providerId
        ) {
            val provider = luzzr.xi.core.provider.ProviderRegistry.getProvider(s.providerId)
            api = apiProvider.createApi(
                providerConfig = provider,
                baseUrlOverride = s.apiBaseUrl,
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
            cachedProviderId = s.providerId
        }
        return api ?: throw AppError.ConfigError("Failed to create API client")
    }

    suspend fun testConnection(): luzzr.xi.domain.model.ModelListResponse {
        val s = settingsDataStore.settings.first()
        if (s.apiKey.isBlank()) {
            throw AppError.ConfigError(context.getString(luzzr.xi.R.string.error_empty_key))
        }
        val provider = luzzr.xi.core.provider.ProviderRegistry.getProvider(s.providerId)
        if (!provider.supportsModelListing) {
            return luzzr.xi.domain.model.ModelListResponse(data = emptyList())
        }
        return getApi().listModels()
    }

    protected suspend fun <T> callWithRetry(
        maxRetries: Int = 2,
        block: suspend () -> Result<T>
    ): Result<T> = withContext(Dispatchers.IO) {
        if (!networkCheck.isNetworkAvailable()) {
            return@withContext Result.failure(AppError.NetworkError())
        }

        val s = settingsDataStore.settings.first()
        if (s.apiKey.isBlank()) {
            return@withContext Result.failure(AppError.ConfigError(context.getString(luzzr.xi.R.string.error_empty_key)))
        }

        var lastError: Exception? = null

        repeat(maxRetries + 1) { attempt ->
            try {
                return@withContext block()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                lastError = e
                Log.w("ApiRepository", "Call attempt $attempt/${maxRetries + 1} failed", e)

                if (e is retrofit2.HttpException) {
                    val code = e.code()
                    if (code in 400..499 && code != 429) {
                        return@withContext Result.failure(AppError.ApiError("HTTP $code: ${e.message()}", code, s.providerId))
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
        block: suspend () -> okhttp3.ResponseBody
    ): Flow<Result<ChatStreamChunk>> = flow {
        if (!networkCheck.isNetworkAvailable()) {
            emit(Result.failure(AppError.NetworkError()))
            return@flow
        }

        val s = settingsDataStore.settings.first()
        if (s.apiKey.isBlank()) {
            emit(Result.failure(AppError.ConfigError(context.getString(luzzr.xi.R.string.error_empty_key))))
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
                lastError = e
                Log.w("ApiRepository", "Stream attempt $attempt/${maxRetries + 1} failed", e)

                if (e is retrofit2.HttpException) {
                    val code = e.code()
                    if (code in 400..499 && code != 429) {
                        emit(Result.failure(AppError.ApiError("HTTP $code: ${e.message()}", code, s.providerId)))
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
