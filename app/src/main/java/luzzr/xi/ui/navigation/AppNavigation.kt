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
import luzzr.xi.core.ui.theme.MotionTokens
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
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Translate.route
) {
    val slideDur = MotionTokens.durMedium

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(MotionTokens.durMedium)) },
        exitTransition = { fadeOut(animationSpec = tween(MotionTokens.durMedium)) }
    ) {
        composable(
            route = Screen.Translate.route,
            enterTransition = {
                slideIntoContainer(getSlideDirection(initialState.destination.route, targetState.destination.route), animationSpec = tween(slideDur))
            },
            exitTransition = {
                slideOutOfContainer(getSlideDirection(initialState.destination.route, targetState.destination.route), animationSpec = tween(slideDur))
            }
        ) { TranslateScreen() }

        composable(
            route = Screen.Essay.route,
            enterTransition = {
                slideIntoContainer(getSlideDirection(initialState.destination.route, targetState.destination.route), animationSpec = tween(slideDur))
            },
            exitTransition = {
                slideOutOfContainer(getSlideDirection(initialState.destination.route, targetState.destination.route), animationSpec = tween(slideDur))
            }
        ) { EssayScreen() }

        composable(
            route = Screen.Settings.route,
            enterTransition = {
                slideIntoContainer(getSlideDirection(initialState.destination.route, targetState.destination.route), animationSpec = tween(slideDur))
            },
            exitTransition = {
                slideOutOfContainer(getSlideDirection(initialState.destination.route, targetState.destination.route), animationSpec = tween(slideDur))
            }
        ) { SettingsScreen() }
    }
}
