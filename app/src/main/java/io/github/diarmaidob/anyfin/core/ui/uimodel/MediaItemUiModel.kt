package io.github.diarmaidob.anyfin.core.ui.uimodel

import io.github.diarmaidob.anyfin.core.entity.JellyfinImage

data class MediaItemCommonUiModel(
    val id: String,
    val name: String,
    val type: MediaItemUiType,
    val images: MediaItemImageSetUiModel
) {
    enum class MediaItemUiType {
        MOVIE,
        SERIES,
        SEASON,
        EPISODE,
        LIBRARY,
        UNKNOWN
    }
}

data class MediaItemImageSetUiModel(
    val primary: JellyfinImage,
    val backdrop: JellyfinImage,
    val logo: JellyfinImage?,
    val thumb: JellyfinImage?
)

data class MediaItemListItemUiModel(
    val common: MediaItemCommonUiModel,
    val subtitle: String?,
    val isPlayed: Boolean,
    val isFavorite: Boolean,
    val playbackProgress: Float
) {
    val id = common.id
}


data class MediaItemDetailsUiModel(
    val common: MediaItemCommonUiModel,
    val overview: String,
    val tagline: String?,
    val officialRating: String?,
    val starRating: String?,
    val specs: List<String>,
    val isFavorite: Boolean,
    val playButton: PlayButtonUiModel
) {
    val id = common.id
}

data class PlayButtonUiModel(
    val label: String,
    val action: PlayAction,
    val isEnabled: Boolean
)

sealed interface PlayAction {
    data class PlayItem(val itemId: String) : PlayAction
    data class NavigateToChild(val itemId: String) : PlayAction
    data object None : PlayAction
}