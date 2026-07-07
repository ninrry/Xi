package luzzr.xi.core.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * Abstract stroke-based icons — hand-drawn line art.
 *
 * Design language:
 * - Uniform 0.07f stroke width ratio
 * - StrokeCap.Round + StrokeJoin.Round everywhere (hand-drawn feel)
 * - Generous proportions, icons fill the canvas with breathing room
 * - Uses MaterialTheme.colorScheme.primary as default tint
 * - No filled shapes unless semantically required (pupil dot, etc.)
 */
object AbstractIcons {

    @Composable
    fun Translate(
        modifier: Modifier = Modifier,
        tint: Color = Color.Unspecified,
        contentDescription: String? = null
    ) {
        val c = if (tint == Color.Unspecified) MaterialTheme.colorScheme.primary else tint
        val finalModifier = if (contentDescription != null) modifier.semantics { this.contentDescription = contentDescription } else modifier
        Canvas(modifier = finalModifier) {
            val s = size.minDimension
            val sw = s * 0.07f
            val cx = center.x
            val cy = center.y

            // Left: speech bubble with "A"
            val bubbleW = s * 0.28f
            val bubbleH = s * 0.34f
            val blx = cx - s * 0.24f
            val bly = cy - s * 0.10f
            // Bubble outline
            drawRoundRect(c, blx, bly, bubbleW, bubbleH, CornerRadius(s * 0.06f), sw)
            // Bubble tail
            drawLine(c, blx + s * 0.06f, bly + bubbleH - s * 0.02f,
                blx + s * 0.12f, bly + bubbleH + s * 0.08f, sw)
            drawLine(c, blx + s * 0.12f, bly + bubbleH + s * 0.08f,
                blx + s * 0.18f, bly + bubbleH - s * 0.02f, sw)
            // "A" inside bubble
            val alx = blx + bubbleW * 0.5f
            val aly = bly + bubbleH * 0.5f
            drawLine(c, alx - s * 0.06f, aly + s * 0.08f, alx, aly - s * 0.10f, sw)
            drawLine(c, alx + s * 0.06f, aly + s * 0.08f, alx, aly - s * 0.10f, sw)
            drawLine(c, alx - s * 0.04f, aly + s * 0.02f, alx + s * 0.04f, aly + s * 0.02f, sw)

            // Right: speech bubble with "文"
            val brx = cx + s * 0.04f
            val bry = cy - s * 0.22f
            drawRoundRect(c, brx, bry, bubbleW, bubbleH, CornerRadius(s * 0.06f), sw)
            // Bubble tail
            drawLine(c, brx + bubbleW - s * 0.18f, bry + bubbleH - s * 0.02f,
                brx + bubbleW - s * 0.12f, bry + bubbleH + s * 0.08f, sw)
            drawLine(c, brx + bubbleW - s * 0.12f, bry + bubbleH + s * 0.08f,
                brx + bubbleW - s * 0.06f, bry + bubbleH - s * 0.02f, sw)
            // "文" inside bubble — top horizontal
            val wrx = brx + bubbleW * 0.5f
            val wry = bry + bubbleH * 0.5f
            drawLine(c, wrx - s * 0.08f, wry - s * 0.08f, wrx + s * 0.08f, wry - s * 0.08f, sw)
            // Center vertical
            drawLine(c, wrx, wry - s * 0.08f, wrx, wry + s * 0.08f, sw)
            // Left diagonal
            drawLine(c, wrx - s * 0.08f, wry + s * 0.02f, wrx, wry + s * 0.08f, sw)
            // Right diagonal
            drawLine(c, wrx, wry + s * 0.08f, wrx + s * 0.08f, wry + s * 0.02f, sw)

            // Connection arc between bubbles
            drawArc(c, 340f, 40f, false,
                cx - s * 0.08f, cy - s * 0.06f, s * 0.16f, s * 0.16f, sw)
        }
    }

