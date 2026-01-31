package io.github.diarmaidob.anyfin.core.entity

data class ScreenState<Event>(
    val events: List<UniqueEvent<Event>> = emptyList(),
    val isPullRefreshing: Boolean = false,
    val isSyncing: Boolean = false,
    val isRawSyncing: Boolean = false,
    val hasInitialized: Boolean = false,
    val error: Throwable? = null
)