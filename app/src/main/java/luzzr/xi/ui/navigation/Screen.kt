package luzzr.xi.ui.navigation

import androidx.annotation.StringRes
import luzzr.xi.R

sealed class Screen(
    val route: String,
    @StringRes val titleResId: Int
) {
    data object Translate : Screen("translate", R.string.tab_translate)
    data object Essay : Screen("essay", R.string.tab_essay)
    data object History : Screen("history", R.string.tab_history)
    data object Settings : Screen("settings", R.string.tab_settings)
}

val bottomNavItems = listOf(Screen.Translate, Screen.Essay, Screen.History, Screen.Settings)
