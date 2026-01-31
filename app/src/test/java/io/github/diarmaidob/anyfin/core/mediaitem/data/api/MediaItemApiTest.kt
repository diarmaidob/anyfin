package io.github.diarmaidob.anyfin.core.mediaitem.data.api

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

class MediaItemApiTest {

    private lateinit var mediaItemApi: MediaItemApi

    @Before
    fun setUp() {
        mediaItemApi = mockk()
    }

    @Test
    fun `getResumeItems should return successful response with valid data`() = runTest {
        val userId = "user-123"
        val params = mapOf("limit" to "10", "fields" to "Overview")
        val expectedMediaItems = listOf(
            createTestMediaItemResponse("item-1", "Test Movie 1"),
            createTestMediaItemResponse("item-2", "Test Movie 2")
        )
        val expectedResponse = MediaItemsResponse(
            items = expectedMediaItems,
            totalRecordCount = 2,
            startIndex = 0
        )

        coEvery {
            mediaItemApi.getResumeItems(userId, params)
        } returns Response.success(expectedResponse)

        val result = mediaItemApi.getResumeItems(userId, params)

        assertTrue(result.isSuccessful)
        assertNotNull(result.body())
        assertEquals(expectedResponse, result.body())
        assertEquals(2, result.body()?.items?.size)
        assertEquals(2, result.body()?.totalRecordCount)
        assertEquals(0, result.body()?.startIndex)
        assertEquals("item-1", result.body()?.items?.get(0)?.id)
        assertEquals("Test Movie 1", result.body()?.items?.get(0)?.name)
    }

    @Test
    fun `getResumeItems should return error response when request fails`() = runTest {
        val userId = "user-123"
        val params = mapOf("limit" to "10")
        val errorResponseBody = "Unauthorized".toResponseBody("application/json".toMediaType())

        coEvery {
            mediaItemApi.getResumeItems(userId, params)
        } returns Response.error(401, errorResponseBody)

        val result = mediaItemApi.getResumeItems(userId, params)

        assertTrue(!result.isSuccessful)
        assertEquals(401, result.code())
        assertEquals("Unauthorized", result.errorBody()?.string())
    }

    @Test
    fun `getNextUpItems should return successful response with valid data`() = runTest {
        val params = mapOf("userId" to "user-123", "limit" to "5")
        val expectedMediaItems = listOf(
            createTestMediaItemResponse("nextup-1", "Next Up Episode 1"),
            createTestMediaItemResponse("nextup-2", "Next Up Episode 2")
        )
        val expectedResponse = MediaItemsResponse(
            items = expectedMediaItems,
            totalRecordCount = 2,
            startIndex = 0
        )

        coEvery {
            mediaItemApi.getNextUpItems(params)
        } returns Response.success(expectedResponse)

        val result = mediaItemApi.getNextUpItems(params)

        assertTrue(result.isSuccessful)
        assertNotNull(result.body())
        assertEquals(expectedResponse, result.body())
        assertEquals(2, result.body()?.items?.size)
        assertEquals("nextup-1", result.body()?.items?.get(0)?.id)
        assertEquals("Next Up Episode 1", result.body()?.items?.get(0)?.name)
    }

    @Test
    fun `getNextUpItems should return error response when server error occurs`() = runTest {
        val params = mapOf("userId" to "user-123")
        val errorResponseBody = "Internal Server Error".toResponseBody("application/json".toMediaType())

        coEvery {
            mediaItemApi.getNextUpItems(params)
        } returns Response.error(500, errorResponseBody)

        val result = mediaItemApi.getNextUpItems(params)

        assertTrue(!result.isSuccessful)
        assertEquals(500, result.code())
        assertEquals("Internal Server Error", result.errorBody()?.string())
    }

    @Test
    fun `getLatestItems should return successful response with list of media items`() = runTest {
        val userId = "user-456"
        val params = mapOf("limit" to "10", "includeItemTypes" to "Movie")
        val expectedMediaItems = listOf(
            createTestMediaItemResponse("latest-1", "Latest Movie 1"),
            createTestMediaItemResponse("latest-2", "Latest Movie 2"),
            createTestMediaItemResponse("latest-3", "Latest Movie 3")
        )

        coEvery {
            mediaItemApi.getLatestItems(userId, params)
        } returns Response.success(expectedMediaItems)

        val result = mediaItemApi.getLatestItems(userId, params)

        assertTrue(result.isSuccessful)
        assertNotNull(result.body())
        assertEquals(expectedMediaItems, result.body())
        assertEquals(3, result.body()?.size)
        assertEquals("latest-1", result.body()?.get(0)?.id)
        assertEquals("Latest Movie 1", result.body()?.get(0)?.name)
    }

