package luzzr.xi

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import luzzr.xi.core.network.ApiProvider
import luzzr.xi.core.network.OpenAiApi
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

    // ── helpers ──────────────────────────────────────────────────────

    /**
     * Extract the Retrofit instance from the dynamic proxy returned by createApi.
     * Retrofit wraps the service interface in a java.lang.reflect.Proxy whose
     * InvocationHandler holds a reference to the Retrofit instance.
     */
    private fun extractRetrofit(api: OpenAiApi): Retrofit {
        val handler = java.lang.reflect.Proxy.getInvocationHandler(api)
        val field = handler.javaClass.declaredFields.first {
            Retrofit::class.java.isAssignableFrom(it.type)
        }
        field.isAccessible = true
        return field.get(handler) as Retrofit
    }

    /**
     * Extract the OkHttpClient (Call.Factory) from a Retrofit instance via reflection.
     */
    private fun extractClient(retrofit: Retrofit): OkHttpClient {
        val field = retrofit.javaClass.getDeclaredField("callFactory")
        field.isAccessible = true
        return field.get(retrofit) as OkHttpClient
    }

    /**
     * Extract the base URL from a Retrofit instance via reflection.
     */
    private fun extractBaseUrl(retrofit: Retrofit): String {
        val field = retrofit.javaClass.getDeclaredField("baseUrl")
        field.isAccessible = true
        val httpUrl = field.get(retrofit)
        return httpUrl.toString()
    }

    // ── tests ────────────────────────────────────────────────────────

    @Test
    fun `createApi returns OpenAiApi instance`() {
        val api = ApiProvider.createApi(
            baseUrl = "https://api.example.com",
            apiKey = "sk-test-key"
        )

        assertNotNull(api)
        // Verify it's a Retrofit dynamic proxy implementing OpenAiApi
        assertTrue(java.lang.reflect.Proxy.isProxyClass(api.javaClass))
        assertTrue(api is OpenAiApi)
    }

    @Test
    fun `createApi adds trailing slash when missing`() {
        val withoutSlash = ApiProvider.createApi(
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
        val withSlash = ApiProvider.createApi(
            baseUrl = "https://api.example.com/v1/",
            apiKey = "key"
        )
        val retrofit = extractRetrofit(withSlash)
        val baseUrl = extractBaseUrl(retrofit)

        assertTrue(
            "Base URL should end with slash",
            baseUrl.endsWith("/")
        )
        // Should not double-slash
        assertEquals(
            "https://api.example.com/v1/",
            baseUrl
        )
    }

    @Test
    fun `createApi configures proxy when enabled`() {
        val api = ApiProvider.createApi(
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
        // InetSocketAddress address
        val address = proxy.address() as java.net.InetSocketAddress
        // InetSocketAddress may resolve 127.0.0.1 to localhost
        assertTrue(
            "Proxy host should be 127.0.0.1 or localhost",
            address.hostName == "127.0.0.1" || address.hostName == "localhost"
        )
        assertEquals(8080, address.port)
    }

    @Test
    fun `createApi skips proxy when disabled`() {
        val api = ApiProvider.createApi(
            baseUrl = "https://api.example.com",
            apiKey = "key",
            proxyEnabled = false,
            proxyHost = "127.0.0.1",
            proxyPort = 8080
        )

        val retrofit = extractRetrofit(api)
        val client = extractClient(retrofit)
        val proxy = client.proxy

        assertNull("Proxy should be null when disabled", proxy)
    }

    @Test
    fun `createApi sets auth header via interceptor`() {
        val api = ApiProvider.createApi(
            baseUrl = "https://api.example.com",
            apiKey = "sk-secret-123"
        )

        val retrofit = extractRetrofit(api)
        val client = extractClient(retrofit)
        val interceptors = client.interceptors

        assertTrue("Should have at least one interceptor", interceptors.isNotEmpty())

        // The first interceptor added is the auth interceptor
        val authInterceptor = interceptors.first()

        // Build a mock chain to invoke the interceptor and verify headers
        val originalRequest = Request.Builder()
            .url("https://api.example.com/test")
            .build()

        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns originalRequest
        every { chain.proceed(any()) } answers {
            val capturedRequest = firstArg<Request>()
            // Verify Authorization header was added
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
}
