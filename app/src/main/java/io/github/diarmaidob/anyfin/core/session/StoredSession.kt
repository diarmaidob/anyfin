package io.github.diarmaidob.anyfin.core.session

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StoredSession(
    val serverUrl: String? = null,
    val authToken: String? = null,
    val userId: String? = null
)