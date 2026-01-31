package io.github.diarmaidob.anyfin.feature.mediaitem.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.diarmaidob.anyfin.core.common.ScreenStateDelegate
import io.github.diarmaidob.anyfin.core.entity.MediaItem
import io.github.diarmaidob.anyfin.core.entity.MediaItemStreamOptions
import io.github.diarmaidob.anyfin.core.ui.uimodel.MediaItemListItemUiModel
import io.github.diarmaidob.anyfin.core.ui.uimodel.PlayAction
import io.github.diarmaidob.anyfin.navigation.MediaItemDetailsDestination
import io.github.diarmaidob.anyfin.navigation.PlayerDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MediaItemDetailsData(
    val item: MediaItem,
    val children: List<MediaItem> = emptyList(),
    val nextUp: MediaItem? = null,
    val streamOptions: MediaItemStreamOptions? = null
)

@HiltViewModel
class MediaItemDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeItem: ObserveMediaItemDetailsUseCase,
    private val refreshItem: RefreshMediaItemDetailsUseCase,
    private val uiModelConverter: MediaItemDetailsScreenUiModelConverter,
    private val screenStateDelegate: ScreenStateDelegate<MediaItemDetailsEvent>
) : ViewModel() {

    private data class ViewModelState(
        val audioIndex: Int? = null,
        val subtitleIndex: Int? = null
    )

    private val destination = savedStateHandle.toRoute<MediaItemDetailsDestination>()
    private val _vmState = MutableStateFlow(ViewModelState())

    val uiModel: StateFlow<MediaItemDetailsScreenUiModel> = combine(
        observeItem(destination.itemId),
        _vmState,
        screenStateDelegate.bind(viewModelScope)
    ) { data, vmState, screenState ->
        uiModelConverter.toUiModel(
            data = data,
            audioIndex = vmState.audioIndex,
            subtitleIndex = vmState.subtitleIndex,
            screenState = screenState,
            title = destination.title
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MediaItemDetailsScreenUiModel(title = destination.title)
    )

    fun onScreenVisible() {
        performRefresh(force = false)
    }

    fun onPullToRefresh() {
        performRefresh(force = true)
    }

    private fun performRefresh(force: Boolean) {
        viewModelScope.launch {
            screenStateDelegate.load(isManualRefresh = force) {
                refreshItem(itemId = destination.itemId)
            }
        }
    }

    fun onEventConsumed(eventId: String) {
        screenStateDelegate.consumeEvent(eventId)
    }


    fun onAudioSelected(index: Int) {
        _vmState.update { it.copy(audioIndex = index) }
    }

    fun onSubtitleSelected(index: Int?) {
        _vmState.update { it.copy(subtitleIndex = index) }
    }

    fun onPlayClick() {
        val state = uiModel.value
        val action = state.details?.playButton?.action ?: return

        val currentAudio = _vmState.value.audioIndex
            ?: state.audioTracks.firstOrNull { it.isSelected }?.index

        val currentSub = _vmState.value.subtitleIndex

        val event = when (action) {
            is PlayAction.PlayItem -> MediaItemDetailsEvent.NavigateToPlayer(
                PlayerDestination(action.itemId, currentAudio, currentSub)
            )

            is PlayAction.NavigateToChild -> MediaItemDetailsEvent.NavigateToPlayer(
                PlayerDestination(action.itemId, null, null)
            )

            PlayAction.None -> null
        }

        if (event != null) {
            screenStateDelegate.performThrottledAction {
                screenStateDelegate.sendEvent(event)
            }
        }
    }

    fun onChildClick(child: MediaItemListItemUiModel) {
        screenStateDelegate.performThrottledAction {
            val dest = MediaItemDetailsDestination(itemId = child.id, title = child.common.name)
            screenStateDelegate.sendEvent(MediaItemDetailsEvent.NavigateToChild(dest))
        }
    }

}