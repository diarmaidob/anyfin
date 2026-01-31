package io.github.diarmaidob.anyfin.feature.library

import io.github.diarmaidob.anyfin.core.ui.uimodel.MediaItemListItemUiModel
import io.github.diarmaidob.anyfin.core.ui.uimodel.ScreenStateUiModel
import io.github.diarmaidob.anyfin.navigation.MediaItemListDestination

data class LibraryScreenUiModel(
    val title: String = "Library",
    val items: List<MediaItemListItemUiModel> = emptyList(),
    val screenStateUiModel: ScreenStateUiModel<LibraryEvent> = ScreenStateUiModel()
)

sealed interface LibraryEvent {
    data class NavigateToLibraryContents(val dest: MediaItemListDestination) : LibraryEvent
}