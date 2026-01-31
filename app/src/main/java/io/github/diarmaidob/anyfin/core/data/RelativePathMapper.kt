package io.github.diarmaidob.anyfin.core.data

import coil3.map.Mapper
import coil3.request.Options
import io.github.diarmaidob.anyfin.core.entity.serverUrlOrNull
import io.github.diarmaidob.anyfin.core.session.SessionRepo
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import javax.inject.Inject

class RelativePathMapper @Inject constructor(
    private val sessionRepo: SessionRepo
) : Mapper<String, HttpUrl> {

    override fun map(data: String, options: Options): HttpUrl? {
        if (!data.startsWith("/")) return null

        val baseUrl: String = sessionRepo.getCurrentSessionState().serverUrlOrNull() ?: ""

        if (baseUrl.isBlank()) return null

        val cleanBase = baseUrl.removeSuffix("/")
        val fullUrl = "$cleanBase$data"

        return fullUrl.toHttpUrl()
    }
}