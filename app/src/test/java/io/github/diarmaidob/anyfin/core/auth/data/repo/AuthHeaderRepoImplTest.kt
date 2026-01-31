package io.github.diarmaidob.anyfin.core.auth.data.repo

import io.github.diarmaidob.anyfin.core.auth.AuthHeaderRepo
import io.github.diarmaidob.anyfin.core.common.JellyfinConstants
import io.github.diarmaidob.anyfin.core.device.DeviceRepo
import io.github.diarmaidob.anyfin.core.entity.SessionState
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

class AuthHeaderRepoImplTest {

    private val deviceRepo = mockk<DeviceRepo>()
    private lateinit var authHeaderRepo: AuthHeaderRepo

    @Before
    fun setUp() {
        authHeaderRepo = AuthHeaderRepoImpl(deviceRepo)
    }

    @Test
    fun `buildAuthHeader when session is LoggedIn returns header including UserId`() {
        val testDeviceId = "device-123"
        val testUserId = "user-456"
        val sessionState = SessionState.LoggedIn(userId = testUserId, serverUrl = "serverUrl", authToken = "authToken")
        every { deviceRepo.getDeviceId() } returns testDeviceId

        val expected = "MediaBrowser " +
                "Client=\"${JellyfinConstants.CLIENT_NAME}\", " +
                "Device=\"${JellyfinConstants.DEVICE_TYPE}\", " +
                "DeviceId=\"$testDeviceId\", " +
                "Version=\"${JellyfinConstants.CLIENT_VERSION}\", " +
                "UserId=\"$testUserId\""

        val `actual` = authHeaderRepo.buildAuthHeader(sessionState)

        assertEquals(expected, `actual`)
    }

    @Test
    fun `buildAuthHeader when session is not LoggedIn returns header without UserId`() {
        val testDeviceId = "device-789"
        val sessionState = SessionState.LoggedOut
        every { deviceRepo.getDeviceId() } returns testDeviceId

        val expected = "MediaBrowser " +
                "Client=\"${JellyfinConstants.CLIENT_NAME}\", " +
                "Device=\"${JellyfinConstants.DEVICE_TYPE}\", " +
                "DeviceId=\"$testDeviceId\", " +
                "Version=\"${JellyfinConstants.CLIENT_VERSION}\""

        val `actual` = authHeaderRepo.buildAuthHeader(sessionState)

        assertEquals(expected, `actual`)
    }

    @Test
    fun `buildAuthHeader changes based on device id`() {
        val sessionState = SessionState.LoggedOut

        every { deviceRepo.getDeviceId() } returns "id-1"
        val header1 = authHeaderRepo.buildAuthHeader(sessionState)

        every { deviceRepo.getDeviceId() } returns "id-2"
        val header2 = authHeaderRepo.buildAuthHeader(sessionState)

        assertNotEquals(header1, header2)
    }

    @Test
    fun `buildAuthHeader matches constant client name`() {
        val testDeviceId = "id"
        val sessionState = SessionState.LoggedOut
        every { deviceRepo.getDeviceId() } returns testDeviceId

        val actual = authHeaderRepo.buildAuthHeader(sessionState)

        val expectedPrefix = "MediaBrowser Client=\"${JellyfinConstants.CLIENT_NAME}\""
        val actualPrefix = actual.substring(0, expectedPrefix.length)

        assertEquals(expectedPrefix, actualPrefix)
    }
}