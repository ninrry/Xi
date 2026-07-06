package luzzr.xi

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import luzzr.xi.core.datastore.AppSettings
import luzzr.xi.core.datastore.SettingsDataStore
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsDataStoreTest {

    private val dataStore = mockk<DataStore<Preferences>>(relaxed = true)
    private lateinit var settingsDataStore: SettingsDataStore

    @Before
    fun setUp() {
        val emptyPrefs = androidx.datastore.preferences.core.mutablePreferencesOf()
        every { dataStore.data } returns flowOf(emptyPrefs)
        settingsDataStore = SettingsDataStore(mockk(relaxed = true), dataStore)
    }

    @Test
    fun `settings emits default values for fresh install`() = runTest {
        val settings = settingsDataStore.settings.first()
        assertEquals("https://opencode.ai/zen/go/v1", settings.apiBaseUrl)
        assertEquals("", settings.apiKey)
        assertEquals("mimo-v2.5", settings.model)
        assertFalse(settings.proxyEnabled)
        assertEquals("", settings.proxyHost)
        assertEquals(0, settings.proxyPort)
        assertFalse(settings.overlayEnabled)
        assertEquals("medium", settings.translateThinkingLevel)
        assertEquals("high", settings.essayThinkingLevel)
    }
}
