package luzzr.xi.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import luzzr.xi.core.ui.theme.MotionTokens

/**
 * A reusable container that applies a spring scale-down effect on press.
 * Eliminates the repeated interactionSource + animateFloatAsState + graphicsLayer pattern.
 *
 * @param onPressScale The scale factor when pressed (default 0.92).
 * @param onClick The click handler.
 * @param enabled Whether clicks are enabled.
 * @param modifier Modifier for the Box.
 * @param contentAlignment Alignment of content within the Box.
 * @param content The composable content.
 */
@Composable
fun PressScaleBox(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onPressScale: Float = 0.92f,
    enabled: Boolean = true,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) onPressScale else 1f,
        animationSpec = MotionTokens.springGentle(),
        label = "press_scale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = contentAlignment,
        content = content
    )
}
