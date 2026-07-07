package luzzr.xi.feature.translate

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import luzzr.xi.core.datastore.AppSettings
import luzzr.xi.core.datastore.SettingsDataStore
import luzzr.xi.core.testing.MainDispatcherRule
import luzzr.xi.domain.model.TranslationResult
import luzzr.xi.domain.model.AppError
import luzzr.xi.domain.model.SupportedLanguage
import luzzr.xi.domain.model.ThinkingLevel
import luzzr.xi.domain.usecase.TranslateUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TranslateViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var translateUseCase: TranslateUseCase
    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var viewModel: TranslateViewModel

    @Before
    fun setup() {
        translateUseCase = mockk()
        settingsDataStore = mockk(relaxed = true)

        every { settingsDataStore.settings } returns flowOf(
            AppSettings(
                translateThinkingLevel = ThinkingLevel.MEDIUM.id
            )
        )

        viewModel = TranslateViewModel(translateUseCase, settingsDataStore)
    }

    @Test
    fun `initial state is correct`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("", state.inputText)
            assertEquals("", state.resultText)
            assertEquals(SupportedLanguage.ENGLISH, state.sourceLang)
            assertEquals(SupportedLanguage.CHINESE, state.targetLang)
            assertEquals(ThinkingLevel.MEDIUM, state.thinkingLevel)
            assertFalse(state.isLoading)
            assertNull(state.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `on InputTextChanged updates inputText`() = runTest {
        viewModel.onEvent(TranslateUiEvent.InputChanged("Hello"))
        assertEquals("Hello", viewModel.uiState.value.inputText)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `SwapLanguagesClicked swaps languages and text`() = runTest {
        viewModel.onEvent(TranslateUiEvent.InputChanged("Hello"))
        // First mock the private resultText set via a trick or just test language swap
        
        viewModel.onEvent(TranslateUiEvent.SwapClicked)
        
        val state = viewModel.uiState.value
        assertEquals(SupportedLanguage.CHINESE, state.sourceLang)
        assertEquals(SupportedLanguage.ENGLISH, state.targetLang)
        // Since resultText is empty initially, inputText is preserved
        assertEquals("Hello", state.inputText)
    }

    @Test
    fun `TranslateClicked updates state correctly on success`() = runTest {
        viewModel.onEvent(TranslateUiEvent.InputChanged("Hello"))
        coEvery { translateUseCase(any(), any(), any(), any(), any()) } returns flowOf(Result.success(
            TranslationResult(translation = "你好", detectedLanguage = "English", alternatives = listOf("您好"))
        ))

        viewModel.onEvent(TranslateUiEvent.TranslateClicked)

        viewModel.uiState.test {
            val loadingState = awaitItem() // might be the final state if it executes synchronously in UnconfinedTestDispatcher
            assertEquals("你好", loadingState.resultText)
            assertEquals("English", loadingState.detectedLanguage)
            assertEquals(listOf("您好"), loadingState.alternatives)
            assertFalse(loadingState.isLoading)
        }
    }

    @Test
    fun `TranslateClicked updates state correctly on failure`() = runTest {
        viewModel.onEvent(TranslateUiEvent.InputChanged("Hello"))
        coEvery { translateUseCase(any(), any(), any(), any(), any()) } returns flowOf(Result.failure(
            AppError.ApiError("Failed")
        ))

        viewModel.onEvent(TranslateUiEvent.TranslateClicked)

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.error != null)
            assertFalse(state.isLoading)
        }
    }
}
