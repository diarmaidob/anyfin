package io.github.diarmaidob.anyfin.core.mediaitem.data.source

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.github.diarmaidob.anyfin.core.entity.AnyfinDatabase
import io.github.diarmaidob.anyfin.core.entity.MediaItemDetail
import io.github.diarmaidob.anyfin.core.entity.MediaItemImageTagInfo
import io.github.diarmaidob.anyfin.core.entity.MediaItemInfo
import io.github.diarmaidob.anyfin.core.entity.MediaItemQueries
import io.github.diarmaidob.anyfin.core.entity.MediaItemSource
import io.github.diarmaidob.anyfin.core.entity.MediaItemStream
import io.github.diarmaidob.anyfin.core.entity.MediaItemUserData
import io.github.diarmaidob.anyfin.core.entity.MediaListEntry
import io.github.diarmaidob.anyfin.core.mediaitem.data.api.MediaItemResponse
import io.github.diarmaidob.anyfin.core.mediaitem.data.api.MediaSourceResponse
import io.github.diarmaidob.anyfin.core.mediaitem.data.api.MediaStreamResponse
import io.github.diarmaidob.anyfin.core.mediaitem.data.api.UserDataResponse
import io.github.diarmaidob.anyfin.core.mediaitem.data.api.toImageTagInfo
import io.github.diarmaidob.anyfin.core.mediaitem.data.api.toInfo
import io.github.diarmaidob.anyfin.core.mediaitem.data.api.toUserData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MediaItemLocalDataSourceTest {

    private lateinit var sqlDriver: SqlDriver
    private lateinit var database: AnyfinDatabase
    private lateinit var queries: MediaItemQueries
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var dataSource: MediaItemLocalDataSource

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        sqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AnyfinDatabase.Schema.create(sqlDriver)
        database = AnyfinDatabase(sqlDriver)
        queries = database.mediaItemQueries
        dataSource = MediaItemLocalDataSource(queries, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        sqlDriver.close()
    }

    @Test
    fun `observeList emits expected list`() = runTest {
        val cacheKey = "test_cache_key"
        val expectedInfo = createTestMediaItemInfo("item1", "Test Item 1")
        val expectedDetail = createTestMediaItemDetail("item1")
        val expectedUserData = createTestMediaItemUserData("item1")
        val expectedImageTags = createTestMediaItemImageTagInfo("item1")

        queries.upsertInfo(expectedInfo)
        queries.upsertDetail(expectedDetail)
        queries.upsertUserData(expectedUserData)
        queries.upsertImageTags(expectedImageTags)
        queries.insertListEntry(MediaListEntry(cacheKey, "item1", 0L))

        val result = dataSource.observeList(cacheKey).first()

        assertEquals(1, result.size)
        val actualItem = result[0]
        assertEquals(expectedInfo.id, actualItem.id)
        assertEquals(expectedInfo.name, actualItem.name)
        assertEquals(expectedDetail.overview, actualItem.overview)
        assertEquals(expectedUserData.isPlayed, actualItem.isPlayed)
        assertEquals(expectedImageTags.primary, actualItem.primary)
    }

    @Test
    fun `observeItem emits expected item`() = runTest {
        val id = "item_1"
        val expectedInfo = createTestMediaItemInfo(id, "Test Item 1")
        val expectedDetail = createTestMediaItemDetail(id)
        val expectedUserData = createTestMediaItemUserData(id)
        val expectedImageTags = createTestMediaItemImageTagInfo(id)

        queries.upsertInfo(expectedInfo)
        queries.upsertDetail(expectedDetail)
        queries.upsertUserData(expectedUserData)
        queries.upsertImageTags(expectedImageTags)

        val result = dataSource.observeItem(id).first()

        assertNotNull(result)
        assertEquals(id, result?.id)
        assertEquals(expectedInfo.name, result?.name)
        assertEquals(expectedDetail.overview, result?.overview)
        assertEquals(expectedUserData.isPlayed, result?.isPlayed)
        assertEquals(expectedImageTags.primary, result?.primary)
    }

    @Test
    fun `observeItem emits null when not found`() = runTest {
        val id = "missing_item"

        val result = dataSource.observeItem(id).first()

        assertNull(result)
    }

    @Test
    fun `replaceMediaList performs transaction and updates data correctly`() = runTest {
        val cacheKey = "list_key"
        val item1 = createMediaItemResponse("id1", "Name 1")
        val item2 = createMediaItemResponse("id2", "Name 2")
        val items = listOf(item1, item2)

        dataSource.replaceMediaList(cacheKey, items)

        val listItems = queries.getListItems(cacheKey).executeAsList()
        assertEquals(2, listItems.size)
        assertEquals("id1", listItems[0].id)
        assertEquals("id2", listItems[1].id)

        val item1FromDb = queries.getItem("id1").executeAsOneOrNull()
        val item2FromDb = queries.getItem("id2").executeAsOneOrNull()
        assertNotNull(item1FromDb)
        assertNotNull(item2FromDb)
        assertEquals("Name 1", item1FromDb?.name)
        assertEquals("Name 2", item2FromDb?.name)
    }

    @Test
    fun `updateItemDetails performs transaction and updates details and sources`() = runTest {
        val id = "detail_id"
        val streamResponse = MediaStreamResponse(
            codec = "h264", language = "eng", timeBase = null, videoRange = null,
            displayTitle = "English", isInterlaced = false, bitRate = 1000,
            channels = 2, sampleRate = 48000, width = 1920, height = 1080,
            aspectRatio = "16:9", index = 0, isDefault = true, isForced = false, type = "Video"
        )
        val sourceResponse = MediaSourceResponse(
            id = "source_1", container = "mkv", protocol = "http", path = "/path",
            name = "Source 1", isRemote = false, runTimeTicks = 1000L, bitrate = 2000L,
            mediaStreamResponses = listOf(streamResponse), transcodingUrl = null,
            transcodingSubProtocol = null, transcodingContainer = null,
            supportsTranscoding = true, supportsDirectStream = true, supportsDirectPlay = true
        )
        val itemResponse = createMediaItemResponse(id, "Detailed Item").copy(
            mediaSourceResponses = listOf(sourceResponse)
        )

        queries.upsertInfo(itemResponse.toInfo())
        queries.upsertUserData(itemResponse.toUserData())
        queries.upsertImageTags(itemResponse.toImageTagInfo())

        dataSource.updateItemDetails(id, itemResponse)

        val updatedItem = queries.getItem(id).executeAsOneOrNull()
        assertNotNull(updatedItem)
        assertEquals("Detailed Item overview", updatedItem?.overview)

        val sources = queries.getSourcesForMedia(id).executeAsList()
        assertEquals(1, sources.size)
        assertEquals("source_1", sources[0].id)

        val streams = queries.getStreamsForSource("source_1").executeAsList()
        assertEquals(1, streams.size)
        assertEquals("h264", streams[0].codec)
    }

    @Test
    fun `getPrimarySource returns first source`() = runTest {
        val itemId = "item_id"
        val expectedSource = createTestMediaItemSource("source_1", itemId)
        queries.upsertSource(expectedSource)

        val result = dataSource.getPrimarySource(itemId)

        assertEquals(expectedSource, result)
    }

    @Test
    fun `getPrimarySource returns null when empty`() = runTest {
        val itemId = "item_id"

        val result = dataSource.getPrimarySource(itemId)

        assertEquals(null, result)
    }

    @Test
    fun `getStreams returns list of streams`() = runTest {
        val sourceId = "source_id"
        val expectedStreams = listOf(
            createTestMediaItemStream(sourceId, 0, "Video"),
            createTestMediaItemStream(sourceId, 1, "Audio")
        )
        expectedStreams.forEach { queries.upsertStream(it) }

        val result = dataSource.getStreams(sourceId)

        assertEquals(2, result.size)
        assertEquals("Video", result[0].type)
        assertEquals("Audio", result[1].type)
    }

    @Test
    fun `getSourceById returns source when exists`() = runTest {
        val sourceId = "source_id"
        val expectedSource = createTestMediaItemSource(sourceId, "item_id")
        queries.upsertSource(expectedSource)

        val result = dataSource.getSourceById(sourceId)

        assertEquals(expectedSource, result)
    }

    @Test
    fun `getSourceById returns null when missing`() = runTest {
        val sourceId = "missing_source"

        val result = dataSource.getSourceById(sourceId)

        assertEquals(null, result)
    }

    private fun createMediaItemResponse(id: String, name: String): MediaItemResponse {
        return MediaItemResponse(
            id = id, name = name, originalTitle = null, serverId = "server1", etag = null,
            type = "Movie", mediaType = "Video", isFolder = false, overview = "Detailed Item overview",
            taglines = null, genres = null, productionYear = null, premiereDate = null,
            endDate = null, dateCreated = null, officialRating = null, criticRating = null,
            communityRating = null, customRating = null, productionLocations = null,
            path = null, seriesName = null, seriesId = null, seasonName = null,
            seasonId = null, indexNumber = null, parentIndexNumber = null,
            indexNumberEnd = null, parentId = null, collectionType = null, album = null,
            albumId = null, albumArtist = null, artists = null, artistItemResponses = null,
            runTimeTicks = null, container = null, video3DFormat = null, aspectRatio = null,
            mediaSourceResponses = null, playAccess = null, canDownload = null, people = null,
            imageTags = null, backdropImageTags = null, parentLogoImageTag = null,
            parentThumbImageTag = null, seriesPrimaryImageTag = null,
            parentBackdropImageTags = null, sortName = name, forcedSortName = null,
            displayPreferencesId = null, userData = UserDataResponse(
                isPlayed = false,
                playbackPositionTicks = 0,
                playCount = 0,
                isFavorite = false,
                lastPlayedDate = null,
                playedPercentage = 0.0,
                unplayedItemCount = null,
                key = "key",
            ),
            providerIds = null, studios = null, chapterResponses = null, partCount = null,
            childCount = null, recursiveItemCount = null, specialFeatureCount = null,
            localTrailerCount = null
        )
    }

    private fun createTestMediaItemInfo(id: String, name: String): MediaItemInfo {
        return MediaItemInfo(
            id = id,
            serverId = "server1",
            type = "Movie",
            name = name,
            sortName = name,
            originalTitle = null,
            path = null,
            parentId = null,
            seriesId = null,
            seriesName = null,
            seasonId = null,
            seasonName = null,
            indexNumber = null,
            parentIndexNumber = null,
            productionYear = null,
            premiereDate = null,
            endDate = null
        )
    }

    private fun createTestMediaItemDetail(mediaId: String): MediaItemDetail {
        return MediaItemDetail(
            mediaId = mediaId,
            overview = "Test overview",
            tagline = "Test tagline",
            officialRating = "PG-13",
            communityRating = 7.5,
            criticRating = 85,
            dateCreated = 1234567890,
            runTimeTicks = 54000000000L,
            container = "mkv"
        )
    }

    private fun createTestMediaItemUserData(mediaId: String): MediaItemUserData {
        return MediaItemUserData(
            mediaId = mediaId,
            isPlayed = false,
            playbackPositionTicks = 0,
            isFavorite = true,
            lastPlayedDate = 1234567890,
            playCount = 0
        )
    }

    private fun createTestMediaItemImageTagInfo(mediaId: String): MediaItemImageTagInfo {
        return MediaItemImageTagInfo(
            mediaId = mediaId,
            primary = "primary_tag",
            backdrop = "backdrop_tag",
            logo = null,
            thumb = null,
            banner = null,
            seriesPrimary = null,
            parentLogo = null,
            parentThumb = null,
            parentBackdrop = null
        )
    }

    private fun createTestMediaItemSource(id: String, mediaId: String): MediaItemSource {
        return MediaItemSource(
            id = id,
            mediaId = mediaId,
            container = "mkv",
            protocol = "File",
            name = "Test Source",
            supportsDirectPlay = true,
            supportsTranscoding = true
        )
    }

    private fun createTestMediaItemStream(sourceId: String, indexNumber: Long, type: String): MediaItemStream {
        return MediaItemStream(
            sourceId = sourceId,
            indexNumber = indexNumber,
            type = type,
            codec = "h264",
            language = "eng",
            displayTitle = "Test $type",
            isDefault = false,
            isForced = false,
            channels = if (type == "Audio") 2 else null,
            width = if (type == "Video") 1920 else null,
            height = if (type == "Video") 1080 else null
        )
    }
}