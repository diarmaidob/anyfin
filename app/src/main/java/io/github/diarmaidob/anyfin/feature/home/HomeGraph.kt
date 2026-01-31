package io.github.diarmaidob.anyfin.feature.home

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import io.github.diarmaidob.anyfin.feature.mediaitem.details.mediaItemDetailsDestination
import io.github.diarmaidob.anyfin.navigation.HomeDestination
import io.github.diarmaidob.anyfin.navigation.HomeGraph
import io.github.diarmaidob.anyfin.navigation.PlayerDestination

fun NavGraphBuilder.homeGraph(
    navController: NavController,
    onNavigateToPlayer: (PlayerDestination) -> Unit,
) {
    navigation<HomeGraph>(startDestination = HomeDestination) {

        composable<HomeDestination> {
            HomeRoute(
                onNavigateToDetails = { navController.navigate(it) },
                onNavigateToSection = { navController.navigate(it) },
                onNavigateToPlayer = onNavigateToPlayer
            )
        }

        mediaItemDetailsDestination(navController, onNavigateToPlayer)
    }
}