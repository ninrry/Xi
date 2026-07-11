package luzzr.xi.feature.overlay

import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import luzzr.xi.R
import luzzr.xi.core.ui.theme.XiTheme
import luzzr.xi.feature.overlay.ui.EdgePillTrigger
import luzzr.xi.feature.overlay.ui.TranslationPanelContent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    @Inject lateinit var overlayController: OverlayController

    private lateinit var windowManager: WindowManager
    private var pillView: android.view.View? = null
    private var panelView: android.view.View? = null
    private var touchHandler: BubbleTouchHandler? = null

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var isStopping = false
    private var panelCloseTime = 0L
    private var bubbleDocked by mutableStateOf(true)

    companion object {
        val isRunning = java.util.concurrent.atomic.AtomicBoolean(false)
        private const val PANEL_TOGGLE_DEBOUNCE_MS = 300L
        private const val BUBBLE_SIZE_DP = 36
        private const val EDGE_MARGIN_DP = 6
    }

    override fun onCreate() {
        isRunning.set(true)
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        OverlayNotificationHelper.createChannel(this)
        startForeground(OverlayNotificationHelper.NOTIFICATION_ID, OverlayNotificationHelper.createNotification(this))

        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        showPill()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isRunning.set(false)
        overlayController.cancelTranslate()
        touchHandler?.cancelLongPress()
        touchHandler = null
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        removeViewsPhysical()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == OverlayNotificationHelper.ACTION_STOP) {
            stopWithAnimation()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    // ── Floating Bubble ───────────────────────────────────────

    private fun showPill() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
            && !Settings.canDrawOverlays(this)) {
            Log.w("Overlay", "SYSTEM_ALERT_WINDOW permission not granted")
            return
        }

        val density = resources.displayMetrics.density
        val screenWidth = resources.displayMetrics.widthPixels
        val displayHeight = resources.displayMetrics.heightPixels
        val bubbleSizePx = (BUBBLE_SIZE_DP * density).toInt()
        val marginX = (EDGE_MARGIN_DP * density).toInt()

        val params = WindowManager.LayoutParams(
            bubbleSizePx,
            bubbleSizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenWidth - bubbleSizePx - marginX
            y = displayHeight / 2 - bubbleSizePx / 2
        }

        val view = androidx.compose.ui.platform.ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
            setContent {
                XiTheme {
                    EdgePillTrigger(docked = bubbleDocked)
                }
            }
        }

        touchHandler = BubbleTouchHandler(
            params = params,
            screenWidth = screenWidth,
            displayHeight = displayHeight,
            density = density,
            windowManager = windowManager,
            view = view,
            onClick = { togglePanel() },
            onLongPress = {
                    android.widget.Toast.makeText(this@OverlayService, getString(R.string.overlay_service_stopping), android.widget.Toast.LENGTH_SHORT).show()
                    stopWithAnimation()
                },
            onPositionChanged = {},
            onDockStateChanged = { docked -> bubbleDocked = docked }
        )

        view.setOnTouchListener { _, event -> touchHandler?.handleTouchEvent(event) ?: false }

        pillView = view
        windowManager.addView(view, params)
        touchHandler?.positionAtRightEdge()
    }

    // ── Panel ──────────────────────────────────────────────────

    private fun togglePanel() {
        val now = System.currentTimeMillis()
        if (now - panelCloseTime < PANEL_TOGGLE_DEBOUNCE_MS) return

        if (overlayController.uiState.value.isPanelVisible) {
            hidePanel()
        } else {
            showPanel()
        }
    }

    private fun hidePanel() {
        overlayController.cancelTranslate()
        overlayController.setPanelVisible(false)
        panelCloseTime = System.currentTimeMillis()
    }

    private fun showPanel() {
        if (panelView != null) {
            isStopping = false
            overlayController.setPanelVisible(true)
            return
        }
        val density = resources.displayMetrics.density
        val panelWidth = minOf(
            (resources.displayMetrics.widthPixels - 64 * density).toInt(),
            (420 * density).toInt()
        )

        val params = WindowManager.LayoutParams(
            panelWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            x = 0
            y = 0
        }

        val view = androidx.compose.ui.platform.ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
            setContent {
                XiTheme {
                    val state by overlayController.uiState.collectAsStateWithLifecycle()
                    val context = this@OverlayService
                    TranslationPanelContent(
                        visible = state.isPanelVisible,
                        inputText = state.inputText,
                        onInputChange = { text -> overlayController.updateInputText(text) },
                        resultText = state.resultText,
                        usage = state.usage,
                        isTranslating = state.isTranslating,
                        isModelDownloading = state.isModelDownloading,
                        error = state.errorMsg?.asString(context),
                        sourceLang = state.sourceLang,
                        targetLang = state.targetLang,
                        engine = state.engine,
                        thinkingLevel = state.thinkingLevel,
                        onTranslate = { overlayController.translate() },
                        onSwap = { overlayController.swapLanguages() },
                        onCopy = { copyResult() },
                        onDismiss = { hidePanel() },
                        onClear = { overlayController.clear() },
                        onLaunchEssay = {
                            overlayController.setPanelVisible(false)
                            val mainIntent = Intent(this@OverlayService, luzzr.xi.MainActivity::class.java).apply {
                                putExtra("target_screen", "essay")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            }
                            try { startActivity(mainIntent) }
                            catch (e: Exception) { Log.e("OverlayService", "Failed to launch MainActivity", e) }
                        },
                        onExitAnimationFinished = {
                            removePanelPhysical()
                            if (isStopping) stopSelf()
                        },
                        onSourceLangChange = { lang -> overlayController.updateSourceLang(lang) },
                        onTargetLangChange = { lang -> overlayController.updateTargetLang(lang) },
                        onEngineChange = { eng -> overlayController.updateEngine(eng) },
                        onThinkingLevelChange = { lvl -> overlayController.updateThinkingLevel(lvl) }
                    )
                }
            }
        }

        panelView = view
        overlayController.setPanelVisible(true)
        windowManager.addView(view, params)

        panelView?.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_OUTSIDE) {
                overlayController.setPanelVisible(false)
                panelCloseTime = System.currentTimeMillis()
                true
            } else false
        }
    }

    // ── Stop flow ──────────────────────────────────────────────

    private fun stopWithAnimation() {
        overlayController.cancelTranslate()
        if (overlayController.uiState.value.isPanelVisible) {
            isStopping = true
            overlayController.setPanelVisible(false)
            // onExitAnimationFinished will call stopSelf()
        } else {
            stopSelf()
        }
    }

    private fun removePanelPhysical() {
        panelView?.let {
            try { if (it.isAttachedToWindow) windowManager.removeView(it) }
            catch (e: IllegalArgumentException) { Log.w("Overlay", "removeView failed", e) }
        }
        panelView = null
    }

    private fun removeViewsPhysical() {
        pillView?.let {
            try { if (it.isAttachedToWindow) windowManager.removeView(it) }
            catch (e: IllegalArgumentException) { Log.w("Overlay", "removeView failed", e) }
        }
        removePanelPhysical()
        pillView = null
    }

    // ── Configuration change (screen rotation) ───────────────────

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Re-position bubble to stay within new screen bounds
        pillView?.let { view ->
            val density = resources.displayMetrics.density
            val screenWidth = resources.displayMetrics.widthPixels
            val displayHeight = resources.displayMetrics.heightPixels
            val bubbleSizePx = (BUBBLE_SIZE_DP * density).toInt()
            val marginPx = (EDGE_MARGIN_DP * density).toInt()

            val params = view.layoutParams as WindowManager.LayoutParams
            // Keep bubble on right edge, clamp Y to new height
            params.x = screenWidth - bubbleSizePx - marginPx
            params.y = params.y.coerceIn(0, displayHeight - bubbleSizePx)
            try {
                windowManager.updateViewLayout(view, params)
            } catch (e: Exception) {
                Log.w("Overlay", "onConfigurationChanged update failed", e)
            }
        }
        // Hide panel on rotation to avoid size mismatch
        if (overlayController.uiState.value.isPanelVisible) {
            hidePanel()
        }
    }

    // ── Business logic ────────────────────────────────────────

    private fun copyResult() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("translation", overlayController.uiState.value.resultText))
    }
}
