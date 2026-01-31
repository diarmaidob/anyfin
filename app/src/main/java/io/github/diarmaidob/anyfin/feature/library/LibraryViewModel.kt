package io.github.diarmaidob.anyfin.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.diarmaidob.anyfin.core.common.ScreenStateDelegate
import io.github.diarmaidob.anyfin.core.entity.MediaItem
import io.github.diarmaidob.anyfin.core.entity.MediaItemQuery
import io.github.diarmaidob.anyfin.core.entity.ScreenState
import io.github.diarmaidob.anyfin.core.ui.uimodel.MediaItemListItemUiModel
import io.github.diarmaidob.anyfin.feature.mediaitem.list.GetMediaItemListDataUseCase
import io.github.diarmaidob.anyfin.navigation.MediaItemListDestination
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject


data class LibraryScreenData(
    val screenState: ScreenState<LibraryEvent>,
    val items: List<MediaItem>
) {

    val hasData = items.isNotEmpty()

}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val getMediaListUseCase: GetMediaItemListDataUseCase,
    private val screenStateDelegate: ScreenStateDelegate<LibraryEvent>,
    private val libraryScreenUiModelConverter: LibraryScreenUiModelConverter
) : ViewModel() {

    private val query = MediaItemQuery.UserViews()

    val uiModel: StateFlow<LibraryScreenUiModel> =
        combine(
            getMediaListUseCase.observe(query),
            screenStateDelegate.bind(viewModelScope)
        ) { data, screenState ->
            LibraryScreenData(screenState = screenState, items = data)
        }.map {
            libraryScreenUiModelConverter.toUiModel(it)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LibraryScreenUiModel(),
        )

    private fun performRefresh(force: Boolean) {
        viewModelScope.launch {
            screenStateDelegate.load(isManualRefresh = force) {
                getMediaListUseCase.refresh(query)
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

    fun onItemClick(item: MediaItemListItemUiModel) {
        screenStateDelegate.performThrottledAction {
            viewModelScope.launch {
                val dest = MediaItemListDestination(
                    listId = item.id,
                    title = item.common.name,
                    type = MediaItemListDestination.ListType.LIBRARY_CONTENTS
                )
                screenStateDelegate.sendEvent(LibraryEvent.NavigateToLibraryContents(dest))
            }
        }
    }

}