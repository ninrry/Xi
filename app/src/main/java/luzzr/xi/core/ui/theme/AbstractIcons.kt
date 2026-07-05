package luzzr.xi.core.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Abstract stroke-based icons following the project design language:
 * minimalist line art, 0.08f stroke width, rounded caps, low-saturation accents.
 */
object AbstractIcons {

    @Composable
    fun Translate(modifier: Modifier = Modifier, tint: Color = Color.Unspecified) {
        val c = if (tint == Color.Unspecified) Color(0xFF8B7355) else tint
        Canvas(modifier = modifier) {
            val s = size.minDimension
            val sw = s * 0.08f
            val cx = center.x
            val cy = center.y
            val r = s * 0.16f
            // Left circle (abstract)
            drawCircle(c, r, Offset(cx - s * 0.10f, cy), sw)
            // Right circle (abstract)
            drawCircle(c, r, Offset(cx + s * 0.10f, cy), sw)
            // Connecting bridges (concept of conversion)
            drawLine(c, cx - s * 0.08f, cy - s * 0.08f, cx + s * 0.08f, cy - s * 0.08f, sw)
            drawLine(c, cx - s * 0.08f, cy + s * 0.08f, cx + s * 0.08f, cy + s * 0.08f, sw)
        }
    }

    @Composable
    fun Edit(modifier: Modifier = Modifier, tint: Color = Color.Unspecified) {
        val c = if (tint == Color.Unspecified) Color(0xFF8B7355) else tint
        Canvas(modifier = modifier) {
            val s = size.minDimension
            val sw = s * 0.10f
            val cx = center.x
            val cy = center.y
            // Pen body - diagonal lines
            drawLine(c, cx - s * 0.20f, cy + s * 0.16f, cx + s * 0.12f, cy - s * 0.16f, sw)
            // Pen tip - sharp point
            drawLine(c, cx - s * 0.20f, cy + s * 0.16f, cx - s * 0.24f, cy + s * 0.06f, sw)
            drawLine(c, cx - s * 0.20f, cy + s * 0.16f, cx - s * 0.10f, cy + s * 0.20f, sw)
            drawLine(c, cx - s * 0.24f, cy + s * 0.06f, cx - s * 0.10f, cy + s * 0.20f, sw)
            // Writing surface base line (extended slightly)
            drawLine(c, cx - s * 0.28f, cy + s * 0.22f, cx + s * 0.20f, cy + s * 0.22f, sw)
        }
    }

    @Composable
    fun Settings(modifier: Modifier = Modifier, tint: Color = Color.Unspecified) {
        val c = if (tint == Color.Unspecified) Color(0xFF8B7355) else tint
        Canvas(modifier = modifier) {
            val s = size.minDimension
            val sw = s * 0.08f
            val cx = center.x
            val cy = center.y
            val outerR = s * 0.28f
            val innerR = s * 0.10f
            // Outer geometric boundary
            drawCircle(c, outerR, center, sw)
            // Inner core
            drawCircle(c, innerR, center, sw)
            // Radial mechanical lines (abstract settings)
            drawLine(c, cx, cy - outerR, cx, cy + outerR, sw)
            drawLine(c, cx - outerR, cy, cx + outerR, cy, sw)
        }
    }

    @Composable
    fun Sparkle(modifier: Modifier = Modifier, tint: Color = Color.Unspecified) {
        val c = if (tint == Color.Unspecified) Color(0xFF8B7355) else tint
        Canvas(modifier = modifier) {
            val s = size.minDimension
            val sw = s * 0.08f
            val cx = center.x
            val cy = center.y
            // Horizontal ray
            drawLine(c, cx - s * 0.28f, cy, cx + s * 0.28f, cy, sw)
            // Vertical ray
            drawLine(c, cx, cy - s * 0.28f, cx, cy + s * 0.28f, sw)
            // Four accent sparkle points (abstract AI sparks)
            val offset = s * 0.12f
            drawCircle(c, sw * 0.5f, Offset(cx - offset, cy - offset), sw, Fill)
            drawCircle(c, sw * 0.5f, Offset(cx + offset, cy - offset), sw, Fill)
            drawCircle(c, sw * 0.5f, Offset(cx - offset, cy + offset), sw, Fill)
            drawCircle(c, sw * 0.5f, Offset(cx + offset, cy + offset), sw, Fill)
        }
    }

