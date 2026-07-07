package luzzr.xi.core.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut

object MotionTokens {
    fun <T> springDefault() = spring<T>(
        dampingRatio = 0.65f,
        stiffness = 450f
    )

    fun <T> springGentle() = spring<T>(
        dampingRatio = 0.75f,
        stiffness = 320f
    )

    fun <T> springSnappy() = spring<T>(
        dampingRatio = 0.55f,
        stiffness = 600f
    )

    const val durShort = 150
    const val durMedium = 300
    const val durLong = 500

    fun <T> tweenShort() = tween<T>(durShort)
    fun <T> tweenMedium() = tween<T>(durMedium)
    fun <T> tweenLong() = tween<T>(durLong)
    fun <T> tweenShortEasing() = tween<T>(durShort, easing = EaseOut)
    fun <T> tweenMediumEasing() = tween<T>(durMedium, easing = EaseInOut)
}
