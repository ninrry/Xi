package luzzr.xi.core.network

import luzzr.xi.BuildConfig
import luzzr.xi.core.provider.ProviderConfig
import luzzr.xi.domain.model.UiText
import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiProvider @Inject constructor() {

    private val sharedClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    fun createApi(
        baseUrl: String,
        apiKey: String,
        proxyEnabled: Boolean = false,
        proxyHost: String = "",
        proxyPort: Int = 0,
        timeoutSeconds: Long = 30
    ): OpenAiApi = createApi(
        providerConfig = ProviderConfig(
            id = "_legacy",
            displayNameRes = luzzr.xi.R.string.provider_custom,
            descriptionRes = luzzr.xi.R.string.provider_custom_desc,
            defaultBaseUrl = baseUrl,
            defaultModel = "",
            authType = luzzr.xi.core.provider.AuthType.BEARER,
            authHeaderName = "Authorization",
            authHeaderValuePrefix = "Bearer "
        ),
        baseUrlOverride = baseUrl,
        apiKey = apiKey,
        proxyEnabled = proxyEnabled,
        proxyHost = proxyHost,
        proxyPort = proxyPort,
        timeoutSeconds = timeoutSeconds
    )

    fun createApi(
        providerConfig: ProviderConfig,
        baseUrlOverride: String = "",
        apiKey: String,
        proxyEnabled: Boolean = false,
        proxyHost: String = "",
        proxyPort: Int = 0,
        timeoutSeconds: Long = 30
    ): OpenAiApi {
        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader(providerConfig.authHeaderName, "${providerConfig.authHeaderValuePrefix}$apiKey")
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(request)
        }

        val sensitiveHeaders = setOf(
            "Authorization",
            providerConfig.authHeaderName,
            "api-key",
            "x-api-key"
        )
        val logging = HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
            override fun log(message: String) {
                val masked = message.lineSequence().joinToString("\n") { line ->
                    val header = sensitiveHeaders.firstOrNull { h ->
                        line.startsWith("$h:", ignoreCase = true)
                    }
                    if (header != null) "$header: <redacted>" else line
                }
                Log.d("ApiProvider", masked)
            }
        }).apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }

        val clientBuilder = sharedClient.newBuilder()
            .addInterceptor(logging)
            .addInterceptor(authInterceptor)
            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)

        if (proxyEnabled && proxyHost.isNotBlank() && proxyPort > 0) {
            clientBuilder.proxy(
                Proxy(Proxy.Type.HTTP, InetSocketAddress(proxyHost, proxyPort))
            )
        } else {
            clientBuilder.proxy(Proxy.NO_PROXY)
        }

        val client = clientBuilder.build()
        val effectiveBaseUrl = baseUrlOverride.ifBlank { providerConfig.defaultBaseUrl }
        if (effectiveBaseUrl.isBlank() || (!effectiveBaseUrl.startsWith("http://") && !effectiveBaseUrl.startsWith("https://"))) {
            throw luzzr.xi.domain.model.AppError.ConfigError(UiText.DynamicString("Invalid base URL"))
        }
        val url = if (effectiveBaseUrl.endsWith("/")) effectiveBaseUrl else "$effectiveBaseUrl/"

        return Retrofit.Builder()
            .baseUrl(url)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenAiApi::class.java)
    }
}
