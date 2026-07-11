package luzzr.xi.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import luzzr.xi.domain.model.GrammarError
import luzzr.xi.domain.model.HistoryRecord
import luzzr.xi.domain.model.HistoryType
import luzzr.xi.domain.model.ScoreBreakdown
import luzzr.xi.domain.repository.HistoryGateway
import javax.inject.Inject

enum class HistoryFilter { ALL, TRANSLATE, ESSAY }

data class HistoryStats(
    val translateThisWeek: Int = 0,
    val essayThisWeek: Int = 0,
    val latestEssayScore: Int? = null
)

data class HistoryUiState(
    val filter: HistoryFilter = HistoryFilter.ALL,
    val records: List<HistoryRecord> = emptyList(),
    val stats: HistoryStats = HistoryStats(),
    val selected: HistoryRecord? = null,
    val isEmpty: Boolean = true
)

sealed interface HistoryUiEvent {
    data class FilterChanged(val filter: HistoryFilter) : HistoryUiEvent
    data class RecordClicked(val id: Long) : HistoryUiEvent
    data object DetailDismissed : HistoryUiEvent
    data object ClearAllClicked : HistoryUiEvent
    data class DeleteRecord(val id: Long) : HistoryUiEvent
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyGateway: HistoryGateway,
    private val gson: Gson
) : ViewModel() {

    private val filterFlow = MutableStateFlow(HistoryFilter.ALL)
    private val selectedIdFlow = MutableStateFlow<Long?>(null)
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                filterFlow,
                historyGateway.observeAll(),
                selectedIdFlow
            ) { filter, all, selectedId ->
                val filtered = when (filter) {
                    HistoryFilter.ALL -> all
                    HistoryFilter.TRANSLATE -> all.filter { it.type == HistoryType.TRANSLATE }
                    HistoryFilter.ESSAY -> all.filter { it.type == HistoryType.ESSAY }
                }
                Triple(filter, filtered, selectedId?.let { id -> all.find { it.id == id } })
            }.collect { (filter, records, selected) ->
                _uiState.update {
                    it.copy(
                        filter = filter,
                        records = records,
                        selected = selected,
                        isEmpty = records.isEmpty()
                    )
                }
                refreshStats()
            }
        }
    }

    fun onEvent(event: HistoryUiEvent) {
        when (event) {
            is HistoryUiEvent.FilterChanged -> filterFlow.value = event.filter
            is HistoryUiEvent.RecordClicked -> selectedIdFlow.value = event.id
            HistoryUiEvent.DetailDismissed -> selectedIdFlow.value = null
            HistoryUiEvent.ClearAllClicked -> viewModelScope.launch {
                historyGateway.deleteAll()
                selectedIdFlow.value = null
                refreshStats()
            }
            is HistoryUiEvent.DeleteRecord -> viewModelScope.launch {
                historyGateway.deleteById(event.id)
                if (selectedIdFlow.value == event.id) selectedIdFlow.value = null
                refreshStats()
            }
        }
    }

    fun parseGrammarErrors(json: String?): List<GrammarError> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            gson.fromJson(json, Array<GrammarError>::class.java)?.toList() ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun parseTips(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            gson.fromJson(json, Array<String>::class.java)?.toList() ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun toScore(record: HistoryRecord): ScoreBreakdown? {
        val total = record.scoreTotal ?: return null
        return ScoreBreakdown(
            grammar = record.scoreGrammar ?: 0,
            vocabulary = record.scoreVocab ?: 0,
            structure = record.scoreStructure ?: 0,
            style = record.scoreStyle ?: 0,
            total = total,
            grade = record.grade
        )
    }

    private fun refreshStats() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    stats = HistoryStats(
                        translateThisWeek = historyGateway.countTranslateThisWeek(),
                        essayThisWeek = historyGateway.countEssayThisWeek(),
                        latestEssayScore = historyGateway.latestEssayScore()
                    )
                )
            }
        }
    }
}
