package luzzr.xi.feature.overlay.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import luzzr.xi.R
import luzzr.xi.domain.model.SupportedLanguage
import luzzr.xi.domain.model.TranslationEngine
import luzzr.xi.core.ui.theme.AbstractIcons
import luzzr.xi.core.ui.theme.AppShape
import luzzr.xi.core.ui.theme.AppSpacing
import luzzr.xi.core.ui.components.PressScaleBox
import luzzr.xi.core.ui.components.ThinkingSelector
import luzzr.xi.core.ui.theme.LocalExtendedColors
import luzzr.xi.core.ui.theme.MotionTokens
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import luzzr.xi.core.ui.components.EngineSelector

@Composable
fun TranslationPanelContent(
    visible: Boolean,
    inputText: String,
    onInputChange: (String) -> Unit,
    resultText: String,
    usage: luzzr.xi.domain.model.Usage?,
    isTranslating: Boolean,
    isModelDownloading: Boolean = false,
    error: String?,
    sourceLang: SupportedLanguage,
    targetLang: SupportedLanguage,
    engine: TranslationEngine,
    thinkingLevel: luzzr.xi.domain.model.ThinkingLevel,
    onTranslate: () -> Unit,
    onSwap: () -> Unit,
    onCopy: () -> Unit,
    onDismiss: () -> Unit,
    onStop: () -> Unit,
    onLaunchEssay: () -> Unit,
    onExitAnimationFinished: () -> Unit,
    onSourceLangChange: (SupportedLanguage) -> Unit,
    onTargetLangChange: (SupportedLanguage) -> Unit,
    onEngineChange: (TranslationEngine) -> Unit,
    onThinkingLevelChange: (luzzr.xi.domain.model.ThinkingLevel) -> Unit
) {
    val context = LocalContext.current
    val closePanelDesc = stringResource(R.string.semantics_close_panel)
    val stopServiceDesc = stringResource(R.string.semantics_stop_service)
    // E4: Unified exit timing — both scale and alpha use same duration
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.85f,
        animationSpec = MotionTokens.springGentle(),
        label = "panel_scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = MotionTokens.tweenMedium(),
        label = "panel_alpha",
        finishedListener = { if (!visible) onExitAnimationFinished() }
    )

    var showSourcePicker by remember { mutableStateOf(false) }
    var showTargetPicker by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var copyJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppSpacing.md)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .clip(AppShape.card)
            .background(MaterialTheme.colorScheme.background)
            .border(0.5.dp, MaterialTheme.colorScheme.outline, AppShape.card)
            .padding(AppSpacing.lg)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.overlay_panel_title),
                    style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                    // U4: Copy button with success feedback
                    PressScaleBox(
                        onClick = {
                            onCopy()
                            copied = true
                            copyJob?.cancel()
                            copyJob = scope.launch {
                                delay(1500)
                                copied = false
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(AppShape.button)
                    ) {
                        if (copied) {
                            AbstractIcons.CheckCircle(modifier = Modifier.size(16.dp), tint = LocalExtendedColors.current.correctionAdd)
                        } else {
                            AbstractIcons.Copy(modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    PressScaleBox(
                        onClick = onLaunchEssay,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(AppShape.button)
                    ) {
                        AbstractIcons.Edit(modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    }

                    // U2: Close button — CircleShape → AppShape.mini
                    PressScaleBox(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(AppShape.button)
                            .semantics { contentDescription = closePanelDesc }
                    ) {
                        val closeDesc = stringResource(R.string.overlay_close)
                        AbstractIcons.Close(
                            modifier = Modifier.size(16.dp).semantics { contentDescription = closeDesc },
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // Language row with dropdowns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PressScaleBox(
                    onClick = {
                        showSourcePicker = !showSourcePicker
                        showTargetPicker = false
                    },
                    onPressScale = 0.97f,
                    modifier = Modifier
                        .clip(AppShape.button)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                        .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(sourceLang.nativeName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(AppSpacing.xs))
                        val srcArrowRotation by animateFloatAsState(
                            targetValue = if (showSourcePicker) 180f else 0f,
                            animationSpec = MotionTokens.springDefault(),
                            label = "srcArrow"
                        )
                        AbstractIcons.ArrowDropDown(
                            modifier = Modifier
                                .size(12.dp)
                                .graphicsLayer { rotationZ = srcArrowRotation },
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(AppSpacing.md))
                
                // E7: Swap button with rotation animation
                var swapRotation by remember { mutableStateOf(0f) }
                val animatedSwapRotation by animateFloatAsState(
                    targetValue = swapRotation,
                    animationSpec = MotionTokens.springDefault(),
                    label = "swapRotation"
                )
                PressScaleBox(
                    onClick = {
                        swapRotation += 180f
                        onSwap()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .graphicsLayer {
                            rotationZ = animatedSwapRotation
                        }
                        .clip(AppShape.button)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                ) {
                    AbstractIcons.Swap(modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                }
                
                Spacer(modifier = Modifier.width(AppSpacing.md))

                PressScaleBox(
                    onClick = {
                        showTargetPicker = !showTargetPicker
                        showSourcePicker = false
                    },
                    onPressScale = 0.97f,
                    modifier = Modifier
                        .clip(AppShape.button)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                        .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(targetLang.nativeName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(AppSpacing.xs))
                        val tgtArrowRotation by animateFloatAsState(
                            targetValue = if (showTargetPicker) 180f else 0f,
                            animationSpec = MotionTokens.springDefault(),
                            label = "tgtArrow"
                        )
                        AbstractIcons.ArrowDropDown(
                            modifier = Modifier
                                .size(12.dp)
                                .graphicsLayer { rotationZ = tgtArrowRotation },
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Collapsible multi-language grid
            LanguagePickerGrid(
                showSourcePicker = showSourcePicker,
                showTargetPicker = showTargetPicker,
                sourceLang = sourceLang,
                targetLang = targetLang,
                onSourceLangChange = { lang ->
                    onSourceLangChange(lang)
                    showSourcePicker = false
                },
                onTargetLangChange = { lang ->
                    onTargetLangChange(lang)
                    showTargetPicker = false
                },
                onDismiss = {
                    showSourcePicker = false
                    showTargetPicker = false
                }
            )

            // Thinking Level selector
            ThinkingSelector(
                currentLevel = thinkingLevel,
                onLevelChange = onThinkingLevelChange,
                modifier = Modifier.fillMaxWidth()
            )

            // Engine selector
            EngineSelector(
                currentEngine = engine,
                onEngineChange = onEngineChange,
                modifier = Modifier.fillMaxWidth()
            )

            // Input
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp, max = 120.dp),
                placeholder = { Text(stringResource(R.string.overlay_input_hint), color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    cursorColor = MaterialTheme.colorScheme.primary, focusedTextColor = MaterialTheme.colorScheme.onBackground, unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                ),
                shape = AppShape.input,
                textStyle = MaterialTheme.typography.bodySmall
            )

            // Translate button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                PressScaleBox(
                    enabled = !isTranslating,
                    onClick = onTranslate,
                    onPressScale = 0.97f,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(AppShape.button)
                        .background(if (isTranslating) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary)
                ) {
                    if (isTranslating) {
                        if (isModelDownloading) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = MaterialTheme.colorScheme.background, strokeWidth = 2.dp)
                                Text(stringResource(R.string.mlkit_downloading), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.background)
                            }
                        } else {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.background, strokeWidth = 2.dp)
                        }
                    } else {
                        Text(stringResource(R.string.overlay_translate_btn), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.background)
                    }
                }
                
                // U2: Stop button — CircleShape → AppShape.mini
                PressScaleBox(
                    onClick = onStop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(AppShape.button)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(0.5.dp, MaterialTheme.colorScheme.outline, AppShape.button)
                        .semantics { contentDescription = stopServiceDesc }
                ) {
                    AbstractIcons.Close(modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                }
            }

            // E10: Error message with entrance animation
            AnimatedVisibility(
                visible = error != null,
                enter = fadeIn(MotionTokens.tweenShortEasing()) + expandVertically(MotionTokens.tweenMedium()),
                exit = fadeOut(MotionTokens.tweenShort()) + shrinkVertically(MotionTokens.tweenShort())
            ) {
                if (error != null) {
                    Text(error, color = LocalExtendedColors.current.correctionDelete, style = MaterialTheme.typography.bodySmall)
                }
            }

            // E9: Result with entrance animation
            AnimatedVisibility(
                visible = resultText.isNotEmpty(),
                enter = fadeIn(MotionTokens.tweenMedium()) + slideInVertically(
                    initialOffsetY = { it / 4 },
                    animationSpec = MotionTokens.springDefault()
                ),
                exit = fadeOut(MotionTokens.tweenShort()) + shrinkVertically(MotionTokens.tweenShort())
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .clip(AppShape.card)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(0.5.dp, MaterialTheme.colorScheme.outline, AppShape.card)
                        .padding(AppSpacing.lg)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(resultText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground, lineHeight = 18.sp)
                    if (usage != null && usage.totalTokens != null) {
                        Spacer(modifier = Modifier.height(AppSpacing.xs))
                        Text(
                            stringResource(R.string.tokens_format, usage.totalTokens, usage.promptTokens ?: 0, usage.completionTokens ?: 0),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.align(Alignment.BottomEnd)
                        )
                    }
                }
            }
        }
    }
}
