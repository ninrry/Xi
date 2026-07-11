package luzzr.xi

import androidx.test.ext.junit.runners.AndroidJUnit4
import luzzr.xi.data.local.AppSettings
import luzzr.xi.data.model.SupportedLanguage
import luzzr.xi.data.model.ThinkingLevel
import luzzr.xi.domain.model.AppError
import luzzr.xi.service.OverlayUiState
import luzzr.xi.ui.navigation.Screen
import luzzr.xi.viewmodel.EssayUiState
import luzzr.xi.viewmodel.InputMode
import luzzr.xi.viewmodel.SettingsUiState
import luzzr.xi.viewmodel.TestStatus
import luzzr.xi.viewmodel.TranslateUiState
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for OverlayComponents and Screen navigation.
 *
 * These are pure logic tests that verify data classes, enums, and sealed classes
 * without requiring actual Compose rendering.
 */
@RunWith(AndroidJUnit4::class)
class ComposeUiTest {

    // ==================== Screen sealed class tests ====================

    @Test
    fun screen_allScreensHaveCorrectRoutes() {
        assertEquals("translate", Screen.Translate.route)
        assertEquals("essay", Screen.Essay.route)
        assertEquals("history", Screen.History.route)
        assertEquals("settings", Screen.Settings.route)
    }

    @Test
    fun screen_allScreensHaveValidTitleResId() {
        // titleResId should be a valid positive resource ID
        assertTrue("Translate titleResId should be positive", Screen.Translate.titleResId > 0)
        assertTrue("Essay titleResId should be positive", Screen.Essay.titleResId > 0)
        assertTrue("History titleResId should be positive", Screen.History.titleResId > 0)
        assertTrue("Settings titleResId should be positive", Screen.Settings.titleResId > 0)

        // All screens should have distinct resource IDs
        val titleResIds = setOf(
            Screen.Translate.titleResId,
            Screen.Essay.titleResId,
            Screen.History.titleResId,
            Screen.Settings.titleResId
        )
        assertEquals("All screens should have unique titleResId", 4, titleResIds.size)
    }

    // ==================== SupportedLanguage enum tests ====================

    @Test
    fun supportedLanguage_getByCodeReturnsCorrectLanguage() {
        assertEquals(SupportedLanguage.CHINESE, SupportedLanguage.getByCode("zh"))
        assertEquals(SupportedLanguage.ENGLISH, SupportedLanguage.getByCode("en"))
        assertEquals(SupportedLanguage.JAPANESE, SupportedLanguage.getByCode("ja"))
        assertEquals(SupportedLanguage.KOREAN, SupportedLanguage.getByCode("ko"))
        assertEquals(SupportedLanguage.SPANISH, SupportedLanguage.getByCode("es"))
        assertEquals(SupportedLanguage.FRENCH, SupportedLanguage.getByCode("fr"))
        assertEquals(SupportedLanguage.GERMAN, SupportedLanguage.getByCode("de"))
        assertEquals(SupportedLanguage.ITALIAN, SupportedLanguage.getByCode("it"))
        assertEquals(SupportedLanguage.PORTUGUESE, SupportedLanguage.getByCode("pt"))
        assertEquals(SupportedLanguage.RUSSIAN, SupportedLanguage.getByCode("ru"))
        assertEquals(SupportedLanguage.ARABIC, SupportedLanguage.getByCode("ar"))
        assertEquals(SupportedLanguage.HINDI, SupportedLanguage.getByCode("hi"))
        assertEquals(SupportedLanguage.THAI, SupportedLanguage.getByCode("th"))
        assertEquals(SupportedLanguage.VIETNAMESE, SupportedLanguage.getByCode("vi"))
        assertEquals(SupportedLanguage.INDONESIAN, SupportedLanguage.getByCode("id"))
        assertEquals(SupportedLanguage.TURKISH, SupportedLanguage.getByCode("tr"))
        assertEquals(SupportedLanguage.DUTCH, SupportedLanguage.getByCode("nl"))
        assertEquals(SupportedLanguage.POLISH, SupportedLanguage.getByCode("pl"))
        assertEquals(SupportedLanguage.SWEDISH, SupportedLanguage.getByCode("sv"))
        assertEquals(SupportedLanguage.UKRAINIAN, SupportedLanguage.getByCode("uk"))
    }

    @Test
    fun supportedLanguage_getByCodeReturnsEnglishForInvalidCode() {
        assertEquals(SupportedLanguage.ENGLISH, SupportedLanguage.getByCode("invalid"))
        assertEquals(SupportedLanguage.ENGLISH, SupportedLanguage.getByCode(""))
        assertEquals(SupportedLanguage.ENGLISH, SupportedLanguage.getByCode("xx"))
        assertEquals(SupportedLanguage.ENGLISH, SupportedLanguage.getByCode("EN"))
    }

    // ==================== ThinkingLevel enum tests ====================

    @Test
    fun thinkingLevel_fromIdReturnsCorrectLevel() {
        assertEquals(ThinkingLevel.LOW, ThinkingLevel.fromId("low"))
        assertEquals(ThinkingLevel.MEDIUM, ThinkingLevel.fromId("medium"))
        assertEquals(ThinkingLevel.HIGH, ThinkingLevel.fromId("high"))
        assertEquals(ThinkingLevel.MEDIUM, ThinkingLevel.fromId("xhigh"))
    }

