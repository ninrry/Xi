package luzzr.xi.feature.settings
import luzzr.xi.domain.model.*

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import luzzr.xi.core.network.ApiProvider
import luzzr.xi.core.datastore.AppSettings
import luzzr.xi.core.datastore.SettingsDataStore
import luzzr.xi.domain.model.SupportedLanguage
import luzzr.xi.domain.model.UiText
import luzzr.xi.data.repository.ApiRepositoryImpl
import luzzr.xi.data.repository.ModelDownloadProgress
import luzzr.xi.data.repository.ModelDownloadState
import luzzr.xi.data.repository.MlKitModelManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val testStatus: TestStatus = TestStatus.Idle,
    val availableModels: List<String> = emptyList(),
    val mlKitDownloadState: ModelDownloadState = ModelDownloadState.IDLE,
    val mlKitDownloadMessage: UiText? = null,
    val mlKitDownloadProgress: Float = 0f,
    val mlKitDownloading: Boolean = false
)

sealed class TestStatus {
    data object Idle : TestStatus()
    data object Testing : TestStatus()
    data class Success(val modelCount: Int) : TestStatus()
    data class Failure(val message: String) : TestStatus()
}

sealed interface SettingsUiEvent {
    data class ApiBaseUrlChanged(val url: String) : SettingsUiEvent
    data class ApiKeyChanged(val key: String) : SettingsUiEvent
    data class ModelChanged(val model: String) : SettingsUiEvent
    data object TestConnectionClicked : SettingsUiEvent
    data object DownloadMlKitModelClicked : SettingsUiEvent
    data object CancelMlKitDownloadClicked : SettingsUiEvent
    data object RetryMlKitDownloadClicked : SettingsUiEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val mlKitModelManager: MlKitModelManager,
    private val apiRepository: ApiRepositoryImpl,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val defaultSourceLang = SupportedLanguage.ENGLISH.displayName
    private val defaultTargetLang = SupportedLanguage.CHINESE.displayName
    private val defaultSourceCode = "en"
    private val defaultTargetCode = "zh"

    init {
        viewModelScope.launch {
            settingsDataStore.settings.collect { settings ->
                _uiState.value = _uiState.value.copy(settings = settings)
            }
        }

        // Listen for download progress updates
        mlKitModelManager.addProgressListener { _, progress ->
            viewModelScope.launch {
                val state = when (progress.state) {
                    ModelDownloadState.DOWNLOADING -> ModelDownloadState.DOWNLOADING
                    ModelDownloadState.COMPLETED -> ModelDownloadState.COMPLETED
                    ModelDownloadState.FAILED -> ModelDownloadState.FAILED
                    ModelDownloadState.IDLE -> ModelDownloadState.IDLE
                }
                _uiState.value = _uiState.value.copy(
                    mlKitDownloadState = state,
                    mlKitDownloadProgress = progress.progress,
                    mlKitDownloading = state == ModelDownloadState.DOWNLOADING,
                    mlKitDownloadMessage = run {
                        if (progress.message.isNotBlank()) return@run UiText.DynamicString(progress.message)
                        when (state) {
                            ModelDownloadState.COMPLETED -> UiText.StringResource(luzzr.xi.R.string.mlkit_model_ready)
                            ModelDownloadState.DOWNLOADING -> UiText.StringResource(luzzr.xi.R.string.mlkit_downloading)
                            ModelDownloadState.FAILED -> UiText.StringResource(luzzr.xi.R.string.mlkit_download_failed)
                            ModelDownloadState.IDLE -> null
                        }
                    }
                )
            }
        }

        checkMlKitStatus()
    }

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            is SettingsUiEvent.ApiBaseUrlChanged -> updateApiBaseUrl(event.url)
            is SettingsUiEvent.ApiKeyChanged -> updateApiKey(event.key)
            is SettingsUiEvent.ModelChanged -> updateModel(event.model)
            SettingsUiEvent.TestConnectionClicked -> testConnection()
            SettingsUiEvent.DownloadMlKitModelClicked -> downloadMlKitModel()
            SettingsUiEvent.CancelMlKitDownloadClicked -> cancelMlKitDownload()
            SettingsUiEvent.RetryMlKitDownloadClicked -> retryMlKitDownload()
        }
    }

    private fun updateApiBaseUrl(url: String) {
        viewModelScope.launch { settingsDataStore.updateApiBaseUrl(url) }
    }

    private fun updateApiKey(key: String) {
        viewModelScope.launch { settingsDataStore.updateApiKey(key) }
    }

    private fun updateModel(model: String) {
        viewModelScope.launch { settingsDataStore.updateModel(model) }
    }

    private fun testConnection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(testStatus = TestStatus.Testing)
            try {
                val api = apiRepository.getCurrentApi()
                val response = api.listModels()
                val models = response.data?.mapNotNull { it.id } ?: emptyList()
                _uiState.value = _uiState.value.copy(
                    testStatus = TestStatus.Success(modelCount = models.size),
                    availableModels = models
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    testStatus = TestStatus.Failure(e.message ?: "Unknown error")
                )
            }
        }
    }

    private fun downloadMlKitModel() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                mlKitDownloading = true,
                mlKitDownloadState = ModelDownloadState.DOWNLOADING,
                mlKitDownloadProgress = 0f,
                mlKitDownloadMessage = UiText.StringResource(luzzr.xi.R.string.mlkit_prepare_download)
            )

            val result = mlKitModelManager.downloadModel(
                sourceLang = defaultSourceLang,
                targetLang = defaultTargetLang,
                sourceCode = defaultSourceCode,
                targetCode = defaultTargetCode
            )

            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        mlKitDownloading = false,
                        mlKitDownloadState = ModelDownloadState.COMPLETED,
                        mlKitDownloadProgress = 1f,
                        mlKitDownloadMessage = UiText.StringResource(luzzr.xi.R.string.mlkit_download_success)
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        mlKitDownloading = false,
                        mlKitDownloadState = ModelDownloadState.FAILED,
                        mlKitDownloadMessage = e.message?.let { UiText.DynamicString(it) }
                            ?: UiText.StringResource(luzzr.xi.R.string.mlkit_download_retry)
                    )
                }
            )
        }
    }

    private fun cancelMlKitDownload() {
        mlKitModelManager.cancelDownload()
        _uiState.value = _uiState.value.copy(
            mlKitDownloading = false,
            mlKitDownloadState = ModelDownloadState.IDLE,
            mlKitDownloadProgress = 0f,
            mlKitDownloadMessage = UiText.StringResource(luzzr.xi.R.string.mlkit_download_cancelled)
        )
    }

    private fun retryMlKitDownload() {
        _uiState.value = _uiState.value.copy(
            mlKitDownloading = false,
            mlKitDownloadState = ModelDownloadState.IDLE,
            mlKitDownloadProgress = 0f,
            mlKitDownloadMessage = null
        )
        downloadMlKitModel()
    }

    private fun checkMlKitStatus() {
        viewModelScope.launch {
            val enZh = mlKitModelManager.isModelDownloaded(defaultSourceLang, defaultTargetLang)
            _uiState.value = _uiState.value.copy(
                mlKitDownloadState = if (enZh) ModelDownloadState.COMPLETED else ModelDownloadState.IDLE,
                mlKitDownloading = false,
                mlKitDownloadProgress = if (enZh) 1f else 0f,
                mlKitDownloadMessage = if (enZh) UiText.StringResource(luzzr.xi.R.string.mlkit_model_ready) else null
            )
        }
    }
}
