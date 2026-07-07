package luzzr.xi.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
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
import luzzr.xi.core.ui.theme.MotionTokens
import luzzr.xi.core.ui.theme.AppSpacing
import luzzr.xi.core.ui.components.PressScaleBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.settings
    val context = LocalContext.current
    var modelExpanded by remember { mutableStateOf(false) }
    var apiKeyVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .widthIn(max = 600.dp)
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
    ) {
        // ====== AI Config Section ======
        SectionCard {
            SectionTitle(stringResource(R.string.settings_section_ai))

            // Provider Selector
            Column(modifier = Modifier.fillMaxWidth()) {
                luzzr.xi.core.provider.ProviderRegistry.getAllProviders().forEach { provider ->
                    val isSelected = provider.id == settings.providerId
                    PressScaleBox(
                        onClick = { viewModel.onEvent(SettingsUiEvent.ProviderChanged(provider.id)) },
                        onPressScale = 0.98f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(AppShape.small)
                            .border(
                                if (isSelected) 1.5.dp else 0.5.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                AppShape.small
                            )
                            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(provider.displayNameRes),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                                Text(
                                    stringResource(provider.descriptionRes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            if (isSelected) {
                                AbstractIcons.CheckCircle(
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    if (provider != luzzr.xi.core.provider.ProviderRegistry.getAllProviders().last()) {
                        Spacer(modifier = Modifier.height(AppSpacing.xs))
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // API Base URL
            OutlinedTextField(
                value = settings.apiBaseUrl,
                onValueChange = { viewModel.onEvent(SettingsUiEvent.ApiBaseUrlChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.settings_api_url), style = MaterialTheme.typography.bodySmall) },
                placeholder = {
                    Text(
                        stringResource(R.string.settings_api_url_placeholder),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodySmall
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

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            OutlinedTextField(
                value = settings.apiKey,
                onValueChange = { viewModel.onEvent(SettingsUiEvent.ApiKeyChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.settings_api_key), style = MaterialTheme.typography.bodySmall) },
                placeholder = {
                    Text(
                        stringResource(R.string.settings_api_key_placeholder),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                visualTransformation = if (!apiKeyVisible) {
                    PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                },
                trailingIcon = {
                    PressScaleBox(
                        onClick = { apiKeyVisible = !apiKeyVisible },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(AppShape.mini)
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

            // Provider-specific helper link
            val currentProvider = luzzr.xi.core.provider.ProviderRegistry.getProvider(settings.providerId)
            if (currentProvider.websiteUrl.isNotBlank() && currentProvider.helpLinkTextRes != 0) {
                PressScaleBox(
                    onClick = {
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(currentProvider.websiteUrl))
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier.padding(top = AppSpacing.xs)
                ) {
                    Text(
                        stringResource(currentProvider.helpLinkTextRes),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            Column(modifier = Modifier.fillMaxWidth()) {
                PressScaleBox(
                    onClick = {
                        if (uiState.availableModels.isNotEmpty()) {
                            modelExpanded = !modelExpanded
                        }
                    },
                    onPressScale = 0.98f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppShape.input)
                ) {
                    OutlinedTextField(
                        value = settings.model,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        readOnly = true,
                        label = { Text(stringResource(R.string.settings_model), style = MaterialTheme.typography.bodySmall) },
                        placeholder = {
                            Text(
                                stringResource(R.string.settings_model_placeholder),
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.bodySmall
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

                if (uiState.availableModels.isEmpty()) {
                    Text(
                        text = stringResource(R.string.settings_connect_first),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = AppSpacing.lg, top = AppSpacing.xs)
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
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(top = 6.dp)
                            .clip(AppShape.small)
                            .background(MaterialTheme.colorScheme.background)
                            .border(0.5.dp, MaterialTheme.colorScheme.outline, AppShape.small)
                            .padding(vertical = 4.dp)
                    ) {
                        uiState.availableModels.forEach { model ->
                            val isSelected = model == settings.model
                            PressScaleBox(
                                onClick = {
                                    viewModel.onEvent(SettingsUiEvent.ModelChanged(model))
                                    modelExpanded = false
                                },
                                onPressScale = 0.97f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(AppShape.mini)
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else androidx.compose.ui.graphics.Color.Transparent)
                                    .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md)
                            ) {
                                Text(
                                    text = model,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // Test Connection button
            PressScaleBox(
                enabled = uiState.testStatus !is TestStatus.Testing && settings.apiKey.isNotBlank() && settings.apiBaseUrl.isNotBlank(),
                onClick = { viewModel.onEvent(SettingsUiEvent.TestConnectionClicked) },
                onPressScale = 0.97f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(AppShape.button)
                    .background(if (uiState.testStatus is TestStatus.Testing) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (uiState.testStatus is TestStatus.Testing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.background,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(AppSpacing.sm))
                        Text(
                            stringResource(R.string.settings_connection_testing),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.background
                        )
                    } else {
                        AbstractIcons.Refresh(modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.background)
                        Spacer(modifier = Modifier.width(AppSpacing.sm))
                        Text(
                            stringResource(R.string.settings_test_connection),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.background
                        )
                    }
                }
            }

            // Test result with smooth animation
            AnimatedVisibility(
                visible = uiState.testStatus !is TestStatus.Idle,
                enter = expandVertically(
                    animationSpec = MotionTokens.tweenMedium()
                ) + fadeIn(
                    animationSpec = MotionTokens.tweenMedium()
                ),
                exit = shrinkVertically(
                    animationSpec = MotionTokens.tweenMedium()
                ) + fadeOut(
                    animationSpec = MotionTokens.tweenMedium()
                )
            ) {
                when (val status = uiState.testStatus) {
                    is TestStatus.Success -> {
                        Row(
                            modifier = Modifier.padding(top = AppSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AbstractIcons.CheckCircle(tint = CorrectionAdd, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(AppSpacing.sm))
                            Text(
                                stringResource(
                                    R.string.settings_connection_success,
                                    status.modelCount
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    is TestStatus.Failure -> {
                        Row(
                            modifier = Modifier.padding(top = AppSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AbstractIcons.ErrorExclamation(tint = CorrectionDelete, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(AppSpacing.sm))
                            Column {
                                Text(
                                    stringResource(R.string.settings_connection_fail),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = CorrectionDelete
                                )
                                Text(
                                    status.message,
                                    style = MaterialTheme.typography.labelMedium,
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

        // Provider switch confirmation dialog
        uiState.showProviderSwitchDialog?.let { pendingProviderId ->
            androidx.compose.ui.window.Dialog(onDismissRequest = { viewModel.onEvent(SettingsUiEvent.ProviderSwitchCancelled) }) {
                Box(
                    modifier = Modifier
                        .clip(AppShape.dialog)
                        .background(MaterialTheme.colorScheme.background)
                        .border(0.5.dp, MaterialTheme.colorScheme.outline, AppShape.dialog)
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            stringResource(R.string.provider_switch_confirm),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PressScaleBox(
                                onClick = { viewModel.onEvent(SettingsUiEvent.ProviderSwitchCancelled) },
                                modifier = Modifier.clip(AppShape.small).padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(stringResource(R.string.permission_cancel), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                            }
                            Spacer(modifier = Modifier.width(AppSpacing.sm))
                            PressScaleBox(
                                onClick = { viewModel.onEvent(SettingsUiEvent.ProviderSwitchConfirmed(pendingProviderId)) },
                                onPressScale = 0.97f,
                                modifier = Modifier
                                    .clip(AppShape.button)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .padding(horizontal = 14.dp, vertical = AppSpacing.sm)
                            ) {
                                Text(
                                    stringResource(R.string.provider_switch_confirm_yes),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.background
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom spacing for scroll comfort
        Spacer(modifier = Modifier.height(120.dp))
    }
}


