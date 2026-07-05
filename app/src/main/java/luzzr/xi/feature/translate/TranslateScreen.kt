package luzzr.xi.feature.translate

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import luzzr.xi.R
import luzzr.xi.domain.model.SupportedLanguage
import luzzr.xi.domain.model.ThinkingLevel
import luzzr.xi.feature.overlay.OverlayService
import luzzr.xi.core.ui.components.ThinkingSelector
import luzzr.xi.core.ui.components.EngineSelector
import luzzr.xi.core.ui.theme.AbstractIcons

import luzzr.xi.core.ui.theme.AppShape
import luzzr.xi.core.ui.theme.CorrectionAdd
import luzzr.xi.core.ui.theme.CorrectionDelete
import luzzr.xi.feature.translate.TranslateViewModel
import luzzr.xi.feature.translate.TranslateUiEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TranslateScreen(
    viewModel: TranslateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isEmpty = uiState.inputText.isEmpty() && uiState.resultText.isEmpty()

    var showSourceLangSheet by remember { mutableStateOf(false) }
    var showTargetLangSheet by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(600), androidx.compose.animation.core.RepeatMode.Reverse), label = "pulse"
    )

    var swapAngle by remember { mutableStateOf(0f) }
    val swapRotation by animateFloatAsState(
        targetValue = swapAngle,
        animationSpec = luzzr.xi.core.ui.theme.MotionTokens.springGentle(),
        label = "swap_rotation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ThinkingSelector(
            currentLevel = uiState.thinkingLevel,
            onLevelChange = { viewModel.onEvent(TranslateUiEvent.ThinkingLevelChanged(it)) },
            modifier = Modifier.fillMaxWidth()
        )

        // Engine selector
        EngineSelector(
            currentEngine = uiState.engine,
            onEngineChange = { viewModel.onEvent(TranslateUiEvent.EngineChanged(it)) },
            modifier = Modifier.fillMaxWidth()
        )

        // Language selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            LanguageSelector(language = uiState.sourceLang, onClick = { showSourceLangSheet = true })
            Spacer(modifier = Modifier.width(12.dp))
            val swapInteractionSource = remember { MutableInteractionSource() }
            val isSwapPressed by swapInteractionSource.collectIsPressedAsState()
            val swapBtnScale by animateFloatAsState(
                targetValue = if (isSwapPressed) 0.90f else 1f,
                animationSpec = luzzr.xi.core.ui.theme.MotionTokens.springDefault(),
                label = "swap_btn_scale"
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .graphicsLayer {
                        scaleX = swapBtnScale
                        scaleY = swapBtnScale
                        rotationZ = swapRotation
                    }
                    .clip(AppShape.small)
                    .clickable(
                        interactionSource = swapInteractionSource,
                        indication = null,
                        onClick = {
                            swapAngle += 180f
                            viewModel.onEvent(TranslateUiEvent.SwapClicked)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                val swapDesc = stringResource(R.string.translate_swap)
                AbstractIcons.Swap(
                    modifier = Modifier.size(22.dp).semantics { contentDescription = swapDesc },
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            LanguageSelector(language = uiState.targetLang, onClick = { showTargetLangSheet = true })
        }

        // Input — adaptive height
        OutlinedTextField(
            value = uiState.inputText,
            onValueChange = { viewModel.onEvent(TranslateUiEvent.InputChanged(it)) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 200.dp),
            placeholder = { Text(stringResource(R.string.translate_input_hint), color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                cursorColor = MaterialTheme.colorScheme.primary, focusedTextColor = MaterialTheme.colorScheme.onBackground, unfocusedTextColor = MaterialTheme.colorScheme.onBackground
            ),
            shape = AppShape.input,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { viewModel.onEvent(TranslateUiEvent.TranslateClicked) })
        )

        // Action buttons
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val translateInteractionSource = remember { MutableInteractionSource() }
            val isTranslatePressed by translateInteractionSource.collectIsPressedAsState()
            val pressScale by animateFloatAsState(
                targetValue = if (isTranslatePressed) 0.95f else 1f,
                animationSpec = luzzr.xi.core.ui.theme.MotionTokens.springDefault(),
                label = "translate_press"
            )
            val btnScale = if (uiState.isLoading) pulseScale else pressScale
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .graphicsLayer { scaleX = btnScale; scaleY = btnScale }
                    .clip(AppShape.button)
                    .background(if (uiState.isLoading) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary)
                    .clickable(
                        enabled = !uiState.isLoading,
                        interactionSource = translateInteractionSource,
                        indication = null
                    ) { viewModel.onEvent(TranslateUiEvent.TranslateClicked) },
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.background, strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.translate_btn), fontSize = 15.sp, color = MaterialTheme.colorScheme.background, fontWeight = FontWeight.Medium)
                }
            }
            ClearButton(onClick = { viewModel.onEvent(TranslateUiEvent.ClearClicked) })
        }

        // Overlay toggle
        OverlayToggleButton(context)

        // Error
        AnimatedVisibility(visible = uiState.error != null, enter = fadeIn(tween(200)) + expandVertically(tween(300)), exit = fadeOut(tween(150)) + shrinkVertically(tween(200))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(uiState.error ?: "", color = CorrectionDelete, fontSize = 13.sp, modifier = Modifier.weight(1f))
                if (uiState.inputText.isNotBlank() && uiState.error != null) {
                    TextButton(onClick = { viewModel.onEvent(TranslateUiEvent.TranslateClicked) }) {
                        Text(stringResource(R.string.retry), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Empty state
        AnimatedVisibility(visible = isEmpty, enter = fadeIn(tween(500)) + expandVertically(tween(400)), exit = fadeOut(tween(200)) + shrinkVertically(tween(200))) {
            EmptyState(title = stringResource(R.string.translate_empty_title), desc = stringResource(R.string.translate_empty_desc))
        }

        // Result
        AnimatedVisibility(visible = uiState.resultText.isNotEmpty(), enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it / 3 }, animationSpec = luzzr.xi.core.ui.theme.MotionTokens.springGentle()), exit = fadeOut(tween(200)) + shrinkVertically(tween(200))) {
            ResultCard(resultText = uiState.resultText, onCopy = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("translation", uiState.resultText))
            })
        }

        // Prevent bottom bar obstruction
        Spacer(modifier = Modifier.height(120.dp))
    }

    if (showSourceLangSheet) {
        LanguagePickerSheet(title = stringResource(R.string.translate_select_source), currentLang = uiState.sourceLang, excludeLang = uiState.targetLang, onSelect = { viewModel.onEvent(TranslateUiEvent.SourceLangChanged(it)); showSourceLangSheet = false }, onDismiss = { showSourceLangSheet = false })
    }
    if (showTargetLangSheet) {
        LanguagePickerSheet(title = stringResource(R.string.translate_select_target), currentLang = uiState.targetLang, excludeLang = uiState.sourceLang, onSelect = { viewModel.onEvent(TranslateUiEvent.TargetLangChanged(it)); showTargetLangSheet = false }, onDismiss = { showTargetLangSheet = false })
    }
}

