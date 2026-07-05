package luzzr.xi

import android.net.Uri
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import luzzr.xi.data.local.AppSettings
import luzzr.xi.data.local.SettingsDataStore
import luzzr.xi.data.model.ThinkingLevel
import luzzr.xi.data.repository.CorrectionResult
import luzzr.xi.data.repository.EssayRepository
import luzzr.xi.data.repository.MediaProcessor
import luzzr.xi.viewmodel.EssayUiEvent
import luzzr.xi.viewmodel.EssayViewModel
import luzzr.xi.viewmodel.InputMode
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EssayViewModelTest {

    @get:Rule
    val testDispatcherRule = TestDispatcherRule()

    private val essayRepo = mockk<EssayRepository>(relaxed = true)
    private val mediaProcessor = mockk<MediaProcessor>(relaxed = true)
    private val settingsDataStore = mockk<SettingsDataStore>(relaxed = true)
    private val context = mockk<android.content.Context>(relaxed = true)

    private lateinit var viewModel: EssayViewModel

    @Before
    fun setUp() {
        val appSettings = AppSettings(essayThinkingLevel = "high")
        every { settingsDataStore.settings } returns flowOf(appSettings)
        every { context.getString(any<Int>()) } returns ""
        every { context.getString(any<Int>(), any()) } returns ""
        viewModel = EssayViewModel(essayRepo, mediaProcessor, settingsDataStore, context)
    }

    @Test
    fun `initialization loads thinking level from settings`() {
        assertEquals(ThinkingLevel.HIGH, viewModel.uiState.value.thinkingLevel)
    }

    @Test
    fun `onEvent EssayTextChanged updates text and clears error`() {
        viewModel.onEvent(EssayUiEvent.ErrorDismissed("Some error"))
        viewModel.onEvent(EssayUiEvent.EssayTextChanged("New essay content"))
        assertEquals("New essay content", viewModel.uiState.value.essayText)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `onEvent ErrorDismissed clears error field`() {
        viewModel.onEvent(EssayUiEvent.ErrorDismissed())
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `onEvent InputModeChanged updates mode and resets media uri`() {
        val mockUri = mockk<Uri>()
        viewModel.onEvent(EssayUiEvent.ImageUriSelected(mockUri))
        viewModel.onEvent(EssayUiEvent.InputModeChanged(InputMode.TEXT))
        assertEquals(InputMode.TEXT, viewModel.uiState.value.inputMode)
        assertNull(viewModel.uiState.value.imageUri)
        assertEquals(0, viewModel.uiState.value.pdfPageCount)
    }

    @Test
    fun `onEvent ClearClicked clears fields but preserves thinking level`() {
        viewModel.onEvent(EssayUiEvent.EssayTextChanged("To be cleared text"))
        viewModel.onEvent(EssayUiEvent.ErrorDismissed("Error to be cleared"))
        viewModel.onEvent(EssayUiEvent.ClearClicked)
        assertEquals("", viewModel.uiState.value.essayText)
        assertNull(viewModel.uiState.value.error)
        assertEquals(ThinkingLevel.HIGH, viewModel.uiState.value.thinkingLevel)
    }

    @Test
    fun `onEvent CorrectClicked with empty text triggers warning error`() = runTest {
        viewModel.onEvent(EssayUiEvent.InputModeChanged(InputMode.TEXT))
        viewModel.onEvent(EssayUiEvent.EssayTextChanged(""))
        viewModel.onEvent(EssayUiEvent.CorrectClicked)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.error)
    }

    @Test
    fun `onEvent CorrectClicked success updates results`() = runTest {
        viewModel.onEvent(EssayUiEvent.InputModeChanged(InputMode.TEXT))
        viewModel.onEvent(EssayUiEvent.EssayTextChanged("My english essay text"))

        val response = CorrectionResult(
            corrections = "corrections-diff",
            correctedEssay = "corrected-result",
            overallScore = "90",
            writingTips = "some tips"
        )
        coEvery { essayRepo.correctEssay("My english essay text", "high") } returns Result.success(response)

        viewModel.onEvent(EssayUiEvent.CorrectClicked)

        val state = viewModel.uiState.value
        assertEquals("corrections-diff", state.corrections)
        assertEquals("corrected-result", state.correctedEssay)
        assertEquals("90", state.overallScore)
        assertEquals("some tips", state.writingTips)
        assertTrue(state.hasResult)
        assertNull(state.error)
    }

    @Test
    fun `onEvent CorrectClicked failure updates error`() = runTest {
        viewModel.onEvent(EssayUiEvent.InputModeChanged(InputMode.TEXT))
        viewModel.onEvent(EssayUiEvent.EssayTextChanged("My english essay text"))

        coEvery { essayRepo.correctEssay("My english essay text", "high") } returns Result.failure(Exception("API Connection timeout"))

        viewModel.onEvent(EssayUiEvent.CorrectClicked)

        val state = viewModel.uiState.value
        assertEquals("API Connection timeout", state.error)
        assertFalse(state.hasResult)
    }

    @Test
    fun `onEvent ThinkingLevelChanged updates thinking level`() {
        viewModel.onEvent(EssayUiEvent.ThinkingLevelChanged(ThinkingLevel.LOW))
        assertEquals(ThinkingLevel.LOW, viewModel.uiState.value.thinkingLevel)
    }

    @Test
    fun `onEvent ImageUriSelected sets imageUri and switches to IMAGE mode`() {
        val mockUri = mockk<Uri>()
        viewModel.onEvent(EssayUiEvent.ImageUriSelected(mockUri))
        assertEquals(mockUri, viewModel.uiState.value.imageUri)
        assertEquals(InputMode.IMAGE, viewModel.uiState.value.inputMode)
    }

    @Test
    fun `onEvent PdfUriSelected calls mediaProcessor and sets pdfPageCount`() = runTest {
        val mockUri = mockk<Uri>()
        val fakeBitmap1 = mockk<android.graphics.Bitmap>()
        val fakeBitmap2 = mockk<android.graphics.Bitmap>()
        coEvery { mediaProcessor.renderPdfPages(mockUri) } returns listOf(fakeBitmap1, fakeBitmap2)

        viewModel.onEvent(EssayUiEvent.PdfUriSelected(mockUri))
        testDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify { mediaProcessor.renderPdfPages(mockUri) }
        assertEquals(mockUri, viewModel.uiState.value.imageUri)
        assertEquals(2, viewModel.uiState.value.pdfPageCount)
        assertEquals(0, viewModel.uiState.value.pdfCurrentPage)
        assertEquals(InputMode.PDF, viewModel.uiState.value.inputMode)
    }

    @Test
    fun `onEvent CorrectClicked in IMAGE mode with null uri triggers error`() = runTest {
        viewModel.onEvent(EssayUiEvent.InputModeChanged(InputMode.IMAGE))
        viewModel.onEvent(EssayUiEvent.CorrectClicked)

        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.error)
    }

    @Test
    fun `onEvent CorrectClicked in IMAGE mode with uri calls correctEssayFromImage`() = runTest {
        val mockUri = mockk<Uri>()
        viewModel.onEvent(EssayUiEvent.ImageUriSelected(mockUri))

        val response = CorrectionResult(
            corrections = "img-corrections",
            correctedEssay = "img-corrected",
            overallScore = "85",
            writingTips = "img-tips"
        )
        coEvery { essayRepo.correctEssayFromImage(mockUri, "high") } returns Result.success(response)

        viewModel.onEvent(EssayUiEvent.CorrectClicked)
        testDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify { essayRepo.correctEssayFromImage(mockUri, "high") }
        assertTrue(viewModel.uiState.value.hasResult)
        assertEquals("img-corrections", viewModel.uiState.value.corrections)
    }

    @Test
    fun `onEvent CorrectClicked in PDF mode with null uri triggers error`() = runTest {
        viewModel.onEvent(EssayUiEvent.InputModeChanged(InputMode.PDF))
        viewModel.onEvent(EssayUiEvent.CorrectClicked)

        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.error)
    }

    @Test
    fun `onEvent CorrectClicked in PDF mode with uri calls correctEssayFromBase64Images`() = runTest {
        val mockUri = mockk<Uri>()
        val fakeBitmap = mockk<android.graphics.Bitmap>()
        coEvery { mediaProcessor.renderPdfPages(mockUri) } returns listOf(fakeBitmap)
        coEvery { mediaProcessor.renderPdfPagesAsBase64(mockUri) } returns listOf("base64page1")
        viewModel.onEvent(EssayUiEvent.PdfUriSelected(mockUri))
        testDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val response = CorrectionResult(
            corrections = "pdf-corrections",
            correctedEssay = "pdf-corrected",
            overallScore = "88",
            writingTips = "pdf-tips"
        )
        coEvery { essayRepo.correctEssayFromBase64Images(listOf("base64page1"), "high") } returns Result.success(response)

        viewModel.onEvent(EssayUiEvent.CorrectClicked)
        testDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify { essayRepo.correctEssayFromBase64Images(listOf("base64page1"), "high") }
        assertTrue(viewModel.uiState.value.hasResult)
        assertEquals("pdf-corrections", viewModel.uiState.value.corrections)
    }

    @Test
    fun `onEvent CorrectClicked failure emits ShowError effect`() = runTest {
        viewModel.onEvent(EssayUiEvent.InputModeChanged(InputMode.TEXT))
        viewModel.onEvent(EssayUiEvent.EssayTextChanged("Some essay"))

        coEvery { essayRepo.correctEssay("Some essay", "high") } returns Result.failure(Exception("Network error"))

        viewModel.onEvent(EssayUiEvent.CorrectClicked)
        testDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.error)
    }

    @Test
    fun `onEvent ThinkingLevelChanged persists to datastore`() = runTest {
        viewModel.onEvent(EssayUiEvent.ThinkingLevelChanged(ThinkingLevel.LOW))
        testDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify { settingsDataStore.updateEssayThinkingLevel("low") }
        assertEquals(ThinkingLevel.LOW, viewModel.uiState.value.thinkingLevel)
    }
}