    @Test
    fun thinkingLevel_fromIdReturnsMediumForInvalidId() {
        assertEquals(ThinkingLevel.MEDIUM, ThinkingLevel.fromId("invalid"))
        assertEquals(ThinkingLevel.MEDIUM, ThinkingLevel.fromId(""))
        assertEquals(ThinkingLevel.MEDIUM, ThinkingLevel.fromId("MEDIUM"))
        assertEquals(ThinkingLevel.MEDIUM, ThinkingLevel.fromId("ultra"))
    }

    // ==================== AppError sealed class tests ====================

    @Test
    fun appError_allErrorTypesHaveCorrectMessages() {
        // NetworkError
        val networkError = AppError.NetworkError()
        assertEquals("Network Error", networkError.message)

        // ApiError
        val apiError = AppError.ApiError("Rate limit exceeded", code = 429)
        assertEquals("Rate limit exceeded", apiError.message)
        assertEquals(429, apiError.code)

        // ConfigError
        val configError = AppError.ConfigError("Invalid API key")
        assertEquals("Invalid API key", configError.message)

        // EmptyResultError with default message
        val emptyResultDefault = AppError.EmptyResultError()
        assertEquals("Result is empty", emptyResultDefault.message)

        // EmptyResultError with custom message
        val emptyResultCustom = AppError.EmptyResultError("No translations found")
        assertEquals("No translations found", emptyResultCustom.message)

        // UnknownError
        val cause = RuntimeException("something broke")
        val unknownError = AppError.UnknownError(cause)
        assertEquals("Unknown error: something broke", unknownError.message)
        assertEquals(cause, unknownError.cause)
    }

    // ==================== AppSettings data class tests ====================

    @Test
    fun appSettings_defaultValuesAreCorrect() {
        val settings = AppSettings()

        assertEquals("https://opencode.ai/zen/go/v1", settings.apiBaseUrl)
        assertEquals("", settings.apiKey)
        assertEquals("mimo-v2.5", settings.model)
        assertFalse(settings.proxyEnabled)
        assertEquals("", settings.proxyHost)
        assertEquals(0, settings.proxyPort)
        assertFalse(settings.overlayEnabled)
        assertEquals("low", settings.translateThinkingLevel)
        assertEquals("xhigh", settings.essayThinkingLevel)
    }

    @Test
    fun appSettings_copyWorksCorrectly() {
        val original = AppSettings()

        // Copy with single field change
        val withApiKey = original.copy(apiKey = "sk-test-key-123")
        assertEquals("sk-test-key-123", withApiKey.apiKey)
        // Other fields should remain default
        assertEquals(original.apiBaseUrl, withApiKey.apiBaseUrl)
        assertEquals(original.model, withApiKey.model)

        // Copy with multiple field changes
        val customized = original.copy(
            apiBaseUrl = "https://custom.api.com/v1",
            model = "gpt-4",
            proxyEnabled = true,
            proxyHost = "127.0.0.1",
            proxyPort = 8080,
            overlayEnabled = true
        )
        assertEquals("https://custom.api.com/v1", customized.apiBaseUrl)
        assertEquals("gpt-4", customized.model)
        assertTrue(customized.proxyEnabled)
        assertEquals("127.0.0.1", customized.proxyHost)
        assertEquals(8080, customized.proxyPort)
        assertTrue(customized.overlayEnabled)
        // Unchanged fields
        assertEquals(original.apiKey, customized.apiKey)
        assertEquals(original.translateThinkingLevel, customized.translateThinkingLevel)
        assertEquals(original.essayThinkingLevel, customized.essayThinkingLevel)
    }

    // ==================== UI State default values tests ====================

    @Test
    fun translateUiState_defaultValues() {
        val state = TranslateUiState()

        assertEquals("", state.inputText)
        assertEquals("", state.resultText)
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(SupportedLanguage.ENGLISH, state.sourceLang)
        assertEquals(SupportedLanguage.CHINESE, state.targetLang)
        assertEquals(ThinkingLevel.LOW, state.thinkingLevel)
    }

    @Test
    fun essayUiState_defaultValues() {
        val state = EssayUiState()

        assertEquals("", state.essayText)
        assertEquals("", state.corrections)
        assertEquals("", state.correctedEssay)
        assertEquals("", state.overallScore)
        assertEquals("", state.writingTips)
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertFalse(state.hasResult)
        assertEquals(InputMode.TEXT, state.inputMode)
        assertNull(state.imageUriString)
        assertEquals(0, state.pdfPageCount)
        assertEquals(0, state.pdfCurrentPage)
        assertEquals(ThinkingLevel.HIGH, state.thinkingLevel)
    }

    @Test
    fun settingsUiState_defaultValues() {
        val state = SettingsUiState()

        // Default settings should match AppSettings defaults
        assertEquals(AppSettings(), state.settings)
        assertEquals(TestStatus.Idle, state.testStatus)
        assertTrue(state.availableModels.isEmpty())
    }

    @Test
    fun overlayUiState_defaultValues() {
        val state = OverlayUiState()

        assertFalse(state.isPanelVisible)
        assertEquals("", state.inputText)
        assertEquals("", state.resultText)
        assertFalse(state.isTranslating)
        assertNull(state.errorMsg)
        assertEquals(SupportedLanguage.ENGLISH, state.sourceLang)
        assertEquals(SupportedLanguage.CHINESE, state.targetLang)
    }
}
