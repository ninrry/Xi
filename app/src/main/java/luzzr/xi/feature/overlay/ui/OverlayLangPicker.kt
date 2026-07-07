package luzzr.xi.feature.overlay.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import luzzr.xi.R
import luzzr.xi.domain.model.SupportedLanguage
import luzzr.xi.core.ui.theme.AbstractIcons
import luzzr.xi.core.ui.theme.AppShape
import luzzr.xi.core.ui.theme.AppSpacing
import luzzr.xi.core.ui.components.PressScaleBox
import luzzr.xi.core.ui.theme.MotionTokens

@Composable
fun LanguagePickerGrid(
    showSourcePicker: Boolean,
    showTargetPicker: Boolean,
    sourceLang: SupportedLanguage,
    targetLang: SupportedLanguage,
    onSourceLangChange: (SupportedLanguage) -> Unit,
    onTargetLangChange: (SupportedLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = showSourcePicker || showTargetPicker,
        enter = expandVertically(MotionTokens.springDefault()) + fadeIn(MotionTokens.tweenShortEasing()),
        exit = shrinkVertically(MotionTokens.springDefault()) + fadeOut(MotionTokens.tweenShort())
    ) {
        val isSrc = showSourcePicker
        val currentLang = if (isSrc) sourceLang else targetLang
        val excludeLang = if (isSrc) targetLang else sourceLang

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShape.small)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(0.5.dp, MaterialTheme.colorScheme.outline, AppShape.small)
                .padding(AppSpacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = AppSpacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isSrc) stringResource(R.string.translate_select_source) else stringResource(R.string.translate_select_target),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                PressScaleBox(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(AppShape.mini)
                        .padding(AppSpacing.xs)
                ) {
                    AbstractIcons.Close(modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.secondary)
                }
            }

            val languages = SupportedLanguage.entries.filter { it != excludeLang && (isSrc || it != SupportedLanguage.AUTO) }
            val rows = languages.chunked(4)
            Column(
                modifier = Modifier.fillMaxWidth().semantics { selectableGroup() },
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                for (rowItems in rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                    ) {
                        for (lang in rowItems) {
                            val isSelected = lang == currentLang

                            PressScaleBox(
                                onClick = {
                                    if (isSrc) {
                                        onSourceLangChange(lang)
                                    } else {
                                        onTargetLangChange(lang)
                                    }
                                },
                                onPressScale = 0.97f,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(AppShape.mini)
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background)
                                    .border(0.5.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, AppShape.mini)
                                    .padding(horizontal = AppSpacing.xs, vertical = AppSpacing.sm)
                                    .semantics {
                                        contentDescription = lang.nativeName
                                        selected = isSelected
                                        role = Role.Tab
                                    }
                            ) {
                                Text(
                                    text = lang.nativeName,
                                    style = if (isSelected) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                        val emptySlots = 4 - rowItems.size
                        if (emptySlots > 0) {
                            Spacer(modifier = Modifier.weight(emptySlots.toFloat()))
                        }
                    }
                }
            }
        }
    }
}


