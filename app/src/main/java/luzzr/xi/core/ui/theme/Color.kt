package luzzr.xi.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Warm ivory palette
val Ivory = Color(0xFFF4F1EA)
val IvoryDark = Color(0xFFEDE9E0)
val BrownGray = Color(0xFF3D3530)
val BrownGrayLight = Color(0xFF6B5E54)
val WarmAccent = Color(0xFF8B7355)
val WarmAccentLight = Color(0xFFB8A68E)
val DividerColor = Color(0xFFE8E2DA)

// Correction colors
val CorrectionDelete = Color(0xFFD4574A)
val CorrectionAdd = Color(0xFF4A8B5C)
val CorrectionAddBg = Color(0xFFEAF5EC)
val CorrectionNote = Color(0xFFB8860B)
val CorrectionNoteBg = Color(0xFFFFF8E1)

val XiShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(16.dp),
)

// Overlay
val OverlayBg = Color(0xF5F4F1EA)
val OverlayBubble = Color(0xCC8B7355)

// Shape tokens — consistent radii scale
object AppShape {
    /** Large cards, panels */
    val card = RoundedCornerShape(28.dp)
    /** Dialogs */
    val dialog = RoundedCornerShape(32.dp)
    /** Buttons (primary, secondary) */
    val button = RoundedCornerShape(28.dp)
    /** Input fields (OutlinedTextField) */
    val input = RoundedCornerShape(24.dp)
    /** Small cards, tabs, chips */
    val small = RoundedCornerShape(16.dp)
    /** Inline items, tips, corrections */
    val mini = RoundedCornerShape(12.dp)
}