    @Composable
    fun Edit(
        modifier: Modifier = Modifier,
        tint: Color = Color.Unspecified,
        contentDescription: String? = null
    ) {
        val c = if (tint == Color.Unspecified) MaterialTheme.colorScheme.primary else tint
        val finalModifier = if (contentDescription != null) modifier.semantics { this.contentDescription = contentDescription } else modifier
        Canvas(modifier = finalModifier) {
            val s = size.minDimension
            val sw = s * 0.07f
            val cx = center.x
            val cy = center.y

            // Pen body — diagonal stroke
            drawLine(c, cx - s * 0.22f, cy + s * 0.22f, cx + s * 0.18f, cy - s * 0.18f, sw)
            // Pen nib — angular tip
            drawLine(c, cx - s * 0.22f, cy + s * 0.22f, cx - s * 0.28f, cy + s * 0.14f, sw)
            drawLine(c, cx - s * 0.22f, cy + s * 0.22f, cx - s * 0.14f, cy + s * 0.28f, sw)
            drawLine(c, cx - s * 0.28f, cy + s * 0.14f, cx - s * 0.14f, cy + s * 0.28f, sw)
            // Grip lines on pen
            drawLine(c, cx - s * 0.06f, cy + s * 0.06f, cx + s * 0.00f, cy + s * 0.12f, sw)
            drawLine(c, cx - s * 0.02f, cy + s * 0.02f, cx + s * 0.04f, cy + s * 0.08f, sw)
            // Writing baseline
            drawLine(c, cx - s * 0.32f, cy + s * 0.30f, cx + s * 0.26f, cy + s * 0.30f, sw)
        }
    }

    @Composable
    fun Settings(
        modifier: Modifier = Modifier,
        tint: Color = Color.Unspecified,
        contentDescription: String? = null
    ) {
        val c = if (tint == Color.Unspecified) MaterialTheme.colorScheme.primary else tint
        val finalModifier = if (contentDescription != null) modifier.semantics { this.contentDescription = contentDescription } else modifier
        Canvas(modifier = finalModifier) {
            val s = size.minDimension
            val sw = s * 0.07f
            val cx = center.x
            val cy = center.y

            // Sliders: three horizontal bars with adjustable knobs
            val barW = s * 0.52f
            val barH = s * 0.06f
            val y1 = cy - s * 0.16f
            val y2 = cy
            val y3 = cy + s * 0.16f
            val barLeft = cx - barW * 0.5f

            // Bar 1 — knob at 30%
            drawRoundRect(c, barLeft, y1 - barH * 0.5f, barW, barH, CornerRadius(barH * 0.5f), sw)
            val knob1X = barLeft + barW * 0.30f
            drawCircle(c, barH * 0.9f, Offset(knob1X, y1), sw)

            // Bar 2 — knob at 60%
            drawRoundRect(c, barLeft, y2 - barH * 0.5f, barW, barH, CornerRadius(barH * 0.5f), sw)
            val knob2X = barLeft + barW * 0.60f
            drawCircle(c, barH * 0.9f, Offset(knob2X, y2), sw)

            // Bar 3 — knob at 45%
            drawRoundRect(c, barLeft, y3 - barH * 0.5f, barW, barH, CornerRadius(barH * 0.5f), sw)
            val knob3X = barLeft + barW * 0.45f
            drawCircle(c, barH * 0.9f, Offset(knob3X, y3), sw)
        }
    }

    @Composable
    fun Sparkle(
        modifier: Modifier = Modifier,
        tint: Color = Color.Unspecified,
        contentDescription: String? = null
    ) {
        val c = if (tint == Color.Unspecified) MaterialTheme.colorScheme.primary else tint
        val finalModifier = if (contentDescription != null) modifier.semantics { this.contentDescription = contentDescription } else modifier
        Canvas(modifier = finalModifier) {
            val s = size.minDimension
            val sw = s * 0.07f
            val cx = center.x
            val cy = center.y

            // 4 main rays
            drawLine(c, cx, cy - s * 0.34f, cx, cy + s * 0.34f, sw)
            drawLine(c, cx - s * 0.34f, cy, cx + s * 0.34f, cy, sw)
            // 4 diagonal rays — shorter
            val d = s * 0.20f
            drawLine(c, cx - d, cy - d, cx + d, cy + d, sw)
            drawLine(c, cx - d, cy + d, cx + d, cy - d, sw)
            // Center dot
            drawCircle(c, sw * 0.8f, center, sw, Fill)
        }
    }

