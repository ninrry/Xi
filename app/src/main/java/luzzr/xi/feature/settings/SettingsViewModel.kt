package luzzr.xi.feature.settings
import luzzr.xi.domain.model.*

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import luzzr.xi.core.network.ApiProvider
import luzzr.xi.core.datastore.AppSettings
import luzzr.xi.core.datastore.SettingsDataStore
import luzzr.xi.domain.model.SupportedLanguage
import luzzr.xi.domain.model.UiText
import luzzr.xi.domain.model.ModelDownloadProgress
import luzzr.xi.domain.model.ModelDownloadState
import luzzr.xi.domain.repository.MlKitModelGateway
import luzzr.xi.domain.repository.SettingsGateway
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val testStatus: TestStatus = TestStatus.Idle,
    val availableModels: List<String> = emptyList(),
    val mlKitDownloadState: ModelDownloadState = ModelDownloadState.IDLE,
    val mlKitDownloadMessage: UiText? = null,
    val mlKitDownloadProgress: Float = 0f,
    val mlKitDownloading: Boolean = false,
    val showProviderSwitchDialog: String? = null,
    val mlKitSourceLang: SupportedLanguage = SupportedLanguage.ENGLISH,
    val mlKitTargetLang: SupportedLanguage = SupportedLanguage.CHINESE
)

sealed class TestStatus {
    data object Idle : TestStatus()
    data object Testing : TestStatus()
    data class Success(val modelCount: Int) : TestStatus()
    data class Failure(val message: String) : TestStatus()
}

sealed interface SettingsUiEvent {
    data class ProviderChanged(val providerId: String) : SettingsUiEvent
    data class ProviderSwitchConfirmed(val providerId: String) : SettingsUiEvent
    data object ProviderSwitchCancelled : SettingsUiEvent
    data class ApiBaseUrlChanged(val url: String) : SettingsUiEvent
    data class ApiKeyChanged(val key: String) : SettingsUiEvent
    data class ModelChanged(val model: String) : SettingsUiEvent
    data object TestConnectionClicked : SettingsUiEvent
    data object DownloadMlKitModelClicked : SettingsUiEvent
    data object CancelMlKitDownloadClicked : SettingsUiEvent
    data object RetryMlKitDownloadClicked : SettingsUiEvent
    data class MlKitSourceLangChanged(val lang: SupportedLanguage) : SettingsUiEvent
    data class MlKitTargetLangChanged(val lang: SupportedLanguage) : SettingsUiEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val mlKitModelManager: MlKitModelGateway,
    private val apiRepository: SettingsGateway,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var mlKitStatusCached: Boolean? = null
    private var mlKitStatusCacheTime: Long = 0
    private val mlKitStatusCacheValidityMs = 30_000L // 30 seconds

    private data class MlKitLanguageConfig(
        val sourceLang: SupportedLanguage = SupportedLanguage.ENGLISH,
        val targetLang: SupportedLanguage = SupportedLanguage.CHINESE,
        val sourceCode: String = "en",
        val targetCode: String = "zh"
    )

    private var mlKitLanguageConfig = MlKitLanguageConfig()

    private val progressListener: (String, ModelDownloadProgress) -> Unit = { _, progress ->
        viewModelScope.launch(Dispatchers.Main) {
            val state = when (progress.state) {
                ModelDownloadState.DOWNLOADING -> ModelDownloadState.DOWNLOADING
                ModelDownloadState.COMPLETED -> ModelDownloadState.COMPLETED
                ModelDownloadState.FAILED -> ModelDownloadState.FAILED
                ModelDownloadState.IDLE -> ModelDownloadState.IDLE
            }
            _uiState.update { it.copy(
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
            ) }
        }
    }

    init {
        viewModelScope.launch {
            settingsDataStore.settings.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }

        // Listen for download progress updates
        mlKitModelManager.addProgressListener(progressListener)

        checkMlKitStatus()
    }

