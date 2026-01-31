package io.github.diarmaidob.anyfin.feature.mediaitem.details

import io.github.diarmaidob.anyfin.core.entity.MediaItem
import io.github.diarmaidob.anyfin.core.entity.ScreenState
import io.github.diarmaidob.anyfin.core.ui.uimodel.converter.MediaItemDetailsUiModelConverter
import io.github.diarmaidob.anyfin.core.ui.uimodel.converter.MediaItemListItemUiModelConverter
import io.github.diarmaidob.anyfin.core.ui.uimodel.converter.ScreenStateUiModelConverter
import javax.inject.Inject

class MediaItemDetailsScreenUiModelConverter @Inject constructor(
    private val screenStateUiModelConverter: ScreenStateUiModelConverter,
    private val mediaItemDetailsUiModelConverter: MediaItemDetailsUiModelConverter,
    private val mediaItemListItemUiModelConverter: MediaItemListItemUiModelConverter
) {

    fun toUiModel(
        data: MediaItemDetailsData,
        audioIndex: Int?,
        subtitleIndex: Int?,
        screenState: ScreenState<MediaItemDetailsEvent>,
        title: String
    ): MediaItemDetailsScreenUiModel = with(data) {
        val defaultAudioIndex = data.streamOptions?.audio?.firstOrNull()?.indexNumber?.toInt()
        val activeAudio = audioIndex ?: defaultAudioIndex

        val screenStateUiModel = screenStateUiModelConverter.toUiModel(state = screenState, hasData = true)

        val streams = this.streamOptions

        val audioTracks = streams?.audio?.map {
            TrackUiModel(
                index = it.indexNumber.toInt(),
                name = it.displayTitle ?: it.language ?: "Unknown",
                isSelected = it.indexNumber.toInt() == activeAudio
            )
        } ?: emptyList()

        val selectedAudioName = audioTracks.find { it.isSelected }?.name ?: "Default"

        val subTracks = streams?.subtitles?.map {
            TrackUiModel(
                index = it.indexNumber.toInt(),
                name = it.displayTitle ?: it.language ?: "Unknown",
                isSelected = it.indexNumber.toInt() == subtitleIndex
            )
        } ?: emptyList()

        val selectedSubName = subTracks.find { it.isSelected }?.name ?: "Off"

        val (layoutType, childTitle) = when (this.item) {
            is MediaItem.Series -> ChildLayoutType.Seasons to "Seasons"
            is MediaItem.Season -> ChildLayoutType.Episodes to "Episodes"
            else -> ChildLayoutType.Default to ""
        }

        return MediaItemDetailsScreenUiModel(
            title = title,
            screenStateUiModel = screenStateUiModel,
            details = mediaItemDetailsUiModelConverter.toUiModel(item = item, nextUp = nextUp),
            children = children.map { mediaItemListItemUiModelConverter.toUiModel(it) },
            childrenTitle = childTitle,
            childLayoutType = layoutType,
            hasMediaSources = streams != null,
            audioTracks = audioTracks,
            subtitleTracks = subTracks,
            selectedAudioName = selectedAudioName,
            selectedSubtitleName = selectedSubName
        )
    }

}