package io.github.diarmaidob.anyfin.feature.mediaitem.details

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import io.github.diarmaidob.anyfin.navigation.MediaItemDetailsDestination
import io.github.diarmaidob.anyfin.navigation.PlayerDestination

fun NavGraphBuilder.mediaItemDetailsDestination(
    navController: NavController,
    onNavigateToPlayer: (PlayerDestination) -> Unit
) {
    composable<MediaItemDetailsDestination> { backStackEntry ->

        MediaItemDetailsRoute(
            onNavigateToPlayer = onNavigateToPlayer,
            onNavigateUp = { navController.navigateUp() },
            onNavigateToChild = { navController.navigate(it) }
        )
    }
}