package luzzr.xi.core.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * Abstract stroke-based icons — avant-garde, minimalist line art.
 *
 * Design language:
 * - Uniform 0.05f stroke width ratio (thinner, more elegant)
 * - Pure path-based, abstract representation
 * - High spatial utilization (breathing room)
 * - StrokeCap.Round + StrokeJoin.Round everywhere
 */
object AbstractIcons {

    @Composable
    fun Translate(
        modifier: Modifier = Modifier,
        tint: Color = Color.Unspecified,
        contentDescription: String? = null
    ) {
        val c = if (tint == Color.Unspecified) MaterialTheme.colorScheme.primary else tint
        val finalMod = if (contentDescription != null) modifier.semantics { this.contentDescription = contentDescription } else modifier
        Canvas(modifier = finalMod) {
            val s = size.minDimension
            val sw = s * 0.05f
            
            // Abstract intertwining concept: two interlocking open curves representing translation
            val path1 = Path().apply {
                moveTo(s * 0.2f, s * 0.4f)
                cubicTo(s * 0.2f, s * 0.15f, s * 0.6f, s * 0.15f, s * 0.6f, s * 0.4f)
                cubicTo(s * 0.6f, s * 0.55f, s * 0.4f, s * 0.65f, s * 0.3f, s * 0.75f)
            }
            
            val path2 = Path().apply {
                moveTo(s * 0.8f, s * 0.6f)
                cubicTo(s * 0.8f, s * 0.85f, s * 0.4f, s * 0.85f, s * 0.4f, s * 0.6f)
                cubicTo(s * 0.4f, s * 0.45f, s * 0.6f, s * 0.35f, s * 0.7f, s * 0.25f)
            }
            
            drawPathLine(path1, c, sw)
            drawPathLine(path2, c, sw)
        }
    }

    @Composable
    fun Edit(
        modifier: Modifier = Modifier,
        tint: Color = Color.Unspecified,
        contentDescription: String? = null
    ) {
        val c = if (tint == Color.Unspecified) MaterialTheme.colorScheme.primary else tint
        val finalMod = if (contentDescription != null) modifier.semantics { this.contentDescription = contentDescription } else modifier
        Canvas(modifier = finalMod) {
            val s = size.minDimension
            val sw = s * 0.05f

            // A sweeping minimalist curve signifying flow and writing
            val path = Path().apply {
                moveTo(s * 0.15f, s * 0.75f)
                cubicTo(s * 0.35f, s * 0.75f, s * 0.45f, s * 0.65f, s * 0.65f, s * 0.35f)
                lineTo(s * 0.75f, s * 0.2f)
            }
            drawPathLine(path, c, sw)
            
            // A solitary dot at the tip
            drawCircle(c, radius = sw * 1.5f, center = Offset(s * 0.8f, s * 0.15f))
        }
    }

    @Composable
    fun Settings(
        modifier: Modifier = Modifier,
        tint: Color = Color.Unspecified,
        contentDescription: String? = null
    ) {
        val c = if (tint == Color.Unspecified) MaterialTheme.colorScheme.primary else tint
        val finalMod = if (contentDescription != null) modifier.semantics { this.contentDescription = contentDescription } else modifier
        Canvas(modifier = finalMod) {
            val s = size.minDimension
            val sw = s * 0.05f

            // Abstract sliders: simple floating horizontal lines with precise gaps
            val path = Path().apply {
                moveTo(s * 0.15f, s * 0.3f)
                lineTo(s * 0.4f, s * 0.3f)
                moveTo(s * 0.55f, s * 0.3f)
                lineTo(s * 0.85f, s * 0.3f)

                moveTo(s * 0.15f, s * 0.5f)
                lineTo(s * 0.65f, s * 0.5f)
                moveTo(s * 0.8f, s * 0.5f)
                lineTo(s * 0.85f, s * 0.5f)

                moveTo(s * 0.15f, s * 0.7f)
                lineTo(s * 0.2f, s * 0.7f)
                moveTo(s * 0.35f, s * 0.7f)
                lineTo(s * 0.85f, s * 0.7f)
            }
            drawPathLine(path, c, sw)
        }
    }

