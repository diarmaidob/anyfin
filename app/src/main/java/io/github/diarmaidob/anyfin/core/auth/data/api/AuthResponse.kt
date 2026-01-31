package io.github.diarmaidob.anyfin.core.auth.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginResponse(
    @Json(name = "AccessToken") val accessToken: String,
    @Json(name = "User") val user: UserResponse,
    @Json(name = "ServerId") val serverId: String?
)

@JsonClass(generateAdapter = true)
data class UserResponse(
    @Json(name = "Id") val id: String,
    @Json(name = "Name") val name: String?
)