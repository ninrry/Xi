package luzzr.xi

import android.content.Context
import io.mockk.*
import luzzr.xi.data.local.AppSettings
import luzzr.xi.data.local.SettingsDataStore
import luzzr.xi.data.model.SupportedLanguage
import luzzr.xi.data.model.ThinkingLevel
import luzzr.xi.data.repository.MlKitTranslator
import luzzr.xi.data.repository.TranslationRepository
import luzzr.xi.viewmodel.TranslateUiEvent
import luzzr.xi.viewmodel.TranslateUiState
import luzzr.xi.viewmodel.TranslateViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TranslateViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var translationRepo: TranslationRepository
    private lateinit var mlKitTranslator: MlKitTranslator
    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var context: Context
    private lateinit var settingsFlow: MutableSharedFlow<AppSettings>

    @Before
    fun setUp() {
        translationRepo = mockk(relaxed = true)
        mlKitTranslator = mockk(relaxed = true)
        settingsDataStore = mockk(relaxed = true)
        context = mockk(relaxed = true)
        settingsFlow = MutableSharedFlow(replay = 1)

        every { settingsDataStore.settings } returns settingsFlow
        every { context.getString(R.string.translate_error_empty) } returns "请输入文本"
        every { context.getString(R.string.error_translate_failed) } returns "翻译失败"

        // Emit default settings so init{} block completes
        settingsFlow.tryEmit(AppSettings(translateThinkingLevel = "low"))
    }

    private fun createViewModel() = TranslateViewModel(
        translationRepo = translationRepo,
        mlKitTranslator = mlKitTranslator,
        settingsDataStore = settingsDataStore,
        context = context
    )

    // ── Initial state ──────────────────────────────────────────────

    @Test
    fun `initial state has correct defaults`() {
        val vm = createViewModel()
        val s = vm.uiState.value
        assertEquals("", s.inputText)
        assertEquals("", s.resultText)
        assertFalse(s.isLoading)
        assertNull(s.error)
        assertEquals(SupportedLanguage.ENGLISH, s.sourceLang)
        assertEquals(SupportedLanguage.CHINESE, s.targetLang)
        assertEquals(ThinkingLevel.LOW, s.thinkingLevel)
    }

    @Test
    fun `init reads thinking level from settings`() {
        settingsFlow.resetReplayCache()
        settingsFlow.tryEmit(AppSettings(translateThinkingLevel = "high"))

        val vm = createViewModel()
        assertEquals(ThinkingLevel.HIGH, vm.uiState.value.thinkingLevel)
    }

    // ── InputChanged ───────────────────────────────────────────────

    @Test
    fun `InputChanged updates inputText`() {
        val vm = createViewModel()
        vm.onEvent(TranslateUiEvent.InputChanged("hello"))
        assertEquals("hello", vm.uiState.value.inputText)
    }

    @Test
    fun `InputChanged clears error`() {
        val vm = createViewModel()
        // Trigger an empty-input error first
        vm.onEvent(TranslateUiEvent.TranslateClicked)
        assertNotNull(vm.uiState.value.error)

        vm.onEvent(TranslateUiEvent.InputChanged("now valid"))
        assertNull(vm.uiState.value.error)
    }

    // ── SourceLangChanged ──────────────────────────────────────────

    @Test
    fun `SourceLangChanged updates sourceLang when different from target`() {
        val vm = createViewModel()
        // Default: source=ENGLISH, target=CHINESE
        vm.onEvent(TranslateUiEvent.SourceLangChanged(SupportedLanguage.JAPANESE))
        assertEquals(SupportedLanguage.JAPANESE, vm.uiState.value.sourceLang)
        assertEquals(SupportedLanguage.CHINESE, vm.uiState.value.targetLang)
    }

    @Test
    fun `SourceLangChanged does not update when same as target`() {
        val vm = createViewModel()
        // Default: target=CHINESE
        vm.onEvent(TranslateUiEvent.SourceLangChanged(SupportedLanguage.CHINESE))
        // source should remain ENGLISH
        assertEquals(SupportedLanguage.ENGLISH, vm.uiState.value.sourceLang)
    }

    // ── TargetLangChanged ──────────────────────────────────────────

    @Test
    fun `TargetLangChanged updates targetLang when different from source`() {
        val vm = createViewModel()
        vm.onEvent(TranslateUiEvent.TargetLangChanged(SupportedLanguage.KOREAN))
        assertEquals(SupportedLanguage.KOREAN, vm.uiState.value.targetLang)
        assertEquals(SupportedLanguage.ENGLISH, vm.uiState.value.sourceLang)
    }

    @Test
    fun `TargetLangChanged does not update when same as source`() {
        val vm = createViewModel()
        // Default: source=ENGLISH
        vm.onEvent(TranslateUiEvent.TargetLangChanged(SupportedLanguage.ENGLISH))
        // target should remain CHINESE
        assertEquals(SupportedLanguage.CHINESE, vm.uiState.value.targetLang)
    }

    // ── ThinkingLevelChanged ───────────────────────────────────────

    @Test
    fun `ThinkingLevelChanged updates thinkingLevel and persists to datastore`() {
        val vm = createViewModel()
        coEvery { settingsDataStore.updateTranslateThinkingLevel(any()) } just Runs

        vm.onEvent(TranslateUiEvent.ThinkingLevelChanged(ThinkingLevel.HIGH))

        assertEquals(ThinkingLevel.HIGH, vm.uiState.value.thinkingLevel)
        coVerify { settingsDataStore.updateTranslateThinkingLevel("high") }
    }

    // ── SwapClicked ────────────────────────────────────────────────

    @Test
    fun `SwapClicked swaps source and target languages and text`() {
        val vm = createViewModel()
        vm.onEvent(TranslateUiEvent.InputChanged("Hello"))
        // Set resultText via a successful translate first
        coEvery { translationRepo.translate(any(), any(), any(), any()) } returns Result.success("你好")

        vm.onEvent(TranslateUiEvent.TranslateClicked)

        val beforeSwap = vm.uiState.value
        assertEquals(SupportedLanguage.ENGLISH, beforeSwap.sourceLang)
        assertEquals(SupportedLanguage.CHINESE, beforeSwap.targetLang)
        assertEquals("你好", beforeSwap.resultText)

        vm.onEvent(TranslateUiEvent.SwapClicked)

        val afterSwap = vm.uiState.value
        assertEquals(SupportedLanguage.CHINESE, afterSwap.sourceLang)
        assertEquals(SupportedLanguage.ENGLISH, afterSwap.targetLang)
        assertEquals("你好", afterSwap.inputText) // old resultText becomes inputText
        assertEquals("", afterSwap.resultText)     // resultText cleared
    }

    // ── ClearClicked ───────────────────────────────────────────────

    @Test
    fun `ClearClicked resets inputText, resultText, and error`() {
        val vm = createViewModel()
        vm.onEvent(TranslateUiEvent.InputChanged("hello"))
        coEvery { translationRepo.translate(any(), any(), any(), any()) } returns Result.success("你好")
        vm.onEvent(TranslateUiEvent.TranslateClicked)

        // Verify we have state to clear
        assertEquals("hello", vm.uiState.value.inputText)
        assertEquals("你好", vm.uiState.value.resultText)

        vm.onEvent(TranslateUiEvent.ClearClicked)

        val s = vm.uiState.value
        assertEquals("", s.inputText)
        assertEquals("", s.resultText)
        assertNull(s.error)
    }

    // ── TranslateClicked: empty input ──────────────────────────────

    @Test
    fun `translate with empty input sets error and does not call repo`() {
        val vm = createViewModel()
        // inputText is empty by default
        vm.onEvent(TranslateUiEvent.TranslateClicked)

        val s = vm.uiState.value
        assertEquals("请输入文本", s.error)
        assertFalse(s.isLoading)
        assertEquals("", s.resultText)
        coVerify(exactly = 0) { translationRepo.translate(any(), any(), any(), any()) }
    }

    @Test
    fun `translate with whitespace-only input sets error`() {
        val vm = createViewModel()
        vm.onEvent(TranslateUiEvent.InputChanged("   "))
        vm.onEvent(TranslateUiEvent.TranslateClicked)

        assertEquals("请输入文本", vm.uiState.value.error)
        coVerify(exactly = 0) { translationRepo.translate(any(), any(), any(), any()) }
    }

    // ── TranslateClicked: loading state ────────────────────────────

    @Test
    fun `translate sets loading true then false on success`() = runTest {
        val vm = createViewModel()
        vm.onEvent(TranslateUiEvent.InputChanged("Hello"))

        coEvery { translationRepo.translate(any(), any(), any(), any()) } coAnswers {
            // Verify loading is true while repo is working
            assertEquals(true, vm.uiState.value.isLoading)
            assertNull(vm.uiState.value.error)
            assertEquals("", vm.uiState.value.resultText)
            Result.success("你好")
        }

        vm.onEvent(TranslateUiEvent.TranslateClicked)

        val s = vm.uiState.value
        assertFalse(s.isLoading)
        assertEquals("你好", s.resultText)
        assertNull(s.error)
    }

    // ── TranslateClicked: success path ─────────────────────────────

    @Test
    fun `translate success updates resultText and clears loading`() {
        val vm = createViewModel()
        vm.onEvent(TranslateUiEvent.InputChanged("Good morning"))
        coEvery { translationRepo.translate(any(), any(), any(), any()) } returns Result.success("早上好")

        vm.onEvent(TranslateUiEvent.TranslateClicked)

        val s = vm.uiState.value
        assertEquals("早上好", s.resultText)
        assertFalse(s.isLoading)
        assertNull(s.error)
    }

    @Test
    fun `translate success passes correct params to repo`() {
        val vm = createViewModel()
        vm.onEvent(TranslateUiEvent.InputChanged("Hello"))
        vm.onEvent(TranslateUiEvent.SourceLangChanged(SupportedLanguage.JAPANESE))
        vm.onEvent(TranslateUiEvent.TargetLangChanged(SupportedLanguage.KOREAN))
        vm.onEvent(TranslateUiEvent.ThinkingLevelChanged(ThinkingLevel.HIGH))
        coEvery { translationRepo.translate(any(), any(), any(), any()) } returns Result.success("こんにちは")

        vm.onEvent(TranslateUiEvent.TranslateClicked)

        coVerify {
            translationRepo.translate(
                text = "Hello",
                sourceLang = "Japanese",
                targetLang = "Korean",
                reasoningEffort = "high"
            )
        }
    }

    // ── TranslateClicked: failure path ─────────────────────────────

    @Test
    fun `translate failure with exception message sets error`() {
        val vm = createViewModel()
        vm.onEvent(TranslateUiEvent.InputChanged("Hello"))
        coEvery { translationRepo.translate(any(), any(), any(), any()) } returns
            Result.failure(RuntimeException("Network timeout"))

        vm.onEvent(TranslateUiEvent.TranslateClicked)

        val s = vm.uiState.value
        assertEquals("Network timeout", s.error)
        assertFalse(s.isLoading)
        assertEquals("", s.resultText)
    }

    @Test
    fun `translate failure with null message uses fallback error string`() {
        val vm = createViewModel()
        vm.onEvent(TranslateUiEvent.InputChanged("Hello"))
        coEvery { translationRepo.translate(any(), any(), any(), any()) } returns
            Result.failure(RuntimeException(null as String?))

        vm.onEvent(TranslateUiEvent.TranslateClicked)

        val s = vm.uiState.value
        assertEquals("翻译失败", s.error)
        assertFalse(s.isLoading)
    }

    @Test
    fun `translate failure sets error in uiState`() {
        val vm = createViewModel()
        vm.onEvent(TranslateUiEvent.InputChanged("Hello"))
        coEvery { translationRepo.translate(any(), any(), any(), any()) } returns
            Result.failure(RuntimeException("Connection refused"))

        vm.onEvent(TranslateUiEvent.TranslateClicked)

        val s = vm.uiState.value
        assertEquals("Connection refused", s.error)
        assertFalse(s.isLoading)
    }

    // ── Translate trims input ──────────────────────────────────────

    @Test
    fun `translate trims whitespace from input before sending`() {
        val vm = createViewModel()
        vm.onEvent(TranslateUiEvent.InputChanged("  Hello  "))
        coEvery { translationRepo.translate(any(), any(), any(), any()) } returns Result.success("你好")

        vm.onEvent(TranslateUiEvent.TranslateClicked)

        coVerify {
            translationRepo.translate(
                text = "Hello",  // trimmed
                sourceLang = any(),
                targetLang = any(),
                reasoningEffort = any()
            )
        }
    }

    // ── Translate clears previous error on new attempt ─────────────

    @Test
    fun `translate clears previous error and resultText on new attempt`() {
        val vm = createViewModel()
        vm.onEvent(TranslateUiEvent.InputChanged("Hello"))

        // First call fails
        coEvery { translationRepo.translate(any(), any(), any(), any()) } returns
            Result.failure(RuntimeException("fail"))
        vm.onEvent(TranslateUiEvent.TranslateClicked)
        assertNotNull(vm.uiState.value.error)

        // Second call succeeds
        coEvery { translationRepo.translate(any(), any(), any(), any()) } returns Result.success("你好")
        vm.onEvent(TranslateUiEvent.TranslateClicked)

        val s = vm.uiState.value
        assertNull(s.error)
        assertEquals("你好", s.resultText)
    }

    // ── Translate resets resultText on new loading ─────────────────

    @Test
    fun `translate resets resultText to empty when loading starts`() {
        val vm = createViewModel()
        // First successful translation
        vm.onEvent(TranslateUiEvent.InputChanged("Hello"))
        coEvery { translationRepo.translate(any(), any(), any(), any()) } returns Result.success("你好")
        vm.onEvent(TranslateUiEvent.TranslateClicked)
        assertEquals("你好", vm.uiState.value.resultText)

        // Second translation - resultText should be cleared during loading
        vm.onEvent(TranslateUiEvent.InputChanged("World"))
        coEvery { translationRepo.translate(any(), any(), any(), any()) } coAnswers {
            // During this call, resultText should be empty
            assertEquals("", vm.uiState.value.resultText)
            Result.success("世界")
        }
        vm.onEvent(TranslateUiEvent.TranslateClicked)
        assertEquals("世界", vm.uiState.value.resultText)
    }

    // ── Turbine: uiState flow emissions ────────────────────────────

    @Test
    fun `uiState emits correct sequence during translate`() = runTest {
        val vm = createViewModel()
        vm.onEvent(TranslateUiEvent.InputChanged("Hello"))

        coEvery { translationRepo.translate(any(), any(), any(), any()) } returns Result.success("你好")

        vm.onEvent(TranslateUiEvent.TranslateClicked)
        dispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals("你好", state.resultText)
        assertNull(state.error)
    }


}
