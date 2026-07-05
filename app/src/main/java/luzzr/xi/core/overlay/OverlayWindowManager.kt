package luzzr.xi.core.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OverlayWindowManager @Inject constructor(
    private val context: Context
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null

    fun showOverlay(view: View) {
        if (overlayView != null) {
            removeOverlay()
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
            preferredRefreshRate = 120f
        }

        windowManager.addView(view, params)
        overlayView = view
    }

    fun updateOverlayPosition(x: Int, y: Int) {
        overlayView?.let { view ->
            val params = view.layoutParams as WindowManager.LayoutParams
            params.x = x
            params.y = y
            windowManager.updateViewLayout(view, params)
        }
    }

    fun removeOverlay() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                // Ignore
            }
        }
        overlayView = null
    }
}