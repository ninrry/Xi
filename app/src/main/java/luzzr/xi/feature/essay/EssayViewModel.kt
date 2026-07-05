package luzzr.xi.feature.essay

import android.net.Uri
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import luzzr.xi.domain.model.ThinkingLevel
import luzzr.xi.data.repository.CorrectionResult
import luzzr.xi.domain.usecase.CorrectEssayUseCase
import luzzr.xi.core.datastore.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import luzzr.xi.R

enum class InputMode { TEXT, IMAGE, PDF }

data class EssayUiState(
    val essayText: String = "",
    val corrections: String = "",
    val correctedEssay: String = "",
    val overallScore: String = "",
    val writingTips: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasResult: Boolean = false,
    val inputMode: InputMode = InputMode.TEXT,
    val imageUri: Uri? = null,
    val pdfPageCount: Int = 0,
    val pdfCurrentPage: Int = 0,
    val thinkingLevel: ThinkingLevel = ThinkingLevel.HIGH,
)

sealed interface EssayUiEvent {
    data class EssayTextChanged(val text: String) : EssayUiEvent
    data class InputModeChanged(val mode: InputMode) : EssayUiEvent
    data class ImageUriSelected(val uri: Uri) : EssayUiEvent
    data class PdfUriSelected(val uri: Uri) : EssayUiEvent
    data object CorrectClicked : EssayUiEvent
    data object ClearClicked : EssayUiEvent
    data class ErrorDismissed(val error: String? = null) : EssayUiEvent
    data class ThinkingLevelChanged(val level: ThinkingLevel) : EssayUiEvent
}

@HiltViewModel
class EssayViewModel @Inject constructor(
    private val correctEssayUseCase: CorrectEssayUseCase,
    private val settingsDataStore: SettingsDataStore,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _uiState = MutableStateFlow(EssayUiState())
    val uiState: StateFlow<EssayUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsDataStore.settings.first().let { s ->
                _uiState.value = _uiState.value.copy(
                    thinkingLevel = ThinkingLevel.fromId(s.essayThinkingLevel)
                )
            }
        }
    }

    fun onEvent(event: EssayUiEvent) {
        when (event) {
            is EssayUiEvent.EssayTextChanged -> {
                _uiState.value = _uiState.value.copy(essayText = event.text, error = null)
            }
            is EssayUiEvent.InputModeChanged -> {
                _uiState.value = _uiState.value.copy(
                    inputMode = event.mode,
                    imageUri = null,
                    pdfPageCount = 0
                )
            }
            is EssayUiEvent.ImageUriSelected -> {
                _uiState.value = _uiState.value.copy(imageUri = event.uri, inputMode = InputMode.IMAGE)
            }
            is EssayUiEvent.PdfUriSelected -> {
                _uiState.value = _uiState.value.copy(
                    imageUri = event.uri,
                    pdfPageCount = 1, // Will handle correct count inside if needed, simpler logic
                    pdfCurrentPage = 0,
                    inputMode = InputMode.PDF
                )
            }
            EssayUiEvent.CorrectClicked -> {
                correctEssay()
            }
            EssayUiEvent.ClearClicked -> {
                _uiState.value = EssayUiState(thinkingLevel = _uiState.value.thinkingLevel)
            }
            is EssayUiEvent.ErrorDismissed -> {
                _uiState.value = _uiState.value.copy(error = event.error)
            }
            is EssayUiEvent.ThinkingLevelChanged -> {
                _uiState.value = _uiState.value.copy(thinkingLevel = event.level)
                viewModelScope.launch {
                    settingsDataStore.updateEssayThinkingLevel(event.level.id)
                }
            }
        }
    }

    private fun correctEssay() {
        val state = _uiState.value
        val level = state.thinkingLevel
        val effort = level.id

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val result = when (state.inputMode) {
                    InputMode.TEXT -> correctEssayUseCase.correctFromText(state.essayText, effort)
                    InputMode.IMAGE -> correctEssayUseCase.correctFromImage(state.imageUri, effort)
                    InputMode.PDF -> correctEssayUseCase.correctFromPdf(state.imageUri, effort)
                }

                result.fold(
                    onSuccess = { correction ->
                        _uiState.value = _uiState.value.copy(
                            corrections = correction.corrections,
                            correctedEssay = correction.correctedEssay,
                            overallScore = correction.overallScore,
                            writingTips = correction.writingTips,
                            hasResult = true
                        )
                    },
                    onFailure = { e ->
                        val msg = e.message
                        val errorMsg = when (msg) {
                            "TEXT_EMPTY" -> context.getString(R.string.essay_error_empty)
                            "IMAGE_NULL" -> context.getString(R.string.essay_error_no_image)
                            "PDF_NULL" -> context.getString(R.string.essay_error_no_pdf)
                            "PDF_UNREADABLE" -> context.getString(R.string.error_ai_unreadable_image)
                            else -> context.getString(R.string.error_essay_failed)
                        }
                        _uiState.value = _uiState.value.copy(
                            error = errorMsg
                        )
                    }
                )
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}