@Composable
private fun ClearButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = luzzr.xi.core.ui.theme.MotionTokens.springDefault(),
        label = "clear_scale"
    )
    Box(
        modifier = Modifier
            .size(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(AppShape.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        val clearDesc = stringResource(R.string.translate_clear)
        AbstractIcons.Delete(
            modifier = Modifier.size(18.dp).semantics { contentDescription = clearDesc },
            tint = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
private fun ResultCard(resultText: String, onCopy: () -> Unit) {
    var copied by remember { mutableStateOf(false) }
    val checkScale by animateFloatAsState(
        targetValue = if (copied) 1.2f else 1f,
        animationSpec = luzzr.xi.core.ui.theme.MotionTokens.springGentle(),
        label = "check_scale"
    )
    val copyInteractionSource = remember { MutableInteractionSource() }
    val isCopyPressed by copyInteractionSource.collectIsPressedAsState()
    val copyScale by animateFloatAsState(
        targetValue = if (isCopyPressed) 0.90f else 1f,
        animationSpec = luzzr.xi.core.ui.theme.MotionTokens.springDefault(),
        label = "copy_scale"
    )

    Column(modifier = Modifier.fillMaxWidth().animateContentSize().clip(AppShape.card).background(MaterialTheme.colorScheme.surfaceVariant).padding(14.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.translate_result_label), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground)
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .scale(copyScale)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        interactionSource = copyInteractionSource,
                        indication = null,
                        onClick = { onCopy(); copied = true }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (copied) {
                    AbstractIcons.CheckCircle(
                        Modifier.size(16.dp).graphicsLayer {
                            scaleX = checkScale; scaleY = checkScale
                        },
                        tint = CorrectionAdd
                    )
                } else {
                val copyDesc = stringResource(R.string.translate_copy)
                AbstractIcons.Copy(
                    Modifier.size(16.dp).semantics { contentDescription = copyDesc },
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(6.dp))
        Text(resultText, fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground, lineHeight = 22.sp)
    }
    androidx.compose.runtime.LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(1500)
            copied = false
        }
    }
}

@Composable
private fun LanguageSelector(language: SupportedLanguage, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = luzzr.xi.core.ui.theme.MotionTokens.springDefault(),
        label = "lang_scale"
    )
    Row(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(AppShape.small)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(language.nativeName, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium, fontSize = 13.sp)
        Spacer(modifier = Modifier.width(4.dp))
        AbstractIcons.ArrowDropDown(modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguagePickerSheet(title: String, currentLang: SupportedLanguage, excludeLang: SupportedLanguage, onSelect: (SupportedLanguage) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(12.dp))
            val languages = SupportedLanguage.entries.filter { it != excludeLang }
            val rows = languages.chunked(2)
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (rowItems in rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (lang in rowItems) {
                            val isSelected = lang == currentLang
                            val interactionSource = remember { MutableInteractionSource() }
                            val isPressed by interactionSource.collectIsPressedAsState()
                            val itemScale by animateFloatAsState(
                                targetValue = if (isPressed) 0.90f else 1f,
                                animationSpec = luzzr.xi.core.ui.theme.MotionTokens.springDefault(),
                                label = "item_scale"
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .graphicsLayer {
                                        scaleX = itemScale
                                        scaleY = itemScale
                                    }
                                    .clip(AppShape.small)
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable(interactionSource = interactionSource, indication = null) { onSelect(lang) }
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                                    .semantics { contentDescription = lang.nativeName }
                            ) {
                                Column {
                                    Text(lang.nativeName, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground)
                                    Text(lang.displayName, fontSize = 11.sp, color = if (isSelected) MaterialTheme.colorScheme.background.copy(alpha = 0.7f) else MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }
                        val emptySlots = 2 - rowItems.size
                        if (emptySlots > 0) {
                            Spacer(modifier = Modifier.weight(emptySlots.toFloat()))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun EmptyState(title: String, desc: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        AbstractIcons.Translate(Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        Spacer(modifier = Modifier.height(10.dp))
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(4.dp))
        Text(desc, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary, textAlign = TextAlign.Center)
    }
}

@Composable
private fun OverlayToggleButton(context: Context) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = luzzr.xi.core.ui.theme.MotionTokens.springDefault(),
        label = "overlay_scale"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(AppShape.button)
            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
            .clickable(interactionSource = interactionSource, indication = null) {
                if (!Settings.canDrawOverlays(context)) {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                } else {
                    context.startForegroundService(Intent(context, OverlayService::class.java))
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AbstractIcons.Visibility(modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary, visible = true)
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.translate_overlay_btn), fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium)
        }
    }
}
