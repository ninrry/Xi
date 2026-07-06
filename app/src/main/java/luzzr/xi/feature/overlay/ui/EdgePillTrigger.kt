package luzzr.xi.feature.overlay.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.border
import luzzr.xi.core.ui.theme.WarmAccent
import androidx.compose.ui.unit.dp
import luzzr.xi.core.ui.theme.AbstractIcons
import luzzr.xi.core.ui.theme.AppShape
import luzzr.xi.core.ui.theme.Ivory
import luzzr.xi.core.ui.theme.OverlayBubble

/**
 * Mini floating bubble — a 36dp circle offset 6dp from the screen edge.
 *
 * Tap to open the translation panel.
 * Drag to reposition vertically.
 * Long press to stop the overlay service.
 *
 * By sitting 6dp inside the screen edge, it avoids Android 10+ edge-swipe-back
 * gesture conflict entirely.
 */
@Composable
fun EdgePillTrigger() {
    val infiniteTransition = rememberInfiniteTransition(label = "bubble_glow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bubbleGlow"
    )

    Box(
        modifier = Modifier
            .size(36.dp)
            .scale(glowScale)
            .clip(AppShape.bubble)
            .background(OverlayBubble)
            .border(0.5.dp, WarmAccent.copy(alpha = 0.3f), AppShape.bubble),
        contentAlignment = Alignment.Center
    ) {
        // Subtle translate icon inside bubble for affordance
        AbstractIcons.Translate(
            modifier = Modifier.size(20.dp),
            tint = Ivory.copy(alpha = 0.9f)
        )
    }
}
