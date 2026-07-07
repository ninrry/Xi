package luzzr.xi.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.ExperimentalMaterial3Api

private val LightColorScheme = lightColorScheme(
    primary = WarmAccent,
    onPrimary = Ivory,
    primaryContainer = WarmAccentLight,
    onPrimaryContainer = BrownGray,
    secondary = BrownGrayLight,
    onSecondary = Ivory,
    tertiary = WarmAccentLight,
    onTertiary = BrownGray,
    error = Color(0xFFB08880),
    onError = Color.White,
    errorContainer = Color(0xFFE8DAD6),
    onErrorContainer = Color(0xFF5C2018),
    background = Ivory,
    onBackground = BrownGray,
    surface = Ivory,
    onSurface = BrownGray,
    surfaceVariant = IvoryDark,
    onSurfaceVariant = BrownGrayLight,
    outline = DividerColor,
    outlineVariant = Color(0xFFD0C8BE),
)

private val DarkColorScheme = darkColorScheme(
    primary = WarmAccentLight,
    onPrimary = BrownGray,
    primaryContainer = WarmAccent,
    onPrimaryContainer = Color.White,
    secondary = BrownGrayLight,
    onSecondary = Color.White,
    tertiary = WarmAccent,
    onTertiary = Ivory,
    error = Color(0xFFD4A8A0),
    onError = Color(0xFF5C2018),
    errorContainer = Color(0xFF7A3830),
    onErrorContainer = Color(0xFFE8DAD6),
    background = Color(0xFF1C1916),
    onBackground = Color(0xFFE8E2DA),
    surface = Color(0xFF1C1916),
    onSurface = Color(0xFFE8E2DA),
    surfaceVariant = Color(0xFF2A2520),
    onSurfaceVariant = Color(0xFFB8A68E),
    outline = Color(0xFF3D3530),
    outlineVariant = Color(0xFF5A5248),
)

@Composable
fun XiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity
            if (activity != null) {
                val window = activity.window
                window.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors
    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        @OptIn(ExperimentalMaterial3Api::class)
        MaterialTheme(
            colorScheme = colorScheme,
            typography = XiTypography,
            shapes = Shapes(
                extraSmall = AppShape.mini,
                small = AppShape.small,
                medium = AppShape.input,
                large = AppShape.card,
                extraLarge = AppShape.dialog
            ),
            content = {
                CompositionLocalProvider(
                    LocalRippleConfiguration provides null,
                    content = content
                )
            }
        )
    }
}