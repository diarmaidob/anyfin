package io.github.diarmaidob.anyfin.core.session

import androidx.datastore.core.DataStore
import io.github.diarmaidob.anyfin.core.entity.SessionState
import io.github.diarmaidob.anyfin.injection.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepoImpl @Inject constructor(
    private val dataStore: DataStore<StoredSession>,
    @ApplicationScope private val scope: CoroutineScope
) : SessionRepo {

    override val sessionState: StateFlow<SessionState> = dataStore.data
        .map { data ->
            if (data.serverUrl != null && data.authToken != null && data.userId != null) {
                SessionState.LoggedIn(
                    serverUrl = data.serverUrl,
                    authToken = data.authToken,
                    userId = data.userId
                )
            } else {
                SessionState.LoggedOut
            }
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Companion.Eagerly,
            initialValue = SessionState.LoggedOut
        )

    override fun getCurrentSessionState(): SessionState =
        sessionState.value


    override fun saveSessionState(state: SessionState) {
        scope.launch {
            dataStore.updateData {
                when (state) {
                    is SessionState.LoggedIn -> {
                        val cleanUrl = state.serverUrl.trimEnd('/')
                        StoredSession(
                            serverUrl = cleanUrl,
                            authToken = state.authToken,
                            userId = state.userId
                        )
                    }

                    SessionState.LoggedOut -> StoredSession()
                }
            }
        }
    }

}