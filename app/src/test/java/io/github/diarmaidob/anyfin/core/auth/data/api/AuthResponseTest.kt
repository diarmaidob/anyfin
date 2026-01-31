package io.github.diarmaidob.anyfin.core.auth.data.api

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthResponseTest {

    private lateinit var moshi: Moshi
    private lateinit var loginResponseAdapter: JsonAdapter<LoginResponse>
    private lateinit var userResponseAdapter: JsonAdapter<UserResponse>

    @Before
    fun setUp() {
        moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        loginResponseAdapter = moshi.adapter(LoginResponse::class.java)
        userResponseAdapter = moshi.adapter(UserResponse::class.java)
    }

    @Test
    fun `UserResponse should create with valid id and name`() {
        val id = "user123"
        val name = "Test User"

        val userResponse = UserResponse(id, name)

        assertEquals(id, userResponse.id)
        assertEquals(name, userResponse.name)
    }

    @Test
    fun `UserResponse should create with null name`() {
        val id = "user123"
        val name = null

        val userResponse = UserResponse(id, name)

        assertEquals(id, userResponse.id)
        assertNull(userResponse.name)
    }

    @Test
    fun `UserResponse should handle empty strings`() {
        val id = ""
        val name = ""

        val userResponse = UserResponse(id, name)

        assertEquals(id, userResponse.id)
        assertEquals(name, userResponse.name)
    }

    @Test
    fun `UserResponse should handle special characters`() {
        val id = "user@123.com"
        val name = "Test User!@#$%"

        val userResponse = UserResponse(id, name)

        assertEquals(id, userResponse.id)
        assertEquals(name, userResponse.name)
    }

    @Test
    fun `UserResponse should handle unicode characters`() {
        val id = "用户123"
        val name = "测试用户"

        val userResponse = UserResponse(id, name)

        assertEquals(id, userResponse.id)
        assertEquals(name, userResponse.name)
    }

    @Test
    fun `UserResponse should serialize to JSON with correct field names`() {
        val userResponse = UserResponse("user123", "Test User")

        val json = userResponseAdapter.toJson(userResponse)

        assertNotNull(json)
        assertTrue(json.contains("\"Id\":\"user123\""))
        assertTrue(json.contains("\"Name\":\"Test User\""))
    }

    @Test
    fun `UserResponse should serialize to JSON with null name`() {
        val userResponse = UserResponse("user123", null)

        val json = userResponseAdapter.toJson(userResponse)

        assertNotNull(json)
        assertTrue(json.contains("\"Id\":\"user123\""))
        // Moshi might omit null fields entirely or include them as null
        assertTrue(json.contains("\"Name\":null") || !json.contains("\"Name\""))
    }

    @Test
    fun `UserResponse should deserialize from JSON with correct field names`() {
        val json = """{"Id":"user123","Name":"Test User"}"""

        val userResponse = userResponseAdapter.fromJson(json)

        assertNotNull(userResponse)
        assertEquals("user123", userResponse?.id)
        assertEquals("Test User", userResponse?.name)
    }

    @Test
    fun `UserResponse should deserialize from JSON with null name`() {
        val json = """{"Id":"user123","Name":null}"""

        val userResponse = userResponseAdapter.fromJson(json)

        assertNotNull(userResponse)
        assertEquals("user123", userResponse?.id)
        assertNull(userResponse?.name)
    }

    @Test
    fun `UserResponse should handle JSON with unicode characters`() {
        val json = """{"Id":"用户123","Name":"测试用户"}"""

        val userResponse = userResponseAdapter.fromJson(json)

        assertNotNull(userResponse)
        assertEquals("用户123", userResponse?.id)
        assertEquals("测试用户", userResponse?.name)
    }

    @Test
    fun `UserResponse should be equal when properties match`() {
        val user1 = UserResponse("user123", "Test User")
        val user2 = UserResponse("user123", "Test User")

        assertEquals(user1, user2)
        assertEquals(user1.hashCode(), user2.hashCode())
    }

    @Test
    fun `UserResponse should not be equal when properties differ`() {
        val user1 = UserResponse("user123", "Test User")
        val user2 = UserResponse("user456", "Test User")
        val user3 = UserResponse("user123", "Different User")

        assertTrue(user1 != user2)
        assertTrue(user1 != user3)
        assertTrue(user2 != user3)
    }

    @Test
    fun `LoginResponse should create with valid accessToken, user, and serverId`() {
        val accessToken = "token123"
        val user = UserResponse("user123", "Test User")
        val serverId = "server456"

        val loginResponse = LoginResponse(accessToken, user, serverId)

        assertEquals(accessToken, loginResponse.accessToken)
        assertEquals(user, loginResponse.user)
        assertEquals(serverId, loginResponse.serverId)
    }

    @Test
    fun `LoginResponse should create with null serverId`() {
        val accessToken = "token123"
        val user = UserResponse("user123", "Test User")
        val serverId = null

        val loginResponse = LoginResponse(accessToken, user, serverId)

        assertEquals(accessToken, loginResponse.accessToken)
        assertEquals(user, loginResponse.user)
        assertNull(loginResponse.serverId)
    }

    @Test
    fun `LoginResponse should handle empty strings`() {
        val accessToken = ""
        val user = UserResponse("", "")
        val serverId = ""

        val loginResponse = LoginResponse(accessToken, user, serverId)

        assertEquals(accessToken, loginResponse.accessToken)
        assertEquals(user, loginResponse.user)
        assertEquals(serverId, loginResponse.serverId)
    }

    @Test
    fun `LoginResponse should handle special characters`() {
        val accessToken = "token@123!#$%"
        val user = UserResponse("user@123.com", "Test User!@#$%")
        val serverId = "server@456!#$%"

        val loginResponse = LoginResponse(accessToken, user, serverId)

        assertEquals(accessToken, loginResponse.accessToken)
        assertEquals(user, loginResponse.user)
        assertEquals(serverId, loginResponse.serverId)
    }

    @Test
    fun `LoginResponse should handle unicode characters`() {
        val accessToken = "令牌123"
        val user = UserResponse("用户123", "测试用户")
        val serverId = "服务器456"

        val loginResponse = LoginResponse(accessToken, user, serverId)

        assertEquals(accessToken, loginResponse.accessToken)
        assertEquals(user, loginResponse.user)
        assertEquals(serverId, loginResponse.serverId)
    }

    @Test
    fun `LoginResponse should serialize to JSON with correct field names`() {
        val user = UserResponse("user123", "Test User")
        val loginResponse = LoginResponse("token123", user, "server456")

        val json = loginResponseAdapter.toJson(loginResponse)

        assertNotNull(json)
        assertTrue(json.contains("\"AccessToken\":\"token123\""))
        assertTrue(json.contains("\"User\":{"))
        assertTrue(json.contains("\"ServerId\":\"server456\""))
    }

    @Test
    fun `LoginResponse should serialize to JSON with null serverId`() {
        val user = UserResponse("user123", "Test User")
        val loginResponse = LoginResponse("token123", user, null)

        val json = loginResponseAdapter.toJson(loginResponse)

        assertNotNull(json)
        assertTrue(json.contains("\"AccessToken\":\"token123\""))
        assertTrue(json.contains("\"User\":{"))
        // Moshi might omit null fields entirely or include them as null
        assertTrue(json.contains("\"ServerId\":null") || !json.contains("\"ServerId\""))
    }

    @Test
    fun `LoginResponse should deserialize from JSON with correct field names`() {
        val json = """{"AccessToken":"token123","User":{"Id":"user123","Name":"Test User"},"ServerId":"server456"}"""

        val loginResponse = loginResponseAdapter.fromJson(json)

        assertNotNull(loginResponse)
        assertEquals("token123", loginResponse?.accessToken)
        assertEquals("user123", loginResponse?.user?.id)
        assertEquals("Test User", loginResponse?.user?.name)
        assertEquals("server456", loginResponse?.serverId)
    }

    @Test
    fun `LoginResponse should deserialize from JSON with null serverId`() {
        val json = """{"AccessToken":"token123","User":{"Id":"user123","Name":"Test User"},"ServerId":null}"""

        val loginResponse = loginResponseAdapter.fromJson(json)

        assertNotNull(loginResponse)
        assertEquals("token123", loginResponse?.accessToken)
        assertEquals("user123", loginResponse?.user?.id)
        assertEquals("Test User", loginResponse?.user?.name)
        assertNull(loginResponse?.serverId)
    }

    @Test
    fun `LoginResponse should deserialize from JSON with null user name`() {
        val json = """{"AccessToken":"token123","User":{"Id":"user123","Name":null},"ServerId":"server456"}"""

        val loginResponse = loginResponseAdapter.fromJson(json)

        assertNotNull(loginResponse)
        assertEquals("token123", loginResponse?.accessToken)
        assertEquals("user123", loginResponse?.user?.id)
        assertNull(loginResponse?.user?.name)
        assertEquals("server456", loginResponse?.serverId)
    }

    @Test
    fun `LoginResponse should handle JSON with unicode characters`() {
        val json = """{"AccessToken":"令牌123","User":{"Id":"用户123","Name":"测试用户"},"ServerId":"服务器456"}"""

        val loginResponse = loginResponseAdapter.fromJson(json)

        assertNotNull(loginResponse)
        assertEquals("令牌123", loginResponse?.accessToken)
        assertEquals("用户123", loginResponse?.user?.id)
        assertEquals("测试用户", loginResponse?.user?.name)
        assertEquals("服务器456", loginResponse?.serverId)
    }

    @Test
    fun `LoginResponse should be equal when properties match`() {
        val user = UserResponse("user123", "Test User")
        val response1 = LoginResponse("token123", user, "server456")
        val response2 = LoginResponse("token123", user, "server456")

        assertEquals(response1, response2)
        assertEquals(response1.hashCode(), response2.hashCode())
    }

    @Test
    fun `LoginResponse should not be equal when properties differ`() {
        val user1 = UserResponse("user123", "Test User")
        val user2 = UserResponse("user456", "Different User")
        val response1 = LoginResponse("token123", user1, "server456")
        val response2 = LoginResponse("token456", user1, "server456")
        val response3 = LoginResponse("token123", user2, "server456")
        val response4 = LoginResponse("token123", user1, "server789")

        assertTrue(response1 != response2)
        assertTrue(response1 != response3)
        assertTrue(response1 != response4)
        assertTrue(response2 != response3)
        assertTrue(response2 != response4)
        assertTrue(response3 != response4)
    }

    @Test
    fun `LoginResponse toString should contain property values`() {
        val user = UserResponse("user123", "Test User")
        val loginResponse = LoginResponse("token123", user, "server456")

        val toString = loginResponse.toString()

        assertTrue(toString.contains("token123"))
        assertTrue(toString.contains("user123"))
        assertTrue(toString.contains("Test User"))
        assertTrue(toString.contains("server456"))
        assertTrue(toString.contains("LoginResponse"))
    }

    @Test
    fun `LoginResponse copy should create identical instance`() {
        val user = UserResponse("user123", "Test User")
        val originalResponse = LoginResponse("token123", user, "server456")

        val copiedResponse = originalResponse.copy()

        assertEquals(originalResponse, copiedResponse)
        assertEquals(originalResponse.accessToken, copiedResponse.accessToken)
        assertEquals(originalResponse.user, copiedResponse.user)
        assertEquals(originalResponse.serverId, copiedResponse.serverId)
    }

    @Test
    fun `LoginResponse copy with new values should update correctly`() {
        val originalUser = UserResponse("user123", "Test User")
        val newUser = UserResponse("user456", "New User")
        val originalResponse = LoginResponse("token123", originalUser, "server456")

        val updatedResponse = originalResponse.copy(
            accessToken = "newtoken",
            user = newUser,
            serverId = "newserver"
        )

        assertEquals("newtoken", updatedResponse.accessToken)
        assertEquals(newUser, updatedResponse.user)
        assertEquals("newserver", updatedResponse.serverId)
        assertTrue(originalResponse != updatedResponse)
    }

    @Test
    fun `LoginResponse should handle very long strings`() {
        val longToken = "t".repeat(1000)
        val longUserId = "u".repeat(1000)
        val longUserName = "n".repeat(1000)
        val longServerId = "s".repeat(1000)
        val user = UserResponse(longUserId, longUserName)

        val loginResponse = LoginResponse(longToken, user, longServerId)

        assertEquals(longToken, loginResponse.accessToken)
        assertEquals(longUserId, loginResponse.user.id)
        assertEquals(longUserName, loginResponse.user.name)
        assertEquals(longServerId, loginResponse.serverId)
    }

    @Test
    fun `LoginResponse should serialize and deserialize correctly`() {
        val user = UserResponse("user123", "Test User")
        val originalResponse = LoginResponse("token123", user, "server456")

        val json = loginResponseAdapter.toJson(originalResponse)
        val deserializedResponse = loginResponseAdapter.fromJson(json)

        assertNotNull(deserializedResponse)
        assertEquals(originalResponse.accessToken, deserializedResponse?.accessToken)
        assertEquals(originalResponse.user.id, deserializedResponse?.user?.id)
        assertEquals(originalResponse.user.name, deserializedResponse?.user?.name)
        assertEquals(originalResponse.serverId, deserializedResponse?.serverId)
    }

    @Test
    fun `UserResponse should serialize and deserialize correctly`() {
        val originalUser = UserResponse("user123", "Test User")

        val json = userResponseAdapter.toJson(originalUser)
        val deserializedUser = userResponseAdapter.fromJson(json)

        assertNotNull(deserializedUser)
        assertEquals(originalUser.id, deserializedUser?.id)
        assertEquals(originalUser.name, deserializedUser?.name)
    }
}
