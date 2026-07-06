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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import luzzr.xi.R
import luzzr.xi.core.datastore.SettingsDataStore
import luzzr.xi.domain.model.TranslationEngine
import luzzr.xi.domain.model.UiText
import luzzr.xi.domain.usecase.TranslateUseCase
import luzzr.xi.core.ui.theme.XiTheme
import luzzr.xi.feature.overlay.ui.EdgePillTrigger
import luzzr.xi.feature.overlay.ui.TranslationPanelContent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class OverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    @Inject lateinit var settingsDataStore: SettingsDataStore
    @Inject lateinit var translateUseCase: TranslateUseCase

    private lateinit var windowManager: WindowManager
    private var pillView: android.view.View? = null
    private var panelView: android.view.View? = null
    private var touchHandler: BubbleTouchHandler? = null
    private var translateJob: Job? = null

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private val _uiState = MutableStateFlow(OverlayUiState())
    val uiState = _uiState.asStateFlow()

    private var isStopping = false
    private var panelCloseTime = 0L

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
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        showPill()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isRunning.set(false)
        translateJob?.cancel()
        touchHandler?.cancelAutoHide()
        touchHandler = null
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        serviceScope.cancel()
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
                    EdgePillTrigger()
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
            onLongPress = { stopWithAnimation() },
            onPositionChanged = {}
        )

        view.setOnTouchListener { _, event -> touchHandler?.handleTouchEvent(event) ?: false }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
            && !Settings.canDrawOverlays(this)) {
            Log.w("Overlay", "SYSTEM_ALERT_WINDOW permission not granted")
            return
        }

        pillView = view
        windowManager.addView(view, params)
        touchHandler?.positionAtRightEdge()
    }

    // ── Panel ──────────────────────────────────────────────────

    private fun togglePanel() {
        val now = System.currentTimeMillis()
        if (now - panelCloseTime < PANEL_TOGGLE_DEBOUNCE_MS) return

        if (_uiState.value.isPanelVisible) {
            hidePanel()
        } else {
            showPanel()
        }
    }

    private fun hidePanel() {
        cancelTranslate()
        _uiState.update { it.copy(isPanelVisible = false) }
        panelCloseTime = System.currentTimeMillis()
    }

    private fun showPanel() {
        if (panelView != null) {
            isStopping = false
            _uiState.update { it.copy(isPanelVisible = true) }
            return
        }
        val density = resources.displayMetrics.density
        val panelWidth = (resources.displayMetrics.widthPixels - 64 * density).toInt()

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
                    val state by uiState.collectAsState()
                    val context = this@OverlayService
                    TranslationPanelContent(
                        visible = state.isPanelVisible,
                        inputText = state.inputText,
                        onInputChange = { text -> _uiState.update { it.copy(inputText = text) } },
                        resultText = state.resultText,
                        isTranslating = state.isTranslating,
                        isModelDownloading = state.isModelDownloading,
                        error = state.errorMsg?.asString(context),
                        sourceLang = state.sourceLang,
                        targetLang = state.targetLang,
                        engine = state.engine,
                        onTranslate = { doTranslate() },
                        onSwap = { swapOverlayLanguages() },
                        onCopy = { copyResult() },
                        onDismiss = { hidePanel() },
                        onStop = {
                            _uiState.update { it.copy(isPanelVisible = false) }
                            isStopping = true
                        },
                        onLaunchEssay = {
                            _uiState.update { it.copy(isPanelVisible = false) }
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
                        onSourceLangChange = { lang -> _uiState.update { it.copy(sourceLang = lang) } },
                        onTargetLangChange = { lang -> _uiState.update { it.copy(targetLang = lang) } },
                        onEngineChange = { eng -> _uiState.update { it.copy(engine = eng) } }
                    )
                }
            }
        }

        panelView = view
        _uiState.update { it.copy(isPanelVisible = true) }
        windowManager.addView(view, params)

        panelView?.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_OUTSIDE) {
                hidePanel()
                true
            } else false
        }
    }

    // ── Stop flow ──────────────────────────────────────────────

    private fun stopWithAnimation() {
        cancelTranslate()
        if (_uiState.value.isPanelVisible) {
            isStopping = true
            _uiState.update { it.copy(isPanelVisible = false) }
            // onExitAnimationFinished will call stopSelf()
        } else {
            stopSelf()
        }
    }

    private fun cancelTranslate() {
        translateJob?.cancel()
        translateJob = null
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
            val bubbleSizePx = (36 * density).toInt()
            val marginPx = (6 * density).toInt()

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
        if (_uiState.value.isPanelVisible) {
            hidePanel()
        }
    }

    // ── Business logic ────────────────────────────────────────

    private fun doTranslate() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) {
            _uiState.update { it.copy(errorMsg = UiText.StringResource(R.string.translate_error_empty)) }
            return
        }

        cancelTranslate()
        translateJob = serviceScope.launch {
            _uiState.update { it.copy(isTranslating = true, isModelDownloading = false, errorMsg = null, resultText = "") }
            try {
                val engine = _uiState.value.engine

                // Show model downloading state for ML Kit
                if (engine == TranslationEngine.MLKIT) {
                    _uiState.update { it.copy(isModelDownloading = true) }
                }

                val result = translateUseCase(
                    text = text,
                    sourceLang = _uiState.value.sourceLang.displayName,
                    targetLang = _uiState.value.targetLang.displayName,
                    engine = engine,
                    thinkingLevelId = settingsDataStore.settings.first().translateThinkingLevel
                )

                result.fold(
                    onSuccess = { res -> _uiState.update { it.copy(resultText = res) } },
                    onFailure = { err -> _uiState.update { it.copy(errorMsg = UiText.DynamicString(err.message ?: getString(R.string.error_network))) } }
                )
            } catch (e: CancellationException) {
                // Cancelled — silent
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                _uiState.update { it.copy(errorMsg = UiText.StringResource(R.string.error_translate_timeout)) }
            } finally {
                _uiState.update { it.copy(isTranslating = false, isModelDownloading = false) }
            }
        }
    }

    private fun swapOverlayLanguages() {
        _uiState.update {
            it.copy(
                sourceLang = it.targetLang,
                targetLang = it.sourceLang,
                inputText = it.resultText,
                resultText = it.inputText
            )
        }
    }

    private fun copyResult() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("translation", _uiState.value.resultText))
    }
}
