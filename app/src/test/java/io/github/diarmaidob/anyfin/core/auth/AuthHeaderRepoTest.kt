package io.github.diarmaidob.anyfin.core.auth

import io.github.diarmaidob.anyfin.core.auth.data.repo.AuthHeaderRepoImpl
import io.github.diarmaidob.anyfin.core.common.JellyfinConstants
import io.github.diarmaidob.anyfin.core.device.DeviceRepo
import io.github.diarmaidob.anyfin.core.entity.SessionState
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthHeaderRepoTest {

    @MockK
    private lateinit var deviceRepo: DeviceRepo

    private lateinit var authHeaderRepo: AuthHeaderRepo

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        authHeaderRepo = AuthHeaderRepoImpl(deviceRepo)
    }

    @Test
    fun `buildAuthHeader interface contract - returns non-empty string for LoggedIn session`() {
        val testDeviceId = "test-device-id"
        val sessionState = SessionState.LoggedIn(
            userId = "user-123",
            serverUrl = "https://example.com",
            authToken = "auth-token-456"
        )
        every { deviceRepo.getDeviceId() } returns testDeviceId

        val result = authHeaderRepo.buildAuthHeader(sessionState)

        assertNotNull("AuthHeaderRepo should return non-null header", result)
        assertTrue("Header should not be empty", result.isNotEmpty())
    }

    @Test
    fun `buildAuthHeader interface contract - returns non-empty string for LoggedOut session`() {
        val testDeviceId = "test-device-id"
        val sessionState = SessionState.LoggedOut
        every { deviceRepo.getDeviceId() } returns testDeviceId

        val result = authHeaderRepo.buildAuthHeader(sessionState)

        assertNotNull("AuthHeaderRepo should return non-null header", result)
        assertTrue("Header should not be empty", result.isNotEmpty())
    }

    @Test
    fun `buildAuthHeader interface contract - header starts with MediaBrowser prefix`() {
        val testDeviceId = "test-device-id"
        val sessionState = SessionState.LoggedOut
        every { deviceRepo.getDeviceId() } returns testDeviceId

        val result = authHeaderRepo.buildAuthHeader(sessionState)

        assertTrue("Header should start with 'MediaBrowser '", result.startsWith("MediaBrowser "))
    }

    @Test
    fun `buildAuthHeader interface contract - contains required client information`() {
        val testDeviceId = "test-device-id"
        val sessionState = SessionState.LoggedOut
        every { deviceRepo.getDeviceId() } returns testDeviceId

        val result = authHeaderRepo.buildAuthHeader(sessionState)

        assertTrue("Header should contain client name", result.contains("Client=\"${JellyfinConstants.CLIENT_NAME}\""))
        assertTrue("Header should contain device type", result.contains("Device=\"${JellyfinConstants.DEVICE_TYPE}\""))
        assertTrue("Header should contain device ID", result.contains("DeviceId=\"$testDeviceId\""))
        assertTrue("Header should contain client version", result.contains("Version=\"${JellyfinConstants.CLIENT_VERSION}\""))
    }

    @Test
    fun `buildAuthHeader interface contract - includes UserId for LoggedIn session`() {
        val testDeviceId = "test-device-id"
        val testUserId = "user-789"
        val sessionState = SessionState.LoggedIn(
            userId = testUserId,
            serverUrl = "https://example.com",
            authToken = "auth-token-123"
        )
        every { deviceRepo.getDeviceId() } returns testDeviceId

        val result = authHeaderRepo.buildAuthHeader(sessionState)

        assertTrue("Header should contain UserId for logged in session", result.contains("UserId=\"$testUserId\""))
    }

    @Test
    fun `buildAuthHeader interface contract - excludes UserId for LoggedOut session`() {
        val testDeviceId = "test-device-id"
        val sessionState = SessionState.LoggedOut
        every { deviceRepo.getDeviceId() } returns testDeviceId

        val result = authHeaderRepo.buildAuthHeader(sessionState)

        assertTrue("Header should not contain UserId for logged out session", !result.contains("UserId"))
    }

    @Test
    fun `buildAuthHeader interface contract - handles different SessionState implementations`() {
        val testDeviceId = "test-device-id"
        every { deviceRepo.getDeviceId() } returns testDeviceId

        val loggedInResult = authHeaderRepo.buildAuthHeader(
            SessionState.LoggedIn("user1", "server1", "token1")
        )
        val loggedOutResult = authHeaderRepo.buildAuthHeader(SessionState.LoggedOut)

        assertNotNull("Result should not be null for LoggedIn", loggedInResult)
        assertNotNull("Result should not be null for LoggedOut", loggedOutResult)
        assertTrue("Results should be different for different session states", loggedInResult != loggedOutResult)
    }

    @Test
    fun `buildAuthHeader interface contract - consistent format for same inputs`() {
        val testDeviceId = "test-device-id"
        val sessionState = SessionState.LoggedOut
        every { deviceRepo.getDeviceId() } returns testDeviceId

        val result1 = authHeaderRepo.buildAuthHeader(sessionState)
        val result2 = authHeaderRepo.buildAuthHeader(sessionState)

        assertEquals("Results should be consistent for same inputs", result1, result2)
    }

    @Test
    fun `buildAuthHeader interface contract - validates header format structure`() {
        val testDeviceId = "test-device-id"
        val sessionState = SessionState.LoggedOut
        every { deviceRepo.getDeviceId() } returns testDeviceId

        val result = authHeaderRepo.buildAuthHeader(sessionState)

        val expectedPattern = "MediaBrowser Client=\".*\", Device=\".*\", DeviceId=\".*\", Version=\".*\""
        assertTrue(
            "Header should follow expected format pattern",
            result.matches(Regex(expectedPattern))
        )
    }
}
