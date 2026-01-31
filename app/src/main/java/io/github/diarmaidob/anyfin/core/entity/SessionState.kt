package io.github.diarmaidob.anyfin.core.entity

sealed interface SessionState {

    data class LoggedIn(
        val serverUrl: String,
        val authToken: String,
        val userId: String
    ) : SessionState

    data object LoggedOut : SessionState

}


fun SessionState.userIdOrNull(): String? =
    (this as? SessionState.LoggedIn)?.userId

fun SessionState.authTokenOrNull(): String? =
    (this as? SessionState.LoggedIn)?.authToken

fun SessionState.serverUrlOrNull(): String? =
    (this as? SessionState.LoggedIn)?.serverUrl

