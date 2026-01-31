package io.github.diarmaidob.anyfin.core.data

import coil3.map.Mapper
import coil3.request.Options
import io.github.diarmaidob.anyfin.core.entity.JellyfinImage
import io.github.diarmaidob.anyfin.core.entity.SessionState
import io.github.diarmaidob.anyfin.core.session.SessionRepo
import javax.inject.Inject

class JellyfinImageMapper @Inject constructor(
    private val sessionRepo: SessionRepo
) : Mapper<JellyfinImage, String> {

    override fun map(data: JellyfinImage, options: Options): String? {
        val tag = data.tag ?: return null
        val baseUrl = (sessionRepo.getCurrentSessionState() as? SessionState.LoggedIn)?.serverUrl ?: return null


        return when (data) {
            is JellyfinImage.Primary -> "$baseUrl/Items/${data.itemId}/Images/Primary?tag=$tag"
            is JellyfinImage.Backdrop -> "$baseUrl/Items/${data.itemId}/Images/Backdrop?tag=$tag"
            is JellyfinImage.Logo -> "$baseUrl/Items/${data.itemId}/Images/Logo?tag=$tag"
            is JellyfinImage.Thumb -> "$baseUrl/Items/${data.itemId}/Images/Thumb?tag=$tag"
        }
    }
}