package io.github.diarmaidob.anyfin.feature.home

import io.github.diarmaidob.anyfin.core.ui.uimodel.converter.MediaItemListItemUiModelConverter
import io.github.diarmaidob.anyfin.core.ui.uimodel.converter.ScreenStateUiModelConverter
import io.github.diarmaidob.anyfin.navigation.MediaItemListDestination
import javax.inject.Inject

class HomeScreenUiModelConverter @Inject constructor(
    private val homeSectionUiModelConverter: HomeSectionUiModelConverter,
    private val screenStateUiModelConverter: ScreenStateUiModelConverter
) {

    fun toUiModel(homeData: HomeData): HomeScreenUiModel {
        return HomeScreenUiModel(
            sectionUiModels = homeData.sections.map { homeSectionUiModelConverter.toUiModel(it) },
            screenStateUiModel = screenStateUiModelConverter.toUiModel(homeData.screenState, homeData.hasData)
        )
    }

}


class HomeSectionUiModelConverter @Inject constructor(
    private val mediaItemListItemUiModelConverter: MediaItemListItemUiModelConverter
) {

    fun toUiModel(entity: HomeSection): HomeSectionUiModel {
        val title = when (entity.type) {
            HomeSection.SectionType.RESUME -> "Continue Watching"
            HomeSection.SectionType.NEXT_UP -> "Next Up"
            HomeSection.SectionType.LATEST_MOVIES -> "Recently Added in Movies"
            HomeSection.SectionType.LATEST_SERIES -> "Recently Added in Shows"
        }
        val posterType = when (entity.type) {
            HomeSection.SectionType.RESUME -> HomeSectionUiModel.SectionPosterType.Resume
            HomeSection.SectionType.NEXT_UP -> HomeSectionUiModel.SectionPosterType.NextUp
            HomeSection.SectionType.LATEST_MOVIES -> HomeSectionUiModel.SectionPosterType.Poster
            HomeSection.SectionType.LATEST_SERIES -> HomeSectionUiModel.SectionPosterType.Poster
        }
        val navigationListType = when (entity.type) {
            HomeSection.SectionType.RESUME -> MediaItemListDestination.ListType.RESUME
            HomeSection.SectionType.NEXT_UP -> MediaItemListDestination.ListType.NEXT_UP
            HomeSection.SectionType.LATEST_MOVIES -> MediaItemListDestination.ListType.LATEST_MOVIES
            HomeSection.SectionType.LATEST_SERIES -> MediaItemListDestination.ListType.LATEST_SHOWS
        }
        return HomeSectionUiModel(
            id = entity.id,
            title = title,
            items = entity.items.map { mediaItemListItemUiModelConverter.toUiModel(it) },
            posterType = posterType,
            navigationListType = navigationListType
        )
    }

}