    @Composable
    fun Swap(
        modifier: Modifier = Modifier,
        tint: Color = Color.Unspecified,
        contentDescription: String? = null
    ) {
        val c = if (tint == Color.Unspecified) MaterialTheme.colorScheme.primary else tint
        val finalModifier = if (contentDescription != null) modifier.semantics { this.contentDescription = contentDescription } else modifier
        Canvas(modifier = finalModifier) {
            val s = size.minDimension
            val sw = s * 0.07f
            val cx = center.x
            val cy = center.y

            // Top arrow: left → right
            drawLine(c, cx - s * 0.26f, cy - s * 0.12f, cx + s * 0.26f, cy - s * 0.12f, sw)
            drawLine(c, cx + s * 0.14f, cy - s * 0.22f, cx + s * 0.26f, cy - s * 0.12f, sw)
            drawLine(c, cx + s * 0.14f, cy - s * 0.02f, cx + s * 0.26f, cy - s * 0.12f, sw)
            // Bottom arrow: right → left
            drawLine(c, cx + s * 0.26f, cy + s * 0.12f, cx - s * 0.26f, cy + s * 0.12f, sw)
            drawLine(c, cx - s * 0.14f, cy + s * 0.02f, cx - s * 0.26f, cy + s * 0.12f, sw)
            drawLine(c, cx - s * 0.14f, cy + s * 0.22f, cx - s * 0.26f, cy + s * 0.12f, sw)
        }
    }

    @Composable
    fun Copy(
        modifier: Modifier = Modifier,
        tint: Color = Color.Unspecified,
        contentDescription: String? = null
    ) {
        val c = if (tint == Color.Unspecified) MaterialTheme.colorScheme.primary else tint
        val finalModifier = if (contentDescription != null) modifier.semantics { this.contentDescription = contentDescription } else modifier
        Canvas(modifier = finalModifier) {
            val s = size.minDimension
            val sw = s * 0.07f
            val cx = center.x
            val cy = center.y

            // Back document
            drawRoundRect(c, cx - s * 0.24f, cy - s * 0.26f, s * 0.40f, s * 0.48f, CornerRadius(s * 0.06f), sw)
            // Front document — overlapping
            drawRoundRect(c, cx - s * 0.06f, cy - s * 0.10f, s * 0.40f, s * 0.48f, CornerRadius(s * 0.06f), sw)
            // Content lines on front
            drawLine(c, cx + s * 0.04f, cy + s * 0.04f, cx + s * 0.26f, cy + s * 0.04f, sw)
            drawLine(c, cx + s * 0.04f, cy + s * 0.14f, cx + s * 0.20f, cy + s * 0.14f, sw)
            drawLine(c, cx + s * 0.04f, cy + s * 0.24f, cx + s * 0.16f, cy + s * 0.24f, sw)
        }
    }

    @Composable
    fun Delete(
        modifier: Modifier = Modifier,
        tint: Color = Color.Unspecified,
        contentDescription: String? = null
    ) {
        val c = if (tint == Color.Unspecified) MaterialTheme.colorScheme.primary else tint
        val finalModifier = if (contentDescription != null) modifier.semantics { this.contentDescription = contentDescription } else modifier
        Canvas(modifier = finalModifier) {
            val s = size.minDimension
            val sw = s * 0.07f
            val cx = center.x
            val cy = center.y

            // Handle
            drawLine(c, cx - s * 0.10f, cy - s * 0.26f, cx + s * 0.10f, cy - s * 0.26f, sw)
            // Lid
            drawLine(c, cx - s * 0.28f, cy - s * 0.18f, cx + s * 0.28f, cy - s * 0.18f, sw)
            // Bin body
            drawRoundRect(c, cx - s * 0.22f, cy - s * 0.18f, s * 0.44f, s * 0.46f, CornerRadius(s * 0.06f), sw)
            // Inner vertical lines
            drawLine(c, cx - s * 0.08f, cy - s * 0.06f, cx - s * 0.08f, cy + s * 0.16f, sw)
            drawLine(c, cx + s * 0.08f, cy - s * 0.06f, cx + s * 0.08f, cy + s * 0.16f, sw)
        }
    }

