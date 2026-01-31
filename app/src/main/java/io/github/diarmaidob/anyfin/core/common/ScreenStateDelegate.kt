package io.github.diarmaidob.anyfin.core.common

import io.github.diarmaidob.anyfin.core.entity.ScreenState
import io.github.diarmaidob.anyfin.core.entity.UniqueEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

// TODO: split this out
class ScreenStateDelegate<Event> @Inject constructor(
    private val actionThrottler: ActionThrottler,
    private val refreshThrottler: ActionThrottler
) {

    companion object {
        val REFRESH_WINDOW_DURATION = 5.minutes
        val CLICK_WINDOW_DURATION = 150.milliseconds
    }

    private data class LoadStatus(
        val isPullRefreshing: Boolean = false,
        val isRawSyncing: Boolean = false,
        val hasInitialized: Boolean = false
    )

    private val _loadStatus = MutableStateFlow(LoadStatus())
    private val _events = MutableStateFlow<List<UniqueEvent<Event>>>(emptyList())
    private val _error = MutableStateFlow<Throwable?>(null)

    private val _debouncedSyncing = _loadStatus
        .map { it.isRawSyncing }
        .distinctUntilChanged()
        .transformLatest { isSyncing ->
            if (isSyncing) {
                delay(500)
                emit(true)
            } else {
                emit(false)
            }
        }

    fun bind(scope: CoroutineScope): StateFlow<ScreenState<Event>> {
        return combine(
            _events,
            _loadStatus,
            _debouncedSyncing,
            _error
        ) { events, status, debouncedSyncing, error ->
            ScreenState(
                events = events,
                isPullRefreshing = status.isPullRefreshing,
                isSyncing = debouncedSyncing,
                isRawSyncing = status.isRawSyncing,
                hasInitialized = status.hasInitialized,
                error = error
            )
        }.stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ScreenState()
        )
    }

    fun sendEvent(event: Event) {
        _events.update { it + UniqueEvent(content = event) }
    }

    fun consumeEvent(eventId: String) {
        _events.update { list -> list.filterNot { it.id == eventId } }
    }

    fun performThrottledAction(action: () -> Unit) {
        actionThrottler.attempt(CLICK_WINDOW_DURATION, action)
    }

    suspend fun <T> load(
        isManualRefresh: Boolean = false,
        block: suspend () -> T
    ): Result<T>? {

        _loadStatus.update {
            it.copy(
                isPullRefreshing = isManualRefresh,
                isRawSyncing = !isManualRefresh
            )
        }
        _error.value = null

        try {
            val result: T? = if (isManualRefresh) {
                refreshThrottler.forceSuspend(block)
            } else {
                refreshThrottler.attemptSuspend(REFRESH_WINDOW_DURATION, block)
            }
            return result?.let { Result.success(it) }

        } catch (e: Exception) {
            _error.value = e
            return Result.failure(e)
        } finally {
            delay(50)

            _loadStatus.update {
                it.copy(
                    isPullRefreshing = false,
                    isRawSyncing = false,
                    hasInitialized = true
                )
            }
        }
    }
}