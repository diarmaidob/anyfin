package io.github.diarmaidob.anyfin.core.auth.data.api

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthRequestTest {

    private lateinit var moshi: Moshi
    private lateinit var jsonAdapter: JsonAdapter<LoginRequest>

    @Before
    fun setUp() {
        moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        jsonAdapter = moshi.adapter(LoginRequest::class.java)
    }

    @Test
    fun `LoginRequest should create with valid username and password`() {
        val username = "testuser"
        val password = "testpass"

        val loginRequest = LoginRequest(username, password)

        assertEquals(username, loginRequest.username)
        assertEquals(password, loginRequest.password)
    }

    @Test
    fun `LoginRequest should handle empty strings`() {
        val username = ""
        val password = ""

        val loginRequest = LoginRequest(username, password)

        assertEquals(username, loginRequest.username)
        assertEquals(password, loginRequest.password)
    }

    @Test
    fun `LoginRequest should handle special characters`() {
        val username = "test@user.com"
        val password = "p@ssw0rd!#$%"

        val loginRequest = LoginRequest(username, password)

        assertEquals(username, loginRequest.username)
        assertEquals(password, loginRequest.password)
    }

    @Test
    fun `LoginRequest should handle whitespace characters`() {
        val username = "test user"
        val password = "test pass"

        val loginRequest = LoginRequest(username, password)

        assertEquals(username, loginRequest.username)
        assertEquals(password, loginRequest.password)
    }

    @Test
    fun `LoginRequest should handle unicode characters`() {
        val username = "测试用户"
        val password = "测试密码123"

        val loginRequest = LoginRequest(username, password)

        assertEquals(username, loginRequest.username)
        assertEquals(password, loginRequest.password)
    }

    @Test
    fun `LoginRequest should serialize to JSON with correct field names`() {
        val loginRequest = LoginRequest("testuser", "testpass")

        val json = jsonAdapter.toJson(loginRequest)

        assertNotNull(json)
        assertTrue(json.contains("\"Username\":\"testuser\""))
        assertTrue(json.contains("\"Pw\":\"testpass\""))
    }

    @Test
    fun `LoginRequest should deserialize from JSON with correct field names`() {
        val json = """{"Username":"testuser","Pw":"testpass"}"""

        val loginRequest = jsonAdapter.fromJson(json)

        assertNotNull(loginRequest)
        assertEquals("testuser", loginRequest?.username)
        assertEquals("testpass", loginRequest?.password)
    }

    @Test
    fun `LoginRequest should handle JSON with empty strings`() {
        val json = """{"Username":"","Pw":""}"""

        val loginRequest = jsonAdapter.fromJson(json)

        assertNotNull(loginRequest)
        assertEquals("", loginRequest?.username)
        assertEquals("", loginRequest?.password)
    }

    @Test
    fun `LoginRequest should handle JSON with special characters`() {
        val json = """{"Username":"test@user.com","Pw":"p@ssw0rd!#$%"}"""

        val loginRequest = jsonAdapter.fromJson(json)

        assertNotNull(loginRequest)
        assertEquals("test@user.com", loginRequest?.username)
        assertEquals("p@ssw0rd!#$%", loginRequest?.password)
    }

    @Test
    fun `LoginRequest should handle JSON with unicode characters`() {
        val json = """{"Username":"测试用户","Pw":"测试密码123"}"""

        val loginRequest = jsonAdapter.fromJson(json)

        assertNotNull(loginRequest)
        assertEquals("测试用户", loginRequest?.username)
        assertEquals("测试密码123", loginRequest?.password)
    }

    @Test
    fun `LoginRequest should handle JSON with escaped characters`() {
        val json = """{"Username":"test\"user","Pw":"test\\pass"}"""

        val loginRequest = jsonAdapter.fromJson(json)

        assertNotNull(loginRequest)
        assertEquals("test\"user", loginRequest?.username)
        assertEquals("test\\pass", loginRequest?.password)
    }

    @Test
    fun `LoginRequest should serialize and deserialize correctly`() {
        val originalRequest = LoginRequest("testuser", "testpass")

        val json = jsonAdapter.toJson(originalRequest)
        val deserializedRequest = jsonAdapter.fromJson(json)

        assertNotNull(deserializedRequest)
        assertEquals(originalRequest.username, deserializedRequest?.username)
        assertEquals(originalRequest.password, deserializedRequest?.password)
    }

    @Test
    fun `LoginRequest should handle JSON with extra whitespace`() {
        val json = """ { "Username" : "testuser" , "Pw" : "testpass" } """

        val loginRequest = jsonAdapter.fromJson(json)

        assertNotNull(loginRequest)
        assertEquals("testuser", loginRequest?.username)
        assertEquals("testpass", loginRequest?.password)
    }


    @Test
    fun `LoginRequest should be equal when properties match`() {
        val request1 = LoginRequest("testuser", "testpass")
        val request2 = LoginRequest("testuser", "testpass")

        assertEquals(request1, request2)
        assertEquals(request1.hashCode(), request2.hashCode())
    }

    @Test
    fun `LoginRequest should not be equal when properties differ`() {
        val request1 = LoginRequest("testuser", "testpass")
        val request2 = LoginRequest("differentuser", "testpass")
        val request3 = LoginRequest("testuser", "differentpass")

        assertTrue(request1 != request2)
        assertTrue(request1 != request3)
        assertTrue(request2 != request3)
    }

    @Test
    fun `LoginRequest toString should contain property values`() {
        val loginRequest = LoginRequest("testuser", "testpass")

        val toString = loginRequest.toString()

        assertTrue(toString.contains("testuser"))
        assertTrue(toString.contains("testpass"))
        assertTrue(toString.contains("LoginRequest"))
    }

    @Test
    fun `LoginRequest copy should create identical instance`() {
        val originalRequest = LoginRequest("testuser", "testpass")

        val copiedRequest = originalRequest.copy()

        assertEquals(originalRequest, copiedRequest)
        assertEquals(originalRequest.username, copiedRequest.username)
        assertEquals(originalRequest.password, copiedRequest.password)
    }

    @Test
    fun `LoginRequest copy with new values should update correctly`() {
        val originalRequest = LoginRequest("testuser", "testpass")

        val updatedRequest = originalRequest.copy(
            username = "newuser",
            password = "newpass"
        )

        assertEquals("newuser", updatedRequest.username)
        assertEquals("newpass", updatedRequest.password)
        assertTrue(originalRequest != updatedRequest)
    }

    @Test
    fun `LoginRequest should handle very long strings`() {
        val longUsername = "a".repeat(1000)
        val longPassword = "b".repeat(1000)

        val loginRequest = LoginRequest(longUsername, longPassword)

        assertEquals(longUsername, loginRequest.username)
        assertEquals(longPassword, loginRequest.password)
    }

    @Test
    fun `LoginRequest should serialize very long strings correctly`() {
        val longUsername = "a".repeat(100)
        val longPassword = "b".repeat(100)
        val loginRequest = LoginRequest(longUsername, longPassword)

        val json = jsonAdapter.toJson(loginRequest)
        val deserializedRequest = jsonAdapter.fromJson(json)

        assertNotNull(deserializedRequest)
        assertEquals(longUsername, deserializedRequest?.username)
        assertEquals(longPassword, deserializedRequest?.password)
    }
}
