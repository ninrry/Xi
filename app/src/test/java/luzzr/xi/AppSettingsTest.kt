package luzzr.xi

import luzzr.xi.core.datastore.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AppSettingsTest {

    @Test
    fun `default settings have correct values`() {
        val settings = AppSettings()
        assertEquals("https://opencode.ai/zen/go/v1", settings.apiBaseUrl)
        assertEquals("", settings.apiKey)
        assertEquals("mimo-v2.5", settings.model)
        assertFalse(settings.proxyEnabled)
        assertEquals("", settings.proxyHost)
        assertEquals(0, settings.proxyPort)
        assertFalse(settings.overlayEnabled)
    }

    @Test
    fun `settings copy works correctly`() {
        val original = AppSettings()
        val modified = original.copy(
            apiKey = "sk-test123",
            model = "deepseek-v4-flash"
        )
        assertEquals("sk-test123", modified.apiKey)
        assertEquals("deepseek-v4-flash", modified.model)
        assertEquals(original.apiBaseUrl, modified.apiBaseUrl)
    }
}
