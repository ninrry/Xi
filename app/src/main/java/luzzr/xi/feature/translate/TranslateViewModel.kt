package luzzr.xi.feature.translate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import luzzr.xi.core.datastore.SettingsDataStore
import luzzr.xi.domain.model.SupportedLanguage
import luzzr.xi.domain.model.ThinkingLevel
import luzzr.xi.domain.model.TranslationEngine
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
    val isLoading: Boolean = false,
    val error: UiText? = null,
    val sourceLang: SupportedLanguage = SupportedLanguage.ENGLISH,
    val targetLang: SupportedLanguage = SupportedLanguage.CHINESE,
    val thinkingLevel: ThinkingLevel = ThinkingLevel.LOW,
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
            settingsDataStore.settings.first().let { s ->
                _uiState.value = _uiState.value.copy(
                    thinkingLevel = ThinkingLevel.fromId(s.translateThinkingLevel),
                    engine = TranslationEngine.fromId(s.translationEngine)
                )
            }
        }
    }

    fun onEvent(event: TranslateUiEvent) {
        when (event) {
            is TranslateUiEvent.InputChanged -> {
                _uiState.update { it.copy(inputText = event.text, error = null) }
            }
            is TranslateUiEvent.SourceLangChanged -> {
                if (event.lang != _uiState.value.targetLang) {
                    _uiState.update { it.copy(sourceLang = event.lang) }
                }
            }
            is TranslateUiEvent.TargetLangChanged -> {
                if (event.lang != _uiState.value.sourceLang) {
                    _uiState.update { it.copy(targetLang = event.lang) }
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
                        inputText = s.resultText,
                        resultText = ""
                    )
                }
            }
            TranslateUiEvent.ClearClicked -> {
                _uiState.update { it.copy(inputText = "", resultText = "", error = null) }
            }
            TranslateUiEvent.TranslateClicked -> {
                translate()
            }
        }
    }

    private fun translate() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = UiText.StringResource(luzzr.xi.R.string.translate_error_empty))
            return
        }

        val currentEngine = _uiState.value.engine
        val currentSourceLang = _uiState.value.sourceLang.displayName
        val currentTargetLang = _uiState.value.targetLang.displayName
        val currentThinkingLevel = _uiState.value.thinkingLevel.id

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, resultText = "")

            try {
                val result = translateUseCase(
                    text = text,
                    sourceLang = currentSourceLang,
                    targetLang = currentTargetLang,
                    engine = currentEngine,
                    thinkingLevelId = currentThinkingLevel
                )

                result.fold(
                    onSuccess = { translated ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            resultText = translated
                        )
                    },
                    onFailure = { e ->
                        val errorText = e.message?.let { UiText.DynamicString(it) }
                            ?: UiText.StringResource(luzzr.xi.R.string.error_translate_failed)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = errorText
                        )
                    }
                )
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = UiText.StringResource(luzzr.xi.R.string.error_translate_timeout)
                )
            } finally {
                if (_uiState.value.isLoading) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        }
    }
}
