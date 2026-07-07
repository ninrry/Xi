package luzzr.xi

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import luzzr.xi.core.network.ApiProvider
import luzzr.xi.core.network.OpenAiApi
import luzzr.xi.core.provider.AuthType
import luzzr.xi.core.provider.ProviderConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import java.net.Proxy

class ApiProviderTest {

    private val apiProvider = ApiProvider()

    private fun extractRetrofit(api: OpenAiApi): Retrofit {
        val handler = java.lang.reflect.Proxy.getInvocationHandler(api)
        val field = handler.javaClass.declaredFields.first {
            Retrofit::class.java.isAssignableFrom(it.type)
        }
        field.isAccessible = true
        return field.get(handler) as Retrofit
    }

    private fun extractClient(retrofit: Retrofit): OkHttpClient {
        val field = retrofit.javaClass.getDeclaredField("callFactory")
        field.isAccessible = true
        return field.get(retrofit) as OkHttpClient
    }

    private fun extractBaseUrl(retrofit: Retrofit): String {
        val field = retrofit.javaClass.getDeclaredField("baseUrl")
        field.isAccessible = true
        val httpUrl = field.get(retrofit)
        return httpUrl.toString()
    }

    @Test
    fun `createApi returns OpenAiApi instance`() {
        val api = apiProvider.createApi(
            baseUrl = "https://api.example.com",
            apiKey = "sk-test-key"
        )

        assertNotNull(api)
        assertTrue(java.lang.reflect.Proxy.isProxyClass(api.javaClass))
        assertTrue(api is OpenAiApi)
    }

    @Test
    fun `createApi adds trailing slash when missing`() {
        val withoutSlash = apiProvider.createApi(
            baseUrl = "https://api.example.com/v1",
            apiKey = "key"
        )
        val retrofit = extractRetrofit(withoutSlash)
        val baseUrl = extractBaseUrl(retrofit)

        assertTrue(
            "Base URL should end with slash, got: $baseUrl",
            baseUrl.endsWith("/")
        )
        assertTrue(
            "Base URL should contain the original path",
            baseUrl.contains("/v1/")
        )
    }

    @Test
    fun `createApi preserves trailing slash when already present`() {
        val withSlash = apiProvider.createApi(
            baseUrl = "https://api.example.com/v1/",
            apiKey = "key"
        )
        val retrofit = extractRetrofit(withSlash)
        val baseUrl = extractBaseUrl(retrofit)

        assertTrue(
            "Base URL should end with slash",
            baseUrl.endsWith("/")
        )
        assertEquals(
            "https://api.example.com/v1/",
            baseUrl
        )
    }

    @Test
    fun `createApi configures proxy when enabled`() {
        val api = apiProvider.createApi(
            baseUrl = "https://api.example.com",
            apiKey = "key",
            proxyEnabled = true,
            proxyHost = "127.0.0.1",
            proxyPort = 8080
        )

        val retrofit = extractRetrofit(api)
        val client = extractClient(retrofit)
        val proxy = client.proxy

        assertNotNull("Proxy should be set when enabled", proxy)
        assertEquals(Proxy.Type.HTTP, proxy!!.type())
        val address = proxy.address() as java.net.InetSocketAddress
        assertTrue(
            "Proxy host should be 127.0.0.1 or localhost",
            address.hostName == "127.0.0.1" || address.hostName == "localhost"
        )
        assertEquals(8080, address.port)
    }

    @Test
    fun `createApi skips proxy when disabled`() {
        val api = apiProvider.createApi(
            baseUrl = "https://api.example.com",
            apiKey = "key",
            proxyEnabled = false,
            proxyHost = "127.0.0.1",
            proxyPort = 8080
        )

        val retrofit = extractRetrofit(api)
        val client = extractClient(retrofit)
        val proxy = client.proxy

        assertNotNull("Proxy should be NO_PROXY when disabled", proxy)
        assertEquals(Proxy.Type.DIRECT, proxy!!.type())
    }

