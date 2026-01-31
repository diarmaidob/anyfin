package io.github.diarmaidob.anyfin.core.auth.data.api

import android.util.Log
import io.github.diarmaidob.anyfin.BuildConfig
import io.github.diarmaidob.anyfin.core.auth.AuthHeaderRepo
import io.github.diarmaidob.anyfin.core.entity.SessionState
import io.github.diarmaidob.anyfin.core.session.SessionRepo
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val authHeaderRepo: AuthHeaderRepo,
    private val sessionRepo: SessionRepo
) : Interceptor {

    private companion object {
        private const val TAG = "AuthInterceptor"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val builder = originalRequest.newBuilder()
        val sessionState = sessionRepo.getCurrentSessionState()
        val isSessionActive = sessionState is SessionState.LoggedIn

        if (isSessionActive) {
            val serverUrl = sessionState.serverUrl.toHttpUrlOrNull()
            if (serverUrl != null) {
                val newUrl = serverUrl.newBuilder()
                    .encodedPath(originalRequest.url.encodedPath)
                    .encodedQuery(originalRequest.url.encodedQuery)
                    .build()

                builder.url(newUrl)
            } else {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "Invalid serverUrl in session: ${sessionState.serverUrl}")
                }
            }
        }

        val authHeader = authHeaderRepo.buildAuthHeader(sessionState)
        builder.header("X-Emby-Authorization", authHeader)

        val authToken: String? = if (isSessionActive) sessionState.authToken else null
        authToken?.let { token ->
            builder.header("X-MediaBrowser-Token", token)
        }

        return chain.proceed(builder.build())
    }

}