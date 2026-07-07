package luzzr.xi.feature.essay

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import luzzr.xi.core.datastore.SettingsDataStore
import luzzr.xi.core.datastore.AppSettings
import luzzr.xi.core.testing.MainDispatcherRule
import luzzr.xi.domain.model.CorrectionResult
import luzzr.xi.domain.usecase.CorrectEssayUseCase
import luzzr.xi.domain.model.ThinkingLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EssayViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial state loads settings correctly`() = runTest {
        val mockDataStore = mockk<SettingsDataStore>()
        val mockUseCase = mockk<CorrectEssayUseCase>()

        coEvery { mockDataStore.settings } returns flowOf(AppSettings(essayThinkingLevel = "medium"))

        val viewModel = EssayViewModel(mockUseCase, mockDataStore)

        assertEquals(ThinkingLevel.MEDIUM, viewModel.uiState.value.thinkingLevel)
    }

    @Test
    fun `correctEssay handles success correctly`() = runTest {
        val mockDataStore = mockk<SettingsDataStore>()
        val mockUseCase = mockk<CorrectEssayUseCase>()

        coEvery { mockDataStore.settings } returns flowOf(AppSettings())
        val fakeResult = CorrectionResult(
            grammarErrors = emptyList(),
            vocabulary = emptyList(),
            structure = null,
            style = null,
            score = null,
            correctedEssay = "Corrected",
            writingTips = emptyList(),
            usage = null
        )
        coEvery { mockUseCase.correctFromText(any(), any()) } returns Result.success(fakeResult)

        val viewModel = EssayViewModel(mockUseCase, mockDataStore)
        viewModel.onEvent(EssayUiEvent.EssayTextChanged("Test essay"))
        viewModel.onEvent(EssayUiEvent.InputModeChanged(InputMode.TEXT))
        viewModel.onEvent(EssayUiEvent.CorrectClicked)

        assertTrue(viewModel.uiState.value.hasResult)
        assertEquals("Corrected", viewModel.uiState.value.correctedEssay)
    }
}
