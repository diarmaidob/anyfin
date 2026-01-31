package io.github.diarmaidob.anyfin.core.mediaitem.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class MediaItemResponseConverterTest {

    @Test
    fun `toInfo should convert complete MediaItemResponse to MediaItemInfo`() {
        val response = createCompleteMediaItemResponse(
            id = "media-123",
            name = "Test Movie",
            type = "Movie",
            serverId = "server-456",
            sortName = "Test Movie, The",
            originalTitle = "The Test Movie",
            path = "/movies/test.mkv",
            parentId = "parent-789",
            seriesId = "series-101",
            seriesName = "Test Series",
            seasonId = "season-202",
            seasonName = "Season 1",
            indexNumber = 5,
            parentIndexNumber = 1,
            productionYear = 2023,
            premiereDate = "2023-01-15T00:00:00Z",
            endDate = "2023-02-15T00:00:00Z"
        )

        val result = response.toInfo()

        assertNotNull(result)
        assertEquals("media-123", result.id)
        assertEquals("server-456", result.serverId)
        assertEquals("Movie", result.type)
        assertEquals("Test Movie", result.name)
        assertEquals("Test Movie, The", result.sortName)
        assertEquals("The Test Movie", result.originalTitle)
        assertEquals("/movies/test.mkv", result.path)
        assertEquals("parent-789", result.parentId)
        assertEquals("series-101", result.seriesId)
        assertEquals("Test Series", result.seriesName)
        assertEquals("season-202", result.seasonId)
        assertEquals("Season 1", result.seasonName)
        assertEquals(5L, result.indexNumber)
        assertEquals(1L, result.parentIndexNumber)
        assertEquals(2023L, result.productionYear)
        assertEquals(Instant.parse("2023-01-15T00:00:00Z").toEpochMilli(), result.premiereDate)
        assertEquals(Instant.parse("2023-02-15T00:00:00Z").toEpochMilli(), result.endDate)
    }

    @Test
    fun `toInfo should handle null and blank name correctly`() {
        val responseWithNullName = createCompleteMediaItemResponse(
            id = "media-null-name",
            name = null,
            originalTitle = "Original Title",
            path = "/path/to/file.mkv"
        )

        val responseWithBlankName = createCompleteMediaItemResponse(
            id = "media-blank-name",
            name = "",
            originalTitle = null,
            path = "/movies/another.mkv"
        )

        val responseWithAllNullNames = createCompleteMediaItemResponse(
            id = "media-all-null",
            name = null,
            originalTitle = null,
            path = null
        )

        val resultNullName = responseWithNullName.toInfo()
        val resultBlankName = responseWithBlankName.toInfo()
        val resultAllNull = responseWithAllNullNames.toInfo()

        assertEquals("Original Title", resultNullName.name)
        assertEquals("another.mkv", resultBlankName.name) // Extracted from path
        assertEquals("Unknown Item", resultAllNull.name) // Fallback
    }

    @Test
    fun `toInfo should use fallback values for null fields`() {
        val response = createCompleteMediaItemResponse(
            id = "media-minimal",
            name = "Minimal Movie",
            type = null,
            serverId = null,
            sortName = null,
            forcedSortName = null,
            premiereDate = null,
            endDate = null
        )

        val result = response.toInfo()

        assertNotNull(result)
        assertEquals("media-minimal", result.id)
        assertEquals("", result.serverId) // Empty string fallback
        assertEquals("BaseItem", result.type) // Fallback type
        assertEquals("Minimal Movie", result.name)
        assertEquals("Minimal Movie", result.sortName) // Falls back to name
        assertNull(result.premiereDate)
        assertNull(result.endDate)
    }

    @Test
    fun `toInfo should handle numeric field conversions correctly`() {
        val response = createCompleteMediaItemResponse(
            id = "media-numeric",
            name = "Numeric Test",
            indexNumber = 10,
            parentIndexNumber = 2,
            productionYear = 2021
        )

        val result = response.toInfo()

        assertEquals(10L, result.indexNumber)
        assertEquals(2L, result.parentIndexNumber)
        assertEquals(2021L, result.productionYear)
    }

    @Test
    fun `toDetail should convert complete MediaItemResponse to MediaItemDetail`() {
        val response = createCompleteMediaItemResponse(
            id = "detail-123",
            overview = "This is a test overview",
            taglines = listOf("Awesome Tagline", "Another Tagline"),
            officialRating = "PG-13",
            communityRating = 8.5,
            criticRating = 92,
            dateCreated = "2023-01-01T12:00:00Z",
            runTimeTicks = 72000000000L, // 2 hours in ticks
            container = "mkv"
        )

        val result = response.toDetail()

        assertNotNull(result)
        assertEquals("detail-123", result.mediaId)
        assertEquals("This is a test overview", result.overview)
        assertEquals("Awesome Tagline", result.tagline) // First tagline
        assertEquals("PG-13", result.officialRating)
        assertEquals(8.5, result.communityRating!!, 0.01)
        assertEquals(92L, result.criticRating)
        assertEquals(Instant.parse("2023-01-01T12:00:00Z").toEpochMilli(), result.dateCreated)
        assertEquals(72000000000L, result.runTimeTicks)
        assertEquals("mkv", result.container)
    }

    @Test
    fun `toDetail should handle null and empty fields correctly`() {
        val response = createCompleteMediaItemResponse(
            id = "detail-nulls",
            overview = null,
            taglines = null,
            officialRating = null,
            communityRating = null,
            criticRating = null,
            dateCreated = null,
            runTimeTicks = null,
            container = null
        )

        val result = response.toDetail()

        assertNotNull(result)
        assertEquals("detail-nulls", result.mediaId)
        assertNull(result.overview)
        assertNull(result.tagline)
        assertNull(result.officialRating)
        assertNull(result.communityRating)
        assertNull(result.criticRating)
        assertEquals(0L, result.dateCreated) // Default fallback
        assertEquals(0L, result.runTimeTicks) // Default fallback
        assertNull(result.container)
    }

    @Test
    fun `toDetail should handle empty taglines list`() {
        val response = createCompleteMediaItemResponse(
            id = "detail-empty-tags",
            taglines = emptyList()
        )

        val result = response.toDetail()

        assertNotNull(result)
        assertEquals("detail-empty-tags", result.mediaId)
        assertNull(result.tagline)
    }

    @Test
    fun `toUserData should convert complete MediaItemResponse to MediaItemUserData`() {
        val userData = UserDataResponse(
            isPlayed = true,
            playbackPositionTicks = 3600000000L,
            isFavorite = true,
            lastPlayedDate = "2023-01-15T20:30:00Z",
            playCount = 3,
            playedPercentage = 75.0,
            unplayedItemCount = null,
            key = null
        )

        val response = createCompleteMediaItemResponse(
            id = "user-123",
            userData = userData
        )

        val result = response.toUserData()

        assertNotNull(result)
        assertEquals("user-123", result.mediaId)
        assertEquals(true, result.isPlayed)
        assertEquals(3600000000L, result.playbackPositionTicks)
        assertEquals(true, result.isFavorite)
        assertEquals(Instant.parse("2023-01-15T20:30:00Z").toEpochMilli(), result.lastPlayedDate)
        assertEquals(3L, result.playCount)
    }

    @Test
    fun `toUserData should handle null userData correctly`() {
        val response = createCompleteMediaItemResponse(
            id = "user-null",
            userData = null
        )

        val result = response.toUserData()

        assertNotNull(result)
        assertEquals("user-null", result.mediaId)
        assertEquals(false, result.isPlayed) // Default fallback
        assertEquals(0L, result.playbackPositionTicks) // Default fallback
        assertEquals(false, result.isFavorite) // Default fallback
        assertEquals(0L, result.lastPlayedDate) // Default fallback
        assertEquals(0L, result.playCount) // Default fallback
    }

    @Test
    fun `toUserData should handle null fields in userData correctly`() {
        val userData = UserDataResponse(
            isPlayed = null,
            playbackPositionTicks = null,
            isFavorite = null,
            lastPlayedDate = null,
            playCount = null,
            playedPercentage = null,
            unplayedItemCount = null,
            key = null
        )

        val response = createCompleteMediaItemResponse(
            id = "user-null-fields",
            userData = userData
        )

        val result = response.toUserData()

        assertNotNull(result)
        assertEquals("user-null-fields", result.mediaId)
        assertEquals(false, result.isPlayed) // null treated as false
        assertEquals(0L, result.playbackPositionTicks) // null treated as 0L
        assertEquals(false, result.isFavorite) // null treated as false
        assertEquals(0L, result.lastPlayedDate) // null treated as 0L
        assertEquals(0L, result.playCount) // null treated as 0L
    }

    @Test
    fun `toImageTagInfo should convert complete MediaItemResponse to MediaItemImageTagInfo`() {
        val response = createCompleteMediaItemResponse(
            id = "image-123",
            imageTags = mapOf(
                "Primary" to "primary-tag-123",
                "Backdrop" to "backdrop-tag-123",
                "Logo" to "logo-tag-123",
                "Thumb" to "thumb-tag-123",
                "Banner" to "banner-tag-123"
            ),
            backdropImageTags = listOf("backdrop-list-1", "backdrop-list-2"),
            seriesPrimaryImageTag = "series-primary-123",
            parentLogoImageTag = "parent-logo-123",
            parentThumbImageTag = "parent-thumb-123",
            parentBackdropImageTags = listOf("parent-backdrop-1", "parent-backdrop-2")
        )

        val result = response.toImageTagInfo()

        assertNotNull(result)
        assertEquals("image-123", result.mediaId)
        assertEquals("primary-tag-123", result.primary)
        assertEquals("backdrop-tag-123", result.backdrop)
        assertEquals("logo-tag-123", result.logo)
        assertEquals("thumb-tag-123", result.thumb)
        assertEquals("banner-tag-123", result.banner)
        assertEquals("series-primary-123", result.seriesPrimary)
        assertEquals("parent-logo-123", result.parentLogo)
        assertEquals("parent-thumb-123", result.parentThumb)
        assertEquals("parent-backdrop-1", result.parentBackdrop)
    }

    @Test
    fun `toImageTagInfo should fallback to backdropImageTags when Backdrop not in imageTags`() {
        val response = createCompleteMediaItemResponse(
            id = "image-fallback",
            imageTags = mapOf(
                "Primary" to "primary-tag-123"
                // No "Backdrop" entry
            ),
            backdropImageTags = listOf("backdrop-fallback-1", "backdrop-fallback-2")
        )

        val result = response.toImageTagInfo()

        assertNotNull(result)
        assertEquals("image-fallback", result.mediaId)
        assertEquals("primary-tag-123", result.primary)
        assertEquals("backdrop-fallback-1", result.backdrop) // Fallback to list
    }

    @Test
    fun `toImageTagInfo should handle null image collections correctly`() {
        val response = createCompleteMediaItemResponse(
            id = "image-nulls",
            imageTags = null,
            backdropImageTags = null,
            seriesPrimaryImageTag = null,
            parentLogoImageTag = null,
            parentThumbImageTag = null,
            parentBackdropImageTags = null
        )

        val result = response.toImageTagInfo()

        assertNotNull(result)
        assertEquals("image-nulls", result.mediaId)
        assertNull(result.primary)
        assertNull(result.backdrop)
        assertNull(result.logo)
        assertNull(result.thumb)
        assertNull(result.banner)
        assertNull(result.seriesPrimary)
        assertNull(result.parentLogo)
        assertNull(result.parentThumb)
        assertNull(result.parentBackdrop)
    }

    @Test
    fun `toImageTagInfo should handle empty collections correctly`() {
        val response = createCompleteMediaItemResponse(
            id = "image-empty",
            imageTags = emptyMap(),
            backdropImageTags = emptyList(),
            parentBackdropImageTags = emptyList()
        )

        val result = response.toImageTagInfo()

        assertNotNull(result)
        assertEquals("image-empty", result.mediaId)
        assertNull(result.primary)
        assertNull(result.backdrop) // Empty list -> null
        assertNull(result.parentBackdrop) // Empty list -> null
    }

    @Test
    fun `toEntity MediaSourceResponse should convert complete response to MediaItemSource`() {
        val response = MediaSourceResponse(
            id = "source-123",
            container = "mkv",
            protocol = "File",
            path = "/movies/test.mkv",
            name = "Test Source",
            isRemote = false,
            runTimeTicks = 72000000000L,
            bitrate = 5000000L,
            mediaStreamResponses = null,
            transcodingUrl = null,
            transcodingSubProtocol = null,
            transcodingContainer = null,
            supportsTranscoding = true,
            supportsDirectStream = true,
            supportsDirectPlay = true
        )

        val result = response.toEntity("media-456")

        assertNotNull(result)
        assertEquals("source-123", result.id)
        assertEquals("media-456", result.mediaId)
        assertEquals("mkv", result.container)
        assertEquals("File", result.protocol)
        assertEquals("Test Source", result.name)
        assertEquals(true, result.supportsDirectPlay)
        assertEquals(true, result.supportsTranscoding)
    }

    @Test
    fun `toEntity MediaSourceResponse should handle null boolean fields correctly`() {
        val response = MediaSourceResponse(
            id = "source-nulls",
            container = null,
            protocol = null,
            path = null,
            name = null,
            isRemote = null,
            runTimeTicks = null,
            bitrate = null,
            mediaStreamResponses = null,
            transcodingUrl = null,
            transcodingSubProtocol = null,
            transcodingContainer = null,
            supportsTranscoding = null,
            supportsDirectStream = null,
            supportsDirectPlay = null
        )

        val result = response.toEntity("media-789")

        assertNotNull(result)
        assertEquals("source-nulls", result.id)
        assertEquals("media-789", result.mediaId)
        assertNull(result.container)
        assertNull(result.protocol)
        assertNull(result.name)
        assertEquals(false, result.supportsDirectPlay) // null treated as false
        assertEquals(false, result.supportsTranscoding) // null treated as false
    }

    @Test
    fun `toEntity MediaStreamResponse should convert complete response to MediaItemStream`() {
        val response = MediaStreamResponse(
            codec = "h264",
            language = "eng",
            timeBase = "1/90000",
            videoRange = "SDR",
            displayTitle = "English (H264)",
            isInterlaced = false,
            bitRate = 5000000L,
            channels = 6,
            sampleRate = 48000,
            width = 1920,
            height = 1080,
            aspectRatio = "16:9",
            index = 0,
            isDefault = true,
            isForced = false,
            type = "Video"
        )

        val result = response.toEntity("source-123")

        assertNotNull(result)
        assertEquals("source-123", result.sourceId)
        assertEquals(0L, result.indexNumber)
        assertEquals("Video", result.type)
        assertEquals("h264", result.codec)
        assertEquals("eng", result.language)
        assertEquals("English (H264)", result.displayTitle)
        assertEquals(true, result.isDefault)
        assertEquals(false, result.isForced)
        assertEquals(6L, result.channels)
        assertEquals(1920L, result.width)
        assertEquals(1080L, result.height)
    }

    @Test
    fun `toEntity MediaStreamResponse should handle null fields correctly`() {
        val response = MediaStreamResponse(
            codec = null,
            language = null,
            timeBase = null,
            videoRange = null,
            displayTitle = null,
            isInterlaced = null,
            bitRate = null,
            channels = null,
            sampleRate = null,
            width = null,
            height = null,
            aspectRatio = null,
            index = null,
            isDefault = null,
            isForced = null,
            type = null
        )

        val result = response.toEntity("source-456")

        assertNotNull(result)
        assertEquals("source-456", result.sourceId)
        assertEquals(0L, result.indexNumber) // null index -> 0L
        assertEquals("Unknown", result.type) // null type -> "Unknown"
        assertNull(result.codec)
        assertNull(result.language)
        assertNull(result.displayTitle) // Falls back to language, but language is null so result is null
        assertEquals(false, result.isDefault) // null -> false
        assertEquals(false, result.isForced) // null -> false
        assertNull(result.channels)
        assertNull(result.width)
        assertNull(result.height)
    }

    @Test
    fun `toEntity MediaStreamResponse should fallback displayTitle to language when null`() {
        val response = MediaStreamResponse(
            codec = "aac",
            language = "fra",
            timeBase = null,
            videoRange = null,
            displayTitle = null,
            isInterlaced = null,
            bitRate = null,
            channels = null,
            sampleRate = null,
            width = null,
            height = null,
            aspectRatio = null,
            index = 1,
            isDefault = false,
            isForced = false,
            type = "Audio"
        )

        val result = response.toEntity("source-789")

        assertNotNull(result)
        assertEquals("source-789", result.sourceId)
        assertEquals(1L, result.indexNumber)
        assertEquals("Audio", result.type)
        assertEquals("aac", result.codec)
        assertEquals("fra", result.language)
        assertEquals("fra", result.displayTitle)
        assertEquals(false, result.isDefault)
        assertEquals(false, result.isForced)
    }

    private fun createCompleteMediaItemResponse(
        id: String,
        name: String? = null,
        type: String? = null,
        serverId: String? = null,
        sortName: String? = null,
        forcedSortName: String? = null,
        originalTitle: String? = null,
        path: String? = null,
        parentId: String? = null,
        seriesId: String? = null,
        seriesName: String? = null,
        seasonId: String? = null,
        seasonName: String? = null,
        indexNumber: Int? = null,
        parentIndexNumber: Int? = null,
        productionYear: Int? = null,
        premiereDate: String? = null,
        endDate: String? = null,
        overview: String? = null,
        taglines: List<String>? = null,
        officialRating: String? = null,
        communityRating: Double? = null,
        criticRating: Int? = null,
        dateCreated: String? = null,
        runTimeTicks: Long? = null,
        container: String? = null,
        userData: UserDataResponse? = null,
        imageTags: Map<String, String>? = null,
        backdropImageTags: List<String>? = null,
        seriesPrimaryImageTag: String? = null,
        parentLogoImageTag: String? = null,
        parentThumbImageTag: String? = null,
        parentBackdropImageTags: List<String>? = null
    ): MediaItemResponse {
        return MediaItemResponse(
            id = id,
            name = name,
            originalTitle = originalTitle,
            serverId = serverId,
            etag = null,
            type = type,
            mediaType = null,
            isFolder = null,
            overview = overview,
            taglines = taglines,
            genres = null,
            productionYear = productionYear,
            premiereDate = premiereDate,
            endDate = endDate,
            dateCreated = dateCreated,
            officialRating = officialRating,
            criticRating = criticRating,
            communityRating = communityRating,
            customRating = null,
            productionLocations = null,
            path = path,
            seriesName = seriesName,
            seriesId = seriesId,
            seasonName = seasonName,
            seasonId = seasonId,
            indexNumber = indexNumber,
            parentIndexNumber = parentIndexNumber,
            indexNumberEnd = null,
            parentId = parentId,
            collectionType = null,
            album = null,
            albumId = null,
            albumArtist = null,
            artists = null,
            artistItemResponses = null,
            runTimeTicks = runTimeTicks,
            container = container,
            video3DFormat = null,
            aspectRatio = null,
            mediaSourceResponses = null,
            playAccess = null,
            canDownload = null,
            people = null,
            imageTags = imageTags,
            backdropImageTags = backdropImageTags,
            parentLogoImageTag = parentLogoImageTag,
            parentThumbImageTag = parentThumbImageTag,
            seriesPrimaryImageTag = seriesPrimaryImageTag,
            parentBackdropImageTags = parentBackdropImageTags,
            sortName = sortName,
            forcedSortName = forcedSortName,
            displayPreferencesId = null,
            userData = userData,
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