    @Composable
    fun Sparkle(
        modifier: Modifier = Modifier,
        tint: Color = Color.Unspecified,
        contentDescription: String? = null
    ) {
        val c = if (tint == Color.Unspecified) MaterialTheme.colorScheme.primary else tint
        val finalMod = if (contentDescription != null) modifier.semantics { this.contentDescription = contentDescription } else modifier
        Canvas(modifier = finalMod) {
            val s = size.minDimension
            val sw = s * 0.04f

            val path = Path().apply {
                moveTo(s * 0.5f, s * 0.1f)
                cubicTo(s * 0.5f, s * 0.4f, s * 0.6f, s * 0.5f, s * 0.9f, s * 0.5f)
                cubicTo(s * 0.6f, s * 0.5f, s * 0.5f, s * 0.6f, s * 0.5f, s * 0.9f)
                cubicTo(s * 0.5f, s * 0.6f, s * 0.4f, s * 0.5f, s * 0.1f, s * 0.5f)
                cubicTo(s * 0.4f, s * 0.5f, s * 0.5f, s * 0.4f, s * 0.5f, s * 0.1f)
            }
            drawPathLine(path, c, sw)
        }
    }

    @Composable
    fun Swap(
        modifier: Modifier = Modifier,
        tint: Color = Color.Unspecified,
        contentDescription: String? = null
    ) {
        val c = if (tint == Color.Unspecified) MaterialTheme.colorScheme.primary else tint
        val finalMod = if (contentDescription != null) modifier.semantics { this.contentDescription = contentDescription } else modifier
        Canvas(modifier = finalMod) {
            val s = size.minDimension
            val sw = s * 0.05f

            val path = Path().apply {
                // Top curve rightward
                moveTo(s * 0.2f, s * 0.35f)
                cubicTo(s * 0.4f, s * 0.2f, s * 0.6f, s * 0.2f, s * 0.8f, s * 0.35f)
                // Bottom curve leftward
                moveTo(s * 0.8f, s * 0.65f)
                cubicTo(s * 0.6f, s * 0.8f, s * 0.4f, s * 0.8f, s * 0.2f, s * 0.65f)
            }
            drawPathLine(path, c, sw)
            
            // Minimal dots indicating direction instead of arrowheads
            drawCircle(c, radius = sw * 1.2f, center = Offset(s * 0.8f, s * 0.35f))
            drawCircle(c, radius = sw * 1.2f, center = Offset(s * 0.2f, s * 0.65f))
        }
    }

    @Composable
    fun Copy(
        modifier: Modifier = Modifier,
        tint: Color = Color.Unspecified,
        contentDescription: String? = null
    ) {
        val c = if (tint == Color.Unspecified) MaterialTheme.colorScheme.primary else tint
        val finalMod = if (contentDescription != null) modifier.semantics { this.contentDescription = contentDescription } else modifier
        Canvas(modifier = finalMod) {
            val s = size.minDimension
            val sw = s * 0.05f

            val path = Path().apply {
                // Offset rect
                moveTo(s * 0.2f, s * 0.3f)
                lineTo(s * 0.2f, s * 0.8f)
                lineTo(s * 0.6f, s * 0.8f)
                
                // Main rect
                moveTo(s * 0.4f, s * 0.7f)
                lineTo(s * 0.8f, s * 0.7f)
                lineTo(s * 0.8f, s * 0.2f)
                lineTo(s * 0.4f, s * 0.2f)
                close()
            }
            drawPathLine(path, c, sw)
        }
    }