    override fun onCleared() {
        super.onCleared()
        mlKitModelManager.removeProgressListener(progressListener)
    }

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            is SettingsUiEvent.ProviderChanged -> {
                if (event.providerId != _uiState.value.settings.providerId) {
                    _uiState.update { it.copy(showProviderSwitchDialog = event.providerId) }
                }
            }
            is SettingsUiEvent.ProviderSwitchConfirmed -> {
                val newProviderId = event.providerId
                val provider = luzzr.xi.core.provider.ProviderRegistry.getProvider(newProviderId)
                viewModelScope.launch {
                    settingsDataStore.updateProviderId(newProviderId)
                    settingsDataStore.updateApiBaseUrl(provider.defaultBaseUrl)
                    settingsDataStore.updateModel(provider.defaultModel)
                    settingsDataStore.updateApiKey("")
                }
                _uiState.update { it.copy(showProviderSwitchDialog = null, availableModels = emptyList(), testStatus = TestStatus.Idle) }
            }
            is SettingsUiEvent.ProviderSwitchCancelled -> {
                _uiState.update { it.copy(showProviderSwitchDialog = null) }
            }
            is SettingsUiEvent.ApiBaseUrlChanged -> updateApiBaseUrl(event.url)
            is SettingsUiEvent.ApiKeyChanged -> updateApiKey(event.key)
            is SettingsUiEvent.ModelChanged -> updateModel(event.model)
            SettingsUiEvent.TestConnectionClicked -> testConnection()
            SettingsUiEvent.DownloadMlKitModelClicked -> downloadMlKitModel()
            SettingsUiEvent.CancelMlKitDownloadClicked -> cancelMlKitDownload()
            SettingsUiEvent.RetryMlKitDownloadClicked -> retryMlKitDownload()
            is SettingsUiEvent.MlKitSourceLangChanged -> {
                mlKitLanguageConfig = mlKitLanguageConfig.copy(
                    sourceLang = event.lang,
                    sourceCode = event.lang.code
                )
                _uiState.update { it.copy(mlKitSourceLang = event.lang) }
            }
            is SettingsUiEvent.MlKitTargetLangChanged -> {
                mlKitLanguageConfig = mlKitLanguageConfig.copy(
                    targetLang = event.lang,
                    targetCode = event.lang.code
                )
                _uiState.update { it.copy(mlKitTargetLang = event.lang) }
            }
        }
    }

    private var apiUrlJob: Job? = null
    private var apiKeyJob: Job? = null

    private fun updateApiBaseUrl(url: String) {
        apiUrlJob?.cancel()
        apiUrlJob = viewModelScope.launch {
            delay(300)
            settingsDataStore.updateApiBaseUrl(url)
        }
    }

    private fun updateApiKey(key: String) {
        apiKeyJob?.cancel()
        apiKeyJob = viewModelScope.launch {
            delay(300)
            settingsDataStore.updateApiKey(key)
        }
    }

    private fun updateModel(model: String) {
        viewModelScope.launch { settingsDataStore.updateModel(model) }
    }

    private fun testConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(testStatus = TestStatus.Testing) }
            try {
                val response = apiRepository.testConnection()
                val models = response.data?.mapNotNull { it.id } ?: emptyList()
                _uiState.update { it.copy(
                    testStatus = TestStatus.Success(modelCount = models.size),
                    availableModels = models
                ) }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    testStatus = TestStatus.Failure(e.message ?: "Unknown error")
                ) }
            }
        }
    }

    private fun downloadMlKitModel() {
        viewModelScope.launch {
            _uiState.update { it.copy(
                mlKitDownloading = true,
                mlKitDownloadState = ModelDownloadState.DOWNLOADING,
                mlKitDownloadProgress = 0f,
                mlKitDownloadMessage = UiText.StringResource(luzzr.xi.R.string.mlkit_prepare_download)
            ) }

            try {
                val config = mlKitLanguageConfig
                val result = mlKitModelManager.downloadModel(
                    sourceLang = config.sourceLang.displayName,
                    targetLang = config.targetLang.displayName,
                    sourceCode = config.sourceCode,
                    targetCode = config.targetCode
                )

                result.fold(
                    onSuccess = {
                        _uiState.update { it.copy(
                            mlKitDownloading = false,
                            mlKitDownloadState = ModelDownloadState.COMPLETED,
                            mlKitDownloadProgress = 1f,
                            mlKitDownloadMessage = UiText.StringResource(luzzr.xi.R.string.mlkit_model_ready)
                        ) }
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(
                            mlKitDownloading = false,
                            mlKitDownloadState = ModelDownloadState.FAILED,
                            mlKitDownloadMessage = e.message?.let { UiText.DynamicString(it) }
                                ?: UiText.StringResource(luzzr.xi.R.string.mlkit_download_retry)
                        ) }
                    }
                )
            } finally {
                _uiState.update { it.copy(mlKitDownloading = false) }
            }
        }
    }

    private fun cancelMlKitDownload() {
        mlKitModelManager.cancelDownload()
        _uiState.update { it.copy(
            mlKitDownloading = false,
            mlKitDownloadState = ModelDownloadState.IDLE,
            mlKitDownloadProgress = 0f,
            mlKitDownloadMessage = UiText.StringResource(luzzr.xi.R.string.mlkit_download_cancelled)
        ) }
    }

    private fun retryMlKitDownload() {
        _uiState.update { it.copy(
            mlKitDownloading = false,
            mlKitDownloadState = ModelDownloadState.IDLE,
            mlKitDownloadProgress = 0f,
            mlKitDownloadMessage = null
        ) }
        downloadMlKitModel()
    }

    private fun checkMlKitStatus() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val cached = mlKitStatusCached
            val isCachedValid = cached != null && (now - mlKitStatusCacheTime) < mlKitStatusCacheValidityMs

            val config = mlKitLanguageConfig
            val enZh = if (isCachedValid) {
                cached
            } else {
                mlKitModelManager.isModelDownloaded(
                    config.sourceLang.displayName,
                    config.targetLang.displayName,
                    config.sourceCode,
                    config.targetCode
                ).also {
                    mlKitStatusCached = it
                    mlKitStatusCacheTime = now
                }
            }

            _uiState.update { it.copy(
                mlKitDownloadState = if (enZh) ModelDownloadState.COMPLETED else ModelDownloadState.IDLE,
                mlKitDownloading = false,
                mlKitDownloadProgress = if (enZh) 1f else 0f,
                mlKitDownloadMessage = if (enZh) UiText.StringResource(luzzr.xi.R.string.mlkit_model_ready) else null
            ) }
        }
    }

}
