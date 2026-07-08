package luzzr.xi.feature.essay

import android.net.Uri
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.compose.material3.AlertDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import luzzr.xi.R
import luzzr.xi.domain.model.ThinkingLevel
import luzzr.xi.domain.model.UiText
import luzzr.xi.core.ui.components.ThinkingSelector
import luzzr.xi.core.ui.theme.AbstractIcons
import luzzr.xi.core.ui.theme.MotionTokens
import luzzr.xi.core.ui.theme.AppShape
import luzzr.xi.core.ui.theme.AppSpacing
import luzzr.xi.core.ui.components.PressScaleBox
import luzzr.xi.core.ui.theme.CorrectionAdd
import luzzr.xi.core.ui.theme.CorrectionDelete
import luzzr.xi.core.ui.theme.CorrectionAddBg
import luzzr.xi.core.ui.theme.CorrectionNoteBg
import luzzr.xi.feature.essay.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EssayScreen(
    viewModel: EssayViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedResultTab by remember { mutableIntStateOf(0) }
    LaunchedEffect(uiState.hasResult) {
        if (uiState.hasResult) selectedResultTab = 0
    }
    val isEmpty = uiState.essayText.isEmpty() && uiState.imageUriString == null && !uiState.hasResult

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            tempCameraUri?.let { uri ->
                viewModel.onEvent(EssayUiEvent.ImageUriSelected(uri.toString(), PhotoSource.CAMERA))
            }
        }
    }

    var showPermissionDeniedDialog by remember { mutableStateOf(false) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            try {
                tempCameraUri?.let { cameraLauncher.launch(it) }
            } catch (e: Exception) {
                viewModel.onEvent(EssayUiEvent.ErrorOccurred(UiText.StringResource(R.string.essay_camera_not_found)))
            }
        } else {
            showPermissionDeniedDialog = true
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val mimeType = context.contentResolver.getType(it)
            if (mimeType == "application/pdf") {
                val pdfUri = it
                coroutineScope.launch(Dispatchers.IO) {
                    var pageCount = 0
                    try {
                        context.contentResolver.openFileDescriptor(pdfUri, "r")?.use { pfd ->
                            android.graphics.pdf.PdfRenderer(pfd).use { renderer ->
                                pageCount = renderer.pageCount
                            }
                        }
                        } catch (e: Exception) { android.util.Log.w("EssayScreen", "pdf page count failed", e) }
                    withContext(Dispatchers.Main) {
                        viewModel.onEvent(EssayUiEvent.PdfUriSelected(pdfUri.toString(), pageCount))
                    }
                }
            } else {
                viewModel.onEvent(EssayUiEvent.ImageUriSelected(it.toString()))
            }
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val pdfUri = it
            coroutineScope.launch(Dispatchers.IO) {
                var pageCount = 0
                try {
                    context.contentResolver.openFileDescriptor(pdfUri, "r")?.use { pfd ->
                        android.graphics.pdf.PdfRenderer(pfd).use { renderer ->
                            pageCount = renderer.pageCount
                        }
                    }
                } catch (e: Exception) { android.util.Log.w("EssayScreen", "pdf open failed", e) }
                withContext(Dispatchers.Main) {
                    viewModel.onEvent(EssayUiEvent.PdfUriSelected(pdfUri.toString(), pageCount))
                }
            }
        }
    }

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
            onLevelChange = { viewModel.onEvent(EssayUiEvent.ThinkingLevelChanged(it)) },
            label = stringResource(R.string.essay_thinking_level),
            modifier = Modifier.fillMaxWidth()
        )

        // Input mode tabs (Redesigned: 4 uniform parallel buttons, no text, custom icons)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val textDesc = stringResource(R.string.essay_input_text)
            val cameraDesc = stringResource(R.string.essay_input_photo)
            val pdfDesc = stringResource(R.string.essay_input_pdf)
            val galleryDesc = stringResource(R.string.essay_gallery)

            // 1. Text Button
            val isTextSelected = uiState.inputMode == InputMode.TEXT
            PressScaleBox(
                onClick = { viewModel.onEvent(EssayUiEvent.InputModeChanged(InputMode.TEXT)) },
                modifier = Modifier
                    .weight(1f)
                    .clip(AppShape.small)
                    .background(if (isTextSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.md)
                    .semantics { contentDescription = textDesc }
            ) {
                AbstractIcons.Edit(
                    modifier = Modifier.size(22.dp),
                    tint = if (isTextSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground
                )
            }

            // 2. Camera Button
            val isCameraSelected = uiState.inputMode == InputMode.IMAGE && uiState.photoSource == PhotoSource.CAMERA
            PressScaleBox(
                onClick = {
                    val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                    if (hasPerm) {
                        try {
                            val file = java.io.File(context.cacheDir, "essay_photo_${System.currentTimeMillis()}.jpg")
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                            tempCameraUri = uri
                            cameraLauncher.launch(uri)
                        } catch (e: Exception) {
                            viewModel.onEvent(EssayUiEvent.ErrorOccurred(UiText.StringResource(R.string.essay_camera_not_found)))
                        }
                    } else {
                        try {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        } catch (e: Exception) {
                            viewModel.onEvent(EssayUiEvent.ErrorOccurred(UiText.StringResource(R.string.essay_camera_permission_denied)))
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .clip(AppShape.small)
                    .background(if (isCameraSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.md)
                    .semantics { contentDescription = cameraDesc }
            ) {
                AbstractIcons.Camera(
                    modifier = Modifier.size(22.dp),
                    tint = if (isCameraSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground
                )
            }

            // 3. PDF Button
            val isPdfSelected = uiState.inputMode == InputMode.PDF
            PressScaleBox(
                onClick = { pdfLauncher.launch("application/pdf") },
                modifier = Modifier
                    .weight(1f)
                    .clip(AppShape.small)
                    .background(if (isPdfSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.md)
                    .semantics { contentDescription = pdfDesc }
            ) {
                AbstractIcons.Document(
                    modifier = Modifier.size(22.dp),
                    tint = if (isPdfSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground
                )
            }

            // 4. Gallery Button
            val isGallerySelected = uiState.inputMode == InputMode.IMAGE && uiState.photoSource == PhotoSource.GALLERY
            PressScaleBox(
                onClick = { galleryLauncher.launch("*/*") },
                modifier = Modifier
                    .weight(1f)
                    .clip(AppShape.small)
                    .background(if (isGallerySelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.md)
                    .semantics { contentDescription = galleryDesc }
            ) {
                AbstractIcons.Gallery(
                    modifier = Modifier.size(22.dp),
                    tint = if (isGallerySelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Input area — adaptive height
        when (uiState.inputMode) {
            InputMode.TEXT -> {
                OutlinedTextField(
                    value = uiState.essayText, onValueChange = { viewModel.onEvent(EssayUiEvent.EssayTextChanged(it)) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 240.dp),
                    placeholder = { Text(stringResource(R.string.essay_input_hint), color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodyMedium) },
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline, cursorColor = MaterialTheme.colorScheme.primary, focusedTextColor = MaterialTheme.colorScheme.onBackground, unfocusedTextColor = MaterialTheme.colorScheme.onBackground),
                    shape = AppShape.input,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { viewModel.onEvent(EssayUiEvent.CorrectClicked) })
                )
            }
            InputMode.IMAGE, InputMode.PDF -> {
                if (uiState.imageUriString != null) {
                    Box(modifier = Modifier.fillMaxWidth().clip(AppShape.card).background(MaterialTheme.colorScheme.surfaceVariant).padding(14.dp)) {
                        Text(
                            if (uiState.pdfPageCount > 0) stringResource(R.string.essay_pdf_pages, uiState.pdfPageCount)
                            else stringResource(R.string.essay_image_selected),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp).clip(AppShape.card).background(MaterialTheme.colorScheme.surfaceVariant).border(0.5.dp, MaterialTheme.colorScheme.outline, AppShape.card), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.essay_no_selection), color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Action buttons
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
            if (!uiState.hasResult) {
                CorrectButton(isLoading = uiState.isLoading, onClick = { viewModel.onEvent(EssayUiEvent.CorrectClicked) }, onCancel = { viewModel.onEvent(EssayUiEvent.CancelCorrectClicked) }, modifier = Modifier.weight(1f))
            }
            PressScaleBox(
                onClick = { viewModel.onEvent(EssayUiEvent.ClearClicked) },
                modifier = Modifier
                    .size(44.dp)
                    .clip(AppShape.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AbstractIcons.Delete(modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
            }
        }

        // Error
        AnimatedVisibility(visible = uiState.error != null, enter = fadeIn(MotionTokens.tweenShortEasing()) + expandVertically(MotionTokens.tweenMedium()), exit = fadeOut(MotionTokens.tweenShort())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(uiState.error?.asString(context) ?: "", color = CorrectionDelete, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                if (!isEmpty && uiState.error != null) {
                    PressScaleBox(onClick = { viewModel.onEvent(EssayUiEvent.CorrectClicked) }, modifier = Modifier.clip(AppShape.small).padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text(stringResource(R.string.retry), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Empty state
        AnimatedVisibility(visible = isEmpty, enter = fadeIn(MotionTokens.tweenLong()) + expandVertically(MotionTokens.tweenMedium()), exit = fadeOut(MotionTokens.tweenShort()) + shrinkVertically(MotionTokens.tweenShort())) {
            EssayEmptyState(title = stringResource(R.string.essay_empty_title), desc = stringResource(R.string.essay_empty_desc))
        }

        // Results
        AnimatedVisibility(visible = uiState.hasResult, enter = fadeIn(MotionTokens.tweenMedium()) + slideInVertically(initialOffsetY = { it / 3 }, animationSpec = MotionTokens.springGentle()) + expandVertically(MotionTokens.tweenMedium()), exit = fadeOut(MotionTokens.tweenShort())) {
            Column(modifier = Modifier.fillMaxWidth().animateContentSize().clip(AppShape.card).background(MaterialTheme.colorScheme.surfaceVariant)) {
                uiState.score?.let { score ->
                    ScoreBreakdownChart(score = score)
                    
                    uiState.usage?.let { usage ->
                        usage.totalTokens?.let {
                        Spacer(modifier = Modifier.height(AppSpacing.xs))
                        Text(
                            stringResource(R.string.tokens_format, usage.totalTokens, usage.promptTokens ?: 0, usage.completionTokens ?: 0),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = AppSpacing.lg).align(Alignment.End)
                        )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm), color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
                }

                val tabTitles = listOf(
                    stringResource(R.string.essay_tab_corrections),
                    stringResource(R.string.essay_tab_corrected),
                    stringResource(R.string.essay_tab_tips)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp).semantics { selectableGroup() },
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        val isSelected = index == selectedResultTab
                        PressScaleBox(
                            onClick = { selectedResultTab = index },
                            onPressScale = 0.97f,
                            modifier = Modifier
                                .weight(1f)
                                .clip(AppShape.small)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
                                .padding(vertical = AppSpacing.sm, horizontal = AppSpacing.xs)
                                .semantics {
                                    contentDescription = title
                                    selected = isSelected
                                    role = Role.Tab
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }

                AnimatedContent(targetState = selectedResultTab, transitionSpec = {
                    if (targetState > initialState) (fadeIn(MotionTokens.tweenMedium()) + slideInVertically(MotionTokens.tweenMedium(), initialOffsetY = { it / 4 })).togetherWith(fadeOut(MotionTokens.tweenShort()))
                    else (fadeIn(MotionTokens.tweenMedium()) + slideInVertically(MotionTokens.tweenMedium(), initialOffsetY = { -it / 4 })).togetherWith(fadeOut(MotionTokens.tweenShort()))
                }, label = "tab_content") { tab ->
                    when (tab) {
                        0 -> CorrectionTab(uiState.grammarErrors, uiState.vocabulary, uiState.structure, uiState.style)
                        1 -> CorrectedEssayTab(uiState.correctedEssay)
                        2 -> WritingTipsTab(uiState.writingTips)
                    }
                }
            }
        }

        // Prevent bottom bar obstruction
        Spacer(modifier = Modifier.height(120.dp))
    }

    if (showPermissionDeniedDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showPermissionDeniedDialog = false }
        ) {
            var animateTrigger by remember { mutableStateOf(false) }
            androidx.compose.runtime.LaunchedEffect(Unit) { animateTrigger = true }
            val scale by animateFloatAsState(
                targetValue = if (animateTrigger) 1f else 0.90f,
                animationSpec = MotionTokens.springDefault(),
                label = "dialog_scale"
            )
            val alpha by animateFloatAsState(
                targetValue = if (animateTrigger) 1f else 0f,
                animationSpec = MotionTokens.tweenShort(),
                label = "dialog_alpha"
            )

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .clip(AppShape.dialog)
                    .background(MaterialTheme.colorScheme.background)
                    .border(0.5.dp, MaterialTheme.colorScheme.outline, AppShape.dialog)
                    .padding(20.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.permission_camera_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.permission_camera_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PressScaleBox(
                            onClick = { showPermissionDeniedDialog = false },
                            modifier = Modifier.clip(AppShape.small).padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(stringResource(R.string.permission_cancel), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                        }
                        Spacer(modifier = Modifier.width(AppSpacing.sm))
                        PressScaleBox(
                            onClick = {
                                showPermissionDeniedDialog = false
                                val intent = android.content.Intent(
                                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    android.net.Uri.fromParts("package", context.packageName, null)
                                )
                                context.startActivity(intent)
                            },
                            onPressScale = 0.97f,
                            modifier = Modifier
                                .clip(AppShape.button)
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 14.dp, vertical = AppSpacing.sm)
                        ) {
                            Text(
                                stringResource(R.string.permission_go_settings),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.background
                            )
                        }
                    }
                }
            }
        }
    }
}

