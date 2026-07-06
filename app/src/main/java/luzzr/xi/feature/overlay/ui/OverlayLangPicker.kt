package luzzr.xi.feature.overlay.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring.DampingRatioMediumBouncy
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import luzzr.xi.R
import luzzr.xi.domain.model.SupportedLanguage
import luzzr.xi.domain.model.TranslationEngine
import luzzr.xi.core.ui.theme.AbstractIcons
import luzzr.xi.core.ui.theme.AppShape

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
        enter = expandVertically(spring(dampingRatio = DampingRatioMediumBouncy)) + fadeIn(tween(200)),
        exit = shrinkVertically(spring(dampingRatio = DampingRatioMediumBouncy)) + fadeOut(tween(150))
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
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isSrc) stringResource(R.string.translate_select_source) else stringResource(R.string.translate_select_target),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Box(
                    modifier = Modifier
                        .clip(AppShape.mini)
                        .clickable { onDismiss() }
                        .padding(4.dp)
                ) {
                    AbstractIcons.Close(modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.secondary)
                }
            }

            val languages = SupportedLanguage.entries.filter { it != excludeLang }
            val rows = languages.chunked(4)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (rowItems in rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (lang in rowItems) {
                            val isSelected = lang == currentLang
                            val itemInteraction = remember { MutableInteractionSource() }
                            val isItemPressed by itemInteraction.collectIsPressedAsState()
                            val itemScale by animateFloatAsState(
                                targetValue = if (isItemPressed) 0.90f else 1f,
                                animationSpec = spring(dampingRatio = 0.85f),
                                label = "langItem"
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .graphicsLayer {
                                        scaleX = itemScale
                                        scaleY = itemScale
                                    }
                                    .clip(AppShape.mini)
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background)
                                    .border(0.5.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, AppShape.mini)
                                    .clickable(interactionSource = itemInteraction, indication = null) {
                                        if (isSrc) {
                                            onSourceLangChange(lang)
                                        } else {
                                            onTargetLangChange(lang)
                                        }
                                    }
                                    .padding(vertical = 8.dp)
                                    .semantics { contentDescription = lang.nativeName },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = lang.nativeName,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
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

@Composable
fun EngineSelector(
    engine: TranslationEngine,
    onEngineChange: (TranslationEngine) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TranslationEngine.entries.forEach { eng ->
            val isSelected = eng == engine
            val engInteraction = remember { MutableInteractionSource() }
            val isEngPressed by engInteraction.collectIsPressedAsState()
            val engScale by animateFloatAsState(
                targetValue = if (isEngPressed) 0.90f else 1f,
                animationSpec = luzzr.xi.core.ui.theme.MotionTokens.springDefault(),
                label = "eng_scale"
            )
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = engScale
                        scaleY = engScale
                    }
                    .clip(AppShape.small)
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background)
                    .border(0.5.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, AppShape.small)
                    .clickable(interactionSource = engInteraction, indication = null) {
                        onEngineChange(eng)
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .semantics { contentDescription = eng.displayName },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = eng.displayName,
                    fontSize = 11.sp,
                    color = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                )
            }
        }
    }
}
