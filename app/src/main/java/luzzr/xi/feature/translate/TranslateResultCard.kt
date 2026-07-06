package luzzr.xi.feature.translate

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
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
import luzzr.xi.core.ui.theme.CorrectionAdd

@Composable
internal fun ResultCard(resultText: String, onCopy: () -> Unit) {
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
    LaunchedEffect(copied) {
        if (copied) {
            delay(1500)
            copied = false
        }
    }
}
