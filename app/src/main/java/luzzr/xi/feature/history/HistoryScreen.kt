package luzzr.xi.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import luzzr.xi.R
import luzzr.xi.core.ui.components.PressScaleBox
import luzzr.xi.core.ui.theme.AbstractIcons
import luzzr.xi.core.ui.theme.AppShape
import luzzr.xi.core.ui.theme.AppSpacing
import luzzr.xi.domain.model.HistoryRecord
import luzzr.xi.domain.model.HistoryType
import luzzr.xi.feature.essay.components.ScoreBreakdownChart
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .widthIn(max = 600.dp)
            .padding(horizontal = AppSpacing.lg)
            .padding(top = AppSpacing.md, bottom = 100.dp)
    ) {
        Text(
            text = stringResource(R.string.history_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(AppSpacing.md))

        HistoryStatsHeader(stats = uiState.stats)
        Spacer(modifier = Modifier.height(AppSpacing.md))

        HistoryFilterRow(
            selected = uiState.filter,
            onSelect = { viewModel.onEvent(HistoryUiEvent.FilterChanged(it)) }
        )
        Spacer(modifier = Modifier.height(AppSpacing.md))

        if (uiState.isEmpty) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AbstractIcons.History(
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.sm))
                    Text(
                        stringResource(R.string.history_empty_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        stringResource(R.string.history_empty_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                contentPadding = PaddingValues(bottom = AppSpacing.xl)
            ) {
                items(uiState.records, key = { it.id }) { record ->
                    HistoryListItem(
                        record = record,
                        onClick = { viewModel.onEvent(HistoryUiEvent.RecordClicked(record.id)) }
                    )
                }
            }
        }
    }

    uiState.selected?.let { record ->
        ModalBottomSheet(
            onDismissRequest = { viewModel.onEvent(HistoryUiEvent.DetailDismissed) },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            HistoryDetailContent(
                record = record,
                viewModel = viewModel,
                onDelete = {
                    viewModel.onEvent(HistoryUiEvent.DeleteRecord(record.id))
                    viewModel.onEvent(HistoryUiEvent.DetailDismissed)
                }
            )
        }
    }
}

@Composable
private fun HistoryStatsHeader(stats: HistoryStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.history_stat_translate_week),
            value = stats.translateThisWeek.toString()
        )
        StatCard(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.history_stat_essay_week),
            value = stats.essayThisWeek.toString()
        )
        StatCard(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.history_stat_latest_score),
            value = stats.latestEssayScore?.toString() ?: "—"
        )
    }
}

@Composable
private fun StatCard(modifier: Modifier = Modifier, label: String, value: String) {
    Column(
        modifier = modifier
            .clip(AppShape.card)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(AppSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HistoryFilterRow(
    selected: HistoryFilter,
    onSelect: (HistoryFilter) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        FilterChip(
            label = stringResource(R.string.history_filter_all),
            selected = selected == HistoryFilter.ALL,
            onClick = { onSelect(HistoryFilter.ALL) }
        )
        FilterChip(
            label = stringResource(R.string.history_filter_translate),
            selected = selected == HistoryFilter.TRANSLATE,
            onClick = { onSelect(HistoryFilter.TRANSLATE) }
        )
        FilterChip(
            label = stringResource(R.string.history_filter_essay),
            selected = selected == HistoryFilter.ESSAY,
            onClick = { onSelect(HistoryFilter.ESSAY) }
        )
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    PressScaleBox(
        onClick = onClick,
        onPressScale = 0.96f,
        modifier = Modifier
            .clip(AppShape.button)
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                0.5.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                AppShape.button
            )
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
private fun HistoryListItem(
    record: HistoryRecord,
    onClick: () -> Unit
) {
    val isTranslate = record.type == HistoryType.TRANSLATE
    val preview = if (isTranslate) {
        record.inputText.orEmpty()
    } else {
        record.originalEssay ?: record.outputText.orEmpty()
    }
    val time = formatTime(record.createdAt)

    PressScaleBox(
        onClick = onClick,
        onPressScale = 0.98f,
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShape.card)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(AppSpacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(AppShape.button)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                if (isTranslate) {
                    AbstractIcons.Translate(modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                } else {
                    AbstractIcons.Edit(modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.width(AppSpacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (isTranslate) stringResource(R.string.history_type_translate)
                        else stringResource(R.string.history_type_essay),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (!isTranslate && record.scoreTotal != null) {
                        Spacer(modifier = Modifier.width(AppSpacing.xs))
                        Text(
                            "${record.scoreTotal}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (isTranslate && record.sourceLang != null && record.targetLang != null) {
                        Spacer(modifier = Modifier.width(AppSpacing.xs))
                        Text(
                            "${record.sourceLang} → ${record.targetLang}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    preview.ifBlank { stringResource(R.string.history_no_preview) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    time,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun HistoryDetailContent(
    record: HistoryRecord,
    viewModel: HistoryViewModel,
    onDelete: () -> Unit
) {
    val isTranslate = record.type == HistoryType.TRANSLATE
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AppSpacing.lg)
            .padding(bottom = AppSpacing.xl)
    ) {
        Text(
            if (isTranslate) stringResource(R.string.history_type_translate)
            else stringResource(R.string.history_type_essay),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            formatTime(record.createdAt),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(AppSpacing.md))

        if (isTranslate) {
            DetailBlock(stringResource(R.string.history_detail_input), record.inputText.orEmpty())
            Spacer(modifier = Modifier.height(AppSpacing.sm))
            DetailBlock(stringResource(R.string.history_detail_output), record.outputText.orEmpty())
            if (record.sourceLang != null) {
                Spacer(modifier = Modifier.height(AppSpacing.xs))
                Text(
                    "${record.sourceLang} → ${record.targetLang} · ${record.engine.orEmpty()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        } else {
            viewModel.toScore(record)?.let { score ->
                ScoreBreakdownChart(score = score)
                Spacer(modifier = Modifier.height(AppSpacing.sm))
            }
            if (!record.originalEssay.isNullOrBlank()) {
                DetailBlock(stringResource(R.string.history_detail_original), record.originalEssay)
                Spacer(modifier = Modifier.height(AppSpacing.sm))
            }
            if (!record.outputText.isNullOrBlank()) {
                DetailBlock(stringResource(R.string.history_detail_corrected), record.outputText)
                Spacer(modifier = Modifier.height(AppSpacing.sm))
            }
            val errors = viewModel.parseGrammarErrors(record.grammarErrorsJson)
            if (errors.isNotEmpty()) {
                Text(
                    stringResource(R.string.essay_grammar_correction) + " (${errors.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                errors.take(5).forEach { err ->
                    Text(
                        "• ${err.original} → ${err.corrected}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(modifier = Modifier.height(AppSpacing.sm))
            }
            val tips = viewModel.parseTips(record.writingTipsJson)
            if (tips.isNotEmpty()) {
                DetailBlock(
                    stringResource(R.string.essay_tab_tips),
                    tips.joinToString("\n") { "• $it" }
                )
            }
        }

        Spacer(modifier = Modifier.height(AppSpacing.lg))
        PressScaleBox(
            onClick = onDelete,
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShape.button)
                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                .padding(vertical = AppSpacing.md),
            contentAlignment = Alignment.Center
        ) {
            Text(
                stringResource(R.string.history_delete_record),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun DetailBlock(title: String, body: String) {
    Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    Spacer(modifier = Modifier.height(4.dp))
    Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
}

private fun formatTime(ms: Long): String {
    val fmt = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return fmt.format(Date(ms))
}
