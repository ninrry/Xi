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
 * Touch handler for the vertical edge pill.
 *
 * Interactions:
 * - Swipe inward (horizontal, any direction) → open panel
 * - Drag along edge (vertical) → reposition pill up/down
 * - Long press (hold still >500ms) → stop overlay service
 *
 * NO tap/click trigger — only swipe and long-press.
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
        private const val SWIPE_THRESHOLD_DP = 12     // lower threshold for easier swipe-in
        private const val DRAG_THRESHOLD_DP = 10      // vertical movement to enter drag
        private const val LONG_PRESS_MS = 500L
        private const val TOUCH_EXPAND_DP = 20        // expand 20dp left+right beyond visual
        private const val PILL_VISUAL_WIDTH_DP = 8
        private const val PILL_VISUAL_HEIGHT_DP = 40
        private const val EDGE_MARGIN_DP = 0          // flush against edge — no margin
        private const val SAFE_MARGIN_DP = 40         // keep pill within this distance from top/bottom
    }

    private val swipeThresholdPx = (SWIPE_THRESHOLD_DP * density)
    private val dragThresholdPx = (DRAG_THRESHOLD_DP * density)
    private val touchExpandPx = (TOUCH_EXPAND_DP * density).toInt()
    private val pillWidthPx = (PILL_VISUAL_WIDTH_DP * density).toInt()
    private val pillHeightPx = (PILL_VISUAL_HEIGHT_DP * density).toInt()
    private val safeMarginPx = (SAFE_MARGIN_DP * density).toInt()

    private var downX = 0f
    private var downY = 0f
    private var initialY = 0
    private var downTime = 0L
    private var isDragging = false
    private var hasTriggeredSwipe = false

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
            // Expand left/right for easier swipe detection
            rect.left -= touchExpandPx
            rect.right += touchExpandPx
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
                initialY = params.y
                downTime = System.currentTimeMillis()
                isDragging = false
                hasTriggeredSwipe = false

                // Schedule long press
                longPressRunnable = Runnable {
                    if (!isDragging && !hasTriggeredSwipe) {
                        onLongPress()
                    }
                }
                mainHandler.postDelayed(longPressRunnable!!, LONG_PRESS_MS)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - downX
                val dy = event.rawY - downY
                val absDx = Math.abs(dx)
                val absDy = Math.abs(dy)

                // Swipe inward — any horizontal movement exceeding threshold (not just leftward)
                // This handles both left-swipe (from right edge) and right-swipe
                if (!isDragging && !hasTriggeredSwipe && absDx > swipeThresholdPx) {
                    // Only trigger if horizontal is dominant over vertical
                    if (absDx > absDy * 0.7f) {
                        hasTriggeredSwipe = true
                        cancelLongPress()
                        onClick()
                        return true
                    }
                }

                // Vertical drag along edge
                if (!isDragging && !hasTriggeredSwipe && absDy > dragThresholdPx) {
                    isDragging = true
                    cancelLongPress()
                }

                if (isDragging) {
                    val newY = initialY + (event.rawY - downY).toInt()
                    // Clamp within safe zone — never let pill go off-screen
                    params.y = newY.coerceIn(safeMarginPx, displayHeight - pillHeightPx - safeMarginPx)
                    try {
                        windowManager.updateViewLayout(view, params)
                    } catch (_: Exception) { }
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                cancelLongPress()
                // No tap trigger — swipe only
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                cancelLongPress()
                return true
            }
        }
        return false
    }

    private fun cancelLongPress() {
        longPressRunnable?.let { mainHandler.removeCallbacks(it) }
        longPressRunnable = null
    }

    fun resetAutoHideTimer() {}
    fun cancelAutoHide() {}

    fun positionAtRightEdge() {
        params.x = screenWidth - pillWidthPx  // flush, no margin
        params.y = (displayHeight / 2) - (pillHeightPx / 2)
        try {
            windowManager.updateViewLayout(view, params)
        } catch (e: Exception) {
            Log.w("Overlay", "positionAtRightEdge failed", e)
        }
        onPositionChanged()
    }
}
