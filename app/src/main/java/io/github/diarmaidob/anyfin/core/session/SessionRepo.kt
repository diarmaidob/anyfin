package io.github.diarmaidob.anyfin.core.session

import io.github.diarmaidob.anyfin.core.entity.SessionState
import kotlinx.coroutines.flow.StateFlow

interface SessionRepo {
    val sessionState: StateFlow<SessionState>
    fun getCurrentSessionState(): SessionState
    fun saveSessionState(state: SessionState)
}