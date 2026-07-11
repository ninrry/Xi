package luzzr.xi

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.content.Intent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import luzzr.xi.ui.XiAppUI
import luzzr.xi.ui.navigation.Screen
import luzzr.xi.core.ui.theme.XiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val startScreenFlow = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val contentView = splashScreenView.view
            val iconView = splashScreenView.iconView

            // Icon: scale up + fade out
            if (iconView != null) {
                iconView.animate()
                    .scaleX(1.15f)
                    .scaleY(1.15f)
                    .alpha(0f)
                    .setDuration(400L)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            }

            // Background: fade out, then remove splash
            contentView.animate()
                .alpha(0f)
                .setDuration(400L)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        splashScreenView.remove()
                    }
                })
                .start()
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        startScreenFlow.value = intent.getStringExtra("target_screen")?.let { screenName ->
                when (screenName) {
                    "essay" -> Screen.Essay.route
                    "history" -> Screen.History.route
                    "settings" -> Screen.Settings.route
                    else -> Screen.Translate.route
                }
        }
        setContent {
            XiTheme {
                val startScreen by startScreenFlow.collectAsStateWithLifecycle()
                XiAppUI(
                    startScreen = startScreen,
                    onStartScreenHandled = { startScreenFlow.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        startScreenFlow.value = intent.getStringExtra("target_screen")
    }
}
