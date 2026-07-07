package luzzr.xi.feature.translate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import luzzr.xi.core.datastore.SettingsDataStore
import luzzr.xi.domain.model.SupportedLanguage
import luzzr.xi.domain.model.ThinkingLevel
import luzzr.xi.domain.model.TranslationEngine
import luzzr.xi.domain.model.AppError
import luzzr.xi.domain.model.UiText
import luzzr.xi.domain.usecase.TranslateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TranslateUiState(
    val inputText: String = "",
    val resultText: String = "",
    val detectedLanguage: String? = null,
    val alternatives: List<String> = emptyList(),
    val usage: luzzr.xi.domain.model.Usage? = null,
    val isLoading: Boolean = false,
    val error: UiText? = null,
    val sourceLang: SupportedLanguage = SupportedLanguage.ENGLISH,
    val targetLang: SupportedLanguage = SupportedLanguage.CHINESE,
    val thinkingLevel: ThinkingLevel = ThinkingLevel.MEDIUM,
    val engine: TranslationEngine = TranslationEngine.MLKIT,
)

sealed interface TranslateUiEvent {
    data class InputChanged(val text: String) : TranslateUiEvent
    data class SourceLangChanged(val lang: SupportedLanguage) : TranslateUiEvent
    data class TargetLangChanged(val lang: SupportedLanguage) : TranslateUiEvent
    data class ThinkingLevelChanged(val level: ThinkingLevel) : TranslateUiEvent
    data class EngineChanged(val engine: TranslationEngine) : TranslateUiEvent
    data object SwapClicked : TranslateUiEvent
    data object TranslateClicked : TranslateUiEvent
    data object ClearClicked : TranslateUiEvent
}

@HiltViewModel
class TranslateViewModel @Inject constructor(
    private val translateUseCase: TranslateUseCase,
    private val settingsDataStore: SettingsDataStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TranslateUiState())
    val uiState: StateFlow<TranslateUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsDataStore.settings.collect { s ->
                _uiState.update {
                    it.copy(
                        thinkingLevel = ThinkingLevel.fromId(s.translateThinkingLevel),
                        engine = TranslationEngine.fromId(s.translationEngine)
                    )
                }
            }
        }
    }

    companion object {
        const val MAX_INPUT_LENGTH = 5000
    }

    private var translateJob: kotlinx.coroutines.Job? = null

    fun onEvent(event: TranslateUiEvent) {
        when (event) {
            is TranslateUiEvent.InputChanged -> {
                if (event.text.length <= MAX_INPUT_LENGTH) {
                    _uiState.update { it.copy(inputText = event.text, error = null) }
                } else {
                    _uiState.update { it.copy(error = UiText.StringResource(luzzr.xi.R.string.error_input_too_long_translate)) }
                }
            }
            is TranslateUiEvent.SourceLangChanged -> {
                if (event.lang != _uiState.value.targetLang) {
                    _uiState.update { it.copy(sourceLang = event.lang, error = null) }
                } else {
                    _uiState.update { it.copy(error = UiText.StringResource(luzzr.xi.R.string.translate_same_language)) }
                }
            }
            is TranslateUiEvent.TargetLangChanged -> {
                if (event.lang != _uiState.value.sourceLang) {
                    _uiState.update { it.copy(targetLang = event.lang, error = null) }
                } else {
                    _uiState.update { it.copy(error = UiText.StringResource(luzzr.xi.R.string.translate_same_language)) }
                }
            }
            is TranslateUiEvent.ThinkingLevelChanged -> {
                _uiState.update { it.copy(thinkingLevel = event.level) }
                viewModelScope.launch {
                    settingsDataStore.updateTranslateThinkingLevel(event.level.id)
                }
            }
            is TranslateUiEvent.EngineChanged -> {
                _uiState.update { it.copy(engine = event.engine) }
                viewModelScope.launch {
                    settingsDataStore.updateTranslationEngine(event.engine.id)
                }
            }
            TranslateUiEvent.SwapClicked -> {
                val s = _uiState.value
                _uiState.update {
                    it.copy(
                        sourceLang = s.targetLang,
                        targetLang = s.sourceLang,
                        inputText = s.resultText.ifEmpty { s.inputText },
                        resultText = "",
                        detectedLanguage = null,
                        alternatives = emptyList(),
                        usage = null
                    )
                }
            }
            TranslateUiEvent.ClearClicked -> {
                _uiState.update { it.copy(inputText = "", resultText = "", detectedLanguage = null, alternatives = emptyList(), usage = null, error = null) }
            }
            TranslateUiEvent.TranslateClicked -> {
                translate()
            }
        }
    }

    private fun translate() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) {
            _uiState.update { it.copy(error = UiText.StringResource(luzzr.xi.R.string.translate_error_empty)) }
            return
        }

        if (_uiState.value.sourceLang == SupportedLanguage.AUTO && _uiState.value.engine == TranslationEngine.MLKIT) {
            _uiState.update { it.copy(error = UiText.StringResource(luzzr.xi.R.string.error_mlkit_auto_not_supported)) }
            return
        }

        if (text.length > MAX_INPUT_LENGTH) {
            _uiState.update { it.copy(error = UiText.StringResource(luzzr.xi.R.string.error_input_too_long_translate)) }
            return
        }

        translateJob?.cancel()

        val currentEngine = _uiState.value.engine
        val currentSourceLang = _uiState.value.sourceLang.displayName
        val currentTargetLang = _uiState.value.targetLang.displayName
        val currentThinkingLevel = _uiState.value.thinkingLevel.id

        translateJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, resultText = "", detectedLanguage = null, alternatives = emptyList(), usage = null) }

            try {
                val flow = translateUseCase(
                    text = text,
                    sourceLang = currentSourceLang,
                    targetLang = currentTargetLang,
                    engine = currentEngine,
                    thinkingLevelId = currentThinkingLevel
                )
                
                flow.collect { result ->
                    result.fold(
                        onSuccess = { res ->
                            _uiState.update {
                                it.copy(
                                    resultText = res.translation ?: "",
                                    detectedLanguage = res.detectedLanguage ?: it.detectedLanguage,
                                    alternatives = if (res.alternatives.isNullOrEmpty()) it.alternatives else res.alternatives,
                                    usage = res.usage ?: it.usage
                                )
                            }
                        },
                        onFailure = { e ->
                            val errorText = when (e) {
                                is AppError -> e.toUiText()
                                else -> UiText.StringResource(luzzr.xi.R.string.error_translate_failed)
                                    }
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = errorText
                                )
                            }
                        }
                    )
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                if (e is kotlinx.coroutines.TimeoutCancellationException) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = UiText.StringResource(luzzr.xi.R.string.error_translate_timeout)
                        )
                    }
                } else {
                    throw e
                }
            } finally {
                if (_uiState.value.isLoading) {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }
}
