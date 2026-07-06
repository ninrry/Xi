package luzzr.xi.feature.overlay.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring.DampingRatioMediumBouncy
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.height
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
import luzzr.xi.core.ui.theme.CorrectionAdd
import luzzr.xi.core.ui.theme.CorrectionDelete

@Composable
fun TranslationPanelContent(
    visible: Boolean,
    inputText: String,
    onInputChange: (String) -> Unit,
    resultText: String,
    isTranslating: Boolean,
    error: String?,
    sourceLang: SupportedLanguage,
    targetLang: SupportedLanguage,
    engine: TranslationEngine,
    onTranslate: () -> Unit,
    onSwap: () -> Unit,
    onCopy: () -> Unit,
    onDismiss: () -> Unit,
    onStop: () -> Unit,
    onLaunchEssay: () -> Unit,
    onExitAnimationFinished: () -> Unit,
    onSourceLangChange: (SupportedLanguage) -> Unit,
    onTargetLangChange: (SupportedLanguage) -> Unit,
    onEngineChange: (TranslationEngine) -> Unit
) {
    // E4: Unified exit timing — both scale and alpha use same duration
    val exitDurationMs = 250
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.85f,
        animationSpec = luzzr.xi.core.ui.theme.MotionTokens.springGentle(),
        label = "panel_scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = exitDurationMs),
        label = "panel_alpha",
        finishedListener = { if (!visible) onExitAnimationFinished() }
    )

    var showSourcePicker by remember { mutableStateOf(false) }
    var showTargetPicker by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    val copyResetScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .clip(AppShape.card)
            .background(MaterialTheme.colorScheme.background)
            .border(0.5.dp, MaterialTheme.colorScheme.outline, AppShape.card)
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.overlay_panel_title),
                    fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // U4: Copy button with success feedback
                    val copyInteraction = remember { MutableInteractionSource() }
                    val isCopyPressed by copyInteraction.collectIsPressedAsState()
                    val copyScale by animateFloatAsState(
                        targetValue = if (isCopyPressed) 0.85f else 1f,
                        animationSpec = luzzr.xi.core.ui.theme.MotionTokens.springDefault(),
                        label = "copy"
                    )
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .graphicsLayer {
                                scaleX = copyScale
                                scaleY = copyScale
                            }
                            .clip(AppShape.mini)
                            .clickable(interactionSource = copyInteraction, indication = null) {
                                onCopy()
                                copied = true
                                copyResetScope.launch { delay(1500); copied = false }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (copied) {
                            AbstractIcons.CheckCircle(modifier = Modifier.size(16.dp), tint = CorrectionAdd)
                        } else {
                            AbstractIcons.Copy(modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    val launchInteraction = remember { MutableInteractionSource() }
                    val isLaunchPressed by launchInteraction.collectIsPressedAsState()
                    val launchScale by animateFloatAsState(
                        targetValue = if (isLaunchPressed) 0.85f else 1f,
                        animationSpec = luzzr.xi.core.ui.theme.MotionTokens.springDefault(),
                        label = "launch"
                    )
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .graphicsLayer {
                                scaleX = launchScale
                                scaleY = launchScale
                            }
                            .clip(AppShape.mini)
                            .clickable(interactionSource = launchInteraction, indication = null, onClick = onLaunchEssay),
                        contentAlignment = Alignment.Center
                    ) {
                        AbstractIcons.Edit(modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    }

                    // U2: Close button — CircleShape → AppShape.mini
                    val closeInteraction = remember { MutableInteractionSource() }
                    val isClosePressed by closeInteraction.collectIsPressedAsState()
                    val closeScale by animateFloatAsState(
                        targetValue = if (isClosePressed) 0.85f else 1f,
                        animationSpec = luzzr.xi.core.ui.theme.MotionTokens.springDefault(),
                        label = "close"
                    )
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .graphicsLayer {
                                scaleX = closeScale
                                scaleY = closeScale
                            }
                            .clip(AppShape.mini)
                            .semantics { contentDescription = "close panel" }
                            .clickable(interactionSource = closeInteraction, indication = null, onClick = onDismiss),
                        contentAlignment = Alignment.Center
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
                val srcInteraction = remember { MutableInteractionSource() }
                val isSrcPressed by srcInteraction.collectIsPressedAsState()
                val srcScale by animateFloatAsState(
                    targetValue = if (isSrcPressed) 0.90f else 1f,
                    animationSpec = luzzr.xi.core.ui.theme.MotionTokens.springDefault(),
                    label = "srcPress"
                )
                Box(
                    modifier = Modifier
                        .graphicsLayer { scaleX = srcScale; scaleY = srcScale }
                        .clip(AppShape.small)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .clickable(interactionSource = srcInteraction, indication = null) {
                            showSourcePicker = !showSourcePicker
                            showTargetPicker = false
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(sourceLang.nativeName, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.width(4.dp))
                        val srcArrowRotation by animateFloatAsState(
                            targetValue = if (showSourcePicker) 180f else 0f,
                            animationSpec = spring(dampingRatio = DampingRatioMediumBouncy),
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

                Spacer(modifier = Modifier.width(12.dp))
                
                // E7: Swap button with rotation animation
                val swapInteraction = remember { MutableInteractionSource() }
                val isSwapPressed by swapInteraction.collectIsPressedAsState()
                val swapScale by animateFloatAsState(
                    targetValue = if (isSwapPressed) 0.85f else 1f,
                    animationSpec = luzzr.xi.core.ui.theme.MotionTokens.springDefault(),
                    label = "swap"
                )
                var swapRotation by remember { mutableStateOf(0f) }
                val animatedSwapRotation by animateFloatAsState(
                    targetValue = swapRotation,
                    animationSpec = spring(dampingRatio = DampingRatioMediumBouncy),
                    label = "swapRotation"
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .graphicsLayer {
                            scaleX = swapScale
                            scaleY = swapScale
                            rotationZ = animatedSwapRotation
                        }
                        .clip(AppShape.mini)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .clickable(interactionSource = swapInteraction, indication = null) {
                            swapRotation += 180f
                            onSwap()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AbstractIcons.Swap(modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                }
                
                Spacer(modifier = Modifier.width(12.dp))

                val tgtInteraction = remember { MutableInteractionSource() }
                val isTgtPressed by tgtInteraction.collectIsPressedAsState()
                val tgtScale by animateFloatAsState(
                    targetValue = if (isTgtPressed) 0.90f else 1f,
                    animationSpec = luzzr.xi.core.ui.theme.MotionTokens.springDefault(),
                    label = "tgtPress"
                )
                Box(
                    modifier = Modifier
                        .graphicsLayer { scaleX = tgtScale; scaleY = tgtScale }
                        .clip(AppShape.small)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .clickable(interactionSource = tgtInteraction, indication = null) {
                            showTargetPicker = !showTargetPicker
                            showSourcePicker = false
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(targetLang.nativeName, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.width(4.dp))
                        val tgtArrowRotation by animateFloatAsState(
                            targetValue = if (showTargetPicker) 180f else 0f,
                            animationSpec = spring(dampingRatio = DampingRatioMediumBouncy),
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

            // Engine selector
            EngineSelector(
                engine = engine,
                onEngineChange = onEngineChange
            )

            // Input
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier.fillMaxWidth().height(80.dp),
                placeholder = { Text(stringResource(R.string.overlay_input_hint), color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    cursorColor = MaterialTheme.colorScheme.primary, focusedTextColor = MaterialTheme.colorScheme.onBackground, unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                ),
                shape = AppShape.input,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
            )

            // Translate button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val translateInteraction = remember { MutableInteractionSource() }
                val isTranslatePressed by translateInteraction.collectIsPressedAsState()
                val translateScale by animateFloatAsState(
                    targetValue = if (isTranslatePressed) 0.95f else 1f,
                    animationSpec = luzzr.xi.core.ui.theme.MotionTokens.springDefault(),
                    label = "trans"
                )
                // E11: Pulse loading animation
                val infiniteTransition = rememberInfiniteTransition(label = "translate_pulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.05f,
                    animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                        animation = androidx.compose.animation.core.tween(600),
                        repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                    ),
                    label = "pulse"
                )
                val btnScale = if (isTranslating) translateScale * pulseScale else translateScale
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .graphicsLayer {
                            scaleX = btnScale
                            scaleY = btnScale
                        }
                        .clip(AppShape.button)
                        .background(if (isTranslating) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary)
                        .clickable(
                            enabled = !isTranslating,
                            interactionSource = translateInteraction,
                            indication = null,
                            onClick = onTranslate
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isTranslating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.background, strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.overlay_translate_btn), fontSize = 13.sp, color = MaterialTheme.colorScheme.background, fontWeight = FontWeight.Medium)
                    }
                }
                
                // U2: Stop button — CircleShape → AppShape.mini
                val stopInteraction = remember { MutableInteractionSource() }
                val isStopPressed by stopInteraction.collectIsPressedAsState()
                val stopScale by animateFloatAsState(
                    targetValue = if (isStopPressed) 0.85f else 1f,
                    animationSpec = luzzr.xi.core.ui.theme.MotionTokens.springDefault(),
                    label = "stop"
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .graphicsLayer {
                            scaleX = stopScale
                            scaleY = stopScale
                        }
                        .clip(AppShape.mini)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .semantics { contentDescription = "stop service" }
                        .clickable(interactionSource = stopInteraction, indication = null, onClick = onStop),
                    contentAlignment = Alignment.Center
                ) {
                    AbstractIcons.Close(modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                }
            }

            // E10: Error message with entrance animation
            AnimatedVisibility(
                visible = error != null,
                enter = fadeIn(tween(200)) + expandVertically(tween(250)),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(200))
            ) {
                if (error != null) {
                    Text(error, color = CorrectionDelete, fontSize = 12.sp)
                }
            }

            // E9: Result with entrance animation
            AnimatedVisibility(
                visible = resultText.isNotEmpty(),
                enter = fadeIn(tween(300)) + slideInVertically(
                    initialOffsetY = { it / 4 },
                    animationSpec = spring(dampingRatio = DampingRatioMediumBouncy)
                ),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(200))
            ) {
                // U8: Result area — RoundedCornerShape(10.dp) → AppShape.mini
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .clip(AppShape.mini)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(10.dp)
                ) {
                    Text(resultText, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground, lineHeight = 18.sp)
                }
            }
        }
    }
}
