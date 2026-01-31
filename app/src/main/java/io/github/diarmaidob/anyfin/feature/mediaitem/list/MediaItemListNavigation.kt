package io.github.diarmaidob.anyfin.feature.mediaitem.list

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import io.github.diarmaidob.anyfin.feature.mediaitem.details.mediaItemDetailsDestination
import io.github.diarmaidob.anyfin.navigation.MediaItemListDestination
import io.github.diarmaidob.anyfin.navigation.PlayerDestination

fun NavGraphBuilder.mediaItemListDestination(
    navController: NavController,
    onNavigateToPlayer: (PlayerDestination) -> Unit
) {
    composable<MediaItemListDestination> { backStackEntry ->
        MediaItemListRoute(
            onNavigateToMediaItemDetails = { navController.navigate(it) },
            onNavigateUp = { navController.navigateUp() }
        )
    }

    mediaItemDetailsDestination(navController = navController, onNavigateToPlayer = onNavigateToPlayer)
}