    @Composable
    fun Delete(
        modifier: Modifier = Modifier,
        tint: Color = Color.Unspecified,
        contentDescription: String? = null
    ) {
        val c = if (tint == Color.Unspecified) MaterialTheme.colorScheme.primary else tint
        val finalMod = if (contentDescription != null) modifier.semantics { this.contentDescription = contentDescription } else modifier
        Canvas(modifier = finalMod) {
            val s = size.minDimension
            val sw = s * 0.05f

            val path = Path().apply {
                // Minimal lid
                moveTo(s * 0.25f, s * 0.25f)
                lineTo(s * 0.75f, s * 0.25f)
                
                // U-shape body
                moveTo(s * 0.35f, s * 0.25f)
                lineTo(s * 0.35f, s * 0.75f)
                cubicTo(s * 0.35f, s * 0.8f, s * 0.4f, s * 0.85f, s * 0.5f, s * 0.85f)
                cubicTo(s * 0.6f, s * 0.85f, s * 0.65f, s * 0.8f, s * 0.65f, s * 0.75f)
                lineTo(s * 0.65f, s * 0.25f)
            }
            drawPathLine(path, c, sw)
        }
    }

    @Composable
    fun CheckCircle(
        modifier: Modifier = Modifier,
        tint: Color = Color.Unspecified,
        contentDescription: String? = null
    ) {
        val c = if (tint == Color.Unspecified) MaterialTheme.colorScheme.primary else tint
        val finalMod = if (contentDescription != null) modifier.semantics { this.contentDescription = contentDescription } else modifier
        Canvas(modifier = finalMod) {
            val s = size.minDimension
            val sw = s * 0.05f

            val path = Path().apply {
                // Flowing check curve
                moveTo(s * 0.25f, s * 0.55f)
                cubicTo(s * 0.3f, s * 0.6f, s * 0.4f, s * 0.75f, s * 0.45f, s * 0.75f)
                cubicTo(s * 0.5f, s * 0.75f, s * 0.65f, s * 0.4f, s * 0.75f, s * 0.25f)
            }
            drawPathLine(path, c, sw)
        }
    }

    @Composable
    fun Close(
        modifier: Modifier = Modifier,
        tint: Color = Color.Unspecified,
        contentDescription: String? = null
    ) {
        val c = if (tint == Color.Unspecified) MaterialTheme.colorScheme.primary else tint
        val finalMod = if (contentDescription != null) modifier.semantics { this.contentDescription = contentDescription } else modifier
        Canvas(modifier = finalMod) {
            val s = size.minDimension
            val sw = s * 0.05f

            val path = Path().apply {
                moveTo(s * 0.3f, s * 0.3f)
                lineTo(s * 0.7f, s * 0.7f)
                moveTo(s * 0.7f, s * 0.3f)
                lineTo(s * 0.3f, s * 0.7f)
            }
            drawPathLine(path, c, sw)
        }
    }

    @Composable
    fun Camera(
        modifier: Modifier = Modifier,
        tint: Color = Color.Unspecified,
        contentDescription: String? = null
    ) {
        val c = if (tint == Color.Unspecified) MaterialTheme.colorScheme.primary else tint
        val finalMod = if (contentDescription != null) modifier.semantics { this.contentDescription = contentDescription } else modifier
        Canvas(modifier = finalMod) {
            val s = size.minDimension
            val sw = s * 0.05f

            val path = Path().apply {
                // Outline
                moveTo(s * 0.2f, s * 0.35f)
                lineTo(s * 0.8f, s * 0.35f)
                lineTo(s * 0.8f, s * 0.75f)
                lineTo(s * 0.2f, s * 0.75f)
                close()
                
                // Lens (just a simple curve and dot)
                moveTo(s * 0.4f, s * 0.55f)
                cubicTo(s * 0.4f, s * 0.65f, s * 0.6f, s * 0.65f, s * 0.6f, s * 0.55f)
            }
            drawPathLine(path, c, sw)
            drawCircle(c, radius = sw, center = Offset(s * 0.5f, s * 0.5f))
        }
    }

