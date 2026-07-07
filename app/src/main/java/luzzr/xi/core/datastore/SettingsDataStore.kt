package luzzr.xi.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

internal val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    val apiBaseUrl: String = "https://api.xiaomimimo.com/v1",
    val apiKey: String = "",
    val model: String = "mimo-v2.5",
    val proxyEnabled: Boolean = false,
    val proxyHost: String = "",
    val proxyPort: Int = 0,
    val overlayEnabled: Boolean = false,
    val translateThinkingLevel: String = "medium",
    val essayThinkingLevel: String = "high",
    val translationEngine: String = "mlkit",
    val providerId: String = "xiaomi_mimo"
)

@Singleton
class SettingsDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    private object Keys {
        val API_BASE_URL = stringPreferencesKey("api_base_url")
        val API_KEY = stringPreferencesKey("api_key")
        val MODEL = stringPreferencesKey("model")
        val PROXY_ENABLED = booleanPreferencesKey("proxy_enabled")
        val PROXY_HOST = stringPreferencesKey("proxy_host")
        val PROXY_PORT = intPreferencesKey("proxy_port")
        val OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
        val TRANSLATE_THINKING = stringPreferencesKey("translate_thinking_level")
        val ESSAY_THINKING = stringPreferencesKey("essay_thinking_level")
        val TRANSLATE_ENGINE = stringPreferencesKey("translation_engine")
        val PROVIDER_ID = stringPreferencesKey("provider_id")
    }

    val settings: Flow<AppSettings>
        get() = dataStore.data.map { prefs ->
        AppSettings(
            apiBaseUrl = prefs[Keys.API_BASE_URL] ?: "https://api.xiaomimimo.com/v1",
            apiKey = prefs[Keys.API_KEY] ?: "",
            model = prefs[Keys.MODEL] ?: "mimo-v2.5",
            proxyEnabled = prefs[Keys.PROXY_ENABLED] ?: false,
            proxyHost = prefs[Keys.PROXY_HOST] ?: "",
            proxyPort = prefs[Keys.PROXY_PORT] ?: 0,
            overlayEnabled = prefs[Keys.OVERLAY_ENABLED] ?: false,
            translateThinkingLevel = prefs[Keys.TRANSLATE_THINKING] ?: "medium",
            essayThinkingLevel = prefs[Keys.ESSAY_THINKING] ?: "high",
            translationEngine = prefs[Keys.TRANSLATE_ENGINE] ?: "mlkit",
            providerId = prefs[Keys.PROVIDER_ID] ?: "xiaomi_mimo"
        )
    }

    suspend fun updateApiBaseUrl(url: String) {
        dataStore.edit { it[Keys.API_BASE_URL] = url }
    }

    suspend fun updateApiKey(key: String) {
        dataStore.edit { it[Keys.API_KEY] = key }
    }

    suspend fun updateModel(model: String) {
        dataStore.edit { it[Keys.MODEL] = model }
    }

    suspend fun updateProxy(enabled: Boolean, host: String, port: Int) {
        dataStore.edit {
            it[Keys.PROXY_ENABLED] = enabled
            it[Keys.PROXY_HOST] = host
            it[Keys.PROXY_PORT] = port
        }
    }

    suspend fun updateOverlayEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.OVERLAY_ENABLED] = enabled }
    }

    suspend fun updateTranslateThinkingLevel(level: String) {
        dataStore.edit { it[Keys.TRANSLATE_THINKING] = level }
    }

    suspend fun updateEssayThinkingLevel(level: String) {
        dataStore.edit { it[Keys.ESSAY_THINKING] = level }
    }

    suspend fun updateTranslationEngine(engine: String) {
        dataStore.edit { it[Keys.TRANSLATE_ENGINE] = engine }
    }

    suspend fun updateProviderId(providerId: String) {
        dataStore.edit { it[Keys.PROVIDER_ID] = providerId }
    }
}
