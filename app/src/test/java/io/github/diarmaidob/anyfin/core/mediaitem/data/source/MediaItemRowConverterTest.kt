package io.github.diarmaidob.anyfin.core.mediaitem.data.source

import io.github.diarmaidob.anyfin.core.entity.MediaItem
import io.github.diarmaidob.anyfin.core.entity.MediaItemRow
import io.github.diarmaidob.anyfin.core.entity.MediaItemSource
import io.github.diarmaidob.anyfin.core.entity.MediaItemStream
import io.mockk.junit4.MockKRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MediaItemRowConverterTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    private val converter = MediaItemRowConverter()

    @Test
    fun `toDomain maps Movie correctly`() {
        val row = createMediaItemRow(
            type = "Movie",
            name = "Test Movie",
            productionYear = 2023,
            isPlayed = true,
            isFavorite = true
        )

        val result = converter.toDomain(row)

        assertEquals(MediaItem.Movie::class.java, result.javaClass)
        val movie = result as MediaItem.Movie
        assertEquals("1", movie.id)
        assertEquals("Test Movie", movie.name)
        assertEquals(2023, movie.productionYear)
        assertEquals(true, movie.userData.isPlayed)
        assertEquals(true, movie.userData.isFavorite)
        assertEquals("overview", movie.details?.overview)
    }

    @Test
    fun `toDomain maps Series Ended correctly`() {
        val row = createMediaItemRow(
            type = "Series",
            name = "Test Series",
            endDate = 1672531200000L,
            productionYear = 2020
        )

        val result = converter.toDomain(row)

        assertEquals(MediaItem.Series::class.java, result.javaClass)
        val series = result as MediaItem.Series
        assertEquals("Test Series", series.name)
        assertEquals("Ended", series.status)
        assertEquals(2020, series.productionYear)
    }

    @Test
    fun `toDomain maps Series Continuing correctly`() {
        val row = createMediaItemRow(
            type = "Series",
            name = "Test Series",
            endDate = null
        )

        val result = converter.toDomain(row)

        assertEquals(MediaItem.Series::class.java, result.javaClass)
        val series = result as MediaItem.Series
        assertEquals("Continuing", series.status)
    }

    @Test
    fun `toDomain maps Episode correctly`() {
        val row = createMediaItemRow(
            type = "Episode",
            name = "Test Episode",
            seriesName = "My Series",
            seriesId = "s1",
            seasonId = "se1",
            parentIndexNumber = 2, // Season number
            indexNumber = 5 // Episode number
        )

        val result = converter.toDomain(row)

        assertEquals(MediaItem.Episode::class.java, result.javaClass)
        val episode = result as MediaItem.Episode
        assertEquals("Test Episode", episode.name)
        assertEquals("My Series", episode.seriesName)
        assertEquals("s1", episode.seriesId)
        assertEquals("se1", episode.seasonId)
        assertEquals(2, episode.seasonNumber)
        assertEquals(5, episode.episodeNumber)
    }

    @Test
    fun `toDomain maps Episode defaults correctly`() {
        val row = createMediaItemRow(
            type = "Episode",
            parentIndexNumber = null,
            indexNumber = null
        )

        val result = converter.toDomain(row)

        assertEquals(MediaItem.Episode::class.java, result.javaClass)
        val episode = result as MediaItem.Episode
        assertEquals(1, episode.seasonNumber)
        assertEquals(0, episode.episodeNumber)
    }

    @Test
    fun `toDomain maps Season correctly`() {
        val row = createMediaItemRow(
            type = "Season",
            name = "Season 3",
            seriesId = "s1",
            indexNumber = 3
        )

        val result = converter.toDomain(row)

        assertEquals(MediaItem.Season::class.java, result.javaClass)
        val season = result as MediaItem.Season
        assertEquals("Season 3", season.name)
        assertEquals("s1", season.seriesId)
        assertEquals(3, season.seasonNumber)
    }

    @Test
    fun `toDomain maps Season defaults correctly`() {
        val row = createMediaItemRow(
            type = "Season",
            indexNumber = null
        )

        val result = converter.toDomain(row)

        assertEquals(MediaItem.Season::class.java, result.javaClass)
        val season = result as MediaItem.Season
        assertEquals(1, season.seasonNumber)
    }

    @Test
    fun `toDomain maps Unknown correctly`() {
        val row = createMediaItemRow(
            type = "Documentary",
            name = "Unknown Doc"
        )

        val result = converter.toDomain(row)

        assertEquals(MediaItem.Unknown::class.java, result.javaClass)
        val unknown = result as MediaItem.Unknown
        assertEquals("Unknown Doc", unknown.name)
    }

    @Test
    fun `toDomain maps details null when dateCreated is null`() {
        val row = createMediaItemRow(
            type = "Movie",
            dateCreated = null
        )

        val result = converter.toDomain(row)

        val movie = result as MediaItem.Movie
        assertEquals(null, movie.details)
    }

    @Test
    fun `toDomain maps UserData defaults`() {
        val row = createMediaItemRow(
            type = "Movie",
            isPlayed = null,
            playbackPositionTicks = null,
            isFavorite = null,
            lastPlayedDate = null,
            playCount = null
        )

        val result = converter.toDomain(row)

        val userData = (result as MediaItem.Movie).userData
        assertEquals(false, userData.isPlayed)
        assertEquals(0L, userData.playbackPositionTicks)
        assertEquals(false, userData.isFavorite)
        assertEquals(0L, userData.lastPlayedDate)
        assertEquals(0L, userData.playCount)
    }

    @Test
    fun `toDomain maps Images correctly`() {
        val row = createMediaItemRow(
            type = "Movie",
            id = "img1",
            primary = "p1",
            backdrop = "b1",
            logo = "l1",
            thumb = "t1",
            banner = "bn1",
            seriesPrimary = "sp1",
            parentLogo = "pl1",
            parentThumb = "pt1",
            parentBackdrop = "pb1"
        )

        val result = converter.toDomain(row)

        val images = (result as MediaItem.Movie).images
        assertEquals("img1", images.mediaId)
        assertEquals("p1", images.primary)
        assertEquals("b1", images.backdrop)
        assertEquals("l1", images.logo)
        assertEquals("t1", images.thumb)
        assertEquals("bn1", images.banner)
        assertEquals("sp1", images.seriesPrimary)
        assertEquals("pl1", images.parentLogo)
        assertEquals("pt1", images.parentThumb)
        assertEquals("pb1", images.parentBackdrop)
    }

    @Test
    fun `toDomainList maps multiple items`() {
        val rows = listOf(
            createMediaItemRow(type = "Movie", name = "Movie 1"),
            createMediaItemRow(type = "Series", name = "Series 1")
        )

        val result = converter.toDomainList(rows)

        assertEquals(2, result.size)
        assertEquals(MediaItem.Movie::class.java, result[0].javaClass)
        assertEquals(MediaItem.Series::class.java, result[1].javaClass)
    }

    @Test
    fun `toStreamOptions filters streams correctly`() {
        val source = MediaItemSource(
            id = "src1",
            mediaId = "m1",
            container = "mkv",
            protocol = "http",
            name = "Source",
            supportsDirectPlay = true,
            supportsTranscoding = true
        )

        val streams = listOf(
            createStream(type = "Audio", index = 0),
            createStream(type = "Video", index = 1),
            createStream(type = "Subtitle", index = 2),
            createStream(type = "Audio", index = 3)
        )

        val result = converter.toStreamOptions(source, streams)

        assertEquals("src1", result.sourceId)
        assertEquals(2, result.audio.size)
        assertEquals(1, result.subtitles.size)
        assertEquals(0L, result.audio[0].indexNumber)
        assertEquals(3L, result.audio[1].indexNumber)
        assertEquals(2L, result.subtitles[0].indexNumber)
    }

    private fun createMediaItemRow(
        id: String = "1",
        serverId: String = "srv1",
        type: String = "Movie",
        name: String = "Name",
        sortName: String = "SortName",
        originalTitle: String? = null,
        path: String? = null,
        parentId: String? = null,
        seriesId: String? = null,
        seriesName: String? = null,
        seasonId: String? = null,
        seasonName: String? = null,
        indexNumber: Long? = null,
        parentIndexNumber: Long? = null,
        productionYear: Long? = 2000,
        premiereDate: Long? = null,
        endDate: Long? = null,
        mediaId: String? = null,
        overview: String? = "overview",
        tagline: String? = null,
        officialRating: String? = null,
        communityRating: Double? = null,
        criticRating: Long? = null,
        dateCreated: Long? = 1000L,
        runTimeTicks: Long? = null,
        container: String? = null,
        mediaId_: String? = null,
        isPlayed: Boolean? = false,
        playbackPositionTicks: Long? = 0L,
        isFavorite: Boolean? = false,
        lastPlayedDate: Long? = 0L,
        playCount: Long? = 0L,
        mediaId__: String? = null,
        primary: String? = null,
        backdrop: String? = null,
        logo: String? = null,
        thumb: String? = null,
        banner: String? = null,
        seriesPrimary: String? = null,
        parentLogo: String? = null,
        parentThumb: String? = null,
        parentBackdrop: String? = null
    ): MediaItemRow {
        return MediaItemRow(
            id, serverId, type, name, sortName, originalTitle, path, parentId,
            seriesId, seriesName, seasonId, seasonName, indexNumber, parentIndexNumber,
            productionYear, premiereDate, endDate, mediaId, overview, tagline,
            officialRating, communityRating, criticRating, dateCreated, runTimeTicks,
            container, mediaId_, isPlayed, playbackPositionTicks, isFavorite,
            lastPlayedDate, playCount, mediaId__, primary, backdrop, logo, thumb,
            banner, seriesPrimary, parentLogo, parentThumb, parentBackdrop
        )
    }

    private fun createStream(type: String, index: Long): MediaItemStream {
        return MediaItemStream(
            sourceId = "src1",
            indexNumber = index,
            type = type,
            codec = "aac",
            language = "eng",
            displayTitle = "English",
            isDefault = false,
            isForced = false,
            channels = 2,
            width = null,
            height = null
        )
    }
}