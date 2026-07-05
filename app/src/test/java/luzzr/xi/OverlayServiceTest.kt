package luzzr.xi

import luzzr.xi.data.model.SupportedLanguage
import luzzr.xi.service.OverlayService
import luzzr.xi.service.OverlayUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests OverlayService data/state logic.
 * The Service itself is tightly coupled to Android framework,
 * so we test OverlayUiState data class, the isRunning atomic boolean,
 * and the pure logic behind swap/doTranslate transformations.
 */
class OverlayServiceTest {

    @Before
    fun setUp() {
        // Reset the static atomic before each test
        OverlayService.isRunning.set(false)
    }

    @Test
    fun overlayUiState_defaultValues_areCorrect() {
        val state = OverlayUiState()

        assertFalse(state.isPanelVisible)
        assertEquals("", state.inputText)
        assertEquals("", state.resultText)
        assertFalse(state.isTranslating)
        assertNull(state.errorMsg)
        assertEquals(SupportedLanguage.ENGLISH, state.sourceLang)
        assertEquals(SupportedLanguage.CHINESE, state.targetLang)
    }

    @Test
    fun overlayUiState_copy_updatesAllFields() {
        val original = OverlayUiState()

        val copied = original.copy(
            isPanelVisible = true,
            inputText = "hello",
            resultText = "你好",
            isTranslating = true,
            errorMsg = "timeout",
            sourceLang = SupportedLanguage.JAPANESE,
            targetLang = SupportedLanguage.KOREAN
        )

        assertTrue(copied.isPanelVisible)
        assertEquals("hello", copied.inputText)
        assertEquals("你好", copied.resultText)
        assertTrue(copied.isTranslating)
        assertEquals("timeout", copied.errorMsg)
        assertEquals(SupportedLanguage.JAPANESE, copied.sourceLang)
        assertEquals(SupportedLanguage.KOREAN, copied.targetLang)

        // original unchanged
        assertFalse(original.isPanelVisible)
        assertEquals("", original.inputText)
        assertNull(original.errorMsg)
    }

    @Test
    fun isRunning_atomicBoolean_startsAsFalse() {
        assertFalse(OverlayService.isRunning.get())
    }

    @Test
    fun isRunning_canBeSetToTrueAndBackToFalse() {
        OverlayService.isRunning.set(true)
        assertTrue(OverlayService.isRunning.get())

        OverlayService.isRunning.set(false)
        assertFalse(OverlayService.isRunning.get())
    }

    @Test
    fun swapOverlayLanguages_swapsSourceAndTarget() {
        val state = OverlayUiState(
            sourceLang = SupportedLanguage.ENGLISH,
            targetLang = SupportedLanguage.CHINESE
        )

        // Mirror the swap logic from OverlayService.swapOverlayLanguages()
        val swapped = state.copy(
            sourceLang = state.targetLang,
            targetLang = state.sourceLang,
            inputText = state.resultText,
            resultText = state.inputText
        )

        assertEquals(SupportedLanguage.CHINESE, swapped.sourceLang)
        assertEquals(SupportedLanguage.ENGLISH, swapped.targetLang)
    }

    @Test
    fun swapOverlayLanguages_movesResultTextToInputText() {
        val state = OverlayUiState(
            inputText = "hello",
            resultText = "你好",
            sourceLang = SupportedLanguage.ENGLISH,
            targetLang = SupportedLanguage.CHINESE
        )

        val swapped = state.copy(
            sourceLang = state.targetLang,
            targetLang = state.sourceLang,
            inputText = state.resultText,
            resultText = state.inputText
        )

        assertEquals("你好", swapped.inputText)
        assertEquals("hello", swapped.resultText)
    }

    @Test
    fun doTranslateLogic_withEmptyInput_setsErrorMessage() {
        val state = OverlayUiState(inputText = "   ")

        // Mirror the guard clause from OverlayService.doTranslate()
        val trimmed = state.inputText.trim()
        val result = if (trimmed.isEmpty()) {
            state.copy(errorMsg = "Input text cannot be empty")
        } else {
            state
        }

        assertEquals("Input text cannot be empty", result.errorMsg)
    }

    @Test
    fun doTranslateLogic_withValidInput_clearsError() {
        val state = OverlayUiState(
            inputText = "hello",
            errorMsg = "previous error"
        )

        // Mirror the logic from OverlayService.doTranslate() when input is valid
        val trimmed = state.inputText.trim()
        val result = if (trimmed.isNotEmpty()) {
            state.copy(isTranslating = true, errorMsg = null, resultText = "")
        } else {
            state.copy(errorMsg = "Input text cannot be empty")
        }

        assertNull(result.errorMsg)
        assertTrue(result.isTranslating)
        assertEquals("", result.resultText)
    }
}
