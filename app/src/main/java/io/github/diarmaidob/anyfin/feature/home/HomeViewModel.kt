package io.github.diarmaidob.anyfin.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.diarmaidob.anyfin.core.common.ScreenStateDelegate
import io.github.diarmaidob.anyfin.core.entity.ScreenState
import io.github.diarmaidob.anyfin.core.ui.uimodel.MediaItemListItemUiModel
import io.github.diarmaidob.anyfin.navigation.MediaItemDetailsDestination
import io.github.diarmaidob.anyfin.navigation.MediaItemListDestination
import io.github.diarmaidob.anyfin.navigation.PlayerDestination
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject


data class HomeData(
    val sections: List<HomeSection>,
    val screenState: ScreenState<HomeEvent>
) {
    val hasData = sections.isNotEmpty()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val useCase: GetHomeSectionsUseCase,
    private val screenStateDelegate: ScreenStateDelegate<HomeEvent>,
    private val homeScreenUiModelConverter: HomeScreenUiModelConverter
) : ViewModel() {

    val uiModel: StateFlow<HomeScreenUiModel> = combine(
        useCase.observe(),
        screenStateDelegate.bind(viewModelScope)
    ) { data, screenState ->
        HomeData(sections = data, screenState = screenState)
    }.map {
        homeScreenUiModelConverter.toUiModel(it)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeScreenUiModel(),
    )

    private fun performRefresh(force: Boolean) {
        viewModelScope.launch {
            screenStateDelegate.load(isManualRefresh = force) {
                useCase.refresh()
            }
        }
    }

    fun onScreenVisible() {
        performRefresh(false)
    }

    fun onPullToRefresh() {
        performRefresh(true)
    }

    fun onEventConsumed(eventId: String) {
        screenStateDelegate.consumeEvent(eventId)
    }

    fun onSectionHeaderClick(section: HomeSectionUiModel) {
        if (section.navigationListType == null) return

        screenStateDelegate.performThrottledAction {
            val route = MediaItemListDestination(
                listId = section.id,
                title = section.title,
                type = section.navigationListType
            )
            screenStateDelegate.sendEvent(HomeEvent.NavigateToMediaItemList(route))
        }
    }

    fun onItemClick(item: MediaItemListItemUiModel) {
        screenStateDelegate.performThrottledAction {
            val dest = MediaItemDetailsDestination(itemId = item.id, title = item.common.name)
            screenStateDelegate.sendEvent(HomeEvent.NavigateToMediaItemDetails(dest))
        }
    }

    fun onResumeClick(item: MediaItemListItemUiModel) {
        screenStateDelegate.performThrottledAction {
            val dest = PlayerDestination(itemId = item.id)
            screenStateDelegate.sendEvent(HomeEvent.NavigateToPlayer(dest))
        }
    }


}