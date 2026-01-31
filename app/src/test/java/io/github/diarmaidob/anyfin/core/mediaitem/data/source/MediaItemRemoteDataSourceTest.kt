package io.github.diarmaidob.anyfin.core.mediaitem.data.source

import io.github.diarmaidob.anyfin.core.common.DataLoadError
import io.github.diarmaidob.anyfin.core.entity.MediaItemQuery
import io.github.diarmaidob.anyfin.core.entity.SessionState
import io.github.diarmaidob.anyfin.core.mediaitem.data.api.MediaItemApi
import io.github.diarmaidob.anyfin.core.mediaitem.data.api.MediaItemResponse
import io.github.diarmaidob.anyfin.core.mediaitem.data.api.MediaItemsResponse
import io.github.diarmaidob.anyfin.core.session.SessionRepo
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@ExperimentalCoroutinesApi
class MediaItemRemoteDataSourceTest {

    @MockK
    lateinit var api: MediaItemApi

    @MockK
    lateinit var sessionRepo: SessionRepo

    private lateinit var subject: MediaItemRemoteDataSource
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
        subject = MediaItemRemoteDataSource(api, sessionRepo, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `fetchBatch throws AuthError when session is LoggedOut`() = runTest {
        every { sessionRepo.getCurrentSessionState() } returns SessionState.LoggedOut
        val queries = listOf(MediaItemQuery.Resume())

        try {
            subject.fetchBatch(queries)
            assertEquals("Expected exception", "Exception was not thrown")
        } catch (e: Exception) {
            assertEquals(DataLoadError.AuthError::class, e::class)
            assertEquals("User not authenticated", e.message)
        }
    }

    @Test
    fun `fetchBatch with Resume query returns mapped items`() = runTest {
        val userId = "user123"
        val query = MediaItemQuery.Resume(limit = 15, mediaTypes = setOf(MediaItemQuery.MediaType.VIDEO))
        val expectedItem = createMediaItem("1")
        val apiResponse = MediaItemsResponse(listOf(expectedItem), 1)
        val paramSlot = slot<Map<String, String>>()

        every { sessionRepo.getCurrentSessionState() } returns SessionState.LoggedIn("url", "token", userId)
        coEvery { api.getResumeItems(userId, capture(paramSlot)) } returns Response.success(apiResponse)

        val result = subject.fetchBatch(listOf(query))

        assertEquals(1, result.size)
        assertEquals(listOf(expectedItem), result[query])

        val params = paramSlot.captured
        assertEquals("15", params["Limit"])
        assertEquals("Video", params["MediaTypes"])
        assertEquals("DatePlayed", params["SortBy"])
        assertEquals("Descending", params["SortOrder"])
    }

    @Test
    fun `fetchBatch with NextUp query returns mapped items`() = runTest {
        val userId = "user123"
        val seriesId = "series99"
        val query = MediaItemQuery.NextUp(limit = 10, seriesId = seriesId)
        val expectedItem = createMediaItem("2")
        val apiResponse = MediaItemsResponse(listOf(expectedItem), 1)
        val paramSlot = slot<Map<String, String>>()

        every { sessionRepo.getCurrentSessionState() } returns SessionState.LoggedIn("url", "token", userId)
        coEvery { api.getNextUpItems(capture(paramSlot)) } returns Response.success(apiResponse)

        val result = subject.fetchBatch(listOf(query))

        assertEquals(listOf(expectedItem), result[query])

        val params = paramSlot.captured
        assertEquals("10", params["Limit"])
        assertEquals(userId, params["UserId"])
        assertEquals(seriesId, params["SeriesId"])
    }

    @Test
    fun `fetchBatch with Latest query returns direct list`() = runTest {
        val userId = "user123"
        val parentId = "parent55"
        val query = MediaItemQuery.Latest(limit = 5, parentId = parentId, mediaTypes = setOf(MediaItemQuery.MediaType.MOVIE))
        val expectedItem = createMediaItem("3")
        val paramSlot = slot<Map<String, String>>()

        every { sessionRepo.getCurrentSessionState() } returns SessionState.LoggedIn("url", "token", userId)
        coEvery { api.getLatestItems(userId, capture(paramSlot)) } returns Response.success(listOf(expectedItem))

        val result = subject.fetchBatch(listOf(query))

        assertEquals(listOf(expectedItem), result[query])

        val params = paramSlot.captured
        assertEquals("5", params["Limit"])
        assertEquals("Movie", params["IncludeItemTypes"])
        assertEquals(parentId, params["ParentId"])
    }

    @Test
    fun `fetchBatch with Browse query constructs correct parameters`() = runTest {
        val userId = "user123"
        val query = MediaItemQuery.Browse(
            cacheKey = "browse",
            limit = 50,
            isRecursive = true,
            sortOrder = MediaItemQuery.SortOrder.DESCENDING,
            sortBy = listOf(MediaItemQuery.Sort.DATE_CREATED),
            filters = setOf(MediaItemQuery.Filter.IS_UNPLAYED),
            mediaTypes = setOf(MediaItemQuery.MediaType.EPISODE),
            excludeItemTypes = setOf("Unknown"),
            parentId = "root"
        )
        val expectedItem = createMediaItem("4")
        val apiResponse = MediaItemsResponse(listOf(expectedItem), 1)
        val paramSlot = slot<Map<String, String>>()

        every { sessionRepo.getCurrentSessionState() } returns SessionState.LoggedIn("url", "token", userId)
        coEvery { api.getItems(userId, capture(paramSlot)) } returns Response.success(apiResponse)

        val result = subject.fetchBatch(listOf(query))

        assertEquals(listOf(expectedItem), result[query])

        val params = paramSlot.captured
        assertEquals("50", params["Limit"])
        assertEquals("true", params["Recursive"])
        assertEquals("Descending", params["SortOrder"])
        assertEquals("DateCreated", params["SortBy"])
        assertEquals("root", params["ParentId"])
        assertEquals("Episode", params["IncludeItemTypes"])
        assertEquals("IsUnplayed", params["Filters"])
        assertEquals("Unknown", params["ExcludeItemTypes"])
    }

    @Test
    fun `fetchBatch with UserViews query returns mapped items`() = runTest {
        val userId = "user123"
        val query = MediaItemQuery.UserViews()
        val expectedItem = createMediaItem("5")
        val apiResponse = MediaItemsResponse(listOf(expectedItem), 1)

        every { sessionRepo.getCurrentSessionState() } returns SessionState.LoggedIn("url", "token", userId)
        coEvery { api.getUserViews(userId) } returns Response.success(apiResponse)

        val result = subject.fetchBatch(listOf(query))

        assertEquals(listOf(expectedItem), result[query])
    }

    @Test
    fun `fetchBatch handles multiple queries concurrently`() = runTest {
        val userId = "user123"
        val query1 = MediaItemQuery.Resume()
        val query2 = MediaItemQuery.UserViews()
        val item1 = createMediaItem("A")
        val item2 = createMediaItem("B")

        every { sessionRepo.getCurrentSessionState() } returns SessionState.LoggedIn("url", "token", userId)
        coEvery { api.getResumeItems(userId, any()) } returns Response.success(MediaItemsResponse(listOf(item1), 1))
        coEvery { api.getUserViews(userId) } returns Response.success(MediaItemsResponse(listOf(item2), 1))

        val result = subject.fetchBatch(listOf(query1, query2))

        assertEquals(2, result.size)
        assertEquals(listOf(item1), result[query1])
        assertEquals(listOf(item2), result[query2])
    }

    @Test
    fun `fetchBatch handles null response body by returning empty list`() = runTest {
        val userId = "user123"
        val query = MediaItemQuery.Resume()

        every { sessionRepo.getCurrentSessionState() } returns SessionState.LoggedIn("url", "token", userId)
        coEvery { api.getResumeItems(userId, any()) } returns Response.success(null)

        val result = subject.fetchBatch(listOf(query))

        assertEquals(emptyList<MediaItemResponse>(), result[query])
    }

    @Test
    fun `fetchItemDetails returns item on success`() = runTest {
        val userId = "user123"
        val itemId = "item-777"
        val expectedItem = createMediaItem(itemId)

        every { sessionRepo.getCurrentSessionState() } returns SessionState.LoggedIn("url", "token", userId)
        coEvery { api.getItemDetails(userId, itemId) } returns Response.success(expectedItem)

        val result = subject.fetchItemDetails(itemId)

        assertEquals(expectedItem, result)
    }

    @Test
    fun `fetchItemDetails throws AuthError when LoggedOut`() = runTest {
        every { sessionRepo.getCurrentSessionState() } returns SessionState.LoggedOut
        val itemId = "item-777"

        try {
            subject.fetchItemDetails(itemId)
            assertEquals("Expected exception", "Exception was not thrown")
        } catch (e: Exception) {
            assertEquals(DataLoadError.AuthError::class, e::class)
        }
    }

    @Test
    fun `fetchItemDetails throws IllegalStateException on null body`() = runTest {
        val userId = "user123"
        val itemId = "item-777"

        every { sessionRepo.getCurrentSessionState() } returns SessionState.LoggedIn("url", "token", userId)
        coEvery { api.getItemDetails(userId, itemId) } returns Response.success(null)

        try {
            subject.fetchItemDetails(itemId)
            assertEquals("Expected exception", "Exception was not thrown")
        } catch (e: Exception) {
            assertEquals(IllegalStateException::class, e::class)
            assertEquals("Empty response body from API for item $itemId", e.message)
        }
    }

    @Test
    fun `fetchBatch with Latest handles null parentId correctly`() = runTest {
        val userId = "user123"
        val query = MediaItemQuery.Latest(parentId = null)
        val paramSlot = slot<Map<String, String>>()

        every { sessionRepo.getCurrentSessionState() } returns SessionState.LoggedIn("url", "token", userId)
        coEvery { api.getLatestItems(userId, capture(paramSlot)) } returns Response.success(emptyList())

        subject.fetchBatch(listOf(query))

        assertEquals("", paramSlot.captured["ParentId"])
    }

    @Test
    fun `fetchBatch with Browse handles optional parameters being empty`() = runTest {
        val userId = "user123"
        val query = MediaItemQuery.Browse(
            cacheKey = "browse",
            parentId = null,
            sortBy = emptyList(),
            filters = emptySet(),
            mediaTypes = emptySet(),
            excludeItemTypes = emptySet()
        )
        val paramSlot = slot<Map<String, String>>()

        every { sessionRepo.getCurrentSessionState() } returns SessionState.LoggedIn("url", "token", userId)
        coEvery { api.getItems(userId, capture(paramSlot)) } returns Response.success(MediaItemsResponse(emptyList(), 0))

        subject.fetchBatch(listOf(query))

        val params = paramSlot.captured
        assertEquals(null, params["ParentId"])
        assertEquals(null, params["SortBy"])
        assertEquals(null, params["Filters"])
        assertEquals(null, params["IncludeItemTypes"])
        assertEquals(null, params["ExcludeItemTypes"])
    }

    private fun createMediaItem(id: String): MediaItemResponse {
        return MediaItemResponse(
            id = id,
            name = "Test Item $id",
            originalTitle = null,
            serverId = null,
            etag = null,
            type = null,
            mediaType = null,
            isFolder = null,
            overview = null,
            taglines = null,
            genres = null,
            productionYear = null,
            premiereDate = null,
            endDate = null,
            dateCreated = null,
            officialRating = null,
            criticRating = null,
            communityRating = null,
            customRating = null,
            productionLocations = null,
            path = null,
            seriesName = null,
            seriesId = null,
            seasonName = null,
            seasonId = null,
            indexNumber = null,
            parentIndexNumber = null,
            indexNumberEnd = null,
            parentId = null,
            collectionType = null,
            album = null,
            albumId = null,
            albumArtist = null,
            artists = null,
            artistItemResponses = null,
            runTimeTicks = null,
            container = null,
            video3DFormat = null,
            aspectRatio = null,
            mediaSourceResponses = null,
            playAccess = null,
            canDownload = null,
            people = null,
            imageTags = null,
            backdropImageTags = null,
            parentLogoImageTag = null,
            parentThumbImageTag = null,
            seriesPrimaryImageTag = null,
            parentBackdropImageTags = null,
            sortName = null,
            forcedSortName = null,
            displayPreferencesId = null,
            userData = null,
            providerIds = null,
            studios = null,
            chapterResponses = null,
            partCount = null,
            childCount = null,
            recursiveItemCount = null,
            specialFeatureCount = null,
            localTrailerCount = null
        )
    }
}