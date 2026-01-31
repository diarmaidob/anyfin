package io.github.diarmaidob.anyfin.core.auth

import io.github.diarmaidob.anyfin.core.common.DataResult
import io.github.diarmaidob.anyfin.core.entity.AuthParams

interface AuthRepo {
    suspend fun login(params: AuthParams): DataResult<Unit>
    suspend fun logout()
}