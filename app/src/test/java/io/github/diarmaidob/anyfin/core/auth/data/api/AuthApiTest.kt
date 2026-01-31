package io.github.diarmaidob.anyfin.core.auth.data.api

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class AuthApiTest {

    private lateinit var authApi: AuthApi

    @Before
    fun setUp() {
        authApi = mockk()
    }

    @Test
    fun `login should return successful response with valid credentials`() = runTest {
        val fullUrl = "https://test-server.com/emby/Users/AuthenticateByName"
        val loginRequest = LoginRequest(
            username = "testuser",
            password = "testpass"
        )
        val authHeader = "Jellyfin Client=\"Android\", Device=\"TestDevice\", DeviceId=\"test-device-id\", Version=\"1.0.0\""
        val expectedResponse = LoginResponse(
            accessToken = "test-access-token-12345",
            user = UserResponse(
                id = "user-123",
                name = "Test User"
            ),
            serverId = "server-456"
        )

        coEvery {
            authApi.login(fullUrl, loginRequest, authHeader)
        } returns Response.success(expectedResponse)

        val result = authApi.login(fullUrl, loginRequest, authHeader)

        assertTrue(result.isSuccessful)
        assertNotNull(result.body())
        assertEquals(expectedResponse, result.body())
        assertEquals("test-access-token-12345", result.body()?.accessToken)
        assertEquals("user-123", result.body()?.user?.id)
        assertEquals("Test User", result.body()?.user?.name)
        assertEquals("server-456", result.body()?.serverId)
    }

    @Test
    fun `login should return successful response with null serverId`() = runTest {
        val fullUrl = "https://test-server.com/emby/Users/AuthenticateByName"
        val loginRequest = LoginRequest(
            username = "testuser",
            password = "testpass"
        )
        val authHeader = "Jellyfin Client=\"Android\", Device=\"TestDevice\", DeviceId=\"test-device-id\", Version=\"1.0.0\""
        val expectedResponse = LoginResponse(
            accessToken = "test-access-token-12345",
            user = UserResponse(
                id = "user-123",
                name = "Test User"
            ),
            serverId = null
        )

        coEvery {
            authApi.login(fullUrl, loginRequest, authHeader)
        } returns Response.success(expectedResponse)

        val result = authApi.login(fullUrl, loginRequest, authHeader)

        assertTrue(result.isSuccessful)
        assertNotNull(result.body())
        assertEquals(expectedResponse, result.body())
        assertEquals("test-access-token-12345", result.body()?.accessToken)
        assertEquals("user-123", result.body()?.user?.id)
        assertEquals("Test User", result.body()?.user?.name)
        assertEquals(null, result.body()?.serverId)
    }

    @Test
    fun `login should return successful response with null userName`() = runTest {
        val fullUrl = "https://test-server.com/emby/Users/AuthenticateByName"
        val loginRequest = LoginRequest(
            username = "testuser",
            password = "testpass"
        )
        val authHeader = "Jellyfin Client=\"Android\", Device=\"TestDevice\", DeviceId=\"test-device-id\", Version=\"1.0.0\""
        val expectedResponse = LoginResponse(
            accessToken = "test-access-token-12345",
            user = UserResponse(
                id = "user-123",
                name = null
            ),
            serverId = "server-456"
        )

        coEvery {
            authApi.login(fullUrl, loginRequest, authHeader)
        } returns Response.success(expectedResponse)

        val result = authApi.login(fullUrl, loginRequest, authHeader)

        assertTrue(result.isSuccessful)
        assertNotNull(result.body())
        assertEquals(expectedResponse, result.body())
        assertEquals("test-access-token-12345", result.body()?.accessToken)
        assertEquals("user-123", result.body()?.user?.id)
        assertEquals(null, result.body()?.user?.name)
        assertEquals("server-456", result.body()?.serverId)
    }

    @Test
    fun `login should return error response when authentication fails`() = runTest {
        val fullUrl = "https://test-server.com/emby/Users/AuthenticateByName"
        val loginRequest = LoginRequest(
            username = "invaliduser",
            password = "wrongpass"
        )
        val authHeader = "Jellyfin Client=\"Android\", Device=\"TestDevice\", DeviceId=\"test-device-id\", Version=\"1.0.0\""
        val errorResponseBody = "Unauthorized".toResponseBody("application/json".toMediaType())

        coEvery {
            authApi.login(fullUrl, loginRequest, authHeader)
        } returns Response.error(401, errorResponseBody)

        val result = authApi.login(fullUrl, loginRequest, authHeader)

        assertTrue(!result.isSuccessful)
        assertEquals(401, result.code())
        assertEquals("Unauthorized", result.errorBody()?.string())
    }

    @Test
    fun `login should return error response when server is not found`() = runTest {
        val fullUrl = "https://nonexistent-server.com/emby/Users/AuthenticateByName"
        val loginRequest = LoginRequest(
            username = "testuser",
            password = "testpass"
        )
        val authHeader = "Jellyfin Client=\"Android\", Device=\"TestDevice\", DeviceId=\"test-device-id\", Version=\"1.0.0\""
        val errorResponseBody = "Not Found".toResponseBody("application/json".toMediaType())

        coEvery {
            authApi.login(fullUrl, loginRequest, authHeader)
        } returns Response.error(404, errorResponseBody)

        val result = authApi.login(fullUrl, loginRequest, authHeader)

        assertTrue(!result.isSuccessful)
        assertEquals(404, result.code())
        assertEquals("Not Found", result.errorBody()?.string())
    }

    @Test
    fun `login should return error response when server error occurs`() = runTest {
        val fullUrl = "https://test-server.com/emby/Users/AuthenticateByName"
        val loginRequest = LoginRequest(
            username = "testuser",
            password = "testpass"
        )
        val authHeader = "Jellyfin Client=\"Android\", Device=\"TestDevice\", DeviceId=\"test-device-id\", Version=\"1.0.0\""
        val errorResponseBody = "Internal Server Error".toResponseBody("application/json".toMediaType())

        coEvery {
            authApi.login(fullUrl, loginRequest, authHeader)
        } returns Response.error(500, errorResponseBody)

        val result = authApi.login(fullUrl, loginRequest, authHeader)

        assertTrue(!result.isSuccessful)
        assertEquals(500, result.code())
        assertEquals("Internal Server Error", result.errorBody()?.string())
    }

    @Test
    fun `login should handle different server URLs correctly`() = runTest {
        val serverUrls = listOf(
            "https://server1.com/emby/Users/AuthenticateByName",
            "https://192.168.1.100:8096/emby/Users/AuthenticateByName",
            "http://localhost:8096/jellyfin/Users/AuthenticateByName"
        )
        val loginRequest = LoginRequest(
            username = "testuser",
            password = "testpass"
        )
        val authHeader = "Jellyfin Client=\"Android\", Device=\"TestDevice\", DeviceId=\"test-device-id\", Version=\"1.0.0\""
        val expectedResponse = LoginResponse(
            accessToken = "test-access-token",
            user = UserResponse(
                id = "user-123",
                name = "Test User"
            ),
            serverId = "server-456"
        )

        serverUrls.forEach { url ->
            coEvery { authApi.login(url, loginRequest, authHeader) } returns Response.success(expectedResponse)
        }

        serverUrls.forEach { url ->
            val result = authApi.login(url, loginRequest, authHeader)
            assertTrue("Should be successful for URL: $url", result.isSuccessful)
            assertEquals(expectedResponse, result.body())
        }
    }

    @Test
    fun `login should handle different auth headers correctly`() = runTest {
        val fullUrl = "https://test-server.com/emby/Users/AuthenticateByName"
        val loginRequest = LoginRequest(
            username = "testuser",
            password = "testpass"
        )
        val authHeaders = listOf(
            "Jellyfin Client=\"Android\", Device=\"TestDevice\", DeviceId=\"test-device-id\", Version=\"1.0.0\"",
            "Jellyfin Client=\"Android\", Device=\"Phone\", DeviceId=\"phone-123\", Version=\"1.0.1\"",
            "Jellyfin Client=\"Android\", Device=\"Tablet\", DeviceId=\"tablet-456\", Version=\"1.0.2\""
        )
        val expectedResponse = LoginResponse(
            accessToken = "test-access-token",
            user = UserResponse(
                id = "user-123",
                name = "Test User"
            ),
            serverId = "server-456"
        )

        authHeaders.forEach { authHeader ->
            coEvery { authApi.login(fullUrl, loginRequest, authHeader) } returns Response.success(expectedResponse)
        }

        authHeaders.forEach { authHeader ->
            val result = authApi.login(fullUrl, loginRequest, authHeader)
            assertTrue("Should be successful for auth header: $authHeader", result.isSuccessful)
            assertEquals(expectedResponse, result.body())
        }
    }

    @Test
    fun `login should handle empty username and password`() = runTest {
        val fullUrl = "https://test-server.com/emby/Users/AuthenticateByName"
        val loginRequest = LoginRequest(
            username = "",
            password = ""
        )
        val authHeader = "Jellyfin Client=\"Android\", Device=\"TestDevice\", DeviceId=\"test-device-id\", Version=\"1.0.0\""
        val errorResponseBody = "Bad Request".toResponseBody("application/json".toMediaType())

        coEvery {
            authApi.login(fullUrl, loginRequest, authHeader)
        } returns Response.error(400, errorResponseBody)

        val result = authApi.login(fullUrl, loginRequest, authHeader)

        assertTrue(!result.isSuccessful)
        assertEquals(400, result.code())
        assertEquals("Bad Request", result.errorBody()?.string())
    }

    @Test
    fun `login should handle special characters in username and password`() = runTest {
        val fullUrl = "https://test-server.com/emby/Users/AuthenticateByName"
        val loginRequest = LoginRequest(
            username = "test@user.com",
            password = "p@ssw0rd!#$%"
        )
        val authHeader = "Jellyfin Client=\"Android\", Device=\"TestDevice\", DeviceId=\"test-device-id\", Version=\"1.0.0\""
        val expectedResponse = LoginResponse(
            accessToken = "test-access-token-special",
            user = UserResponse(
                id = "user-special-123",
                name = "Special User"
            ),
            serverId = "server-special-456"
        )

        coEvery {
            authApi.login(fullUrl, loginRequest, authHeader)
        } returns Response.success(expectedResponse)

        val result = authApi.login(fullUrl, loginRequest, authHeader)

        assertTrue(result.isSuccessful)
        assertNotNull(result.body())
        assertEquals(expectedResponse, result.body())
        assertEquals("test-access-token-special", result.body()?.accessToken)
        assertEquals("user-special-123", result.body()?.user?.id)
        assertEquals("Special User", result.body()?.user?.name)
        assertEquals("server-special-456", result.body()?.serverId)
    }
}
