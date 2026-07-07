package luzzr.xi.feature.overlay

import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.TouchDelegate
import android.view.View
import android.view.WindowManager

/**
 * Touch handler for the mini floating bubble.
 *
 * Interactions:
 * - Tap (short press + release without significant movement) → open/close panel
 * - Drag (vertical or horizontal movement) → reposition bubble
 * - Long press (hold still >500ms) → stop overlay service
 *
 * The bubble sits 6dp inside the screen edge, avoiding Android 10+ edge-swipe-back
 * gesture conflict. Tap is the primary trigger instead of swipe.
 */
internal class BubbleTouchHandler(
    private val params: WindowManager.LayoutParams,
    private val screenWidth: Int,
    private val displayHeight: Int,
    private val density: Float,
    private val windowManager: WindowManager,
    private val view: View,
    private val onClick: () -> Unit,
    private val onLongPress: () -> Unit,
    private val onPositionChanged: () -> Unit = {}
) {
    companion object {
        private const val TAP_THRESHOLD_DP = 10      // max movement to still count as tap
        private const val DRAG_THRESHOLD_DP = 12      // movement to enter drag mode
        private const val LONG_PRESS_MS = 500L
        private const val TOUCH_EXPAND_DP = 12        // expand 12dp beyond visual for easier tapping
        private const val BUBBLE_VISUAL_SIZE_DP = 36
        private const val EDGE_MARGIN_DP = 6          // 6dp from right edge
        private const val SAFE_MARGIN_DP = 40         // keep bubble within this distance from top/bottom
    }

    private val tapThresholdPx = (TAP_THRESHOLD_DP * density)
    private val dragThresholdPx = (DRAG_THRESHOLD_DP * density)
    private val touchExpandPx = (TOUCH_EXPAND_DP * density).toInt()
    private val bubbleSizePx = (BUBBLE_VISUAL_SIZE_DP * density).toInt()
    private val edgeMarginPx = (EDGE_MARGIN_DP * density).toInt()
    private val safeMarginPx = (SAFE_MARGIN_DP * density).toInt()

    private var downX = 0f
    private var downY = 0f
    private var initialX = 0
    private var initialY = 0
    private var downTime = 0L
    private var isDragging = false
    private var hasTriggeredLongPress = false
    private var lastUpdateMs = 0L
    private var lastX = 0
    private var lastY = 0

    private val mainHandler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null

    init {
        expandTouchArea()
    }

    private fun expandTouchArea() {
        val parent = view.parent as? View ?: return
        view.post {
            val rect = Rect()
            view.getHitRect(rect)
            // Expand in all directions for easier tapping
            rect.left -= touchExpandPx
            rect.right += touchExpandPx
            rect.top -= touchExpandPx
            rect.bottom += touchExpandPx
            try {
                parent.touchDelegate = TouchDelegate(rect, view)
            } catch (e: Exception) {
                Log.w("Overlay", "TouchDelegate setup failed", e)
            }
        }
    }

    fun handleTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                initialX = params.x
                initialY = params.y
                downTime = System.currentTimeMillis()
                isDragging = false
                hasTriggeredLongPress = false

                // Schedule long press
                longPressRunnable = Runnable {
                    if (!isDragging) {
                        hasTriggeredLongPress = true
                        onLongPress()
                    }
                }
                mainHandler.postDelayed(longPressRunnable!!, LONG_PRESS_MS)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - downX
                val dy = event.rawY - downY
                val totalDist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                // Enter drag mode if moved beyond threshold
                if (!isDragging && !hasTriggeredLongPress && totalDist > dragThresholdPx) {
                    isDragging = true
                    cancelLongPress()
                }

                if (isDragging) {
                    val newX = initialX + dx.toInt()
                    val newY = initialY + dy.toInt()

                    // Clamp X: keep bubble on-screen, 6dp from right edge preferred
                    val minX = 0
                    val maxX = screenWidth - bubbleSizePx
                    params.x = newX.coerceIn(minX, maxX)

                    // Clamp Y: keep bubble within safe zone
                    params.y = newY.coerceIn(safeMarginPx, displayHeight - bubbleSizePx - safeMarginPx)

                    val now = System.currentTimeMillis()
                    if (now - lastUpdateMs > 16 && (newX != lastX || newY != lastY)) {
                        lastUpdateMs = now
                        lastX = newX
                        lastY = newY
                        try {
                            windowManager.updateViewLayout(view, params)
                        } catch (_: Exception) { }
                    }
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                cancelLongPress()
                if (!isDragging && !hasTriggeredLongPress) {
                    // Tap — open/close panel
                    val totalDist = Math.sqrt(
                        ((event.rawX - downX) * (event.rawX - downX) +
                         (event.rawY - downY) * (event.rawY - downY)).toDouble()
                    ).toFloat()
                    if (totalDist < tapThresholdPx) {
                        onClick()
                    }
                }
                // After drag, snap to nearest edge (left or right)
                if (isDragging) {
                    snapToNearestEdge()
                }
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                cancelLongPress()
                return true
            }
        }
        return false
    }

    /**
     * After dragging, snap the bubble to the nearest horizontal edge
     * (left or right) with 6dp margin.
     */
    private fun snapToNearestEdge() {
        val centerX = params.x + bubbleSizePx / 2
        val snapRight = centerX > screenWidth / 2

        params.x = if (snapRight) {
            screenWidth - bubbleSizePx - edgeMarginPx
        } else {
            edgeMarginPx
        }

        try {
            windowManager.updateViewLayout(view, params)
        } catch (e: Exception) {
            Log.w("Overlay", "snapToEdge failed", e)
        }
        onPositionChanged()
    }

    fun cancelLongPress() {
        longPressRunnable?.let { mainHandler.removeCallbacks(it) }
        longPressRunnable = null
    }

    fun positionAtRightEdge() {
        params.x = screenWidth - bubbleSizePx - edgeMarginPx
        params.y = (displayHeight / 2) - (bubbleSizePx / 2)
        try {
            windowManager.updateViewLayout(view, params)
        } catch (e: Exception) {
            Log.w("Overlay", "positionAtRightEdge failed", e)
        }
        onPositionChanged()
    }
}
