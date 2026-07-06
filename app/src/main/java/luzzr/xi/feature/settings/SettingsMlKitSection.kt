package luzzr.xi.feature.settings

import androidx.compose.animation.core.animateFloatAsState
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
import luzzr.xi.data.repository.ModelDownloadState
import androidx.compose.ui.platform.LocalContext
import luzzr.xi.domain.model.UiText

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
                        stringResource(R.string.mlkit_model_ready),
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
                            stringResource(R.string.mlkit_downloading_label),
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
                                    onEvent(SettingsUiEvent.CancelMlKitDownloadClicked)
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
                                    stringResource(R.string.permission_cancel),
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
                                stringResource(R.string.mlkit_download_failed_label),
                                fontSize = 13.sp,
                                color = CorrectionDelete,
                                fontWeight = FontWeight.Medium
                            )
                            if (uiState.mlKitDownloadMessage != null) {
                                Text(
                                    uiState.mlKitDownloadMessage!!.asString(context),
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
                            .height(44.dp)
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
                                onEvent(SettingsUiEvent.RetryMlKitDownloadClicked)
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
                                stringResource(R.string.mlkit_retry_download),
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
                        stringResource(R.string.mlkit_download_prompt),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
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
                                onEvent(SettingsUiEvent.DownloadMlKitModelClicked)
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
                                stringResource(R.string.mlkit_download_model),
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
}
