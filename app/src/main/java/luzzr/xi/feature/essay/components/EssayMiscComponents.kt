package luzzr.xi.feature.essay.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import luzzr.xi.R
import luzzr.xi.core.ui.theme.AbstractIcons
import luzzr.xi.core.ui.theme.AppShape
import luzzr.xi.core.ui.theme.MotionTokens
import androidx.compose.material3.MaterialTheme

@Composable
fun CorrectButton(isLoading: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "btn")
    val pulse by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 0.85f, animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "pulse")
    
    val translateInteractionSource = remember { MutableInteractionSource() }
    val isTranslatePressed by translateInteractionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isTranslatePressed) 0.95f else 1f,
        animationSpec = MotionTokens.springDefault(),
        label = "correct_press"
    )
    val scale = if (isLoading) pulse else pressScale
    Box(
        modifier = modifier
            .height(44.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(AppShape.button)
            .background(if (isLoading) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary)
            .clickable(
                enabled = !isLoading,
                interactionSource = translateInteractionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.background, strokeWidth = 2.dp)
        else Text(stringResource(R.string.essay_correct_btn), fontSize = 15.sp, color = MaterialTheme.colorScheme.background, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun EssayEmptyState(title: String, desc: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        AbstractIcons.Edit(Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        Spacer(modifier = Modifier.height(10.dp))
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(4.dp))
        Text(desc, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary, textAlign = TextAlign.Center)
    }
}
