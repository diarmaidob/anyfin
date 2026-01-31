package io.github.diarmaidob.anyfin.core.auth.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

interface AuthApi {

    @POST
    suspend fun login(
        @Url fullUrl: String,
        @Body request: LoginRequest,
        @Header("X-Emby-Authorization") authHeader: String
    ): Response<LoginResponse>
}