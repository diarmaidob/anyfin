package io.github.diarmaidob.anyfin.core.auth.data.repo

import android.util.Log
import io.github.diarmaidob.anyfin.BuildConfig
import io.github.diarmaidob.anyfin.core.auth.AuthHeaderRepo
import io.github.diarmaidob.anyfin.core.auth.AuthRepo
import io.github.diarmaidob.anyfin.core.auth.data.api.AuthApi
import io.github.diarmaidob.anyfin.core.auth.data.api.LoginRequest
import io.github.diarmaidob.anyfin.core.common.DataLoadError
import io.github.diarmaidob.anyfin.core.common.DataResult
import io.github.diarmaidob.anyfin.core.entity.AuthParams
import io.github.diarmaidob.anyfin.core.entity.SessionState
import io.github.diarmaidob.anyfin.core.session.SessionRepo
import io.github.diarmaidob.anyfin.injection.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AuthRepoImpl @Inject constructor(
    private val api: AuthApi,
    private val sessionRepo: SessionRepo,
    private val authHeaderRepo: AuthHeaderRepo,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : AuthRepo {

    private companion object {
        private const val TAG = "AuthRepoImpl"
    }

    override suspend fun login(params: AuthParams): DataResult<Unit> = withContext(dispatcher) {
        try {
            var cleanUrl = params.serverUrl.trimEnd('/')
            if (!cleanUrl.startsWith("http")) {
                cleanUrl = "http://$cleanUrl"
            }

            val fullAuthUrl = "$cleanUrl/Users/AuthenticateByName"

            val authHeader = authHeaderRepo.buildAuthHeader(sessionRepo.getCurrentSessionState())

            val response = api.login(
                fullUrl = fullAuthUrl,
                request = LoginRequest(params.username, params.password),
                authHeader = authHeader
            )

            if (!response.isSuccessful) {
                return@withContext DataResult.Error(
                    DataLoadError.HttpError(response.code(), response.message())
                )
            }

            val body = response.body()
            if (body == null) {
                return@withContext DataResult.Error(DataLoadError.UnknownError("Empty response body"))
            }

            sessionRepo.saveSessionState(
                SessionState.LoggedIn(
                    serverUrl = cleanUrl,
                    authToken = body.accessToken,
                    userId = body.user.id
                )
            )

            DataResult.Success(Unit)

        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Login failed", e)
            }
            DataResult.Error(DataLoadError.UnknownError(e.message ?: "Login failed"))
        }
    }

    override suspend fun logout() = withContext(dispatcher) {
        sessionRepo.saveSessionState(SessionState.LoggedOut)
    }

}