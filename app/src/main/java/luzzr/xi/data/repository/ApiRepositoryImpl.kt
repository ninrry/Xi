package luzzr.xi.data.repository

import android.content.Context
import luzzr.xi.core.datastore.SettingsDataStore
import luzzr.xi.core.network.OpenAiApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simple ApiRepository for settings testing.
 * Provides access to the cached API instance.
 */
@Singleton
class ApiRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    settingsDataStore: SettingsDataStore
) : ApiRepository(context, settingsDataStore) {

    /**
     * Get the current API instance for testing.
     */
    suspend fun getCurrentApi(): OpenAiApi = getApi()
}