    @Composable
    fun Swap(modifier: Modifier = Modifier, tint: Color = Color.Unspecified) {
        val c = if (tint == Color.Unspecified) Color(0xFF8B7355) else tint
        Canvas(modifier = modifier) {
            val s = size.minDimension
            val sw = s * 0.08f
            val cx = center.x
            val cy = center.y
            // Top flow arrow (left to right)
            drawLine(c, cx - s * 0.22f, cy - s * 0.08f, cx + s * 0.22f, cy - s * 0.08f, sw)
            drawLine(c, cx + s * 0.12f, cy - s * 0.16f, cx + s * 0.22f, cy - s * 0.08f, sw)
            drawLine(c, cx + s * 0.12f, cy, cx + s * 0.22f, cy - s * 0.08f, sw)
            // Bottom flow arrow (right to left)
            drawLine(c, cx + s * 0.22f, cy + s * 0.08f, cx - s * 0.22f, cy + s * 0.08f, sw)
            drawLine(c, cx - s * 0.12f, cy, cx - s * 0.22f, cy + s * 0.08f, sw)
            drawLine(c, cx - s * 0.12f, cy + s * 0.16f, cx - s * 0.22f, cy + s * 0.08f, sw)
        }
    }

    @Composable
    fun Copy(modifier: Modifier = Modifier, tint: Color = Color.Unspecified) {
        val c = if (tint == Color.Unspecified) Color(0xFF8B7355) else tint
        Canvas(modifier = modifier) {
            val s = size.minDimension
            val sw = s * 0.08f
            val cx = center.x
            val cy = center.y
            val w = s * 0.36f
            // Back layer document
            drawRoundRect(c, cx - s * 0.26f, cy - s * 0.26f, w, w, CornerRadius(s * 0.08f), sw)
            // Front layer document (slightly shifted down-right)
            drawRoundRect(c, cx - s * 0.10f, cy - s * 0.10f, w, w, CornerRadius(s * 0.08f), sw)
        }
    }

    @Composable
    fun Delete(modifier: Modifier = Modifier, tint: Color = Color.Unspecified) {
        val c = if (tint == Color.Unspecified) Color(0xFF8B7355) else tint
        Canvas(modifier = modifier) {
            val s = size.minDimension
            val sw = s * 0.08f
            val cx = center.x
            val cy = center.y
            // Top lid line (extended base)
            drawLine(c, cx - s * 0.26f, cy - s * 0.18f, cx + s * 0.26f, cy - s * 0.18f, sw)
            // Rounded bottom bin container
            drawRoundRect(c, cx - s * 0.18f, cy - s * 0.18f, s * 0.36f, s * 0.44f, CornerRadius(s * 0.08f), sw)
            // Inner abstract erase-line
            drawLine(c, cx, cy - s * 0.04f, cx, cy + s * 0.16f, sw)
        }
    }

    @Composable
    fun CheckCircle(modifier: Modifier = Modifier, tint: Color = Color.Unspecified) {
        val c = if (tint == Color.Unspecified) Color(0xFF8B7355) else tint
        Canvas(modifier = modifier) {
            val s = size.minDimension
            val sw = s * 0.08f
            val cx = center.x
            val cy = center.y
            // Perfect geometric circle
            drawCircle(c, s * 0.34f, center, sw)
            // Geometric clean check mark
            drawLine(c, cx - s * 0.14f, cy, cx - s * 0.02f, cy + s * 0.12f, sw)
            drawLine(c, cx - s * 0.02f, cy + s * 0.12f, cx + s * 0.16f, cy - s * 0.10f, sw)
        }
    }

    @Composable
    fun Close(modifier: Modifier = Modifier, tint: Color = Color.Unspecified) {
        val c = if (tint == Color.Unspecified) Color(0xFF8B7355) else tint
        Canvas(modifier = modifier) {
            val s = size.minDimension
            val sw = s * 0.08f
            val cx = center.x
            val cy = center.y
            // Clean linear X-mark
            drawLine(c, cx - s * 0.22f, cy - s * 0.22f, cx + s * 0.22f, cy + s * 0.22f, sw)
            drawLine(c, cx + s * 0.22f, cy - s * 0.22f, cx - s * 0.22f, cy + s * 0.22f, sw)
        }
    }

