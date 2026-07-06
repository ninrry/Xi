package luzzr.xi.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import luzzr.xi.R
import luzzr.xi.core.ui.theme.AbstractIcons
import luzzr.xi.core.ui.theme.AppShape
import luzzr.xi.core.ui.theme.CorrectionAdd
import luzzr.xi.core.ui.theme.CorrectionDelete
import luzzr.xi.feature.settings.SettingsUiEvent
import luzzr.xi.feature.settings.SettingsViewModel
import luzzr.xi.feature.settings.TestStatus

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
                            .clip(AppShape.mini)
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
                targetValue = if (isModelPressed) 0.92f else 1f,
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
            modifier = Modifier.padding(horizontal = 48.dp),
            color = MaterialTheme.colorScheme.outline,
            thickness = 0.5.dp
        )

        // ====== ML Kit Offline Translation Section ======
        SettingsMlKitSection(
            uiState = uiState,
            onEvent = { viewModel.onEvent(it) }
        )

        // ====== Divider ======
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 48.dp),
            color = MaterialTheme.colorScheme.outline,
            thickness = 0.5.dp
        )

        // ====== About Section ======
        SettingsAboutSection()

        // Bottom spacing for scroll comfort
        Spacer(modifier = Modifier.height(120.dp))
    }
}


