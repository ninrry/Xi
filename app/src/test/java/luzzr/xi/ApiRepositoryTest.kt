package luzzr.xi

import android.content.Context
import android.util.Log
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import luzzr.xi.core.network.ApiProvider
import luzzr.xi.core.network.NetworkCheck
import luzzr.xi.core.datastore.AppSettings
import luzzr.xi.core.datastore.SettingsDataStore
import luzzr.xi.data.repository.ApiRepository
import luzzr.xi.domain.model.AppError
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import luzzr.xi.core.testing.MainDispatcherRule
import org.junit.Test

private class TestableApiRepository(
    context: Context,
    settingsDataStore: SettingsDataStore,
    apiProvider: ApiProvider,
    networkCheck: NetworkCheck
) : ApiRepository(context, settingsDataStore, apiProvider, networkCheck) {

    suspend fun <T> testCallWithRetry(
        maxRetries: Int = 2,
        block: suspend () -> Result<T>
    ): Result<T> = callWithRetry(maxRetries, block)
}

@OptIn(ExperimentalCoroutinesApi::class)
class ApiRepositoryTest {

    @get:Rule
    val testDispatcherRule = MainDispatcherRule()

    private val context = mockk<Context>(relaxed = true)
    private val settingsDataStore = mockk<SettingsDataStore>(relaxed = true)
    private val networkCheck = mockk<NetworkCheck>(relaxed = true)
    private val apiProvider = mockk<ApiProvider>(relaxed = true)

    private lateinit var repository: TestableApiRepository

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { networkCheck.isNetworkAvailable() } returns true
        every { settingsDataStore.settings } returns flowOf(AppSettings(apiKey = "test-key-123"))
        every { context.getString(any()) } returns "API key is empty"

        repository = TestableApiRepository(context, settingsDataStore, apiProvider, networkCheck)
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `callWithRetry returns success on first attempt`() = runTest {
        val result = repository.testCallWithRetry {
            Result.success("hello")
        }

        assertTrue(result.isSuccess)
        assertEquals("hello", result.getOrNull())
    }

    @Test
    fun `callWithRetry retries on failure and succeeds on subsequent attempt`() = runTest {
        var attemptCount = 0

        val result = repository.testCallWithRetry(maxRetries = 2) {
            attemptCount++
            if (attemptCount < 2) {
                throw RuntimeException("Transient failure")
            }
            Result.success("recovered")
        }

        assertTrue(result.isSuccess)
        assertEquals("recovered", result.getOrNull())
        assertEquals(2, attemptCount)
    }

    @Test
    fun `callWithRetry returns failure after exhausting max retries`() = runTest {
        var attemptCount = 0

        val result = repository.testCallWithRetry<String>(maxRetries = 2) {
            attemptCount++
            throw RuntimeException("Persistent failure")
        }

        assertTrue(result.isFailure)
        assertEquals(3, attemptCount)
        assertTrue(result.exceptionOrNull() is AppError.NetworkError)
    }

    @Test
    fun `callWithRetry returns NetworkError when offline`() = runTest {
        every { networkCheck.isNetworkAvailable() } returns false

        val result = repository.testCallWithRetry {
            Result.success("should not reach")
        }

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppError.NetworkError)
    }

    @Test
    fun `callWithRetry returns ConfigError when API key is blank`() = runTest {
        every { settingsDataStore.settings } returns flowOf(AppSettings(apiKey = ""))

        val result = repository.testCallWithRetry {
            Result.success("should not reach")
        }

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppError.ConfigError)
    }

    @Test
    fun `callWithRetry respects custom maxRetries parameter`() = runTest {
        var attemptCount = 0

        val result = repository.testCallWithRetry<String>(maxRetries = 1) {
            attemptCount++
            throw RuntimeException("Always fails")
        }

        assertTrue(result.isFailure)
        assertEquals(2, attemptCount)
    }
}
