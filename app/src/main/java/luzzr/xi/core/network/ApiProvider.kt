package luzzr.xi.core.network

import luzzr.xi.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

object ApiProvider {

    /**
     * Shared connection pool and dispatcher to avoid creating new ones on every config change.
     * When the user changes API settings, we rebuild the Retrofit instance but reuse the
     * underlying OkHttp connection pool for better connection reuse.
     */
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
        timeoutSeconds: Long = 60
    ): OpenAiApi {
        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(request)
        }

        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }

        val clientBuilder = sharedClient.newBuilder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)

        // Apply proxy if configured
        if (proxyEnabled && proxyHost.isNotBlank() && proxyPort > 0) {
            clientBuilder.proxy(
                Proxy(Proxy.Type.HTTP, InetSocketAddress(proxyHost, proxyPort))
            )
        }

        val client = clientBuilder.build()

        val url = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        return Retrofit.Builder()
            .baseUrl(url)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenAiApi::class.java)
    }
}
