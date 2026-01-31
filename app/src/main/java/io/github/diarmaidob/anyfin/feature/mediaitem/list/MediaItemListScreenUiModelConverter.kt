package io.github.diarmaidob.anyfin.feature.mediaitem.list

import io.github.diarmaidob.anyfin.core.ui.uimodel.converter.MediaItemListItemUiModelConverter
import io.github.diarmaidob.anyfin.core.ui.uimodel.converter.ScreenStateUiModelConverter
import javax.inject.Inject

class MediaItemListScreenUiModelConverter @Inject constructor(
    private val screenStateUiModelConverter: ScreenStateUiModelConverter,
    private val mediaItemListItemUiModelConverter: MediaItemListItemUiModelConverter
) {
    fun toUiModel(data: MediaItemListScreenData): MediaItemListScreenUiModel = with(data) {
        return MediaItemListScreenUiModel(
            title = title,
            screenState = screenStateUiModelConverter.toUiModel(state = screenState, hasData = hasData),
            items = items.map { mediaItemListItemUiModelConverter.toUiModel(it) }
        )
    }
}