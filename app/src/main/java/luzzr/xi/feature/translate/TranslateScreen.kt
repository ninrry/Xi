package luzzr.xi.feature.translate

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import luzzr.xi.domain.model.UiText
import luzzr.xi.core.ui.components.ThinkingSelector
import luzzr.xi.core.ui.components.EngineSelector
import luzzr.xi.core.ui.components.PressScaleBox
import luzzr.xi.core.ui.theme.AbstractIcons
import luzzr.xi.core.ui.theme.AppShape
import luzzr.xi.core.ui.theme.AppSpacing
import luzzr.xi.core.ui.theme.LocalExtendedColors
import luzzr.xi.core.ui.theme.MotionTokens
import luzzr.xi.feature.overlay.OverlayService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslateScreen(
    viewModel: TranslateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isEmpty = uiState.inputText.isEmpty() && uiState.resultText.isEmpty()

    var showSourceLangSheet by remember { mutableStateOf(false) }
    var showTargetLangSheet by remember { mutableStateOf(false) }

    val pulseScale by animateFloatAsState(
        targetValue = if (uiState.isLoading) 1.12f else 1f,
        animationSpec = MotionTokens.tweenMediumEasing(), label = "pulse_scale"
    )

    var swapAngle by remember { mutableStateOf(0f) }
    val swapRotation: Float by animateFloatAsState(
        targetValue = swapAngle,
        animationSpec = MotionTokens.springGentle(),
        label = "swap_rotation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .widthIn(max = 600.dp)
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
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
            Spacer(modifier = Modifier.width(AppSpacing.md))
            
            PressScaleBox(
                onClick = {
                    swapAngle += 180f
                    viewModel.onEvent(TranslateUiEvent.SwapClicked)
                },
                modifier = Modifier
                    .size(48.dp)
                    .graphicsLayer {
                        rotationZ = swapRotation
                    }
                    .clip(AppShape.small)
            ) {
                val swapDesc = stringResource(R.string.translate_swap)
                AbstractIcons.Swap(
                    modifier = Modifier.size(22.dp).semantics { contentDescription = swapDesc },
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(AppSpacing.md))
            LanguageSelector(language = uiState.targetLang, onClick = { showTargetLangSheet = true })
        }

        // Input — adaptive height
        OutlinedTextField(
            value = uiState.inputText,
            onValueChange = { viewModel.onEvent(TranslateUiEvent.InputChanged(it)) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 200.dp),
            placeholder = { Text(stringResource(R.string.translate_input_hint), color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodyMedium) },
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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
            val btnScale = pulseScale
            PressScaleBox(
                enabled = !uiState.isLoading,
                onClick = { viewModel.onEvent(TranslateUiEvent.TranslateClicked) },
                onPressScale = 0.97f,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .graphicsLayer { scaleX = btnScale; scaleY = btnScale }
                    .clip(AppShape.button)
                    .background(if (uiState.isLoading) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primary)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.background, strokeWidth = 2.5.dp)
                } else {
                    Text(stringResource(R.string.translate_btn), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.background)
                }
            }
            ClearButton(onClick = { viewModel.onEvent(TranslateUiEvent.ClearClicked) })
        }

        // Overlay toggle
        OverlayToggleButton(context)

        // Error
        AnimatedVisibility(visible = uiState.error != null, enter = fadeIn(MotionTokens.tweenShortEasing()) + expandVertically(MotionTokens.tweenMedium()), exit = fadeOut(MotionTokens.tweenShort()) + shrinkVertically(MotionTokens.tweenShort())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(uiState.error?.asString(context) ?: "", color = LocalExtendedColors.current.correctionDelete, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                if (uiState.inputText.isNotBlank() && uiState.error != null) {
                    PressScaleBox(onClick = { viewModel.onEvent(TranslateUiEvent.TranslateClicked) }, modifier = Modifier.clip(AppShape.small).padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text(stringResource(R.string.retry), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Empty state
        AnimatedVisibility(visible = isEmpty, enter = fadeIn(MotionTokens.tweenLong()) + expandVertically(MotionTokens.tweenMedium()), exit = fadeOut(MotionTokens.tweenShort()) + shrinkVertically(MotionTokens.tweenShort())) {
            EmptyState(title = stringResource(R.string.translate_empty_title), desc = stringResource(R.string.translate_empty_desc))
        }

        // Result
        AnimatedVisibility(visible = uiState.resultText.isNotEmpty(), enter = fadeIn(MotionTokens.tweenMedium()) + slideInVertically(initialOffsetY = { it / 3 }, animationSpec = MotionTokens.springGentle()), exit = fadeOut(MotionTokens.tweenShort()) + shrinkVertically(MotionTokens.tweenShort())) {
            ResultCard(
                resultText = uiState.resultText,
                detectedLanguage = uiState.detectedLanguage,
                alternatives = uiState.alternatives,
                usage = uiState.usage,
                onCopy = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("translation", uiState.resultText))
                }
            )
        }

        if (uiState.isLoading && uiState.inputText.isNotEmpty() && uiState.resultText.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.md, vertical = AppSpacing.lg),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
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
    PressScaleBox(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .clip(AppShape.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        val clearDesc = stringResource(R.string.translate_clear)
        AbstractIcons.Delete(
            modifier = Modifier.size(18.dp).semantics { contentDescription = clearDesc },
            tint = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
private fun EmptyState(title: String, desc: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = AppSpacing.xl), horizontalAlignment = Alignment.CenterHorizontally) {
        AbstractIcons.Translate(Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
        Spacer(modifier = Modifier.height(AppSpacing.md))
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(AppSpacing.xs))
        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary, textAlign = TextAlign.Center)
    }
}

@Composable
private fun OverlayToggleButton(context: Context) {
    PressScaleBox(
        onPressScale = 0.97f,
        onClick = {
            if (!Settings.canDrawOverlays(context)) {
                context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
            } else {
                context.startForegroundService(Intent(context, OverlayService::class.java))
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(AppShape.button)
            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AbstractIcons.Visibility(modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary, visible = true)
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.translate_overlay_btn), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}