    @Composable
    fun CheckCircle(
        modifier: Modifier = Modifier,
        tint: Color = Color.Unspecified,
        contentDescription: String? = null
    ) {
        val c = if (tint == Color.Unspecified) MaterialTheme.colorScheme.primary else tint
        val finalModifier = if (contentDescription != null) modifier.semantics { this.contentDescription = contentDescription } else modifier
        Canvas(modifier = finalModifier) {
            val s = size.minDimension
            val sw = s * 0.07f
            val cx = center.x
            val cy = center.y

            // Circle
            drawCircle(c, s * 0.34f, center, sw)
            // Check mark
            drawLine(c, cx - s * 0.16f, cy + s * 0.02f, cx - s * 0.04f, cy + s * 0.16f, sw)
            drawLine(c, cx - s * 0.04f, cy + s * 0.16f, cx + s * 0.18f, cy - s * 0.12f, sw)
        }
    }

    @Composable
    fun Close(
        modifier: Modifier = Modifier,
        tint: Color = Color.Unspecified,
        contentDescription: String? = null
    ) {
        val c = if (tint == Color.Unspecified) MaterialTheme.colorScheme.primary else tint
        val finalModifier = if (contentDescription != null) modifier.semantics { this.contentDescription = contentDescription } else modifier
        Canvas(modifier = finalModifier) {
            val s = size.minDimension
            val sw = s * 0.07f
            val cx = center.x
            val cy = center.y

            // Clean X-mark — no circle, just the cross
            drawLine(c, cx - s * 0.28f, cy - s * 0.28f, cx + s * 0.28f, cy + s * 0.28f, sw)
            drawLine(c, cx + s * 0.28f, cy - s * 0.28f, cx - s * 0.28f, cy + s * 0.28f, sw)
        }
    }

    @Composable
    fun Camera(
        modifier: Modifier = Modifier,
        tint: Color = Color.Unspecified,
        contentDescription: String? = null
    ) {
        val c = if (tint == Color.Unspecified) MaterialTheme.colorScheme.primary else tint
        val finalModifier = if (contentDescription != null) modifier.semantics { this.contentDescription = contentDescription } else modifier
        Canvas(modifier = finalModifier) {
            val s = size.minDimension
            val sw = s * 0.07f
            val cx = center.x
            val cy = center.y

            // Camera body
            drawRoundRect(c, cx - s * 0.32f, cy - s * 0.14f, s * 0.64f, s * 0.36f, CornerRadius(s * 0.08f), sw)
            // Lens outer ring
            drawCircle(c, s * 0.14f, center, sw)
            // Lens inner ring
            drawCircle(c, s * 0.06f, center, sw)
            // Flash bump
            drawRoundRect(c, cx - s * 0.12f, cy - s * 0.22f, s * 0.24f, s * 0.08f, CornerRadius(s * 0.03f), sw)
        }
    }

    @Composable
    fun Document(
        modifier: Modifier = Modifier,
        tint: Color = Color.Unspecified,
        contentDescription: String? = null
    ) {
        val c = if (tint == Color.Unspecified) MaterialTheme.colorScheme.primary else tint
        val finalModifier = if (contentDescription != null) modifier.semantics { this.contentDescription = contentDescription } else modifier
        Canvas(modifier = finalModifier) {
            val s = size.minDimension
            val sw = s * 0.07f
            val cx = center.x
            val cy = center.y

            // Document body
            drawRoundRect(c, cx - s * 0.20f, cy - s * 0.30f, s * 0.40f, s * 0.60f, CornerRadius(s * 0.06f), sw)
            // Folded corner
            val fold = s * 0.10f
            drawLine(c, cx + s * 0.20f - fold, cy - s * 0.30f, cx + s * 0.20f, cy - s * 0.30f + fold, sw)
            // Content lines
            drawLine(c, cx - s * 0.10f, cy - s * 0.08f, cx + s * 0.12f, cy - s * 0.08f, sw)
            drawLine(c, cx - s * 0.10f, cy + s * 0.04f, cx + s * 0.12f, cy + s * 0.04f, sw)
            drawLine(c, cx - s * 0.10f, cy + s * 0.16f, cx + s * 0.06f, cy + s * 0.16f, sw)
        }
    }

