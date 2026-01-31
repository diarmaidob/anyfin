package io.github.diarmaidob.anyfin.navigation

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import io.github.diarmaidob.anyfin.feature.home.homeGraph
import io.github.diarmaidob.anyfin.feature.library.libraryGraph
import io.github.diarmaidob.anyfin.feature.settings.SettingsRoute

@Composable
fun TabbedNavigation(
    onNavigateToPlayer: (PlayerDestination) -> Unit,
    onLogout: () -> Unit,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    val isSelected = AppTab.isTabSelected(tab, currentDestination)

                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        selected = isSelected,
                        onClick = {
                            navController.navigate(tab.graph) {
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
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = HomeGraph,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
            enterTransition = { Transitions.enter(this) },
            exitTransition = { Transitions.exit(this) },
            popEnterTransition = { Transitions.popEnter(this) },
            popExitTransition = { Transitions.popExit(this) }
        ) {
            homeGraph(navController, onNavigateToPlayer)

            libraryGraph(navController, onNavigateToPlayer)

            navigation<SettingsGraph>(startDestination = SettingsDestination) {
                composable<SettingsDestination> {
                    SettingsRoute(
                        onLogout = onLogout
                    )
                }
            }
        }
    }
}