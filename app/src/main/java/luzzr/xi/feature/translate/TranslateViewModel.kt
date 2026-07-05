package luzzr.xi.feature.translate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import luzzr.xi.core.datastore.SettingsDataStore
import luzzr.xi.domain.model.SupportedLanguage
import luzzr.xi.domain.model.ThinkingLevel
import luzzr.xi.domain.model.TranslationEngine
import luzzr.xi.domain.usecase.TranslateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import luzzr.xi.R

data class TranslateUiState(
    val inputText: String = "",
    val resultText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val sourceLang: SupportedLanguage = SupportedLanguage.ENGLISH,
    val targetLang: SupportedLanguage = SupportedLanguage.CHINESE,
    val thinkingLevel: ThinkingLevel = ThinkingLevel.LOW,
    val engine: TranslationEngine = TranslationEngine.AI,
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
    @ApplicationContext private val context: Context
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
                _uiState.value = _uiState.value.copy(inputText = event.text, error = null)
            }
            is TranslateUiEvent.SourceLangChanged -> {
                if (event.lang != _uiState.value.targetLang) {
                    _uiState.value = _uiState.value.copy(sourceLang = event.lang)
                }
            }
            is TranslateUiEvent.TargetLangChanged -> {
                if (event.lang != _uiState.value.sourceLang) {
                    _uiState.value = _uiState.value.copy(targetLang = event.lang)
                }
            }
            is TranslateUiEvent.ThinkingLevelChanged -> {
                _uiState.value = _uiState.value.copy(thinkingLevel = event.level)
                viewModelScope.launch {
                    settingsDataStore.updateTranslateThinkingLevel(event.level.id)
                }
            }
            is TranslateUiEvent.EngineChanged -> {
                _uiState.value = _uiState.value.copy(engine = event.engine)
                viewModelScope.launch {
                    settingsDataStore.updateTranslationEngine(event.engine.id)
                }
            }
            TranslateUiEvent.SwapClicked -> {
                val s = _uiState.value
                _uiState.value = s.copy(
                    sourceLang = s.targetLang,
                    targetLang = s.sourceLang,
                    inputText = s.resultText,
                    resultText = ""
                )
            }
            TranslateUiEvent.ClearClicked -> {
                _uiState.value = _uiState.value.copy(inputText = "", resultText = "", error = null)
            }
            TranslateUiEvent.TranslateClicked -> {
                translate()
            }
        }
    }

    private fun translate() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = context.getString(R.string.translate_error_empty))
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
                        val errorMsg = e.message ?: context.getString(R.string.error_translate_failed)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = errorMsg
                        )
                    }
                )
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                val errorMsg = context.getString(R.string.error_translate_timeout)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = errorMsg
                )
            }
        }
    }
}
