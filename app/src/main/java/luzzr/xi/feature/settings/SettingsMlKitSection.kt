package luzzr.xi.feature.settings

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import luzzr.xi.R
import luzzr.xi.core.ui.theme.AbstractIcons
import luzzr.xi.core.ui.theme.AppShape
import luzzr.xi.core.ui.theme.CorrectionAdd
import luzzr.xi.core.ui.theme.CorrectionDelete
import luzzr.xi.domain.model.ModelDownloadState
import luzzr.xi.core.ui.theme.AppSpacing
import luzzr.xi.core.ui.components.PressScaleBox
import luzzr.xi.core.ui.theme.MotionTokens
import luzzr.xi.domain.model.UiText
import luzzr.xi.domain.model.SupportedLanguage
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.platform.LocalContext

@Composable
fun SettingsMlKitSection(
    uiState: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit
) {
    val context = LocalContext.current
    SectionCard {
        SectionTitle(stringResource(R.string.mlkit_section_title))
        Text(
            stringResource(R.string.mlkit_section_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(AppSpacing.md))

        val downloadState = uiState.mlKitDownloadState
        when (downloadState) {
            ModelDownloadState.COMPLETED -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AbstractIcons.CheckCircle(tint = CorrectionAdd, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(AppSpacing.sm))
                    Text(
                        stringResource(R.string.mlkit_model_ready),
                        style = MaterialTheme.typography.labelLarge,
                        color = CorrectionAdd
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
                        Spacer(modifier = Modifier.width(AppSpacing.sm))
                        Text(
                            stringResource(R.string.mlkit_downloading),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(AppSpacing.sm))

                    // Smooth animated progress bar
                    val animatedProgress by animateFloatAsState(
                        targetValue = uiState.mlKitDownloadProgress,
                        animationSpec = MotionTokens.tweenShort(),
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

                    Spacer(modifier = Modifier.height(AppSpacing.xs))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${(uiState.mlKitDownloadProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        // Cancel button
                        PressScaleBox(
                            onClick = { onEvent(SettingsUiEvent.CancelMlKitDownloadClicked) },
                            modifier = Modifier
                                .clip(AppShape.small)
                                .border(
                                    0.5.dp,
                                    CorrectionDelete.copy(alpha = 0.5f),
                                    AppShape.small
                                )
                                .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AbstractIcons.Stop(
                                    modifier = Modifier.size(12.dp),
                                    tint = CorrectionDelete
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    stringResource(R.string.permission_cancel),
                                    style = MaterialTheme.typography.labelSmall,
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
                        Spacer(modifier = Modifier.width(AppSpacing.sm))
                        Column {
                            Text(
                                stringResource(R.string.mlkit_download_failed),
                                style = MaterialTheme.typography.labelLarge,
                                color = CorrectionDelete
                            )
                            if (uiState.mlKitDownloadMessage != null) {
                                Text(
                                    uiState.mlKitDownloadMessage!!.asString(context),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CorrectionDelete.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(AppSpacing.sm))

                    // Retry button
                    PressScaleBox(
                        onClick = { onEvent(SettingsUiEvent.RetryMlKitDownloadClicked) },
                        onPressScale = 0.97f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(AppShape.button)
                            .border(
                                0.5.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                AppShape.button
                            )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AbstractIcons.Refresh(
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(AppSpacing.xs))
                            Text(
                                stringResource(R.string.mlkit_download_retry),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            else -> {
                // Idle state - show language selectors and download button
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.translate_select_source),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(AppSpacing.xs))
                            var sourceExpanded by remember { mutableStateOf(false) }
                            Box {
                                PressScaleBox(
                                    onClick = { sourceExpanded = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(AppShape.small)
                                        .border(0.5.dp, MaterialTheme.colorScheme.outline, AppShape.small)
                                        .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.sm)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            uiState.mlKitSourceLang.nativeName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        AbstractIcons.ArrowDropDown(
                                            modifier = Modifier.size(12.dp),
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                                DropdownMenu(
                                    expanded = sourceExpanded,
                                    onDismissRequest = { sourceExpanded = false }
                                ) {
                                    SupportedLanguage.entries.filter { it != SupportedLanguage.AUTO }.forEach { lang ->
                                        DropdownMenuItem(
                                            text = { Text("${lang.nativeName} (${lang.displayName})") },
                                            onClick = {
                                                onEvent(SettingsUiEvent.MlKitSourceLangChanged(lang))
                                                sourceExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.translate_select_target),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(AppSpacing.xs))
                            var targetExpanded by remember { mutableStateOf(false) }
                            Box {
                                PressScaleBox(
                                    onClick = { targetExpanded = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(AppShape.small)
                                        .border(0.5.dp, MaterialTheme.colorScheme.outline, AppShape.small)
                                        .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.sm)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            uiState.mlKitTargetLang.nativeName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        AbstractIcons.ArrowDropDown(
                                            modifier = Modifier.size(12.dp),
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                                DropdownMenu(
                                    expanded = targetExpanded,
                                    onDismissRequest = { targetExpanded = false }
                                ) {
                                    SupportedLanguage.entries.filter { it != SupportedLanguage.AUTO }.forEach { lang ->
                                        DropdownMenuItem(
                                            text = { Text("${lang.nativeName} (${lang.displayName})") },
                                            onClick = {
                                                onEvent(SettingsUiEvent.MlKitTargetLangChanged(lang))
                                                targetExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(AppSpacing.sm))

                    Text(
                        stringResource(R.string.mlkit_download_prompt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    Spacer(modifier = Modifier.height(AppSpacing.sm))

                    PressScaleBox(
                        onClick = { onEvent(SettingsUiEvent.DownloadMlKitModelClicked) },
                        onPressScale = 0.97f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(AppShape.button)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AbstractIcons.Download(
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.background
                            )
                            Spacer(modifier = Modifier.width(AppSpacing.sm))
                            Text(
                                stringResource(R.string.mlkit_download_model),
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
