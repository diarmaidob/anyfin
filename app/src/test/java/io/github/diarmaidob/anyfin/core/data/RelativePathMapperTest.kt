package io.github.diarmaidob.anyfin.core.data

import coil3.request.Options
import io.github.diarmaidob.anyfin.core.entity.SessionState
import io.github.diarmaidob.anyfin.core.session.SessionRepo
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RelativePathMapperTest {

    @MockK
    lateinit var sessionRepo: SessionRepo

    private lateinit var mapper: RelativePathMapper
    private val options = mockk<Options>()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mapper = RelativePathMapper(sessionRepo)
    }

    @Test
    fun `map returns null when data does not start with slash`() {
        val data = "https://example.com/image.png"
        val result = mapper.map(data, options)

        assertEquals(null, result)
    }

    @Test
    fun `map returns null when session state is LoggedOut`() {
        every { sessionRepo.getCurrentSessionState() } returns SessionState.LoggedOut

        val result = mapper.map("/Items/123", options)

        assertEquals(null, result)
    }

    @Test
    fun `map returns null when serverUrl is blank`() {
        val state = SessionState.LoggedIn(
            serverUrl = " ",
            authToken = "token",
            userId = "user"
        )
        every { sessionRepo.getCurrentSessionState() } returns state

        val result = mapper.map("/Items/123", options)

        assertEquals(null, result)
    }

    @Test
    fun `map prepends serverUrl when path is relative`() {
        val state = SessionState.LoggedIn(
            serverUrl = "http://192.168.0.1:8096",
            authToken = "token",
            userId = "user"
        )
        every { sessionRepo.getCurrentSessionState() } returns state

        val result = mapper.map("/Items/123/Images/Primary", options)

        val expected = "http://192.168.0.1:8096/Items/123/Images/Primary".toHttpUrl()
        assertEquals(expected, result)
    }

    @Test
    fun `map removes trailing slash from serverUrl before prepending`() {
        val state = SessionState.LoggedIn(
            serverUrl = "http://192.168.0.1:8096/",
            authToken = "token",
            userId = "user"
        )
        every { sessionRepo.getCurrentSessionState() } returns state

        val result = mapper.map("/Items/123", options)

        val expected = "http://192.168.0.1:8096/Items/123".toHttpUrl()
        assertEquals(expected, result)
    }

    @Test
    fun `map handles query parameters in relative path`() {
        val state = SessionState.LoggedIn(
            serverUrl = "https://myserver.com",
            authToken = "token",
            userId = "user"
        )
        every { sessionRepo.getCurrentSessionState() } returns state

        val result = mapper.map("/Items/1?fillHeight=10", options)

        val expected = "https://myserver.com/Items/1?fillHeight=10".toHttpUrl()
        assertEquals(expected, result)
    }

    @Test
    fun `map handles nested relative paths`() {
        val state = SessionState.LoggedIn(
            serverUrl = "https://myserver.com",
            authToken = "token",
            userId = "user"
        )
        every { sessionRepo.getCurrentSessionState() } returns state

        val result = mapper.map("/some/very/long/path/to/resource", options)

        val expected = "https://myserver.com/some/very/long/path/to/resource".toHttpUrl()
        assertEquals(expected, result)
    }
}