package luzzr.xi.feature.settings

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import luzzr.xi.core.datastore.SettingsDataStore
import luzzr.xi.core.datastore.AppSettings
import luzzr.xi.core.testing.MainDispatcherRule
import luzzr.xi.data.repository.MlKitModelManager
import luzzr.xi.data.repository.ApiRepositoryImpl
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial state loads settings correctly`() = runTest {
        val mockDataStore = mockk<SettingsDataStore>()
        val mockMlKitManager = mockk<MlKitModelManager>()
        val mockApiRepo = mockk<ApiRepositoryImpl>()

        coEvery { mockDataStore.settings } returns flowOf(AppSettings(apiKey = "test_key"))
        coEvery { mockMlKitManager.addProgressListener(any()) } returns Unit
        coEvery { mockMlKitManager.isModelDownloaded(any(), any(), any(), any()) } returns true

        val viewModel = SettingsViewModel(mockDataStore, mockMlKitManager, mockApiRepo)

        assertEquals("test_key", viewModel.uiState.value.settings.apiKey)
    }
}
