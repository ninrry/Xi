package luzzr.xi.feature.overlay.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import luzzr.xi.core.ui.theme.OverlayBubble

/**
 * Vertical edge pill — a thin capsule (8 x 40 dp) flush against the screen edge.
 *
 * Swipe inward to open the translation panel.
 * Drag vertically along the edge to reposition.
 * Long press to stop the overlay service.
 */
@Composable
fun EdgePillTrigger() {
    val infiniteTransition = rememberInfiniteTransition(label = "pill_glow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pillGlow"
    )

    Box(
        modifier = Modifier
            .width(8.dp)
            .height(40.dp)
            .scale(glowScale)
            .clip(RoundedCornerShape(50))
            .background(OverlayBubble)
    )
}
