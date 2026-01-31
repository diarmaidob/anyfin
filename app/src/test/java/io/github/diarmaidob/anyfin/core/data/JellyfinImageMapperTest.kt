package io.github.diarmaidob.anyfin.core.data

import coil3.request.Options
import io.github.diarmaidob.anyfin.core.entity.JellyfinImage
import io.github.diarmaidob.anyfin.core.entity.SessionState
import io.github.diarmaidob.anyfin.core.session.SessionRepo
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class JellyfinImageMapperTest {

    @MockK
    lateinit var sessionRepo: SessionRepo

    @MockK
    lateinit var options: Options

    private lateinit var mapper: JellyfinImageMapper

    private val baseUrl = "https://example.com"
    private val itemId = "item_123"
    private val tag = "tag_abc"

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        mapper = JellyfinImageMapper(sessionRepo)
    }

    @Test
    fun `map returns null when tag is null`() {
        val `data` = JellyfinImage.Primary(itemId, null)

        val result = mapper.map(`data`, options)

        assertEquals(null, result)
    }

    @Test
    fun `map returns null when session state is not LoggedIn`() {
        val `data` = JellyfinImage.Primary(itemId, tag)
        every { sessionRepo.getCurrentSessionState() } returns SessionState.LoggedOut

        val result = mapper.map(`data`, options)

        assertEquals(null, result)
    }

    @Test
    fun `map returns primary image url when logged in`() {
        val `data` = JellyfinImage.Primary(itemId, tag)
        val sessionState = SessionState.LoggedIn(baseUrl, "token", "userId")
        every { sessionRepo.getCurrentSessionState() } returns sessionState

        val result = mapper.map(`data`, options)

        assertEquals("$baseUrl/Items/$itemId/Images/Primary?tag=$tag", result)
    }

    @Test
    fun `map returns backdrop image url when logged in`() {
        val `data` = JellyfinImage.Backdrop(itemId, tag)
        val sessionState = SessionState.LoggedIn(baseUrl, "token", "userId")
        every { sessionRepo.getCurrentSessionState() } returns sessionState

        val result = mapper.map(`data`, options)

        assertEquals("$baseUrl/Items/$itemId/Images/Backdrop?tag=$tag", result)
    }

    @Test
    fun `map returns logo image url when logged in`() {
        val `data` = JellyfinImage.Logo(itemId, tag)
        val sessionState = SessionState.LoggedIn(baseUrl, "token", "userId")
        every { sessionRepo.getCurrentSessionState() } returns sessionState

        val result = mapper.map(`data`, options)

        assertEquals("$baseUrl/Items/$itemId/Images/Logo?tag=$tag", result)
    }

    @Test
    fun `map returns thumb image url when logged in`() {
        val `data` = JellyfinImage.Thumb(itemId, tag)
        val sessionState = SessionState.LoggedIn(baseUrl, "token", "userId")
        every { sessionRepo.getCurrentSessionState() } returns sessionState

        val result = mapper.map(`data`, options)

        assertEquals("$baseUrl/Items/$itemId/Images/Thumb?tag=$tag", result)
    }
}