package luzzr.xi.feature.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import luzzr.xi.MainActivity
import luzzr.xi.R
import luzzr.xi.core.datastore.SettingsDataStore
import luzzr.xi.domain.model.SupportedLanguage
import luzzr.xi.domain.model.TranslationEngine
import luzzr.xi.data.repository.MlKitTranslator
import luzzr.xi.data.repository.TranslationRepository
import luzzr.xi.core.ui.theme.XiTheme
import luzzr.xi.feature.overlay.ui.OverlayComponents
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
    @Inject lateinit var translationRepo: TranslationRepository
    @Inject lateinit var mlKitTranslator: MlKitTranslator

    private lateinit var windowManager: WindowManager
    private var bubbleView: android.view.View? = null
    private var panelView: android.view.View? = null
    private var bubbleParams: WindowManager.LayoutParams? = null

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private val _uiState = MutableStateFlow(OverlayUiState())
    val uiState = _uiState.asStateFlow()
    
    private var isStopping = false
    private var autoHideJob: kotlinx.coroutines.Job? = null
    private var isHalfHidden = false

    companion object {
        val isRunning = java.util.concurrent.atomic.AtomicBoolean(false)
        private const val CHANNEL_ID = "overlay_channel"
        private const val NOTIFICATION_ID = 1001
        private const val CLICK_THRESHOLD = 10
        private const val LONG_PRESS_DURATION = 500L
        const val ACTION_STOP = "luzzr.xi.service.OVERLAY_STOP"
    }

    override fun onCreate() {
        isRunning.set(true)
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        showBubble()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isRunning.set(false)
        autoHideJob?.cancel()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        serviceScope.cancel()
        removeViewsPhysical()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.overlay_notification_channel),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.overlay_notification_text)
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val closeIntent = Intent(this, OverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val closePendingIntent = PendingIntent.getService(
            this, 1, closeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_send)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.overlay_stop), closePendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun showBubble() {
        val density = resources.displayMetrics.density
        val bubbleSizePx = (44 * density).toInt()
        val screenWidth = resources.displayMetrics.widthPixels
        val displayHeight = resources.displayMetrics.heightPixels
        val statusBarHeight = run {
            val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
            if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
        }

        val params = WindowManager.LayoutParams(
            bubbleSizePx,
            bubbleSizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenWidth - bubbleSizePx - (8 * density).toInt()
            y = (resources.displayMetrics.heightPixels / 3)
            preferredRefreshRate = 120f
        }
        bubbleParams = params

        val view = androidx.compose.ui.platform.ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
            setContent {
                XiTheme {
                    OverlayComponents.FloatingBubbleContent(
                        onClick = { togglePanel() }
                    )
                }
            }
        }

        fun updateBubblePositionAndAlpha(fullyVisible: Boolean) {
            val normalX = if (params.x + bubbleSizePx / 2 < screenWidth / 2) {
                (8 * density).toInt()
            } else {
                screenWidth - bubbleSizePx - (8 * density).toInt()
            }
            if (fullyVisible) {
                params.x = normalX
                params.alpha = 1.0f
            } else {
                if (normalX < screenWidth / 2) {
                    params.x = -bubbleSizePx / 2
                } else {
                    params.x = screenWidth - bubbleSizePx / 2
                }
                params.alpha = 0.4f
            }
            try { windowManager.updateViewLayout(view, params) } catch (e: Exception) { Log.w("Overlay", "updateViewLayout failed", e) }
        }

        fun resetAutoHideTimer() {
            autoHideJob?.cancel()
            autoHideJob = serviceScope.launch {
                kotlinx.coroutines.delay(4000L)
                isHalfHidden = true
                updateBubblePositionAndAlpha(fullyVisible = false)
            }
        }

        var downTime = 0L
        var isDragging = false
        var initialTouchX = 0f
        var initialTouchY = 0f
        var initialX = 0
        var initialY = 0

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    initialX = params.x
                    initialY = params.y
                    downTime = System.currentTimeMillis()
                    isDragging = false
                    autoHideJob?.cancel()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                    if (dist > CLICK_THRESHOLD) {
                        if (!isDragging) {
                            isDragging = true
                            if (isHalfHidden) {
                                isHalfHidden = false
                                updateBubblePositionAndAlpha(fullyVisible = true)
                                initialX = params.x
                            }
                        }
                    }
                    if (isDragging) {
                        val offsetX = event.rawX - initialTouchX
                        val offsetY = event.rawY - initialTouchY
                        params.x = (initialX + offsetX).toInt()
                        params.y = (initialY + offsetY).toInt()
                        params.y = params.y.coerceIn(0, displayHeight - bubbleSizePx - statusBarHeight)
                        params.x = params.x.coerceIn(0, screenWidth - bubbleSizePx)
                        windowManager.updateViewLayout(view, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        val elapsed = System.currentTimeMillis() - downTime
                        if (elapsed >= LONG_PRESS_DURATION) {
                            stopSelf()
                        } else {
                            if (isHalfHidden) {
                                isHalfHidden = false
                                updateBubblePositionAndAlpha(fullyVisible = true)
                            }
                            togglePanel()
                        }
                    } else {
                        snapToEdge(params, screenWidth, bubbleSizePx, density)
                        windowManager.updateViewLayout(view, params)
                    }
                    resetAutoHideTimer()
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    if (isDragging) {
                        snapToEdge(params, screenWidth, bubbleSizePx, density)
                        windowManager.updateViewLayout(view, params)
                    }
                    resetAutoHideTimer()
                    true
                }
                else -> false
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Log.w("Overlay", "SYSTEM_ALERT_WINDOW permission not granted, cannot show bubble")
            return
        }

        bubbleView = view
        windowManager.addView(view, params)
        resetAutoHideTimer()
    }

    private fun snapToEdge(params: WindowManager.LayoutParams, screenWidth: Int, sizePx: Int, density: Float) {
        val centerX = params.x + sizePx / 2
        val margin = (8 * density).toInt()
        params.x = if (centerX < screenWidth / 2) {
            margin
        } else {
            screenWidth - sizePx - margin
        }
    }

    private fun togglePanel() {
        if (_uiState.value.isPanelVisible) {
            _uiState.update { it.copy(isPanelVisible = false) }
        } else {
            showPanel()
        }
    }

    private fun hidePanel() {
        _uiState.update { it.copy(isPanelVisible = false) }
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
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            x = 0
            y = 0
            preferredRefreshRate = 120f
        }

        val view = androidx.compose.ui.platform.ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
            setContent {
                XiTheme {
                    val state by uiState.collectAsState()
                    OverlayComponents.TranslationPanelContent(
                        visible = state.isPanelVisible,
                        inputText = state.inputText,
                        onInputChange = { text -> _uiState.update { it.copy(inputText = text) } },
                        resultText = state.resultText,
                        isTranslating = state.isTranslating,
                        error = state.errorMsg,
                        sourceLang = state.sourceLang,
                        targetLang = state.targetLang,
                        engine = state.engine,
                        onTranslate = { doTranslate() },
                        onSwap = { swapOverlayLanguages() },
                        onCopy = { copyResult() },
                        onDismiss = { _uiState.update { it.copy(isPanelVisible = false) } },
                        onStop = {
                            _uiState.update { it.copy(isPanelVisible = false) }
                            isStopping = true
                        },
                        onLaunchEssay = {
                            _uiState.update { it.copy(isPanelVisible = false) }
                            val mainIntent = Intent(this@OverlayService, MainActivity::class.java).apply {
                                putExtra("target_screen", "essay")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            }
                            try {
                                startActivity(mainIntent)
                            } catch (e: Exception) {
                                Log.e("OverlayService", "Failed to launch MainActivity", e)
                            }
                        },
                        onExitAnimationFinished = {
                            removePanelPhysical()
                            if (isStopping && !_uiState.value.isPanelVisible) {
                                stopSelf()
                            }
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
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                hidePanel()
                true
            } else false
        }
    }

    private fun removePanelPhysical() {
        panelView?.let {
            try {
                if (it.isAttachedToWindow) windowManager.removeView(it)
            } catch (e: IllegalArgumentException) {
                Log.w("Overlay", "removeView failed", e)
            }
        }
        panelView = null
    }

    private fun removeViewsPhysical() {
        bubbleView?.let {
            try {
                if (it.isAttachedToWindow) windowManager.removeView(it)
            } catch (e: IllegalArgumentException) {
                Log.w("Overlay", "removeView failed", e)
            }
        }
        removePanelPhysical()
        bubbleView = null
    }

    private fun doTranslate() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) {
            _uiState.update { it.copy(errorMsg = getString(R.string.translate_error_empty)) }
            return
        }

        serviceScope.launch {
            _uiState.update { it.copy(isTranslating = true, errorMsg = null, resultText = "") }
            try {
                val s = settingsDataStore.settings.first()
                val engine = TranslationEngine.fromId(s.translationEngine)

                val result = kotlinx.coroutines.withTimeout(30_000L) {
                    if (engine == TranslationEngine.MLKIT) {
                        mlKitTranslator.translate(
                            text = text,
                            sourceLang = _uiState.value.sourceLang.displayName,
                            targetLang = _uiState.value.targetLang.displayName
                        )
                    } else {
                        translationRepo.translate(
                            text = text,
                            sourceLang = _uiState.value.sourceLang.displayName,
                            targetLang = _uiState.value.targetLang.displayName,
                            reasoningEffort = s.translateThinkingLevel
                        )
                    }
                }

                result.fold(
                    onSuccess = { res -> _uiState.update { it.copy(resultText = res) } },
                    onFailure = { err -> _uiState.update { it.copy(errorMsg = err.message ?: getString(R.string.error_network)) } }
                )
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                _uiState.update { it.copy(errorMsg = getString(R.string.error_translate_timeout)) }
            } finally {
                _uiState.update { it.copy(isTranslating = false) }
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
