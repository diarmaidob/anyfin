package io.github.diarmaidob.anyfin.feature.home

import androidx.compose.runtime.Immutable
import io.github.diarmaidob.anyfin.core.ui.uimodel.MediaItemListItemUiModel
import io.github.diarmaidob.anyfin.core.ui.uimodel.ScreenStateUiModel
import io.github.diarmaidob.anyfin.navigation.MediaItemDetailsDestination
import io.github.diarmaidob.anyfin.navigation.MediaItemListDestination
import io.github.diarmaidob.anyfin.navigation.PlayerDestination

@Immutable
data class HomeScreenUiModel(
    val title: String = "Home",
    val sectionUiModels: List<HomeSectionUiModel> = emptyList(),
    val screenStateUiModel: ScreenStateUiModel<HomeEvent> = ScreenStateUiModel()
)

@Immutable
data class HomeSectionUiModel(
    val id: String,
    val title: String,
    val items: List<MediaItemListItemUiModel>,
    val posterType: SectionPosterType,
    val navigationListType: MediaItemListDestination.ListType?
) {

    enum class SectionPosterType { Resume, NextUp, Poster }
}

sealed interface HomeEvent {
    data class NavigateToMediaItemDetails(val dest: MediaItemDetailsDestination) : HomeEvent
    data class NavigateToPlayer(val dest: PlayerDestination) : HomeEvent
    data class NavigateToMediaItemList(val dest: MediaItemListDestination) : HomeEvent
}