    @Composable
    fun Document(
        modifier: Modifier = Modifier,
        tint: Color = Color.Unspecified,
        contentDescription: String? = null
    ) {
        val c = if (tint == Color.Unspecified) MaterialTheme.colorScheme.primary else tint
        val finalMod = if (contentDescription != null) modifier.semantics { this.contentDescription = contentDescription } else modifier
        Canvas(modifier = finalMod) {
            val s = size.minDimension
            val sw = s * 0.05f

            val path = Path().apply {
                // Outer L shape
                moveTo(s * 0.7f, s * 0.2f)
                lineTo(s * 0.3f, s * 0.2f)
                lineTo(s * 0.3f, s * 0.8f)
                lineTo(s * 0.7f, s * 0.8f)
                
                // Folded corner abstraction
                moveTo(s * 0.7f, s * 0.4f)
                lineTo(s * 0.5f, s * 0.2f)
            }
            drawPathLine(path, c, sw)
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
        val finalMod = if (contentDescription != null) modifier.semantics { this.contentDescription = contentDescription } else modifier
        Canvas(modifier = finalMod) {
            val s = size.minDimension
            val sw = s * 0.05f

            val path = Path().apply {
                // Eye contour abstraction (a single elegant swoosh)
                moveTo(s * 0.2f, s * 0.5f)
                cubicTo(s * 0.4f, s * 0.2f, s * 0.6f, s * 0.2f, s * 0.8f, s * 0.5f)
                
                if (!visible) {
                    moveTo(s * 0.3f, s * 0.7f)
                    lineTo(s * 0.7f, s * 0.3f)
                }
            }
            drawPathLine(path, c, sw)
            
            // Pupil dot
            drawCircle(c, radius = sw * 1.5f, center = Offset(s * 0.5f, s * 0.45f))
        }
    }

    @Composable
    fun Gallery(
        modifier: Modifier = Modifier,
        tint: Color = Color.Unspecified,
        contentDescription: String? = null
    ) {
        val c = if (tint == Color.Unspecified) MaterialTheme.colorScheme.primary else tint
        val finalMod = if (contentDescription != null) modifier.semantics { this.contentDescription = contentDescription } else modifier
        Canvas(modifier = finalMod) {
            val s = size.minDimension
            val sw = s * 0.05f

            val path = Path().apply {
                // A single continuous line depicting mountains
                moveTo(s * 0.2f, s * 0.8f)
                lineTo(s * 0.45f, s * 0.4f)
                lineTo(s * 0.6f, s * 0.6f)
                lineTo(s * 0.75f, s * 0.45f)
                lineTo(s * 0.9f, s * 0.8f)
            }
            drawPathLine(path, c, sw)
            
            // Minimal sun
            drawCircle(c, radius = sw * 2f, center = Offset(s * 0.25f, s * 0.3f))
        }
    }

    @Composable
    fun ArrowDropDown(
        modifier: Modifier = Modifier,
        tint: Color = Color.Unspecified,
        contentDescription: String? = null
    ) {
        val c = if (tint == Color.Unspecified) MaterialTheme.colorScheme.primary else tint
        val finalMod = if (contentDescription != null) modifier.semantics { this.contentDescription = contentDescription } else modifier
        Canvas(modifier = finalMod) {
            val s = size.minDimension
            val sw = s * 0.05f

            val path = Path().apply {
                moveTo(s * 0.3f, s * 0.4f)
                lineTo(s * 0.5f, s * 0.6f)
                lineTo(s * 0.7f, s * 0.4f)
            }
            drawPathLine(path, c, sw)
        }
    }

    @Composable
    fun Refresh(
        modifier: Modifier = Modifier,
        tint: Color = Color.Unspecified,
        contentDescription: String? = null
    ) {
        val c = if (tint == Color.Unspecified) MaterialTheme.colorScheme.primary else tint
        val finalMod = if (contentDescription != null) modifier.semantics { this.contentDescription = contentDescription } else modifier
        Canvas(modifier = finalMod) {
            val s = size.minDimension
            val sw = s * 0.05f

            val path = Path().apply {
                // Almost complete circle
                moveTo(s * 0.5f, s * 0.2f)
                cubicTo(s * 0.8f, s * 0.2f, s * 0.8f, s * 0.8f, s * 0.5f, s * 0.8f)
                cubicTo(s * 0.2f, s * 0.8f, s * 0.2f, s * 0.4f, s * 0.4f, s * 0.25f)
                
                // Elegant flick for arrow
                lineTo(s * 0.25f, s * 0.25f)
            }
            drawPathLine(path, c, sw)
        }
    }

    @Composable
    fun ErrorExclamation(
        modifier: Modifier = Modifier,
        tint: Color = Color.Unspecified,
        contentDescription: String? = null
    ) {
        val c = if (tint == Color.Unspecified) MaterialTheme.colorScheme.primary else tint
        val finalMod = if (contentDescription != null) modifier.semantics { this.contentDescription = contentDescription } else modifier
        Canvas(modifier = finalMod) {
            val s = size.minDimension
            val sw = s * 0.05f

            val path = Path().apply {
                moveTo(s * 0.5f, s * 0.2f)
                lineTo(s * 0.5f, s * 0.65f)
            }
            drawPathLine(path, c, sw)
            drawCircle(c, radius = sw * 1.2f, center = Offset(s * 0.5f, s * 0.8f))
        }
    }

    @Composable
    fun Download(
        modifier: Modifier = Modifier,
        tint: Color = Color.Unspecified,
        contentDescription: String? = null
    ) {
        val c = if (tint == Color.Unspecified) MaterialTheme.colorScheme.primary else tint
        val finalMod = if (contentDescription != null) modifier.semantics { this.contentDescription = contentDescription } else modifier
        Canvas(modifier = finalMod) {
            val s = size.minDimension
            val sw = s * 0.05f

            val path = Path().apply {
                // Elegant downward curve
                moveTo(s * 0.5f, s * 0.2f)
                lineTo(s * 0.5f, s * 0.7f)
                moveTo(s * 0.35f, s * 0.55f)
                lineTo(s * 0.5f, s * 0.7f)
                lineTo(s * 0.65f, s * 0.55f)
                
                // Tray is just a dot to be abstract
            }
            drawPathLine(path, c, sw)
            drawCircle(c, radius = sw * 1.5f, center = Offset(s * 0.5f, s * 0.9f))
        }
    }

    @Composable
    fun Stop(
        modifier: Modifier = Modifier,
        tint: Color = Color.Unspecified,
        contentDescription: String? = null
    ) {
        val c = if (tint == Color.Unspecified) MaterialTheme.colorScheme.primary else tint
        val finalMod = if (contentDescription != null) modifier.semantics { this.contentDescription = contentDescription } else modifier
        Canvas(modifier = finalMod) {
            val s = size.minDimension
            val sw = s * 0.05f

            val path = Path().apply {
                // Perfect geometric abstraction
                moveTo(s * 0.3f, s * 0.3f)
                lineTo(s * 0.7f, s * 0.3f)
                lineTo(s * 0.7f, s * 0.7f)
                lineTo(s * 0.3f, s * 0.7f)
                close()
            }
            drawPathLine(path, c, sw)
        }
    }

    @Composable
    fun Launch(
        modifier: Modifier = Modifier,
        tint: Color = Color.Unspecified,
        contentDescription: String? = null
    ) {
        val c = if (tint == Color.Unspecified) MaterialTheme.colorScheme.primary else tint
        val finalMod = if (contentDescription != null) modifier.semantics { this.contentDescription = contentDescription } else modifier
        Canvas(modifier = finalMod) {
            val s = size.minDimension
            val sw = s * 0.05f

            val path = Path().apply {
                // Outward sweeping curve
                moveTo(s * 0.3f, s * 0.7f)
                cubicTo(s * 0.5f, s * 0.7f, s * 0.7f, s * 0.5f, s * 0.7f, s * 0.3f)
                
                // Minimal arrow head
                lineTo(s * 0.5f, s * 0.3f)
                moveTo(s * 0.7f, s * 0.3f)
                lineTo(s * 0.7f, s * 0.5f)
            }
            drawPathLine(path, c, sw)
        }
    }

    // --- Core drawing primitive ---
    private fun DrawScope.drawPathLine(path: Path, color: Color, strokeWidth: Float) {
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

