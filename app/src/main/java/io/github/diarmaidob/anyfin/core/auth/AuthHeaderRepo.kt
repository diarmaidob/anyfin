package io.github.diarmaidob.anyfin.core.auth

import io.github.diarmaidob.anyfin.core.entity.SessionState

interface AuthHeaderRepo {

    fun buildAuthHeader(sessionState: SessionState): String
}