    @Test
    fun `getLatestItems should return error response when user not found`() = runTest {
        val userId = "nonexistent-user"
        val params = mapOf("limit" to "10")
        val errorResponseBody = "User Not Found".toResponseBody("application/json".toMediaType())

        coEvery {
            mediaItemApi.getLatestItems(userId, params)
        } returns Response.error(404, errorResponseBody)

        val result = mediaItemApi.getLatestItems(userId, params)

        assertTrue(!result.isSuccessful)
        assertEquals(404, result.code())
        assertEquals("User Not Found", result.errorBody()?.string())
    }

    @Test
    fun `getItems should return successful response with paginated data`() = runTest {
        val userId = "user-789"
        val params = mapOf(
            "startIndex" to "0",
            "limit" to "20",
            "recursive" to "true",
            "includeItemTypes" to "Movie,Series"
        )
        val expectedMediaItems = listOf(
            createTestMediaItemResponse("item-1", "Movie 1"),
            createTestMediaItemResponse("item-2", "Series 1"),
            createTestMediaItemResponse("item-3", "Movie 2")
        )
        val expectedResponse = MediaItemsResponse(
            items = expectedMediaItems,
            totalRecordCount = 150,
            startIndex = 0
        )

        coEvery {
            mediaItemApi.getItems(userId, params)
        } returns Response.success(expectedResponse)

        val result = mediaItemApi.getItems(userId, params)

        assertTrue(result.isSuccessful)
        assertNotNull(result.body())
        assertEquals(expectedResponse, result.body())
        assertEquals(3, result.body()?.items?.size)
        assertEquals(150, result.body()?.totalRecordCount)
        assertEquals(0, result.body()?.startIndex)
    }

    @Test
    fun `getItems should return error response when parameters are invalid`() = runTest {
        val userId = "user-789"
        val params = mapOf("limit" to "-1")
        val errorResponseBody = "Bad Request".toResponseBody("application/json".toMediaType())

        coEvery {
            mediaItemApi.getItems(userId, params)
        } returns Response.error(400, errorResponseBody)

        val result = mediaItemApi.getItems(userId, params)

        assertTrue(!result.isSuccessful)
        assertEquals(400, result.code())
        assertEquals("Bad Request", result.errorBody()?.string())
    }

    @Test
    fun `getItemDetails should return successful response with single media item`() = runTest {
        val userId = "user-123"
        val itemId = "item-456"
        val expectedItem = createTestMediaItemResponse(
            id = itemId,
            name = "Detailed Movie",
            overview = "This is a detailed movie overview",
            productionYear = 2023
        )

        coEvery {
            mediaItemApi.getItemDetails(userId, itemId)
        } returns Response.success(expectedItem)

        val result = mediaItemApi.getItemDetails(userId, itemId)

        assertTrue(result.isSuccessful)
        assertNotNull(result.body())
        assertEquals(expectedItem, result.body())
        assertEquals(itemId, result.body()?.id)
        assertEquals("Detailed Movie", result.body()?.name)
        assertEquals("This is a detailed movie overview", result.body()?.overview)
        assertEquals(2023, result.body()?.productionYear)
    }

    @Test
    fun `getItemDetails should return error response when item not found`() = runTest {
        val userId = "user-123"
        val itemId = "nonexistent-item"
        val errorResponseBody = "Item Not Found".toResponseBody("application/json".toMediaType())

        coEvery {
            mediaItemApi.getItemDetails(userId, itemId)
        } returns Response.error(404, errorResponseBody)

        val result = mediaItemApi.getItemDetails(userId, itemId)

        assertTrue(!result.isSuccessful)
        assertEquals(404, result.code())
        assertEquals("Item Not Found", result.errorBody()?.string())
    }

