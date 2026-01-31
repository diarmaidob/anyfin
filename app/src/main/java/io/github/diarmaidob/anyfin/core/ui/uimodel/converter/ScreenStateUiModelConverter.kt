package io.github.diarmaidob.anyfin.core.ui.uimodel.converter

import io.github.diarmaidob.anyfin.core.entity.ScreenState
import io.github.diarmaidob.anyfin.core.ui.uimodel.ScreenStateUiModel
import javax.inject.Inject

class ScreenStateUiModelConverter @Inject constructor() {

    fun <E> toUiModel(state: ScreenState<E>, hasData: Boolean): ScreenStateUiModel<E> {
        val isLoadingActive = state.isRawSyncing || state.isPullRefreshing
        val isInitial = !hasData && (isLoadingActive || !state.hasInitialized)
        val showSyncBar = hasData && state.isSyncing && !state.isPullRefreshing
        val showEmpty = !hasData && !isLoadingActive && state.error == null && state.hasInitialized

        return ScreenStateUiModel(
            events = state.events,
            isInitialLoading = isInitial,
            isPullRefreshing = state.isPullRefreshing,
            isSyncing = showSyncBar,
            isEmpty = showEmpty,
            error = state.error
        )
    }

}