    @Composable
    fun Camera(modifier: Modifier = Modifier, tint: Color = Color.Unspecified) {
        val c = if (tint == Color.Unspecified) Color(0xFF8B7355) else tint
        Canvas(modifier = modifier) {
            val s = size.minDimension
            val sw = s * 0.10f
            val cx = center.x
            val cy = center.y
            // Camera outer shell (highly rounded card style)
            drawRoundRect(c, cx - s * 0.32f, cy - s * 0.14f, s * 0.64f, s * 0.36f, CornerRadius(s * 0.08f), sw)
            // Lens center circular layout
            drawCircle(c, s * 0.12f, center, sw)
            // Flash on top left/center (raised abstract rectangle)
            drawRoundRect(c, cx - s * 0.12f, cy - s * 0.22f, s * 0.24f, s * 0.08f, CornerRadius(s * 0.03f), sw)
        }
    }

    @Composable
    fun Document(modifier: Modifier = Modifier, tint: Color = Color.Unspecified) {
        val c = if (tint == Color.Unspecified) Color(0xFF8B7355) else tint
        Canvas(modifier = modifier) {
            val s = size.minDimension
            val sw = s * 0.10f
            val cx = center.x
            val cy = center.y
            // Outlined vertical folder/document sheet
            drawRoundRect(c, cx - s * 0.20f, cy - s * 0.30f, s * 0.40f, s * 0.60f, CornerRadius(s * 0.08f), sw)
            // Parallel abstract content line tokens
            drawLine(c, cx - s * 0.10f, cy - s * 0.10f, cx + s * 0.10f, cy - s * 0.10f, sw)
            drawLine(c, cx - s * 0.10f, cy + s * 0.04f, cx + s * 0.10f, cy + s * 0.04f, sw)
            drawLine(c, cx - s * 0.10f, cy + s * 0.18f, cx + s * 0.02f, cy + s * 0.18f, sw)
        }
    }

    @Composable
    fun Visibility(modifier: Modifier = Modifier, tint: Color = Color.Unspecified, visible: Boolean = true) {
        val c = if (tint == Color.Unspecified) Color(0xFF8B7355) else tint
        Canvas(modifier = modifier) {
            val s = size.minDimension
            val sw = s * 0.08f
            val cx = center.x
            val cy = center.y
            // Symmetric double-arc eyelid contour
            drawArc(c, 210f, 120f, false, cx - s * 0.32f, cy - s * 0.30f, s * 0.64f, s * 0.64f, sw)
            drawArc(c, 30f, 120f, false, cx - s * 0.32f, cy - s * 0.34f, s * 0.64f, s * 0.64f, sw)
            // Pupil center
            drawCircle(c, s * 0.08f, center, sw)
            // Slash if hidden
            if (!visible) {
                drawLine(c, cx - s * 0.30f, cy - s * 0.20f, cx + s * 0.30f, cy + s * 0.20f, sw)
            }
        }
    }

    @Composable
    fun Gallery(modifier: Modifier = Modifier, tint: Color = Color.Unspecified) {
        val c = if (tint == Color.Unspecified) Color(0xFF8B7355) else tint
        Canvas(modifier = modifier) {
            val s = size.minDimension
            val sw = s * 0.10f
            val cx = center.x
            val cy = center.y
            // Frame container card
            drawRoundRect(c, cx - s * 0.26f, cy - s * 0.26f, s * 0.52f, s * 0.52f, CornerRadius(s * 0.08f), sw)
            // Single main mountain peak (very clean abstract triangle)
            drawLine(c, cx - s * 0.18f, cy + s * 0.16f, cx - s * 0.02f, cy - s * 0.04f, sw)
            drawLine(c, cx - s * 0.02f, cy - s * 0.04f, cx + s * 0.14f, cy + s * 0.16f, sw)
            // Sun
            drawCircle(c, s * 0.06f, Offset(cx + s * 0.08f, cy - s * 0.08f), sw)
        }
    }

    @Composable
    fun ArrowDropDown(modifier: Modifier = Modifier, tint: Color = Color.Unspecified) {
        val c = if (tint == Color.Unspecified) Color(0xFF8B7355) else tint
        Canvas(modifier = modifier) {
            val s = size.minDimension
            val sw = s * 0.08f
            val cx = center.x
            val cy = center.y
            // Minimalist dropdown triangle/arrowhead
            drawLine(c, cx - s * 0.16f, cy - s * 0.06f, cx, cy + s * 0.06f, sw)
            drawLine(c, cx + s * 0.16f, cy - s * 0.06f, cx, cy + s * 0.06f, sw)
        }
    }