    @Test
    fun `getUserViews should return successful response with user views`() = runTest {
        val userId = "user-123"
        val expectedViews = listOf(
            createTestMediaItemResponse("view-1", "Movies"),
            createTestMediaItemResponse("view-2", "TV Shows"),
            createTestMediaItemResponse("view-3", "Music")
        )
        val expectedResponse = MediaItemsResponse(
            items = expectedViews,
            totalRecordCount = 3,
            startIndex = 0
        )

        coEvery {
            mediaItemApi.getUserViews(userId)
        } returns Response.success(expectedResponse)

        val result = mediaItemApi.getUserViews(userId)

        assertTrue(result.isSuccessful)
        assertNotNull(result.body())
        assertEquals(expectedResponse, result.body())
        assertEquals(3, result.body()?.items?.size)
        assertEquals("view-1", result.body()?.items?.get(0)?.id)
        assertEquals("Movies", result.body()?.items?.get(0)?.name)
    }

    @Test
    fun `getUserViews should return error response when user unauthorized`() = runTest {
        val userId = "unauthorized-user"
        val errorResponseBody = "Forbidden".toResponseBody("application/json".toMediaType())

        coEvery {
            mediaItemApi.getUserViews(userId)
        } returns Response.error(403, errorResponseBody)

        val result = mediaItemApi.getUserViews(userId)

        assertTrue(!result.isSuccessful)
        assertEquals(403, result.code())
        assertEquals("Forbidden", result.errorBody()?.string())
    }

    @Test
    fun `getResumeItems should handle empty response correctly`() = runTest {
        val userId = "user-empty"
        val params = mapOf("limit" to "10")
        val expectedResponse = MediaItemsResponse<MediaItemResponse>(
            items = emptyList(),
            totalRecordCount = 0,
            startIndex = 0
        )

        coEvery {
            mediaItemApi.getResumeItems(userId, params)
        } returns Response.success(expectedResponse)

        val result = mediaItemApi.getResumeItems(userId, params)

        assertTrue(result.isSuccessful)
        assertNotNull(result.body())
        assertEquals(0, result.body()?.items?.size)
        assertEquals(0, result.body()?.totalRecordCount)
    }

    @Test
    fun `getItemDetails should handle null fields correctly`() = runTest {
        val userId = "user-123"
        val itemId = "item-nulls"
        val expectedItem = MediaItemResponse(
            id = itemId,
            name = null,
            originalTitle = null,
            serverId = null,
            etag = null,
            type = "Movie",
            mediaType = "Video",
            isFolder = false,
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

        coEvery {
            mediaItemApi.getItemDetails(userId, itemId)
        } returns Response.success(expectedItem)

        val result = mediaItemApi.getItemDetails(userId, itemId)

        assertTrue(result.isSuccessful)
        assertNotNull(result.body())
        assertEquals(expectedItem, result.body())
        assertEquals(itemId, result.body()?.id)
        assertEquals("Movie", result.body()?.type)
        assertEquals("Video", result.body()?.mediaType)
        assertEquals(false, result.body()?.isFolder)
    }

    private fun createTestMediaItemResponse(
        id: String,
        name: String,
        overview: String? = null,
        productionYear: Int? = null
    ): MediaItemResponse {
        return MediaItemResponse(
            id = id,
            name = name,
            originalTitle = name,
            serverId = "server-123",
            etag = "etag-123",
            type = "Movie",
            mediaType = "Video",
            isFolder = false,
            overview = overview,
            taglines = null,
            genres = listOf("Action", "Drama"),
            productionYear = productionYear,
            premiereDate = "2023-01-01",
            endDate = null,
            dateCreated = "2023-01-01T00:00:00Z",
            officialRating = "PG-13",
            criticRating = 85,
            communityRating = 7.8,
            customRating = null,
            productionLocations = null,
            path = "/path/to/movie",
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
            runTimeTicks = 72000000000L,
            container = "mkv",
            video3DFormat = null,
            aspectRatio = "16:9",
            mediaSourceResponses = null,
            playAccess = "Full",
            canDownload = true,
            people = null,
            imageTags = mapOf("Primary" to "image-tag-123"),
            backdropImageTags = null,
            parentLogoImageTag = null,
            parentThumbImageTag = null,
            seriesPrimaryImageTag = null,
            parentBackdropImageTags = null,
            sortName = name,
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
