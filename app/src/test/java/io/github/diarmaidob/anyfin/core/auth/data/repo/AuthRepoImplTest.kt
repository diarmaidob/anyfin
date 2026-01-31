package io.github.diarmaidob.anyfin.core.auth.data.repo

import android.util.Log
import io.github.diarmaidob.anyfin.core.auth.AuthHeaderRepo
import io.github.diarmaidob.anyfin.core.auth.data.api.AuthApi
import io.github.diarmaidob.anyfin.core.auth.data.api.LoginRequest
import io.github.diarmaidob.anyfin.core.auth.data.api.LoginResponse
import io.github.diarmaidob.anyfin.core.auth.data.api.UserResponse
import io.github.diarmaidob.anyfin.core.common.DataLoadError
import io.github.diarmaidob.anyfin.core.common.DataResult
import io.github.diarmaidob.anyfin.core.entity.AuthParams
import io.github.diarmaidob.anyfin.core.entity.SessionState
import io.github.diarmaidob.anyfin.core.session.SessionRepo
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepoImplTest {

    @MockK
    lateinit var api: AuthApi

    @MockK
    lateinit var sessionRepo: SessionRepo

    @MockK
    lateinit var authHeaderRepo: AuthHeaderRepo

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var authRepo: AuthRepoImpl

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
        authRepo = AuthRepoImpl(
            api = api,
            sessionRepo = sessionRepo,
            authHeaderRepo = authHeaderRepo,
            dispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `login returns success and saves session when api call succeeds`() = runTest {
        val params = AuthParams("myserver.com/", "user123", "password123")
        val sessionState = SessionState.LoggedOut
        val authHeader = "MediaBrowser Client=\"Anyfin\""
        val loginResponse = LoginResponse(
            accessToken = "access_token_abc",
            user = UserResponse(id = "user_id_789", name = "name"),
            serverId = "serverId"
        )
        val expectedCleanUrl = "http://myserver.com"
        val expectedFullUrl = "http://myserver.com/Users/AuthenticateByName"

        every { sessionRepo.getCurrentSessionState() } returns sessionState
        every { authHeaderRepo.buildAuthHeader(sessionState) } returns authHeader
        coEvery {
            api.login(
                fullUrl = expectedFullUrl,
                request = LoginRequest("user123", "password123"),
                authHeader = authHeader
            )
        } returns Response.success(loginResponse)
        coEvery { sessionRepo.saveSessionState(any()) } returns Unit

        val result = authRepo.login(params)

        assertEquals(DataResult.Success(Unit), result)
        coVerify {
            sessionRepo.saveSessionState(
                SessionState.LoggedIn(
                    serverUrl = expectedCleanUrl,
                    authToken = "access_token_abc",
                    userId = "user_id_789"
                )
            )
        }
    }

    @Test
    fun `login prepends http protocol when missing from server url`() = runTest {
        val params = AuthParams("jellyfin.local", "user", "pass")
        val expectedFullUrl = "http://jellyfin.local/Users/AuthenticateByName"

        every { sessionRepo.getCurrentSessionState() } returns SessionState.LoggedOut
        every { authHeaderRepo.buildAuthHeader(any()) } returns "header"
        coEvery { api.login(any(), any(), any()) } returns Response.success(mockk(relaxed = true))
        coEvery { sessionRepo.saveSessionState(any()) } returns Unit

        authRepo.login(params)

        coVerify {
            api.login(
                fullUrl = expectedFullUrl,
                request = any(),
                authHeader = any()
            )
        }
    }

    @Test
    fun `login does not prepend http when https protocol is already present`() = runTest {
        val params = AuthParams("https://jellyfin.remote", "user", "pass")
        val expectedFullUrl = "https://jellyfin.remote/Users/AuthenticateByName"

        every { sessionRepo.getCurrentSessionState() } returns SessionState.LoggedOut
        every { authHeaderRepo.buildAuthHeader(any()) } returns "header"
        coEvery { api.login(any(), any(), any()) } returns Response.success(mockk(relaxed = true))
        coEvery { sessionRepo.saveSessionState(any()) } returns Unit

        authRepo.login(params)

        coVerify {
            api.login(
                fullUrl = expectedFullUrl,
                request = any(),
                authHeader = any()
            )
        }
    }

    @Test
    fun `login returns HttpError when api response is unsuccessful`() = runTest {
        val params = AuthParams("http://server", "user", "pass")
        val response = Response.error<LoginResponse>(401, mockk(relaxed = true))

        every { sessionRepo.getCurrentSessionState() } returns SessionState.LoggedOut
        every { authHeaderRepo.buildAuthHeader(any()) } returns "header"
        coEvery { api.login(any(), any(), any()) } returns response

        val result = authRepo.login(params)

        val expectedError = DataResult.Error(DataLoadError.HttpError(401, "Response.error()"))
        assertEquals(expectedError, result)
    }

    @Test
    fun `login returns UnknownError when response body is null`() = runTest {
        val params = AuthParams("http://server", "user", "pass")
        val response = Response.success<LoginResponse>(null)

        every { sessionRepo.getCurrentSessionState() } returns SessionState.LoggedOut
        every { authHeaderRepo.buildAuthHeader(any()) } returns "header"
        coEvery { api.login(any(), any(), any()) } returns response

        val result = authRepo.login(params)

        val expectedError = DataResult.Error(DataLoadError.UnknownError("Empty response body"))
        assertEquals(expectedError, result)
    }

    @Test
    fun `login returns UnknownError when an exception is thrown`() = runTest {
        val params = AuthParams("http://server", "user", "pass")
        val errorMessage = "Connection Timeout"

        every { sessionRepo.getCurrentSessionState() } returns SessionState.LoggedOut
        every { authHeaderRepo.buildAuthHeader(any()) } returns "header"
        coEvery { api.login(any(), any(), any()) } throws RuntimeException(errorMessage)

        val result = authRepo.login(params)

        val expectedError = DataResult.Error(DataLoadError.UnknownError(errorMessage))
        assertEquals(expectedError, result)
    }

    @Test
    fun `logout updates session repo with logged out state`() = runTest {
        coEvery { sessionRepo.saveSessionState(any()) } returns Unit

        authRepo.logout()

        coVerify { sessionRepo.saveSessionState(SessionState.LoggedOut) }
    }
}