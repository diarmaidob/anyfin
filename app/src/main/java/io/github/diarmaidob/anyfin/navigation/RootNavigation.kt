package io.github.diarmaidob.anyfin.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.diarmaidob.anyfin.feature.login.LoginRoute
import io.github.diarmaidob.anyfin.feature.player.PlayerScreen

@Composable
fun RootNavigation(
    startDestination: Any,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { Transitions.enterRoot(this) },
        exitTransition = { Transitions.exitRoot(this) },
        popEnterTransition = { Transitions.enterRoot(this) },
        popExitTransition = { Transitions.exitRoot(this) }
    ) {
        composable<LoginDestination> {
            LoginRoute(
                onLoginSuccess = {
                    navController.navigate(DashboardDestination) {
                        popUpTo(0) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable<DashboardDestination> {
            TabbedNavigation(
                onNavigateToPlayer = { dest ->
                    navController.navigate(dest)
                },
                onLogout = {
                    navController.navigate(LoginDestination) {
                        popUpTo(0) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable<PlayerDestination> { entry ->
            PlayerScreen(
                onNavigateUp = { navController.navigateUp() }
            )
        }
    }
}