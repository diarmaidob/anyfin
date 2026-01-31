package io.github.diarmaidob.anyfin.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import io.github.diarmaidob.anyfin.core.entity.UniqueEvent

/**
 * A side-effect handler that processes a queue of events one by one.
 */
@Composable
fun <T> EventEffect(
    events: List<UniqueEvent<T>>,
    onConsumed: (String) -> Unit,
    action: suspend (T) -> Unit
) {
    val currentEvent = events.firstOrNull()

    val currentOnConsumed by rememberUpdatedState(onConsumed)
    val currentAction by rememberUpdatedState(action)

    LaunchedEffect(currentEvent?.id) {
        if (currentEvent != null) {
            currentAction(currentEvent.content)
            currentOnConsumed(currentEvent.id)
        }
    }
}