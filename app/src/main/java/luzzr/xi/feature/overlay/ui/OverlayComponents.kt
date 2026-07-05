package luzzr.xi.feature.overlay.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.heightIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
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
import luzzr.xi.core.ui.theme.CorrectionAdd
import luzzr.xi.core.ui.theme.CorrectionDelete
import luzzr.xi.core.ui.theme.OverlayBubble
import androidx.compose.animation.core.Spring.DampingRatioMediumBouncy
import androidx.compose.material3.MaterialTheme

enum class ExitAction { NONE, CLOSE, STOP }

/**
 * Composable components extracted from OverlayService to reduce file size below 400 lines.
 */
object OverlayComponents {

    @Composable
    fun FloatingBubbleContent(
        onClick: () -> Unit
    ) {
        var animateTrigger by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            animateTrigger = true
        }

        val enterScale by animateFloatAsState(
            targetValue = if (animateTrigger) 1f else 0f,
            animationSpec = luzzr.xi.core.ui.theme.MotionTokens.springDefault(),
            label = "bubble_enter"
        )

        val infiniteTransition = rememberInfiniteTransition(label = "bubble")
        val idleScale by infiniteTransition.animateFloat(
            initialValue = 0.96f, targetValue = 1.04f,
            animationSpec = infiniteRepeatable(animation = tween(2000),
                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse),
            label = "bubble_scale"
        )
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val pressScale by animateFloatAsState(
            targetValue = if (isPressed) 0.90f else 1f,
            animationSpec = luzzr.xi.core.ui.theme.MotionTokens.springDefault(),
            label = "press_scale"
        )
        Box(
            modifier = Modifier
                .size(52.dp)
                .graphicsLayer {
                    val s = idleScale * pressScale * enterScale
                    scaleX = s
                    scaleY = s
                }
                .clip(CircleShape)
                .background(OverlayBubble)
                .semantics { contentDescription = "打开翻译面板" }
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            val panelTitleDesc = stringResource(R.string.overlay_panel_title)
            AbstractIcons.Translate(
                modifier = Modifier.size(24.dp).semantics { contentDescription = panelTitleDesc },
                tint = Color.White
            )
        }
    }

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
        val scale by animateFloatAsState(
            targetValue = if (visible) 1f else 0.8f,
            animationSpec = luzzr.xi.core.ui.theme.MotionTokens.springGentle(),
            label = "panel_scale"
        )

        val alpha by animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = tween(durationMillis = 200),
            label = "panel_alpha",
            finishedListener = {
                if (!visible) {
                    onExitAnimationFinished()
                }
            }
        )

        var showSourcePicker by remember { mutableStateOf(false) }
        var showTargetPicker by remember { mutableStateOf(false) }

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
                        val copyInteraction = remember { MutableInteractionSource() }
                        val isCopyPressed by copyInteraction.collectIsPressedAsState()
                        val copyScale by animateFloatAsState(
                            targetValue = if (isCopyPressed) 0.90f else 1f,
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
                                .clickable(interactionSource = copyInteraction, indication = null, onClick = onCopy),
                            contentAlignment = Alignment.Center
                        ) {
                            AbstractIcons.Copy(modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        }

                        val launchInteraction = remember { MutableInteractionSource() }
                        val isLaunchPressed by launchInteraction.collectIsPressedAsState()
                        val launchScale by animateFloatAsState(
                            targetValue = if (isLaunchPressed) 0.90f else 1f,
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

                        val closeInteraction = remember { MutableInteractionSource() }
                        val isClosePressed by closeInteraction.collectIsPressedAsState()
                        val closeScale by animateFloatAsState(
                            targetValue = if (isClosePressed) 0.90f else 1f,
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
                                .clip(CircleShape)
                                .semantics { contentDescription = "关闭面板" }
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
                    val srcScale by animateFloatAsState(targetValue = if (isSrcPressed) 0.90f else 1f)
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
                    
                    val swapInteraction = remember { MutableInteractionSource() }
                    val isSwapPressed by swapInteraction.collectIsPressedAsState()
                    val swapScale by animateFloatAsState(
                        targetValue = if (isSwapPressed) 0.90f else 1f,
                        animationSpec = luzzr.xi.core.ui.theme.MotionTokens.springDefault(),
                        label = "swap"
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .graphicsLayer {
                                scaleX = swapScale
                                scaleY = swapScale
                            }
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .clickable(interactionSource = swapInteraction, indication = null, onClick = onSwap),
                        contentAlignment = Alignment.Center
                    ) {
                        AbstractIcons.Swap(modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))

                    val tgtInteraction = remember { MutableInteractionSource() }
                    val isTgtPressed by tgtInteraction.collectIsPressedAsState()
                    val tgtScale by animateFloatAsState(targetValue = if (isTgtPressed) 0.90f else 1f)
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
                AnimatedVisibility(
                    visible = showSourcePicker || showTargetPicker,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
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
                                text = if (isSrc) androidx.compose.ui.res.stringResource(luzzr.xi.R.string.translate_select_source) else androidx.compose.ui.res.stringResource(luzzr.xi.R.string.translate_select_target),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        showSourcePicker = false
                                        showTargetPicker = false
                                    }
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
                                                        showSourcePicker = false
                                                    } else {
                                                        onTargetLangChange(lang)
                                                        showTargetPicker = false
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

                // Engine selector
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
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .graphicsLayer {
                                scaleX = translateScale
                                scaleY = translateScale
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
                    
                    val stopInteraction = remember { MutableInteractionSource() }
                    val isStopPressed by stopInteraction.collectIsPressedAsState()
                    val stopScale by animateFloatAsState(
                        targetValue = if (isStopPressed) 0.90f else 1f,
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
                            .clip(AppShape.small)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .semantics { contentDescription = "停止服务" }
                            .clickable(interactionSource = stopInteraction, indication = null, onClick = onStop),
                        contentAlignment = Alignment.Center
                    ) {
                        AbstractIcons.Close(modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                    }
                }

                // Error
                if (error != null) {
                    Text(error, color = CorrectionDelete, fontSize = 12.sp)
                }

                // Result
                if (resultText.isNotEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(10.dp)
                    ) {
                        Text(resultText, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground, lineHeight = 18.sp)
                    }
                }
            }
        }
    }
}
