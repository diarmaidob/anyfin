package io.github.diarmaidob.anyfin.navigation

import androidx.annotation.Keep
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import kotlinx.serialization.Serializable


@Serializable
object LoginDestination

@Serializable
object DashboardDestination

@Serializable
data class PlayerDestination(
    val itemId: String,
    val audioStreamIndex: Int? = null,
    val subtitleStreamIndex: Int? = null
)

@Serializable
object HomeGraph

@Serializable
object LibraryGraph

@Serializable
object SettingsGraph

@Serializable
object HomeDestination

@Serializable
object LibraryDestination

@Serializable
object SettingsDestination

@Serializable
data class MediaItemDetailsDestination(val itemId: String, val title: String)

@Serializable
data class MediaItemListDestination(
    val listId: String,
    val title: String,
    val type: ListType
) {
    @Keep
    enum class ListType {
        RESUME,
        NEXT_UP,
        LATEST_MOVIES,
        LATEST_SHOWS,
        LIBRARIES,
        LIBRARY_CONTENTS
    }
}

enum class AppTab(
    val label: String,
    val icon: ImageVector,
    val graph: Any
) {
    Home("Home", Icons.Filled.Home, HomeGraph),
    Library("Library", Icons.Filled.VideoLibrary, LibraryGraph),
    Settings("Settings", Icons.Filled.Settings, SettingsGraph);

    companion object {
        fun isTabSelected(tab: AppTab, destination: NavDestination?): Boolean {
            return destination?.hierarchy?.any { it.hasRoute(tab.graph::class) } == true
        }

        fun getTabOwner(destination: NavDestination?): AppTab? {
            return entries.find { isTabSelected(it, destination) }
        }
    }
}