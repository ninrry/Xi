package luzzr.xi

import io.mockk.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import luzzr.xi.data.api.ApiProvider
import luzzr.xi.data.api.OpenAiApi
import luzzr.xi.data.local.AppSettings
import luzzr.xi.data.local.SettingsDataStore
import luzzr.xi.data.model.ModelInfo
import luzzr.xi.data.model.ModelListResponse
import luzzr.xi.data.repository.MlKitModelManager
import luzzr.xi.viewmodel.SettingsViewModel
import luzzr.xi.viewmodel.TestStatus
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val testDispatcherRule = TestDispatcherRule()

    private val settingsDataStore = mockk<SettingsDataStore>(relaxed = true)
    private val openAiApi = mockk<OpenAiApi>()
    private val mlKitModelManager = mockk<MlKitModelManager>(relaxed = true)

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        mockkObject(ApiProvider)
        every { settingsDataStore.settings } returns flowOf(AppSettings())
        viewModel = SettingsViewModel(settingsDataStore, mlKitModelManager)
    }

    @After
    fun tearDown() {
        unmockkObject(ApiProvider)
    }

    // ── init ──────────────────────────────────────────────────────────

    @Test
    fun `init collects settings from datastore and updates uiState`() {
        val custom = AppSettings(
            apiBaseUrl = "https://custom.api.com/v1",
            apiKey = "sk-test",
            model = "gpt-4o"
        )
        every { settingsDataStore.settings } returns flowOf(custom)
        val vm = SettingsViewModel(settingsDataStore, mlKitModelManager)

        assertEquals("https://custom.api.com/v1", vm.uiState.value.settings.apiBaseUrl)
        assertEquals("sk-test", vm.uiState.value.settings.apiKey)
        assertEquals("gpt-4o", vm.uiState.value.settings.model)
    }

    @Test
    fun `init starts with default idle test status and empty models`() {
        assertEquals(TestStatus.Idle, viewModel.uiState.value.testStatus)
        assertTrue(viewModel.uiState.value.availableModels.isEmpty())
    }

    @Test
    fun `init continuously collects settings updates from datastore`() {
        val flow = MutableStateFlow(AppSettings(apiBaseUrl = "https://initial.com/v1"))
        every { settingsDataStore.settings } returns flow
        val vm = SettingsViewModel(settingsDataStore, mlKitModelManager)

        // Initial emission is reflected in uiState
        assertEquals("https://initial.com/v1", vm.uiState.value.settings.apiBaseUrl)

        // Subsequent emissions also propagate
        flow.value = AppSettings(apiBaseUrl = "https://updated.com/v1", model = "gpt-4o")

        assertEquals("https://updated.com/v1", vm.uiState.value.settings.apiBaseUrl)
        assertEquals("gpt-4o", vm.uiState.value.settings.model)
    }

    // ── update delegates ──────────────────────────────────────────────

    @Test
    fun `updateApiBaseUrl delegates to datastore`() {
        viewModel.updateApiBaseUrl("https://new-url.com/v1")
        coVerify { settingsDataStore.updateApiBaseUrl("https://new-url.com/v1") }
    }

    @Test
    fun `updateApiKey delegates to datastore`() {
        viewModel.updateApiKey("sk-new-key")
        coVerify { settingsDataStore.updateApiKey("sk-new-key") }
    }

    @Test
    fun `updateModel delegates to datastore`() {
        viewModel.updateModel("deepseek-v4")
        coVerify { settingsDataStore.updateModel("deepseek-v4") }
    }

    // ── testConnection() success ──────────────────────────────────────

    @Test
    fun `testConnection success updates status and availableModels`() = runTest {
        val settings = AppSettings(apiBaseUrl = "https://api.test.com/v1", apiKey = "sk-key")
        every { settingsDataStore.settings } returns flowOf(settings)
        val vm = SettingsViewModel(settingsDataStore, mlKitModelManager)

        every {
            ApiProvider.createApi(
                baseUrl = "https://api.test.com/v1",
                apiKey = "sk-key",
                proxyEnabled = false,
                proxyHost = "",
                proxyPort = 0
            )
        } returns openAiApi

        val models = listOf(
            ModelInfo("gpt-4o", null),
            ModelInfo("gpt-3.5-turbo", null),
            ModelInfo("deepseek-v4", null)
        )
        coEvery { openAiApi.listModels() } returns ModelListResponse(data = models)

        vm.testConnection()

        val state = vm.uiState.value
        assertTrue("Expected Success but got ${state.testStatus}", state.testStatus is TestStatus.Success)
        assertEquals(3, (state.testStatus as TestStatus.Success).modelCount)
        assertEquals(listOf("gpt-4o", "gpt-3.5-turbo", "deepseek-v4"), state.availableModels)
    }

    @Test
    fun `testConnection success with null data returns empty models`() = runTest {
        every { settingsDataStore.settings } returns flowOf(AppSettings())
        val vm = SettingsViewModel(settingsDataStore, mlKitModelManager)

        every {
            ApiProvider.createApi(
                baseUrl = any(),
                apiKey = any(),
                proxyEnabled = any(),
                proxyHost = any(),
                proxyPort = any()
            )
        } returns openAiApi

        coEvery { openAiApi.listModels() } returns ModelListResponse(data = null)

        vm.testConnection()

        val state = vm.uiState.value
        assertTrue(state.testStatus is TestStatus.Success)
        assertEquals(0, (state.testStatus as TestStatus.Success).modelCount)
        assertTrue(state.availableModels.isEmpty())
    }

    // ── testConnection() Testing status ───────────────────────────────

    @Test
    fun `testConnection sets Testing status during execution`() = runTest {
        val settings = AppSettings(apiBaseUrl = "https://api.test.com/v1", apiKey = "sk-key")
        every { settingsDataStore.settings } returns flowOf(settings)
        val vm = SettingsViewModel(settingsDataStore, mlKitModelManager)

        every {
            ApiProvider.createApi(
                baseUrl = "https://api.test.com/v1",
                apiKey = "sk-key",
                proxyEnabled = false,
                proxyHost = "",
                proxyPort = 0
            )
        } returns openAiApi

        // CompletableDeferred suspends the coroutine inside listModels()
        // so we can observe the intermediate Testing state.
        val deferred = CompletableDeferred<ModelListResponse>()
        coEvery { openAiApi.listModels() } coAnswers { deferred.await() }

        // UnconfinedTestDispatcher runs the coroutine synchronously
        // up to the first suspension point (deferred.await()).
        vm.testConnection()

        // While suspended, testStatus must be Testing.
        assertEquals(TestStatus.Testing, vm.uiState.value.testStatus)

        // Complete the API call - coroutine resumes and finalizes.
        deferred.complete(ModelListResponse(data = listOf(ModelInfo("gpt-4o", null))))

        val state = vm.uiState.value
        assertTrue("Expected Success but got ${state.testStatus}", state.testStatus is TestStatus.Success)
        assertEquals(1, (state.testStatus as TestStatus.Success).modelCount)
    }

    // ── testConnection() failure ──────────────────────────────────────

    @Test
    fun `testConnection failure updates error status`() = runTest {
        every { settingsDataStore.settings } returns flowOf(AppSettings())
        val vm = SettingsViewModel(settingsDataStore, mlKitModelManager)

        every {
            ApiProvider.createApi(
                baseUrl = any(),
                apiKey = any(),
                proxyEnabled = any(),
                proxyHost = any(),
                proxyPort = any()
            )
        } returns openAiApi

        coEvery { openAiApi.listModels() } throws RuntimeException("Connection refused")

        vm.testConnection()

        val state = vm.uiState.value
        assertTrue("Expected Failure but got ${state.testStatus}", state.testStatus is TestStatus.Failure)
        assertEquals("Connection refused", (state.testStatus as TestStatus.Failure).message)
    }

    @Test
    fun `testConnection failure with null message uses fallback text`() = runTest {
        every { settingsDataStore.settings } returns flowOf(AppSettings())
        val vm = SettingsViewModel(settingsDataStore, mlKitModelManager)

        every {
            ApiProvider.createApi(
                baseUrl = any(),
                apiKey = any(),
                proxyEnabled = any(),
                proxyHost = any(),
                proxyPort = any()
            )
        } returns openAiApi

        coEvery { openAiApi.listModels() } throws RuntimeException()

        vm.testConnection()

        val state = vm.uiState.value
        assertTrue(state.testStatus is TestStatus.Failure)
        assertEquals("Unknown error", (state.testStatus as TestStatus.Failure).message)
    }

    @Test
    fun `testConnection passes proxy settings to ApiProvider`() = runTest {
        val settings = AppSettings(
            apiBaseUrl = "https://api.test.com/v1",
            apiKey = "sk-key",
            proxyEnabled = true,
            proxyHost = "proxy.local",
            proxyPort = 8080
        )
        every { settingsDataStore.settings } returns flowOf(settings)
        val vm = SettingsViewModel(settingsDataStore, mlKitModelManager)

        every {
            ApiProvider.createApi(
                baseUrl = "https://api.test.com/v1",
                apiKey = "sk-key",
                proxyEnabled = true,
                proxyHost = "proxy.local",
                proxyPort = 8080
            )
        } returns openAiApi

        coEvery { openAiApi.listModels() } returns ModelListResponse(data = emptyList())

        vm.testConnection()

        verify {
            ApiProvider.createApi(
                baseUrl = "https://api.test.com/v1",
                apiKey = "sk-key",
                proxyEnabled = true,
                proxyHost = "proxy.local",
                proxyPort = 8080
            )
        }
    }

}
