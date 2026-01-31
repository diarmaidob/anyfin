package io.github.diarmaidob.anyfin.feature.library

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import io.github.diarmaidob.anyfin.feature.mediaitem.details.mediaItemDetailsDestination
import io.github.diarmaidob.anyfin.feature.mediaitem.list.mediaItemListDestination
import io.github.diarmaidob.anyfin.navigation.LibraryDestination
import io.github.diarmaidob.anyfin.navigation.LibraryGraph
import io.github.diarmaidob.anyfin.navigation.PlayerDestination

fun NavGraphBuilder.libraryGraph(
    navController: NavController,
    onNavigateToPlayer: (PlayerDestination) -> Unit,
) {
    navigation<LibraryGraph>(startDestination = LibraryDestination) {
        composable<LibraryDestination> {
            LibraryRoute(
                onNavigateToMediaItemList = { navController.navigate(it) }
            )
        }

        mediaItemListDestination(navController, onNavigateToPlayer)

        mediaItemDetailsDestination(navController, onNavigateToPlayer)
    }
}