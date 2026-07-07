package luzzr.xi.feature.essay

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import luzzr.xi.domain.model.ThinkingLevel
import luzzr.xi.domain.model.UiText
import luzzr.xi.domain.model.CorrectionResult
import luzzr.xi.domain.usecase.CorrectEssayUseCase
import luzzr.xi.core.datastore.SettingsDataStore
import luzzr.xi.domain.model.EssayError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import javax.inject.Inject

import luzzr.xi.domain.model.GrammarError
import luzzr.xi.domain.model.VocabularySuggestion
import luzzr.xi.domain.model.StructureAnalysis
import luzzr.xi.domain.model.StyleAnalysis
import luzzr.xi.domain.model.ScoreBreakdown

enum class InputMode { TEXT, IMAGE, PDF }

enum class PhotoSource { NONE, CAMERA, GALLERY }

data class EssayUiState(
    val essayText: String = "",
    val grammarErrors: List<GrammarError> = emptyList(),
    val vocabulary: List<VocabularySuggestion> = emptyList(),
    val structure: StructureAnalysis? = null,
    val style: StyleAnalysis? = null,
    val score: ScoreBreakdown? = null,
    val correctedEssay: String = "",
    val writingTips: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: UiText? = null,
    val hasResult: Boolean = false,
    val inputMode: InputMode = InputMode.TEXT,
    val imageUri: Uri? = null,
    val pdfPageCount: Int = 0,
    val pdfCurrentPage: Int = 0,
    val thinkingLevel: ThinkingLevel = ThinkingLevel.HIGH,
    val usage: luzzr.xi.domain.model.Usage? = null,
    val photoSource: PhotoSource = PhotoSource.NONE
)

sealed interface EssayUiEvent {
    data class EssayTextChanged(val text: String) : EssayUiEvent
    data class InputModeChanged(val mode: InputMode) : EssayUiEvent
    data class ImageUriSelected(val uri: Uri, val source: PhotoSource = PhotoSource.GALLERY) : EssayUiEvent
    data class PdfUriSelected(val uri: Uri, val pageCount: Int) : EssayUiEvent
    data object CorrectClicked : EssayUiEvent
    data object ClearClicked : EssayUiEvent
    data object CancelCorrectClicked : EssayUiEvent
    data class ErrorDismissed(val error: UiText? = null) : EssayUiEvent
    data class ErrorOccurred(val error: UiText) : EssayUiEvent
    data class ThinkingLevelChanged(val level: ThinkingLevel) : EssayUiEvent
}

@HiltViewModel
class EssayViewModel @Inject constructor(
    private val correctEssayUseCase: CorrectEssayUseCase,
    private val settingsDataStore: SettingsDataStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(EssayUiState())
    val uiState: StateFlow<EssayUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsDataStore.settings.collect { s ->
                _uiState.update {
                    it.copy(thinkingLevel = ThinkingLevel.fromId(s.essayThinkingLevel))
                }
            }
        }
    }

    companion object {
        const val MAX_INPUT_LENGTH = 10000
    }

    private var correctJob: Job? = null

    fun onEvent(event: EssayUiEvent) {
        when (event) {
            is EssayUiEvent.EssayTextChanged -> {
                if (event.text.length <= MAX_INPUT_LENGTH) {
                    _uiState.update { it.copy(essayText = event.text, error = null) }
                } else {
                    _uiState.update { it.copy(error = UiText.StringResource(luzzr.xi.R.string.error_input_too_long_essay)) }
                }
            }
            is EssayUiEvent.InputModeChanged -> {
                _uiState.update {
                    it.copy(
                        inputMode = event.mode,
                        imageUri = null,
                        pdfPageCount = 0
                    )
                }
            }
            is EssayUiEvent.ImageUriSelected -> {
                _uiState.update { it.copy(imageUri = event.uri, inputMode = InputMode.IMAGE, photoSource = event.source) }
            }
            is EssayUiEvent.PdfUriSelected -> {
                _uiState.update {
                    it.copy(
                        imageUri = event.uri,
                        pdfPageCount = event.pageCount,
                        pdfCurrentPage = 0,
                        inputMode = InputMode.PDF
                    )
                }
            }
            EssayUiEvent.CorrectClicked -> {
                correctEssay()
            }
            EssayUiEvent.ClearClicked -> {
                _uiState.update {
                    it.copy(
                        essayText = "",
                        grammarErrors = emptyList(),
                        vocabulary = emptyList(),
                        structure = null,
                        style = null,
                        score = null,
                        correctedEssay = "",
                        writingTips = emptyList(),
                        isLoading = false,
                        error = null,
                        hasResult = false,
                        usage = null
                    )
                }
            }
            EssayUiEvent.CancelCorrectClicked -> {
                correctJob?.cancel()
                _uiState.update { it.copy(isLoading = false) }
            }
            is EssayUiEvent.ErrorDismissed -> {
                _uiState.update { it.copy(error = null) }
            }
            is EssayUiEvent.ErrorOccurred -> {
                _uiState.update { it.copy(error = event.error) }
            }
            is EssayUiEvent.ThinkingLevelChanged -> {
                _uiState.update { it.copy(thinkingLevel = event.level) }
                viewModelScope.launch {
                    settingsDataStore.updateEssayThinkingLevel(event.level.id)
                }
            }
        }
    }

    private fun correctEssay() {
        val state = _uiState.value
        if (state.inputMode == InputMode.TEXT && state.essayText.trim().isEmpty()) {
            _uiState.update { it.copy(error = UiText.StringResource(luzzr.xi.R.string.essay_error_empty)) }
            return
        }
        if (state.inputMode == InputMode.TEXT && state.essayText.length > MAX_INPUT_LENGTH) {
            _uiState.update { it.copy(error = UiText.StringResource(luzzr.xi.R.string.error_input_too_long_essay)) }
            return
        }

        correctJob?.cancel()

        val level = state.thinkingLevel
        val effort = level.id

        correctJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val result = when (state.inputMode) {
                    InputMode.TEXT -> correctEssayUseCase.correctFromText(state.essayText, effort)
                    InputMode.IMAGE -> correctEssayUseCase.correctFromImage(state.imageUri, effort)
                    InputMode.PDF -> correctEssayUseCase.correctFromPdf(state.imageUri, effort)
                }

                result.fold(
                    onSuccess = { correction ->
                        _uiState.update {
                            it.copy(
                                grammarErrors = correction.grammarErrors,
                                vocabulary = correction.vocabulary,
                                structure = correction.structure,
                                style = correction.style,
                                score = correction.score,
                                correctedEssay = correction.correctedEssay,
                                writingTips = correction.writingTips,
                                usage = correction.usage,
                                hasResult = true
                            )
                        }
                    },
                    onFailure = { e ->
                        val errorText = when (e) {
                            is EssayError.TextEmpty -> UiText.StringResource(luzzr.xi.R.string.essay_error_empty)
                            is EssayError.ImageNull -> UiText.StringResource(luzzr.xi.R.string.essay_error_no_image)
                            is EssayError.PdfNull -> UiText.StringResource(luzzr.xi.R.string.essay_error_no_pdf)
                            is EssayError.PdfUnreadable -> UiText.StringResource(luzzr.xi.R.string.error_ai_unreadable_image)
                            else -> UiText.StringResource(luzzr.xi.R.string.error_essay_failed)
                        }
                        _uiState.update { it.copy(error = errorText) }
                    }
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Ignore cancellation exception from cancelled job
                throw e
            } finally {
                if (_uiState.value.isLoading) {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }
}
