package luzzr.xi.core.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class ExtendedColors(
    val correctionDelete: Color = CorrectionDelete,
    val correctionAdd: Color = CorrectionAdd,
    val correctionAddBg: Color = CorrectionAddBg,
    val correctionNoteBg: Color = CorrectionNoteBg,
    val overlayBg: Color = OverlayBg,
    val overlayBubble: Color = OverlayBubble,
)

val LightExtendedColors = ExtendedColors(
    correctionDelete = CorrectionDelete,
    correctionAdd = CorrectionAdd,
    correctionAddBg = CorrectionAddBg,
    correctionNoteBg = CorrectionNoteBg,
    overlayBg = OverlayBg,
    overlayBubble = OverlayBubble,
)

val DarkExtendedColors = ExtendedColors(
    correctionDelete = Color(0xFFE8A9A0),
    correctionAdd = Color(0xFFA5C4A1),
    correctionAddBg = Color(0xFF2D3B2E),
    correctionNoteBg = Color(0xFF3D3424),
    overlayBg = Color(0xF5282622),
    overlayBubble = Color(0xFF7A6345),
)

val LocalExtendedColors = staticCompositionLocalOf { ExtendedColors() }
