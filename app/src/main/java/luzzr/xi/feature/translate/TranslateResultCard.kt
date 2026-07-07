package luzzr.xi.feature.translate

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import luzzr.xi.R
import luzzr.xi.core.ui.theme.AbstractIcons
import luzzr.xi.core.ui.theme.AppShape
import luzzr.xi.core.ui.theme.AppSpacing
import luzzr.xi.core.ui.components.PressScaleBox
import luzzr.xi.core.ui.theme.CorrectionAdd
import luzzr.xi.core.ui.theme.MotionTokens

@Composable
internal fun ResultCard(
    resultText: String, 
    detectedLanguage: String? = null, 
    alternatives: List<String> = emptyList(), 
    usage: luzzr.xi.domain.model.Usage? = null,
    onCopy: () -> Unit
) {
    var copied by remember { mutableStateOf(false) }
    val checkScale by animateFloatAsState(
        targetValue = if (copied) 1.2f else 1f,
        animationSpec = MotionTokens.springGentle(),
        label = "check_scale"
    )
    Column(modifier = Modifier.fillMaxWidth().animateContentSize().clip(AppShape.card).background(MaterialTheme.colorScheme.surfaceVariant).border(0.5.dp, MaterialTheme.colorScheme.outline, AppShape.card).padding(AppSpacing.lg)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.translate_result_label), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
                if (!detectedLanguage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(AppSpacing.sm))
                    Box(modifier = Modifier.clip(AppShape.mini).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text(detectedLanguage, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            PressScaleBox(
                onClick = { onCopy(); copied = true },
                modifier = Modifier
                    .size(48.dp)
                    .clip(AppShape.mini)
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
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        Text(resultText, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
        
        if (alternatives.isNotEmpty()) {
            Spacer(modifier = Modifier.height(AppSpacing.md))
            Text(stringResource(R.string.translate_alternatives), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            alternatives.forEachIndexed { index, alt ->
                Text("${index + 1}. $alt", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
        if (usage != null && usage.totalTokens != null) {
            Spacer(modifier = Modifier.height(AppSpacing.md))
            Text(stringResource(R.string.token_usage_format, usage.totalTokens, usage.promptTokens ?: 0, usage.completionTokens ?: 0), 
                 style = MaterialTheme.typography.labelSmall, 
                 color = MaterialTheme.colorScheme.secondary,
                 modifier = Modifier.align(Alignment.End))
        }
    }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1500)
            copied = false
        }
    }
}
