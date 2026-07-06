package luzzr.xi.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import luzzr.xi.core.ui.theme.AppShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import luzzr.xi.ui.navigation.AppNavigation
import luzzr.xi.ui.navigation.Screen
import luzzr.xi.ui.navigation.bottomNavItems
import luzzr.xi.core.ui.theme.AbstractIcons
import androidx.compose.material3.MaterialTheme

@Composable
fun XiAppUI(
    startScreen: String? = null,
    onStartScreenHandled: () -> Unit = {}
) {
    val navController = rememberNavController()

    LaunchedEffect(startScreen) {
        if (startScreen != null) {
            navController.navigate(startScreen) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
            onStartScreenHandled()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            AppNavigation(
                navController = navController,
                modifier = Modifier.fillMaxSize()
            )

            // Floating capsule navigation bar — wider, more generous
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .width(260.dp)
                    .clip(AppShape.button)
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.92f))
                    .border(0.5.dp, MaterialTheme.colorScheme.outline, AppShape.button)
                    .padding(vertical = 6.dp, horizontal = 12.dp)
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        FloatingNavItem(
                            screen = screen,
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.FloatingNavItem(
    screen: Screen,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = luzzr.xi.core.ui.theme.MotionTokens.springDefault(),
        label = "nav_item_scale"
    )

    val tintColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)

    // Selected indicator background
    val indicatorModifier = if (selected) {
        Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(AppShape.small)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
    } else {
        Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
    }

    Box(
        modifier = Modifier
            .weight(1f)
            .then(indicatorModifier)
            .clip(AppShape.small)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 6.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Box(modifier = Modifier.size(24.dp)) {
                when (screen) {
                    Screen.Translate -> AbstractIcons.Translate(modifier = Modifier.fillMaxSize(), tint = tintColor)
                    Screen.Essay -> AbstractIcons.Edit(modifier = Modifier.fillMaxSize(), tint = tintColor)
                    Screen.Settings -> AbstractIcons.Settings(modifier = Modifier.fillMaxSize(), tint = tintColor)
                }
            }
            Text(
                text = stringResource(screen.titleResId),
                fontSize = 11.sp,
                color = tintColor,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}
