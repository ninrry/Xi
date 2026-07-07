package luzzr.xi.data.repository

import android.content.Context
import luzzr.xi.core.datastore.SettingsDataStore
import luzzr.xi.core.network.ApiProvider
import luzzr.xi.core.network.NetworkCheck
import luzzr.xi.core.network.OpenAiApi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import luzzr.xi.domain.repository.SettingsGateway

@Singleton
class ApiRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    settingsDataStore: SettingsDataStore,
    apiProvider: ApiProvider,
    networkCheck: NetworkCheck
) : ApiRepository(context, settingsDataStore, apiProvider, networkCheck), SettingsGateway {

    suspend fun getCurrentApi(): OpenAiApi = getApi()
}
