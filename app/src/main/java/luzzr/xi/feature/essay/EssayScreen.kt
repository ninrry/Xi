package luzzr.xi.feature.essay

import android.net.Uri
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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

import luzzr.xi.core.ui.theme.AppShape
import luzzr.xi.core.ui.theme.CorrectionAdd
import luzzr.xi.core.ui.theme.CorrectionDelete
import luzzr.xi.core.ui.theme.CorrectionAddBg
import luzzr.xi.core.ui.theme.CorrectionNoteBg
import luzzr.xi.feature.essay.EssayViewModel
import luzzr.xi.feature.essay.EssayUiEvent
import luzzr.xi.feature.essay.InputMode
import luzzr.xi.feature.essay.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EssayScreen(
    viewModel: EssayViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedResultTab by remember { mutableIntStateOf(0) }
    val isEmpty = uiState.essayText.isEmpty() && uiState.imageUri == null && !uiState.hasResult

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let {
            val file = java.io.File(context.cacheDir, "essay_photo_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { out -> it.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out) }
            viewModel.onEvent(EssayUiEvent.ImageUriSelected(Uri.fromFile(file)))
        }
    }

    var showPermissionDeniedDialog by remember { mutableStateOf(false) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                viewModel.onEvent(EssayUiEvent.ErrorDismissed(UiText.StringResource(R.string.essay_camera_not_found)))
            }
        } else {
            showPermissionDeniedDialog = true
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val mimeType = context.contentResolver.getType(it)
            if (mimeType == "application/pdf") viewModel.onEvent(EssayUiEvent.PdfUriSelected(it)) else viewModel.onEvent(EssayUiEvent.ImageUriSelected(it))
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.onEvent(EssayUiEvent.PdfUriSelected(it)) }
    }

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
            onLevelChange = { viewModel.onEvent(EssayUiEvent.ThinkingLevelChanged(it)) },
            label = stringResource(R.string.essay_thinking_level),
            modifier = Modifier.fillMaxWidth()
        )

        // Input mode tabs (Redesigned: 4 uniform parallel buttons, no text, custom icons)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Text Button
            val isTextSelected = uiState.inputMode == InputMode.TEXT
            val textInteraction = remember { MutableInteractionSource() }
            val isTextPressed by textInteraction.collectIsPressedAsState()
            val textScale by animateFloatAsState(targetValue = if (isTextPressed) 0.92f else 1f, animationSpec = luzzr.xi.core.ui.theme.MotionTokens.springGentle(), label = "text_scale")
            Box(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer { scaleX = textScale; scaleY = textScale }
                    .clip(AppShape.small)
                    .background(if (isTextSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(interactionSource = textInteraction, indication = null) {
                        viewModel.onEvent(EssayUiEvent.InputModeChanged(InputMode.TEXT))
                    }
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                AbstractIcons.Edit(
                    modifier = Modifier.size(22.dp),
                    tint = if (isTextSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground
                )
            }

            // 2. Camera Button
            val isCameraSelected = uiState.inputMode == InputMode.IMAGE && uiState.imageUri?.toString()?.contains("essay_photo_") == true
            val cameraInteraction = remember { MutableInteractionSource() }
            val isCameraPressed by cameraInteraction.collectIsPressedAsState()
            val cameraScale by animateFloatAsState(targetValue = if (isCameraPressed) 0.92f else 1f, animationSpec = luzzr.xi.core.ui.theme.MotionTokens.springGentle(), label = "camera_scale")
            Box(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer { scaleX = cameraScale; scaleY = cameraScale }
                    .clip(AppShape.small)
                    .background(if (isCameraSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(interactionSource = cameraInteraction, indication = null) {
                        val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                        if (hasPerm) {
                            try {
                                cameraLauncher.launch(null)
                            } catch (e: Exception) {
                                viewModel.onEvent(EssayUiEvent.ErrorDismissed(UiText.StringResource(R.string.essay_camera_not_found)))
                            }
                        } else {
                            try {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            } catch (e: Exception) {
                                viewModel.onEvent(EssayUiEvent.ErrorDismissed(UiText.StringResource(R.string.essay_camera_permission_denied)))
                            }
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                AbstractIcons.Camera(
                    modifier = Modifier.size(22.dp),
                    tint = if (isCameraSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground
                )
            }

            // 3. PDF Button
            val isPdfSelected = uiState.inputMode == InputMode.PDF
            val pdfInteraction = remember { MutableInteractionSource() }
            val isPdfPressed by pdfInteraction.collectIsPressedAsState()
            val pdfScale by animateFloatAsState(targetValue = if (isPdfPressed) 0.92f else 1f, animationSpec = luzzr.xi.core.ui.theme.MotionTokens.springGentle(), label = "pdf_scale")
            Box(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer { scaleX = pdfScale; scaleY = pdfScale }
                    .clip(AppShape.small)
                    .background(if (isPdfSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(interactionSource = pdfInteraction, indication = null) {
                        pdfLauncher.launch("application/pdf")
                    }
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                AbstractIcons.Document(
                    modifier = Modifier.size(22.dp),
                    tint = if (isPdfSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground
                )
            }

            // 4. Gallery Button
            val isGallerySelected = uiState.inputMode == InputMode.IMAGE && uiState.imageUri != null && uiState.imageUri?.toString()?.contains("essay_photo_") != true
            val galleryInteraction = remember { MutableInteractionSource() }
            val isGalleryPressed by galleryInteraction.collectIsPressedAsState()
            val galleryScale by animateFloatAsState(targetValue = if (isGalleryPressed) 0.92f else 1f, animationSpec = luzzr.xi.core.ui.theme.MotionTokens.springGentle(), label = "gallery_scale")
            Box(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer { scaleX = galleryScale; scaleY = galleryScale }
                    .clip(AppShape.small)
                    .background(if (isGallerySelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(interactionSource = galleryInteraction, indication = null) {
                        galleryLauncher.launch("*/*")
                    }
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
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
                    placeholder = { Text(stringResource(R.string.essay_input_hint), color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp) },
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline, cursorColor = MaterialTheme.colorScheme.primary, focusedTextColor = MaterialTheme.colorScheme.onBackground, unfocusedTextColor = MaterialTheme.colorScheme.onBackground),
                    shape = AppShape.input,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { viewModel.onEvent(EssayUiEvent.CorrectClicked) })
                )
            }
            InputMode.IMAGE, InputMode.PDF -> {
                if (uiState.imageUri != null) {
                    Box(modifier = Modifier.fillMaxWidth().clip(AppShape.card).background(MaterialTheme.colorScheme.surfaceVariant).padding(14.dp)) {
                        Text(
                            if (uiState.pdfPageCount > 0) stringResource(R.string.essay_pdf_pages, uiState.pdfPageCount)
                            else stringResource(R.string.essay_image_selected),
                            fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp).clip(AppShape.card).background(MaterialTheme.colorScheme.surfaceVariant).border(0.5.dp, MaterialTheme.colorScheme.outline, AppShape.card), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.essay_no_selection), color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp)
                    }
                }
            }
        }

        // Action buttons
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!uiState.hasResult) {
                CorrectButton(isLoading = uiState.isLoading, onClick = { viewModel.onEvent(EssayUiEvent.CorrectClicked) }, modifier = Modifier.weight(1f))
            }
            val clearInteractionSource = remember { MutableInteractionSource() }
            val isClearPressed by clearInteractionSource.collectIsPressedAsState()
            val clearScale by animateFloatAsState(targetValue = if (isClearPressed) 0.90f else 1f, animationSpec = luzzr.xi.core.ui.theme.MotionTokens.springGentle(), label = "clear_scale")
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .graphicsLayer {
                        scaleX = clearScale
                        scaleY = clearScale
                    }
                    .clip(AppShape.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(interactionSource = clearInteractionSource, indication = null) { viewModel.onEvent(EssayUiEvent.ClearClicked) },
                contentAlignment = Alignment.Center
            ) {
                AbstractIcons.Delete(modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
            }
        }

        // Error
        AnimatedVisibility(visible = uiState.error != null, enter = fadeIn(tween(200)) + expandVertically(tween(300)), exit = fadeOut(tween(150))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(uiState.error?.asString(context) ?: "", color = CorrectionDelete, fontSize = 13.sp, modifier = Modifier.weight(1f))
                if (!isEmpty && uiState.error != null) {
                    TextButton(onClick = { viewModel.onEvent(EssayUiEvent.CorrectClicked) }) {
                        Text(stringResource(R.string.retry), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Empty state
        AnimatedVisibility(visible = isEmpty, enter = fadeIn(tween(500)) + expandVertically(tween(400)), exit = fadeOut(tween(200)) + shrinkVertically(tween(200))) {
            EssayEmptyState(title = stringResource(R.string.essay_empty_title), desc = stringResource(R.string.essay_empty_desc))
        }

        // Results
        AnimatedVisibility(visible = uiState.hasResult, enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it / 3 }, animationSpec = luzzr.xi.core.ui.theme.MotionTokens.springGentle()) + expandVertically(tween(400)), exit = fadeOut(tween(200))) {
            Column(modifier = Modifier.fillMaxWidth().animateContentSize().clip(AppShape.card).background(MaterialTheme.colorScheme.surfaceVariant)) {
                if (uiState.overallScore.isNotBlank() && uiState.overallScore != stringResource(R.string.essay_not_scored)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        AbstractIcons.Sparkle(Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.essay_score), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
                    }
                    Text(uiState.overallScore, modifier = Modifier.padding(horizontal = 14.dp).padding(bottom = 6.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary, lineHeight = 18.sp)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
                }

                val tabTitles = listOf(
                    stringResource(R.string.essay_tab_corrections),
                    stringResource(R.string.essay_tab_corrected),
                    stringResource(R.string.essay_tab_tips)
                )
                TabRow(
                    selectedTabIndex = selectedResultTab,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = index == selectedResultTab,
                            onClick = { selectedResultTab = index },
                            text = {
                                Text(
                                    title,
                                    fontSize = 12.sp,
                                    fontWeight = if (index == selectedResultTab) FontWeight.SemiBold else FontWeight.Normal,
                                    modifier = Modifier.semantics { contentDescription = title }
                                )
                            },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                AnimatedContent(targetState = selectedResultTab, transitionSpec = {
                    if (targetState > initialState) (fadeIn(tween(300)) + slideInVertically(tween(300), initialOffsetY = { it / 4 })).togetherWith(fadeOut(tween(200)))
                    else (fadeIn(tween(300)) + slideInVertically(tween(300), initialOffsetY = { -it / 4 })).togetherWith(fadeOut(tween(200)))
                }, label = "tab_content") { tab ->
                    when (tab) {
                        0 -> CorrectionTab(uiState.corrections)
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
                animationSpec = luzzr.xi.core.ui.theme.MotionTokens.springDefault(),
                label = "dialog_scale"
            )
            val alpha by animateFloatAsState(
                targetValue = if (animateTrigger) 1f else 0f,
                animationSpec = tween(150),
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
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.permission_camera_desc),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        lineHeight = 18.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showPermissionDeniedDialog = false },
                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text(stringResource(R.string.permission_cancel), fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(AppShape.button)
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    showPermissionDeniedDialog = false
                                    val intent = android.content.Intent(
                                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        android.net.Uri.fromParts("package", context.packageName, null)
                                    )
                                    context.startActivity(intent)
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                stringResource(R.string.permission_go_settings),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.background,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

