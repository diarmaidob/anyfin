package io.github.diarmaidob.anyfin.core.auth.data.api

import android.util.Log
import io.github.diarmaidob.anyfin.core.auth.AuthHeaderRepo
import io.github.diarmaidob.anyfin.core.entity.SessionState
import io.github.diarmaidob.anyfin.core.session.SessionRepo
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AuthInterceptorTest {

    @MockK
    lateinit var authHeaderRepo: AuthHeaderRepo

    @MockK
    lateinit var sessionRepo: SessionRepo

    @MockK
    lateinit var chain: Interceptor.Chain

    private lateinit var authInterceptor: AuthInterceptor

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0
        authInterceptor = AuthInterceptor(authHeaderRepo, sessionRepo)
    }

    @Test
    fun `intercept when session is logged in should rewrite url and add both headers`() {
        val serverUrl = "https://myserver.com:8080"
        val authToken = "test_token_abc"
        val sessionState = mockk<SessionState.LoggedIn>(relaxed = true)
        every { sessionState.serverUrl } returns serverUrl
        every { sessionState.authToken } returns authToken

        val originalUrl = "https://placeholder.com/api/v1/items?key=value".toHttpUrl()
        val request = Request.Builder().url(originalUrl).build()
        val capturedRequest = slot<Request>()
        val mockResponse = mockk<Response>()
        val mockAuthHeader = "Jellyfin Client=\"Android\""

        every { sessionRepo.getCurrentSessionState() } returns sessionState
        every { authHeaderRepo.buildAuthHeader(sessionState) } returns mockAuthHeader
        every { chain.request() } returns request
        every { chain.proceed(capture(capturedRequest)) } returns mockResponse

        val result = authInterceptor.intercept(chain)

        assertEquals(mockResponse, result)
        val actualUrl = capturedRequest.captured.url
        assertEquals("myserver.com", actualUrl.host)
        assertEquals(8080, actualUrl.port)
        assertEquals("/api/v1/items", actualUrl.encodedPath)
        assertEquals("key=value", actualUrl.encodedQuery)
        assertEquals(mockAuthHeader, capturedRequest.captured.header("X-Emby-Authorization"))
        assertEquals(authToken, capturedRequest.captured.header("X-MediaBrowser-Token"))
    }

    @Test
    fun `intercept when session is logged out should not rewrite url and only add emby header`() {
        val sessionState = SessionState.LoggedOut
        val originalUrl = "https://placeholder.com/api/v1/items".toHttpUrl()
        val request = Request.Builder().url(originalUrl).build()
        val capturedRequest = slot<Request>()
        val mockAuthHeader = "Jellyfin Anonymous"

        every { sessionRepo.getCurrentSessionState() } returns sessionState
        every { authHeaderRepo.buildAuthHeader(sessionState) } returns mockAuthHeader
        every { chain.request() } returns request
        every { chain.proceed(capture(capturedRequest)) } returns mockk()

        authInterceptor.intercept(chain)

        val actualUrl = capturedRequest.captured.url
        assertEquals("placeholder.com", actualUrl.host)
        assertEquals(mockAuthHeader, capturedRequest.captured.header("X-Emby-Authorization"))
        assertEquals(null, capturedRequest.captured.header("X-MediaBrowser-Token"))
    }

    @Test
    fun `intercept when server url is invalid should maintain original url`() {
        val sessionState = mockk<SessionState.LoggedIn>(relaxed = true)
        every { sessionState.serverUrl } returns "not-a-valid-url"
        every { sessionState.authToken } returns "token"

        val originalUrl = "https://placeholder.com/data".toHttpUrl()
        val request = Request.Builder().url(originalUrl).build()
        val capturedRequest = slot<Request>()

        every { sessionRepo.getCurrentSessionState() } returns sessionState
        every { authHeaderRepo.buildAuthHeader(sessionState) } returns "auth"
        every { chain.request() } returns request
        every { chain.proceed(capture(capturedRequest)) } returns mockk()

        authInterceptor.intercept(chain)

        val actualUrl = capturedRequest.captured.url
        assertEquals("placeholder.com", actualUrl.host)
        assertEquals("auth", capturedRequest.captured.header("X-Emby-Authorization"))
        assertEquals("token", capturedRequest.captured.header("X-MediaBrowser-Token"))
    }

    @Test
    fun `intercept should copy path and query exactly from original request`() {
        val serverUrl = "http://192.168.1.1"
        val sessionState = mockk<SessionState.LoggedIn>(relaxed = true)
        every { sessionState.serverUrl } returns serverUrl

        val originalUrl = "https://any.com/Items/123/Images/Primary?maxHeight=100&tag=abc".toHttpUrl()
        val request = Request.Builder().url(originalUrl).build()
        val capturedRequest = slot<Request>()

        every { sessionRepo.getCurrentSessionState() } returns sessionState
        every { authHeaderRepo.buildAuthHeader(any()) } returns "h"
        every { chain.request() } returns request
        every { chain.proceed(capture(capturedRequest)) } returns mockk()

        authInterceptor.intercept(chain)

        val actualUrl = capturedRequest.captured.url
        assertEquals("/Items/123/Images/Primary", actualUrl.encodedPath)
        assertEquals("maxHeight=100&tag=abc", actualUrl.encodedQuery)
        assertEquals("192.168.1.1", actualUrl.host)
    }

    @Test
    fun `intercept should overwrite any existing server path when rewriting`() {
        val serverUrl = "https://myserver.com/jellyfin"
        val sessionState = mockk<SessionState.LoggedIn>(relaxed = true)
        every { sessionState.serverUrl } returns serverUrl

        val originalUrl = "https://initial.com/Users/1".toHttpUrl()
        val request = Request.Builder().url(originalUrl).build()
        val capturedRequest = slot<Request>()

        every { sessionRepo.getCurrentSessionState() } returns sessionState
        every { authHeaderRepo.buildAuthHeader(any()) } returns "h"
        every { chain.request() } returns request
        every { chain.proceed(capture(capturedRequest)) } returns mockk()

        authInterceptor.intercept(chain)

        val actualUrl = capturedRequest.captured.url
        assertEquals("myserver.com", actualUrl.host)
        assertEquals("/Users/1", actualUrl.encodedPath)
    }
}