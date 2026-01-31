package io.github.diarmaidob.anyfin.core.auth.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "Username") val username: String,
    @Json(name = "Pw") val password: String
)