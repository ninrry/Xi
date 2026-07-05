package luzzr.xi

import luzzr.xi.data.model.SupportedLanguage
import luzzr.xi.service.OverlayUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayUiStateTest {

    @Test
    fun defaultValues_areCorrect_forNewInstance() {
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
    fun copy_updatesAllFields_correctly() {
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
        assertEquals("", original.resultText)
        assertFalse(original.isTranslating)
        assertNull(original.errorMsg)
        assertEquals(SupportedLanguage.ENGLISH, original.sourceLang)
        assertEquals(SupportedLanguage.CHINESE, original.targetLang)
    }
}
