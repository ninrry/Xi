package luzzr.xi

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kotlinx.coroutines.flow.MutableStateFlow
import luzzr.xi.ui.XiAppUI
import luzzr.xi.core.ui.theme.XiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val startScreenFlow = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        startScreenFlow.value = intent?.getStringExtra("target_screen")
        setContent {
            XiTheme {
                val startScreen by startScreenFlow.collectAsState()
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
