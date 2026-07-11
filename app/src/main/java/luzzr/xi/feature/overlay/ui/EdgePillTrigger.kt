package luzzr.xi.feature.overlay.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import luzzr.xi.core.ui.theme.AbstractIcons
import luzzr.xi.core.ui.theme.AppShape
import luzzr.xi.core.ui.theme.Ivory
import luzzr.xi.core.ui.theme.LocalExtendedColors
import luzzr.xi.core.ui.theme.MotionTokens
import luzzr.xi.core.ui.theme.WarmAccent

/**
 * Mini floating bubble — 36dp when active, compact + semi-transparent when docked.
 *
 * Tap to open the translation panel.
 * Drag to reposition.
 * Long press to stop the overlay service.
 */
@Composable
fun EdgePillTrigger(docked: Boolean = true) {
    val sizeDp by animateDpAsState(
        targetValue = if (docked) 28.dp else 36.dp,
        animationSpec = MotionTokens.tweenMedium(),
        label = "bubble_size"
    )
    val iconSizeDp by animateDpAsState(
        targetValue = if (docked) 14.dp else 20.dp,
        animationSpec = MotionTokens.tweenMedium(),
        label = "bubble_icon_size"
    )
    val alpha by animateFloatAsState(
        targetValue = if (docked) 0.4f else 1f,
        animationSpec = MotionTokens.tweenMedium(),
        label = "bubble_alpha"
    )

    Box(
        modifier = Modifier
            .size(sizeDp)
            .alpha(alpha)
            .clip(AppShape.bubble)
            .background(LocalExtendedColors.current.overlayBubble)
            .border(0.5.dp, WarmAccent.copy(alpha = 0.3f), AppShape.bubble),
        contentAlignment = Alignment.Center
    ) {
        AbstractIcons.Translate(
            modifier = Modifier.size(iconSizeDp),
            tint = Ivory.copy(alpha = 0.9f)
        )
    }
}
