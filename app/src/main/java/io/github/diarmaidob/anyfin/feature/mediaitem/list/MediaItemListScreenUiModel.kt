package io.github.diarmaidob.anyfin.feature.mediaitem.list

import io.github.diarmaidob.anyfin.core.ui.uimodel.MediaItemListItemUiModel
import io.github.diarmaidob.anyfin.core.ui.uimodel.ScreenStateUiModel
import io.github.diarmaidob.anyfin.navigation.MediaItemDetailsDestination

data class MediaItemListScreenUiModel(
    val title: String,
    val items: List<MediaItemListItemUiModel>,
    val screenState: ScreenStateUiModel<MediaItemListEvent>
)

sealed interface MediaItemListEvent {
    data class NavigateToMediaItemDetails(val dest: MediaItemDetailsDestination) : MediaItemListEvent
}