    @Composable
    fun Refresh(modifier: Modifier = Modifier, tint: Color = Color.Unspecified) {
        val c = if (tint == Color.Unspecified) Color(0xFF8B7355) else tint
        Canvas(modifier = modifier) {
            val s = size.minDimension
            val sw = s * 0.08f
            val cx = center.x
            val cy = center.y
            val r = s * 0.25f
            // 270 degree circular arc
            drawArc(c, 45f, 270f, false, cx - r, cy - r, r * 2f, r * 2f, sw)
            // Arrow indicator at the end
            drawLine(c, cx + s * 0.20f, cy - s * 0.12f, cx + s * 0.30f, cy - s * 0.12f, sw)
            drawLine(c, cx + s * 0.20f, cy - s * 0.12f, cx + s * 0.20f, cy - s * 0.02f, sw)
        }
    }

    @Composable
    fun ErrorExclamation(modifier: Modifier = Modifier, tint: Color = Color.Unspecified) {
        val c = if (tint == Color.Unspecified) Color(0xFF8B7355) else tint
        Canvas(modifier = modifier) {
            val s = size.minDimension
            val sw = s * 0.08f
            val cx = center.x
            val cy = center.y
            // Warning circle outer line
            drawCircle(c, s * 0.34f, center, sw)
            // Exclamation sign center line
            drawLine(c, cx, cy - s * 0.16f, cx, cy + s * 0.05f, sw)
            // Base exclamation dot
            drawCircle(c, sw * 0.5f, Offset(cx, cy + s * 0.16f), sw, Fill)
        }
    }

    @Composable
    fun Download(modifier: Modifier = Modifier, tint: Color = Color.Unspecified) {
        val c = if (tint == Color.Unspecified) Color(0xFF8B7355) else tint
        Canvas(modifier = modifier) {
            val s = size.minDimension
            val sw = s * 0.08f
            val cx = center.x
            val cy = center.y
            // Down arrow
            drawLine(c, cx, cy - s * 0.26f, cx, cy + s * 0.12f, sw)
            drawLine(c, cx - s * 0.10f, cy + s * 0.02f, cx, cy + s * 0.12f, sw)
            drawLine(c, cx + s * 0.10f, cy + s * 0.02f, cx, cy + s * 0.12f, sw)
            // Base tray
            drawLine(c, cx - s * 0.22f, cy + s * 0.22f, cx + s * 0.22f, cy + s * 0.22f, sw)
        }
    }

    @Composable
    fun Stop(modifier: Modifier = Modifier, tint: Color = Color.Unspecified) {
        val c = if (tint == Color.Unspecified) Color(0xFF8B7355) else tint
        Canvas(modifier = modifier) {
            val s = size.minDimension
            val sw = s * 0.08f
            val cx = center.x
            val cy = center.y
            // Square stop button (rounded)
            drawRoundRect(c, cx - s * 0.20f, cy - s * 0.20f, s * 0.40f, s * 0.40f, CornerRadius(s * 0.06f), sw)
        }
    }

    // --- Helper functions with Canvas customization applied (cap, join, Stroke) ---

    private fun DrawScope.drawLine(c: Color, x1: Float, y1: Float, x2: Float, y2: Float, sw: Float) {
        drawLine(c, Offset(x1, y1), Offset(x2, y2), strokeWidth = sw, cap = StrokeCap.Round)
    }

    private fun DrawScope.drawCircle(c: Color, rad: Float, center: Offset, sw: Float, style: Int = Stroke) {
        if (style == Fill) {
            drawCircle(c, radius = rad, center = center)
        } else {
            drawCircle(c, radius = rad, center = center, style = Stroke(width = sw))
        }
    }

    private fun DrawScope.drawRoundRect(c: Color, left: Float, top: Float, w: Float, h: Float, cr: CornerRadius, sw: Float) {
        drawRoundRect(c, topLeft = Offset(left, top), size = Size(w, h), cornerRadius = cr,
            style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }

    private fun DrawScope.drawArc(c: Color, start: Float, sweep: Float, useCenter: Boolean,
                                   left: Float, top: Float, w: Float, h: Float, sw: Float) {
        drawArc(c, startAngle = start, sweepAngle = sweep, useCenter = useCenter,
            topLeft = Offset(left, top), size = Size(w, h),
            style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }

    private val Fill = 0
    private val Stroke = 1
}
