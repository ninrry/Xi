package luzzr.xi.feature.essay.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import luzzr.xi.R
import luzzr.xi.core.ui.theme.AbstractIcons
import luzzr.xi.core.ui.theme.AppShape
import luzzr.xi.core.ui.theme.LocalExtendedColors
import luzzr.xi.core.ui.theme.AppSpacing
import luzzr.xi.core.ui.components.PressScaleBox
import luzzr.xi.core.ui.theme.MotionTokens
import androidx.compose.material3.MaterialTheme
import luzzr.xi.domain.model.GrammarError
import luzzr.xi.domain.model.VocabularySuggestion
import luzzr.xi.domain.model.StructureAnalysis
import luzzr.xi.domain.model.StyleAnalysis
import luzzr.xi.domain.model.ScoreBreakdown
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription

@Composable
fun CopyableHeader(title: String, content: String) {
    val context = LocalContext.current
    val copyDescription = stringResource(R.string.copy)
    var copied by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
        PressScaleBox(
            onClick = {
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("essay", content))
                copied = true
            },
            modifier = Modifier
                .size(48.dp)
                .clip(AppShape.button)
                .semantics { contentDescription = copyDescription }
        ) {
            if (copied) AbstractIcons.CheckCircle(Modifier.size(16.dp), tint = LocalExtendedColors.current.correctionAdd)
            else AbstractIcons.Copy(Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        }
    }
    androidx.compose.runtime.LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(1500)
            copied = false
        }
    }
}

@Composable
fun ScoreBreakdownChart(score: ScoreBreakdown) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md)) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = AppSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
            AbstractIcons.Sparkle(Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(AppSpacing.xs))
            Text(stringResource(R.string.essay_score), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.weight(1f))
            Text("${score.total}/100", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            if (!score.grade.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(AppSpacing.xs))
                Box(modifier = Modifier.clip(AppShape.mini).background(MaterialTheme.colorScheme.primary).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(score.grade, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.background, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        // 4 Dimensions
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            ScoreBar(modifier = Modifier.weight(1f), title = stringResource(R.string.score_grammar), score = score.grammar, max = 25)
            ScoreBar(modifier = Modifier.weight(1f), title = stringResource(R.string.score_vocab), score = score.vocabulary, max = 25)
            ScoreBar(modifier = Modifier.weight(1f), title = stringResource(R.string.score_structure), score = score.structure, max = 25)
            ScoreBar(modifier = Modifier.weight(1f), title = stringResource(R.string.score_style), score = score.style, max = 25)
        }
    }
}

