package luzzr.xi.core.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring


object MotionTokens {
    // 默认回弹动画，用于多数交互组件（如悬浮球、菜单展开）
    fun <T> springDefault() = spring<T>(
        dampingRatio = 0.6f,
        stiffness = 300f
    )

    // 较柔和的回弹，适用于卡片展开、较大的UI变换
    fun <T> springGentle() = spring<T>(
        dampingRatio = 0.7f,
        stiffness = 200f
    )

}
