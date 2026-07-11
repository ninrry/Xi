package luzzr.xi.feature.overlay

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import luzzr.xi.R
import luzzr.xi.core.datastore.SettingsDataStore
import luzzr.xi.domain.model.TranslationEngine
import luzzr.xi.domain.model.UiText
import luzzr.xi.domain.model.HistorySource
import luzzr.xi.domain.usecase.TranslateUseCase
import luzzr.xi.domain.model.ThinkingLevel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OverlayController @Inject constructor(
    private val translateUseCase: TranslateUseCase,
    private val settingsDataStore: SettingsDataStore
) {
    // Owned, application-scoped scope. The controller is a @Singleton, so its
    // lifetime matches the app, not any individual OverlayService instance.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _uiState = MutableStateFlow(OverlayUiState())
    val uiState = _uiState.asStateFlow()

    private var translateJob: Job? = null

    init {
        scope.launch {
            settingsDataStore.settings.collect { s ->
                _uiState.update { it.copy(
                    thinkingLevel = ThinkingLevel.fromId(s.translateThinkingLevel),
                    engine = TranslationEngine.fromId(s.translationEngine)
                ) }
            }
        }
    }

    fun setPanelVisible(visible: Boolean) {
        _uiState.update { it.copy(isPanelVisible = visible) }
    }

    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun updateSourceLang(lang: luzzr.xi.domain.model.SupportedLanguage) {
        _uiState.update { it.copy(sourceLang = lang) }
    }

    fun updateTargetLang(lang: luzzr.xi.domain.model.SupportedLanguage) {
        _uiState.update { it.copy(targetLang = lang) }
    }

    fun updateEngine(eng: TranslationEngine) {
        _uiState.update { it.copy(engine = eng) }
        scope.launch { settingsDataStore.updateTranslationEngine(eng.id) }
    }

    fun updateThinkingLevel(lvl: ThinkingLevel) {
        _uiState.update { it.copy(thinkingLevel = lvl) }
        scope.launch { settingsDataStore.updateTranslateThinkingLevel(lvl.id) }
    }

    fun swapLanguages() {
        _uiState.update {
            val newInput = it.resultText.ifEmpty { it.inputText }
            it.copy(
                sourceLang = it.targetLang,
                targetLang = it.sourceLang,
                inputText = newInput,
                resultText = "",
                usage = null,
                errorMsg = null
            )
        }
    }

    fun cancelTranslate() {
        translateJob?.cancel()
        translateJob = null
        if (_uiState.value.isTranslating || _uiState.value.isModelDownloading) {
            _uiState.update { it.copy(isTranslating = false, isModelDownloading = false) }
        }
    }

    fun clear() {
        cancelTranslate()
        _uiState.update {
            it.copy(inputText = "", resultText = "", usage = null, errorMsg = null)
        }
    }

    fun translate() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) {
            _uiState.update { it.copy(errorMsg = UiText.StringResource(R.string.translate_error_empty)) }
            return
        }

        cancelTranslate()
        translateJob = scope.launch {
            _uiState.update { it.copy(isTranslating = true, isModelDownloading = false, errorMsg = null, resultText = "", usage = null) }
            try {
                val engine = _uiState.value.engine
                if (engine == TranslationEngine.MLKIT) {
                    _uiState.update { it.copy(isModelDownloading = true) }
                }

                // TranslateUseCase returns a Flow now!
                val flow = translateUseCase(
                    text = text,
                    sourceLang = _uiState.value.sourceLang.displayName,
                    targetLang = _uiState.value.targetLang.displayName,
                    engine = engine,
                    thinkingLevelId = _uiState.value.thinkingLevel.id,
                    source = HistorySource.OVERLAY
                )

                flow.collect { result ->
                    result.fold(
                        onSuccess = { res -> 
                            _uiState.update { it.copy(resultText = res.translation ?: "", usage = res.usage ?: it.usage, isModelDownloading = false) } 
                        },
                        onFailure = { err -> 
                            _uiState.update { it.copy(errorMsg = when (err) {
                                is luzzr.xi.domain.model.AppError -> err.toUiText()
                                else -> UiText.StringResource(luzzr.xi.R.string.error_translate_failed)
                            }, isModelDownloading = false) } 
                        }
                    )
                }
            } catch (e: CancellationException) {
                if (e is kotlinx.coroutines.TimeoutCancellationException) {
                    _uiState.update { it.copy(errorMsg = UiText.StringResource(R.string.error_translate_timeout)) }
                } else {
                    throw e
                }
            } finally {
                withContext(kotlinx.coroutines.NonCancellable) {
                    _uiState.update { it.copy(isTranslating = false, isModelDownloading = false) }
                }
            }
        }
    }
}
