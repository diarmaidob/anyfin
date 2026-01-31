package io.github.diarmaidob.anyfin.feature.mediaitem.details

import io.github.diarmaidob.anyfin.core.ui.uimodel.MediaItemDetailsUiModel
import io.github.diarmaidob.anyfin.core.ui.uimodel.MediaItemListItemUiModel
import io.github.diarmaidob.anyfin.core.ui.uimodel.ScreenStateUiModel
import io.github.diarmaidob.anyfin.navigation.MediaItemDetailsDestination
import io.github.diarmaidob.anyfin.navigation.PlayerDestination

sealed interface MediaItemDetailsEvent {
    data class NavigateToPlayer(val dest: PlayerDestination) : MediaItemDetailsEvent
    data class NavigateToChild(val dest: MediaItemDetailsDestination) : MediaItemDetailsEvent
}

enum class ChildLayoutType { Seasons, Episodes, Default }

data class TrackUiModel(
    val index: Int,
    val name: String,
    val isSelected: Boolean
)

data class MediaItemDetailsScreenUiModel(
    val title: String,
    val screenStateUiModel: ScreenStateUiModel<MediaItemDetailsEvent> = ScreenStateUiModel(),

    val details: MediaItemDetailsUiModel? = null,
    val children: List<MediaItemListItemUiModel> = emptyList(),
    val childrenTitle: String = "",
    val childLayoutType: ChildLayoutType = ChildLayoutType.Default,

    val hasMediaSources: Boolean = false,
    val audioTracks: List<TrackUiModel> = emptyList(),
    val subtitleTracks: List<TrackUiModel> = emptyList(),
    val selectedAudioName: String = "Default",
    val selectedSubtitleName: String = "Off"
)