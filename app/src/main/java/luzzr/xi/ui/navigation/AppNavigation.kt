package luzzr.xi.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import luzzr.xi.feature.essay.EssayScreen
import luzzr.xi.feature.settings.SettingsScreen
import luzzr.xi.feature.translate.TranslateScreen

private fun getSlideDirection(
    initialRoute: String?,
    targetRoute: String?
): AnimatedContentTransitionScope.SlideDirection {
    val initialIdx = when (initialRoute) {
        Screen.Translate.route -> 0
        Screen.Essay.route -> 1
        Screen.Settings.route -> 2
        else -> 0
    }
    val targetIdx = when (targetRoute) {
        Screen.Translate.route -> 0
        Screen.Essay.route -> 1
        Screen.Settings.route -> 2
        else -> 0
    }
    return if (targetIdx > initialIdx) {
        AnimatedContentTransitionScope.SlideDirection.Left
    } else {
        AnimatedContentTransitionScope.SlideDirection.Right
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Translate.route,
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) }
    ) {
        composable(
            route = Screen.Translate.route,
            enterTransition = {
                val dir = getSlideDirection(initialState.destination.route, targetState.destination.route)
                slideIntoContainer(dir, animationSpec = tween(300))
            },
            exitTransition = {
                val dir = getSlideDirection(initialState.destination.route, targetState.destination.route)
                slideOutOfContainer(dir, animationSpec = tween(300))
            }
        ) { TranslateScreen() }

        composable(
            route = Screen.Essay.route,
            enterTransition = {
                val dir = getSlideDirection(initialState.destination.route, targetState.destination.route)
                slideIntoContainer(dir, animationSpec = tween(300))
            },
            exitTransition = {
                val dir = getSlideDirection(initialState.destination.route, targetState.destination.route)
                slideOutOfContainer(dir, animationSpec = tween(300))
            }
        ) { EssayScreen() }

        composable(
            route = Screen.Settings.route,
            enterTransition = {
                val dir = getSlideDirection(initialState.destination.route, targetState.destination.route)
                slideIntoContainer(dir, animationSpec = tween(300))
            },
            exitTransition = {
                val dir = getSlideDirection(initialState.destination.route, targetState.destination.route)
                slideOutOfContainer(dir, animationSpec = tween(300))
            }
        ) { SettingsScreen() }
    }
}
