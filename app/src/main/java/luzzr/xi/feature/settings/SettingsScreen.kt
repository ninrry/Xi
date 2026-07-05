package luzzr.xi.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import luzzr.xi.R
import luzzr.xi.data.repository.ModelDownloadState
import luzzr.xi.core.ui.theme.CorrectionAdd
import luzzr.xi.core.ui.theme.AppShape
import luzzr.xi.core.ui.theme.CorrectionDelete
import luzzr.xi.feature.settings.SettingsViewModel
import luzzr.xi.feature.settings.SettingsUiEvent
import luzzr.xi.feature.settings.TestStatus
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import luzzr.xi.core.ui.theme.AbstractIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings = uiState.settings
    var modelExpanded by remember { mutableStateOf(false) }
    var apiKeyVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ====== AI Config Section ======
        SectionCard {
            SectionTitle(stringResource(R.string.settings_section_ai))

            // API Base URL
            OutlinedTextField(
                value = settings.apiBaseUrl,
                onValueChange = { viewModel.onEvent(SettingsUiEvent.ApiBaseUrlChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.settings_api_url), fontSize = 13.sp) },
                placeholder = {
                    Text(
                        stringResource(R.string.settings_api_url_placeholder),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                ),
                shape = AppShape.input,
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // API Key with visibility toggle
            val eyeInteraction = remember { MutableInteractionSource() }
            val isEyePressed by eyeInteraction.collectIsPressedAsState()
            val eyeScale by animateFloatAsState(
                targetValue = if (isEyePressed) 0.90f else 1f,
                animationSpec = luzzr.xi.core.ui.theme.MotionTokens.springDefault(),
                label = "eye_scale"
            )

            OutlinedTextField(
                value = settings.apiKey,
                onValueChange = { viewModel.onEvent(SettingsUiEvent.ApiKeyChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.settings_api_key), fontSize = 13.sp) },
                placeholder = {
                    Text(
                        stringResource(R.string.settings_api_key_placeholder),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )
                },
                visualTransformation = if (!apiKeyVisible) {
                    PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                },
                trailingIcon = {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .graphicsLayer {
                                scaleX = eyeScale
                                scaleY = eyeScale
                            }
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(interactionSource = eyeInteraction, indication = null) {
                                apiKeyVisible = !apiKeyVisible
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AbstractIcons.Visibility(
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.secondary,
                            visible = apiKeyVisible
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                ),
                shape = AppShape.input,
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Model selector
            val modelSelectorInteraction = remember { MutableInteractionSource() }
            val isModelPressed by modelSelectorInteraction.collectIsPressedAsState()
            val modelScale by animateFloatAsState(
                targetValue = if (isModelPressed) 0.98f else 1f,
                animationSpec = luzzr.xi.core.ui.theme.MotionTokens.springDefault(),
                label = "model_scale"
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(modelScale)
                        .clip(AppShape.input)
                        .clickable(
                            interactionSource = modelSelectorInteraction,
                            indication = null
                        ) {
                            if (uiState.availableModels.isNotEmpty()) {
                                modelExpanded = !modelExpanded
                            }
                        }
                ) {
                    OutlinedTextField(
                        value = settings.model,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        readOnly = true,
                        label = { Text(stringResource(R.string.settings_model), fontSize = 13.sp) },
                        placeholder = {
                            Text(
                                stringResource(R.string.settings_model_placeholder),
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                                fontSize = 13.sp
                            )
                        },
                        trailingIcon = {
                            AbstractIcons.ArrowDropDown(
                                modifier = Modifier
                                    .size(18.dp)
                                    .graphicsLayer { rotationZ = if (modelExpanded) 180f else 0f },
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledTextColor = MaterialTheme.colorScheme.onBackground,
                            disabledLabelColor = MaterialTheme.colorScheme.primary,
                            disabledPlaceholderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                        ),
                        shape = AppShape.input,
                        singleLine = true
                    )
                }

                AnimatedVisibility(
                    visible = modelExpanded && uiState.availableModels.isNotEmpty(),
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .clip(AppShape.small)
                            .background(MaterialTheme.colorScheme.background)
                            .border(0.5.dp, MaterialTheme.colorScheme.outline, AppShape.small)
                            .padding(vertical = 4.dp)
                    ) {
                        uiState.availableModels.forEach { model ->
                            val isSelected = model == settings.model
                            val itemInteraction = remember { MutableInteractionSource() }
                            val isItemPressed by itemInteraction.collectIsPressedAsState()
                            val itemBgColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else if (isItemPressed) MaterialTheme.colorScheme.surfaceVariant else androidx.compose.ui.graphics.Color.Transparent
                            val itemScale by animateFloatAsState(
                                targetValue = if (isItemPressed) 0.95f else 1f,
                                animationSpec = luzzr.xi.core.ui.theme.MotionTokens.springDefault(),
                                label = "model_item_scale"
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        scaleX = itemScale
                                        scaleY = itemScale
                                    }
                                    .clip(AppShape.mini)
                                    .background(itemBgColor)
                                    .clickable(
                                        interactionSource = itemInteraction,
                                        indication = null
                                    ) {
                                        viewModel.onEvent(SettingsUiEvent.ModelChanged(model))
                                        modelExpanded = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = model,
                                    fontSize = 13.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Test Connection button
            val testInteraction = remember { MutableInteractionSource() }
            val isTestPressed by testInteraction.collectIsPressedAsState()
            val testScale by animateFloatAsState(
                targetValue = if (isTestPressed) 0.95f else 1f,
                animationSpec = luzzr.xi.core.ui.theme.MotionTokens.springDefault(),
                label = "test_scale"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .graphicsLayer {
                        scaleX = testScale
                        scaleY = testScale
                    }
                    .clip(AppShape.button)
                    .background(if (uiState.testStatus is TestStatus.Testing) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary)
                    .clickable(
                        enabled = uiState.testStatus !is TestStatus.Testing,
                        interactionSource = testInteraction,
                        indication = null
                    ) {
                        viewModel.onEvent(SettingsUiEvent.TestConnectionClicked)
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (uiState.testStatus is TestStatus.Testing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.background,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.settings_connection_testing),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.background,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        AbstractIcons.Refresh(modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.background)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.settings_test_connection),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.background,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Test result with smooth animation
            AnimatedVisibility(
                visible = uiState.testStatus !is TestStatus.Idle,
                enter = expandVertically(
                    animationSpec = tween(300)
                ) + fadeIn(
                    animationSpec = tween(300)
                ),
                exit = shrinkVertically(
                    animationSpec = tween(250)
                ) + fadeOut(
                    animationSpec = tween(250)
                )
            ) {
                when (val status = uiState.testStatus) {
                    is TestStatus.Success -> {
                        Row(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AbstractIcons.CheckCircle(tint = CorrectionAdd, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(
                                    R.string.settings_connection_success,
                                    status.modelCount
                                ),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    is TestStatus.Failure -> {
                        Row(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AbstractIcons.ErrorExclamation(tint = CorrectionDelete, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    stringResource(R.string.settings_connection_fail),
                                    fontSize = 13.sp,
                                    color = CorrectionDelete,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    status.message,
                                    fontSize = 12.sp,
                                    color = CorrectionDelete.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    else -> {}
                }
            }
        }

        // ====== Divider between sections ======
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 32.dp),
            color = MaterialTheme.colorScheme.outline,
            thickness = 1.dp
        )

        // ====== ML Kit Offline Translation Section ======
        SectionCard {
            SectionTitle("极速翻译")
            Text(
                "使用 Google 本地翻译引擎，模型首次使用时自动下载（约几十MB），之后离线可用。",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(12.dp))

            val downloadState = uiState.mlKitDownloadState
            when (downloadState) {
                ModelDownloadState.COMPLETED -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AbstractIcons.CheckCircle(tint = CorrectionAdd, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "离线翻译模型已就绪",
                            fontSize = 13.sp,
                            color = CorrectionAdd,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                ModelDownloadState.DOWNLOADING -> {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "下载中…",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Smooth animated progress bar
                        val animatedProgress by animateFloatAsState(
                            targetValue = uiState.mlKitDownloadProgress,
                            animationSpec = tween(durationMillis = 200),
                            label = "download_progress"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${(uiState.mlKitDownloadProgress * 100).toInt()}%",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )

                            // Cancel button
                            val cancelInteraction = remember { MutableInteractionSource() }
                            val isCancelPressed by cancelInteraction.collectIsPressedAsState()
                            val cancelScale by animateFloatAsState(
                                targetValue = if (isCancelPressed) 0.90f else 1f,
                                animationSpec = spring(),
                                label = "cancel_scale"
                            )

                            Box(
                                modifier = Modifier
                                    .graphicsLayer {
                                        scaleX = cancelScale
                                        scaleY = cancelScale
                                    }
                                    .clip(AppShape.small)
                                    .border(
                                        0.5.dp,
                                        CorrectionDelete.copy(alpha = 0.5f),
                                        AppShape.small
                                    )
                                    .clickable(
                                        interactionSource = cancelInteraction,
                                        indication = null
                                    ) {
                                        viewModel.onEvent(SettingsUiEvent.CancelMlKitDownloadClicked)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AbstractIcons.Stop(
                                        modifier = Modifier.size(12.dp),
                                        tint = CorrectionDelete
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "取消",
                                        fontSize = 11.sp,
                                        color = CorrectionDelete
                                    )
                                }
                            }
                        }
                    }
                }
                ModelDownloadState.FAILED -> {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AbstractIcons.ErrorExclamation(
                                tint = CorrectionDelete,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    "下载失败",
                                    fontSize = 13.sp,
                                    color = CorrectionDelete,
                                    fontWeight = FontWeight.Medium
                                )
                                if (uiState.mlKitDownloadMessage.isNotBlank()) {
                                    Text(
                                        uiState.mlKitDownloadMessage,
                                        fontSize = 12.sp,
                                        color = CorrectionDelete.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Retry button
                        val retryInteraction = remember { MutableInteractionSource() }
                        val isRetryPressed by retryInteraction.collectIsPressedAsState()
                        val retryScale by animateFloatAsState(
                            targetValue = if (isRetryPressed) 0.95f else 1f,
                            animationSpec = spring(),
                            label = "retry_scale"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .graphicsLayer {
                                    scaleX = retryScale
                                    scaleY = retryScale
                                }
                                .clip(AppShape.button)
                                .border(
                                    0.5.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    AppShape.button
                                )
                                .clickable(
                                    interactionSource = retryInteraction,
                                    indication = null
                                ) {
                                    viewModel.onEvent(SettingsUiEvent.RetryMlKitDownloadClicked)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AbstractIcons.Refresh(
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "重试下载",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                else -> {
                    // Idle state - show download button
                    val downloadInteraction = remember { MutableInteractionSource() }
                    val isDownloadPressed by downloadInteraction.collectIsPressedAsState()
                    val downloadScale by animateFloatAsState(
                        targetValue = if (isDownloadPressed) 0.95f else 1f,
                        animationSpec = spring(),
                        label = "download_scale"
                    )

                    Column {
                        Text(
                            "点击下方按钮下载离线翻译模型，首次下载约需几MB流量。",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .graphicsLayer {
                                    scaleX = downloadScale
                                    scaleY = downloadScale
                                }
                                .clip(AppShape.button)
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable(
                                    interactionSource = downloadInteraction,
                                    indication = null
                                ) {
                                    viewModel.onEvent(SettingsUiEvent.DownloadMlKitModelClicked)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AbstractIcons.Download(
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.background
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "下载离线模型",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.background,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // ====== Divider ======
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 32.dp),
            color = MaterialTheme.colorScheme.outline,
            thickness = 1.dp
        )

        // ====== About Section ======
        SectionCard {
            SectionTitle(stringResource(R.string.settings_section_about))
            Text(
                stringResource(R.string.settings_version),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                stringResource(R.string.settings_desc),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                stringResource(R.string.settings_default_model_info),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
            )
        }

        // Bottom spacing for scroll comfort
        Spacer(modifier = Modifier.height(120.dp))
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShape.card)
            .border(0.5.dp, MaterialTheme.colorScheme.outline, AppShape.card)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(modifier = Modifier.height(12.dp))
}