    @Composable
    fun Visibility(
        modifier: Modifier = Modifier,
        tint: Color = Color.Unspecified,
        visible: Boolean = true,
        contentDescription: String? = null
    ) {
        val c = if (tint == Color.Unspecified) MaterialTheme.colorScheme.primary else tint
        val finalModifier = if (contentDescription != null) modifier.semantics { this.contentDescription = contentDescription } else modifier
        Canvas(modifier = finalModifier) {
            val s = size.minDimension
            val sw = s * 0.07f
            val cx = center.x
            val cy = center.y

            // Eye contour — two arcs
            drawArc(c, 210f, 120f, false, cx - s * 0.32f, cy - s * 0.26f, s * 0.64f, s * 0.64f, sw)
            drawArc(c, 30f, 120f, false, cx - s * 0.32f, cy - s * 0.30f, s * 0.64f, s * 0.64f, sw)
            // Pupil
            drawCircle(c, s * 0.10f, center, sw)
            // Inner dot
            drawCircle(c, sw * 0.5f, center, sw, Fill)
            // Slash if hidden
            if (!visible) {
                drawLine(c, cx - s * 0.32f, cy - s * 0.20f, cx + s * 0.32f, cy + s * 0.20f, sw)
            }
        }
    }

    @Composable
    fun Gallery(
        modifier: Modifier = Modifier,
        tint: Color = Color.Unspecified,
        contentDescription: String? = null
    ) {
        val c = if (tint == Color.Unspecified) MaterialTheme.colorScheme.primary else tint
        val finalModifier = if (contentDescription != null) modifier.semantics { this.contentDescription = contentDescription } else modifier
        Canvas(modifier = finalModifier) {
            val s = size.minDimension
            val sw = s * 0.07f
            val cx = center.x
            val cy = center.y

            // Frame
            drawRoundRect(c, cx - s * 0.30f, cy - s * 0.26f, s * 0.60f, s * 0.52f, CornerRadius(s * 0.06f), sw)
            // Mountain peaks
            drawLine(c, cx - s * 0.20f, cy + s * 0.18f, cx - s * 0.04f, cy - s * 0.04f, sw)
            drawLine(c, cx - s * 0.04f, cy - s * 0.04f, cx + s * 0.08f, cy + s * 0.10f, sw)
            drawLine(c, cx + s * 0.08f, cy + s * 0.10f, cx + s * 0.22f, cy + s * 0.18f, sw)
            // Sun
            drawCircle(c, s * 0.06f, Offset(cx + s * 0.14f, cy - s * 0.10f), sw)
            // Sun ray
            drawLine(c, cx + s * 0.14f, cy - s * 0.20f, cx + s * 0.14f, cy - s * 0.17f, sw)
        }
    }

    @Composable
    fun ArrowDropDown(
        modifier: Modifier = Modifier,
        tint: Color = Color.Unspecified,
        contentDescription: String? = null
    ) {
        val c = if (tint == Color.Unspecified) MaterialTheme.colorScheme.primary else tint
        val finalModifier = if (contentDescription != null) modifier.semantics { this.contentDescription = contentDescription } else modifier
        Canvas(modifier = finalModifier) {
            val s = size.minDimension
            val sw = s * 0.07f
            val cx = center.x
            val cy = center.y

            // Chevron
            drawLine(c, cx - s * 0.18f, cy - s * 0.08f, cx, cy + s * 0.10f, sw)
            drawLine(c, cx + s * 0.18f, cy - s * 0.08f, cx, cy + s * 0.10f, sw)
        }
    }

    @Composable
    fun Refresh(
        modifier: Modifier = Modifier,
        tint: Color = Color.Unspecified,
        contentDescription: String? = null
    ) {
        val c = if (tint == Color.Unspecified) MaterialTheme.colorScheme.primary else tint
        val finalModifier = if (contentDescription != null) modifier.semantics { this.contentDescription = contentDescription } else modifier
        Canvas(modifier = finalModifier) {
            val s = size.minDimension
            val sw = s * 0.07f
            val cx = center.x
            val cy = center.y
            val r = s * 0.24f

            // Arc
            drawArc(c, 30f, 280f, false, cx - r, cy - r, r * 2f, r * 2f, sw)
            // Arrowhead
            val endAngle = Math.toRadians(310.0).toFloat()
            val endX = cx + r * kotlin.math.cos(endAngle)
            val endY = cy + r * kotlin.math.sin(endAngle)
            drawLine(c, endX + s * 0.08f, endY - s * 0.08f, endX, endY, sw)
            drawLine(c, endX - s * 0.10f, endY - s * 0.02f, endX, endY, sw)
        }
    }

