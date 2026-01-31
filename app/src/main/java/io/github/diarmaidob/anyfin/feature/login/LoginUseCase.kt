package io.github.diarmaidob.anyfin.feature.login

import io.github.diarmaidob.anyfin.core.auth.AuthRepo
import io.github.diarmaidob.anyfin.core.common.DataLoadError
import io.github.diarmaidob.anyfin.core.common.DataResult
import io.github.diarmaidob.anyfin.core.entity.AuthParams
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepo
) {
    suspend fun login(params: AuthParams): DataResult<Unit> {
        if (params.serverUrl.isBlank()) {
            return DataResult.Error(DataLoadError.UnknownError("Server URL cannot be empty"))
        }
        if (params.username.isBlank()) {
            return DataResult.Error(DataLoadError.UnknownError("Username cannot be empty"))
        }

        return authRepository.login(params)
    }

}