@Composable
private fun ScoreBar(modifier: Modifier = Modifier, title: String, score: Int, max: Int) {
    val fraction = (score.toFloat() / max.toFloat()).coerceIn(0f, 1f)
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.height(AppSpacing.xs))
        Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(AppShape.mini).background(MaterialTheme.colorScheme.outline)) {
            Box(modifier = Modifier.fillMaxWidth(fraction).height(4.dp).clip(AppShape.mini).background(MaterialTheme.colorScheme.primary))
        }
        Spacer(modifier = Modifier.height(AppSpacing.xs))
        Text("$score", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun CorrectionTab(
    grammarErrors: List<GrammarError>,
    vocabulary: List<VocabularySuggestion>,
    structure: StructureAnalysis?,
    style: StyleAnalysis?
) {
    Column(modifier = Modifier.padding(AppSpacing.lg)) {
        if (grammarErrors.isNotEmpty()) {
            Text(stringResource(R.string.essay_grammar_correction), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(AppSpacing.sm))
            grammarErrors.forEach { error ->
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = AppSpacing.md)) {
                    val lineInfo = if (error.line != null) stringResource(R.string.line_info, error.line) else ""
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)) {
                                append(lineInfo)
                            }
                            withStyle(SpanStyle(color = LocalExtendedColors.current.correctionDelete, textDecoration = TextDecoration.LineThrough)) {
                                append(error.original)
                            }
                            append(" ")
                            withStyle(SpanStyle(color = LocalExtendedColors.current.correctionAdd, fontWeight = FontWeight.SemiBold)) {
                                append(error.corrected)
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(error.explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary, lineHeight = 18.sp)
                }
            }
        }

        if (vocabulary.isNotEmpty()) {
            if (grammarErrors.isNotEmpty()) Spacer(modifier = Modifier.height(AppSpacing.sm))
            Text(stringResource(R.string.essay_vocabulary_optimization), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(AppSpacing.sm))
            vocabulary.forEach { v ->
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = AppSpacing.md)) {
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = LocalExtendedColors.current.correctionDelete, textDecoration = TextDecoration.LineThrough)) {
                                append(v.original)
                            }
                            append(" ")
                            withStyle(SpanStyle(color = LocalExtendedColors.current.correctionAdd, fontWeight = FontWeight.SemiBold)) {
                                append(v.suggested)
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(v.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary, lineHeight = 18.sp)
                }
            }
        }

        val hasStructure = listOfNotNull(structure?.organization, structure?.transitions, structure?.logicalFlow).any { it.isNotBlank() }
        if (hasStructure) {
            if (grammarErrors.isNotEmpty() || vocabulary.isNotEmpty()) Spacer(modifier = Modifier.height(AppSpacing.sm))
            Text(stringResource(R.string.essay_structure_analysis), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(AppSpacing.sm))
            structure?.organization?.takeIf { it.isNotBlank() }?.let { Text("• $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(bottom = 6.dp), lineHeight = 18.sp) }
            structure?.transitions?.takeIf { it.isNotBlank() }?.let { Text("• $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(bottom = 6.dp), lineHeight = 18.sp) }
            structure?.logicalFlow?.takeIf { it.isNotBlank() }?.let { Text("• $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(bottom = 6.dp), lineHeight = 18.sp) }
        }

        val hasStyle = listOfNotNull(style?.sentenceVariety, style?.tone, style?.conciseness, style?.academicRegister).any { it.isNotBlank() }
        if (hasStyle) {
            if (grammarErrors.isNotEmpty() || vocabulary.isNotEmpty() || hasStructure) Spacer(modifier = Modifier.height(AppSpacing.sm))
            Text(stringResource(R.string.essay_style_suggestions), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(AppSpacing.sm))
            style?.sentenceVariety?.takeIf { it.isNotBlank() }?.let { Text("• $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(bottom = 6.dp), lineHeight = 18.sp) }
            style?.tone?.takeIf { it.isNotBlank() }?.let { Text("• $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(bottom = 6.dp), lineHeight = 18.sp) }
            style?.conciseness?.takeIf { it.isNotBlank() }?.let { Text("• $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(bottom = 6.dp), lineHeight = 18.sp) }
            style?.academicRegister?.takeIf { it.isNotBlank() }?.let { Text("• $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(bottom = 6.dp), lineHeight = 18.sp) }
        }
        
        if (grammarErrors.isEmpty() && vocabulary.isEmpty() && !hasStructure && !hasStyle) {
            Text(stringResource(R.string.essay_no_obvious_problems), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
fun CorrectedEssayTab(essay: String) {
    Column(modifier = Modifier.padding(AppSpacing.lg)) {
        CopyableHeader(stringResource(R.string.essay_tab_corrected), essay)
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        Box(modifier = Modifier.fillMaxWidth().clip(AppShape.card).background(LocalExtendedColors.current.correctionAddBg).padding(AppSpacing.md)) {
            Text(essay, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground, lineHeight = 20.sp)
        }
    }
}

@Composable
fun WritingTipsTab(tips: List<String>) {
    Column(modifier = Modifier.padding(AppSpacing.lg)) {
        CopyableHeader(stringResource(R.string.essay_tab_tips), tips.joinToString("\n"))
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        
        if (tips.isEmpty()) {
            Text(stringResource(R.string.essay_no_suggestions), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        } else {
            tips.forEach { tip ->
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = AppSpacing.xs).clip(AppShape.card).background(LocalExtendedColors.current.correctionNoteBg).padding(AppSpacing.md)) {
                    Text(tip.trimStart('-', ' '), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground, lineHeight = 18.sp)
                }
            }
        }
    }
}