    @Test
    fun `createApi sets auth header via interceptor`() {
        val api = apiProvider.createApi(
            baseUrl = "https://api.example.com",
            apiKey = "sk-secret-123"
        )

        val retrofit = extractRetrofit(api)
        val client = extractClient(retrofit)
        val interceptors = client.interceptors

        assertTrue("Should have at least one interceptor", interceptors.isNotEmpty())

        val authInterceptor = interceptors.first()

        val originalRequest = Request.Builder()
            .url("https://api.example.com/test")
            .build()

        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns originalRequest
        every { chain.proceed(any()) } answers {
            val capturedRequest = firstArg<Request>()
            assertEquals("Bearer sk-secret-123", capturedRequest.header("Authorization"))
            assertEquals("application/json", capturedRequest.header("Content-Type"))
            Response.Builder()
                .request(capturedRequest)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .build()
        }

        authInterceptor.intercept(chain)

        verify(exactly = 1) { chain.proceed(any()) }
    }

    @Test
    fun `createApi with ProviderConfig uses api-key header for Xiaomi MiMo`() {
        val mimoConfig = ProviderConfig(
            id = "xiaomi_mimo",
            displayNameRes = luzzr.xi.R.string.provider_xiaomi_mimo,
            descriptionRes = luzzr.xi.R.string.provider_xiaomi_mimo_desc,
            defaultBaseUrl = "https://api.xiaomimimo.com/v1",
            defaultModel = "mimo-v2.5-pro",
            authType = AuthType.API_KEY_HEADER,
            authHeaderName = "api-key",
            authHeaderValuePrefix = ""
        )

        val api = apiProvider.createApi(
            providerConfig = mimoConfig,
            apiKey = "mimo-test-key"
        )

        val retrofit = extractRetrofit(api)
        val client = extractClient(retrofit)
        val authInterceptor = client.interceptors.first()

        val originalRequest = Request.Builder()
            .url("https://api.xiaomimimo.com/v1/chat/completions")
            .build()

        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns originalRequest
        every { chain.proceed(any()) } answers {
            val capturedRequest = firstArg<Request>()
            assertEquals("mimo-test-key", capturedRequest.header("api-key"))
            assertNull(capturedRequest.header("Authorization"))
            assertEquals("application/json", capturedRequest.header("Content-Type"))
            Response.Builder()
                .request(capturedRequest)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .build()
        }

        authInterceptor.intercept(chain)

        verify(exactly = 1) { chain.proceed(any()) }
    }

    @Test
    fun `createApi with ProviderConfig uses baseUrlOverride when provided`() {
        val config = ProviderConfig(
            id = "opencode",
            displayNameRes = luzzr.xi.R.string.provider_opencode,
            descriptionRes = luzzr.xi.R.string.provider_opencode_desc,
            defaultBaseUrl = "https://opencode.ai/zen/go/v1",
            defaultModel = "mimo-v2.5",
            authType = AuthType.BEARER
        )

        val api = apiProvider.createApi(
            providerConfig = config,
            baseUrlOverride = "https://custom.override.com/v1/",
            apiKey = "key"
        )

        val retrofit = extractRetrofit(api)
        val baseUrl = extractBaseUrl(retrofit)

        assertEquals("https://custom.override.com/v1/", baseUrl)
    }

    @Test
    fun `createApi with ProviderConfig falls back to defaultBaseUrl when override is blank`() {
        val config = ProviderConfig(
            id = "opencode",
            displayNameRes = luzzr.xi.R.string.provider_opencode,
            descriptionRes = luzzr.xi.R.string.provider_opencode_desc,
            defaultBaseUrl = "https://opencode.ai/zen/go/v1",
            defaultModel = "mimo-v2.5",
            authType = AuthType.BEARER
        )

        val api = apiProvider.createApi(
            providerConfig = config,
            baseUrlOverride = "",
            apiKey = "key"
        )

        val retrofit = extractRetrofit(api)
        val baseUrl = extractBaseUrl(retrofit)

        assertTrue("Should use default base URL", baseUrl.contains("opencode.ai"))
    }
}
