package io.github.diarmaidob.anyfin.feature.mediaitem.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.diarmaidob.anyfin.core.common.ScreenStateDelegate
import io.github.diarmaidob.anyfin.core.entity.MediaItem
import io.github.diarmaidob.anyfin.core.entity.MediaItemQuery
import io.github.diarmaidob.anyfin.core.entity.ScreenState
import io.github.diarmaidob.anyfin.core.ui.uimodel.MediaItemListItemUiModel
import io.github.diarmaidob.anyfin.core.ui.uimodel.ScreenStateUiModel
import io.github.diarmaidob.anyfin.navigation.MediaItemDetailsDestination
import io.github.diarmaidob.anyfin.navigation.MediaItemListDestination
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MediaItemListScreenData(
    val title: String,
    val items: List<MediaItem>,
    val screenState: ScreenState<MediaItemListEvent>
) {
    val hasData = items.isNotEmpty()
}

@HiltViewModel
class MediaItemListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val useCase: GetMediaItemListDataUseCase,
    private val uiModelConverter: MediaItemListScreenUiModelConverter,
    private val queryFactory: MediaItemListQueryFactory,
    private val screenDelegate: ScreenStateDelegate<MediaItemListEvent>,
) : ViewModel() {

    private val destination = savedStateHandle.toRoute<MediaItemListDestination>()
    private val query: MediaItemQuery by lazy { queryFactory.create(destination) }

    val uiModel: StateFlow<MediaItemListScreenUiModel> = combine(
        useCase.observe(query),
        screenDelegate.bind(viewModelScope)
    ) { items, screenState ->
        MediaItemListScreenData(
            title = destination.title,
            items = items,
            screenState = screenState
        )
    }.map {
        uiModelConverter.toUiModel(it)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MediaItemListScreenUiModel(
            title = destination.title,
            items = emptyList(),
            screenState = ScreenStateUiModel()
        )
    )

    fun onEventConsumed(eventId: String) {
        screenDelegate.consumeEvent(eventId)
    }

    fun onScreenVisible() {
        performRefresh(force = false)
    }

    fun onPullToRefresh() {
        performRefresh(force = true)
    }

    private fun performRefresh(force: Boolean = false) {
        viewModelScope.launch {
            screenDelegate.load(isManualRefresh = force) { useCase.refresh(query) }
        }
    }

    fun onItemClick(item: MediaItemListItemUiModel) {
        screenDelegate.performThrottledAction {
            val dest = MediaItemDetailsDestination(
                itemId = item.id,
                title = item.common.name
            )
            screenDelegate.sendEvent(MediaItemListEvent.NavigateToMediaItemDetails(dest))
        }
    }
}