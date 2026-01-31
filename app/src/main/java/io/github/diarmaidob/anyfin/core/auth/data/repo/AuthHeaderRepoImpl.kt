package io.github.diarmaidob.anyfin.core.auth.data.repo

import io.github.diarmaidob.anyfin.core.auth.AuthHeaderRepo
import io.github.diarmaidob.anyfin.core.common.JellyfinConstants
import io.github.diarmaidob.anyfin.core.device.DeviceRepo
import io.github.diarmaidob.anyfin.core.entity.SessionState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthHeaderRepoImpl @Inject constructor(
    private val deviceRepo: DeviceRepo
) : AuthHeaderRepo {

    override fun buildAuthHeader(sessionState: SessionState): String {
        val userId: String? = if (sessionState is SessionState.LoggedIn) sessionState.userId else null

        return buildString {
            append("MediaBrowser ")
            append("Client=\"${JellyfinConstants.CLIENT_NAME}\", ")
            append("Device=\"${JellyfinConstants.DEVICE_TYPE}\", ")
            append("DeviceId=\"${deviceRepo.getDeviceId()}\", ")
            append("Version=\"${JellyfinConstants.CLIENT_VERSION}\"")
            userId?.let { append(", UserId=\"${it}\"") }
        }
    }

}