    @Composable
    fun ErrorExclamation(
        modifier: Modifier = Modifier,
        tint: Color = Color.Unspecified,
        contentDescription: String? = null
    ) {
        val c = if (tint == Color.Unspecified) MaterialTheme.colorScheme.primary else tint
        val finalModifier = if (contentDescription != null) modifier.semantics { this.contentDescription = contentDescription } else modifier
        Canvas(modifier = finalModifier) {
            val s = size.minDimension
            val sw = s * 0.07f
            val cx = center.x
            val cy = center.y

            // Warning circle
            drawCircle(c, s * 0.34f, center, sw)
            // Exclamation
            drawLine(c, cx, cy - s * 0.16f, cx, cy + s * 0.06f, sw)
            // Dot
            drawCircle(c, sw * 0.6f, Offset(cx, cy + s * 0.16f), sw, Fill)
        }
    }

    @Composable
    fun Download(
        modifier: Modifier = Modifier,
        tint: Color = Color.Unspecified,
        contentDescription: String? = null
    ) {
        val c = if (tint == Color.Unspecified) MaterialTheme.colorScheme.primary else tint
        val finalModifier = if (contentDescription != null) modifier.semantics { this.contentDescription = contentDescription } else modifier
        Canvas(modifier = finalModifier) {
            val s = size.minDimension
            val sw = s * 0.07f
            val cx = center.x
            val cy = center.y

            // Down arrow shaft
            drawLine(c, cx, cy - s * 0.26f, cx, cy + s * 0.12f, sw)
            // Arrowhead
            drawLine(c, cx - s * 0.14f, cy + s * 0.00f, cx, cy + s * 0.12f, sw)
            drawLine(c, cx + s * 0.14f, cy + s * 0.00f, cx, cy + s * 0.12f, sw)
            // Tray
            drawLine(c, cx - s * 0.24f, cy + s * 0.22f, cx + s * 0.24f, cy + s * 0.22f, sw)
        }
    }

    @Composable
    fun Stop(
        modifier: Modifier = Modifier,
        tint: Color = Color.Unspecified,
        contentDescription: String? = null
    ) {
        val c = if (tint == Color.Unspecified) MaterialTheme.colorScheme.primary else tint
        val finalModifier = if (contentDescription != null) modifier.semantics { this.contentDescription = contentDescription } else modifier
        Canvas(modifier = finalModifier) {
            val s = size.minDimension
            val sw = s * 0.07f
            val cx = center.x
            val cy = center.y

            // Rounded square
            drawRoundRect(c, cx - s * 0.22f, cy - s * 0.22f, s * 0.44f, s * 0.44f, CornerRadius(s * 0.06f), sw)
        }
    }

    /** Launch / open-app icon — arrow escaping a square */
    @Composable
    fun Launch(
        modifier: Modifier = Modifier,
        tint: Color = Color.Unspecified,
        contentDescription: String? = null
    ) {
        val c = if (tint == Color.Unspecified) MaterialTheme.colorScheme.primary else tint
        val finalModifier = if (contentDescription != null) modifier.semantics { this.contentDescription = contentDescription } else modifier
        Canvas(modifier = finalModifier) {
            val s = size.minDimension
            val sw = s * 0.07f
            val cx = center.x
            val cy = center.y

            // Box (bottom-left portion)
            drawLine(c, cx - s * 0.24f, cy + s * 0.24f, cx - s * 0.24f, cy - s * 0.08f, sw)
            drawLine(c, cx - s * 0.24f, cy + s * 0.24f, cx + s * 0.08f, cy + s * 0.24f, sw)
            drawLine(c, cx - s * 0.24f, cy - s * 0.08f, cx - s * 0.08f, cy - s * 0.08f, sw)
            drawLine(c, cx + s * 0.08f, cy + s * 0.24f, cx + s * 0.08f, cy + s * 0.08f, sw)
            // Arrow going out
            drawLine(c, cx + s * 0.02f, cy - s * 0.02f, cx + s * 0.26f, cy - s * 0.26f, sw)
            // Arrowhead
            drawLine(c, cx + s * 0.12f, cy - s * 0.26f, cx + s * 0.26f, cy - s * 0.26f, sw)
            drawLine(c, cx + s * 0.26f, cy - s * 0.12f, cx + s * 0.26f, cy - s * 0.26f, sw)
        }
    }

    // --- Drawing helpers — all with Round cap + Round join ---

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
