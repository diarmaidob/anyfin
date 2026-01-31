package io.github.diarmaidob.anyfin.feature.library

import io.github.diarmaidob.anyfin.core.ui.uimodel.converter.MediaItemListItemUiModelConverter
import io.github.diarmaidob.anyfin.core.ui.uimodel.converter.ScreenStateUiModelConverter
import javax.inject.Inject

class LibraryScreenUiModelConverter @Inject constructor(
    private val mediaItemListItemUiModelConverter: MediaItemListItemUiModelConverter,
    private val screenUiModelConverter: ScreenStateUiModelConverter
) {

    fun toUiModel(data: LibraryScreenData): LibraryScreenUiModel {
        return LibraryScreenUiModel(
            screenStateUiModel = screenUiModelConverter.toUiModel(state = data.screenState, hasData = data.hasData),
            items = data.items.map { mediaItemListItemUiModelConverter.toUiModel(it) }
        )
    }

}