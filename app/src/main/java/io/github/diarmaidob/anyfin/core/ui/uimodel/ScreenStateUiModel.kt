package io.github.diarmaidob.anyfin.core.ui.uimodel

import io.github.diarmaidob.anyfin.core.entity.UniqueEvent


data class ScreenStateUiModel<Event>(
    val events: List<UniqueEvent<Event>> = emptyList(),
    val isInitialLoading: Boolean = false,
    val isPullRefreshing: Boolean = false,
    val isSyncing: Boolean = false,
    val isEmpty: Boolean = false,
    val error: Throwable? = null
)