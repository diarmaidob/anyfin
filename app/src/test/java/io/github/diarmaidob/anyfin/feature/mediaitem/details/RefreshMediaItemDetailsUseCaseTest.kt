package io.github.diarmaidob.anyfin.feature.mediaitem.details

import io.github.diarmaidob.anyfin.core.common.DataLoadError
import io.github.diarmaidob.anyfin.core.common.DataResult
import io.github.diarmaidob.anyfin.core.entity.MediaItem
import io.github.diarmaidob.anyfin.core.entity.MediaItemDetail
import io.github.diarmaidob.anyfin.core.entity.MediaItemImageTagInfo
import io.github.diarmaidob.anyfin.core.entity.MediaItemQuery
import io.github.diarmaidob.anyfin.core.entity.MediaItemUserData
import io.github.diarmaidob.anyfin.core.mediaitem.MediaItemRepo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

class RefreshMediaItemDetailsUseCaseTest {

    private lateinit var repo: MediaItemRepo
    private lateinit var useCase: RefreshMediaItemDetailsUseCase

    @Before
    fun setUp() {
        repo = mockk()
        useCase = RefreshMediaItemDetailsUseCase(repo)
        mockkObject(MediaItemQuery.NextUp)
        mockkObject(MediaItemQuery.Browse)
    }

    private fun createMockImages() = MediaItemImageTagInfo(
        mediaId = "test-id",
        primary = null,
        backdrop = null,
        logo = null,
        thumb = null,
        banner = null,
        seriesPrimary = null,
        parentLogo = null,
        parentThumb = null,
        parentBackdrop = null
    )

    private fun createMockUserData() = MediaItemUserData(
        mediaId = "test-id",
        isPlayed = false,
        playbackPositionTicks = 0L,
        isFavorite = false,
        lastPlayedDate = 0L,
        playCount = 0L
    )

    private fun createMockDetails() = MediaItemDetail(
        mediaId = "test-id",
        overview = null,
        tagline = null,
        officialRating = null,
        communityRating = null,
        criticRating = null,
        dateCreated = 0L,
        runTimeTicks = 0L,
        container = null
    )

    @After
    fun tearDown() {
        unmockkObject(MediaItemQuery.NextUp)
        unmockkObject(MediaItemQuery.Browse)
    }

    @Test
    fun `invoke returns success for movie with next up`() = runTest {
        val itemId = "movie-1"
        val movie = MediaItem.Movie(
            id = itemId,
            name = "Test Movie",
            images = createMockImages(),
            userData = createMockUserData(),
            details = createMockDetails(),
            productionYear = 2023
        )

        coEvery { repo.refreshItem(itemId) } returns DataResult.Success(Unit)
        every { repo.observeItem(itemId) } returns flowOf(movie)
        every { MediaItemQuery.NextUp.forItem(movie) } returns null

        val result = useCase(itemId)

        assertEquals(DataResult.Success(Unit), result)
        coVerify { repo.refreshItem(itemId) }
        coVerify(exactly = 0) { repo.refreshList(any<MediaItemQuery.Browse>()) }
        coVerify(exactly = 0) { repo.refreshList(any<MediaItemQuery.NextUp>()) }
    }

    @Test
    fun `invoke returns success for movie without next up`() = runTest {
        val itemId = "movie-2"
        val movie = MediaItem.Movie(
            id = itemId,
            name = "Another Movie",
            images = createMockImages(),
            userData = createMockUserData(),
            details = createMockDetails(),
            productionYear = 2022
        )

        coEvery { repo.refreshItem(itemId) } returns DataResult.Success(Unit)
        every { repo.observeItem(itemId) } returns flowOf(movie)
        every { MediaItemQuery.NextUp.forItem(movie) } returns null

        val result = useCase(itemId)

        assertEquals(DataResult.Success(Unit), result)
        coVerify { repo.refreshItem(itemId) }
        coVerify(exactly = 0) { repo.refreshList(any<MediaItemQuery.Browse>()) }
        coVerify(exactly = 0) { repo.refreshList(any<MediaItemQuery.NextUp>()) }
    }

    @Test
    fun `invoke returns success for series with children and next up`() = runTest {
        val itemId = "series-1"
        val series = MediaItem.Series(
            id = itemId,
            name = "Test Series",
            images = createMockImages(),
            userData = createMockUserData(),
            details = createMockDetails(),
            productionYear = 2023,
            status = "Continuing"
        )
        val browseQuery = mockk<MediaItemQuery.Browse>()
        val nextUpQuery = mockk<MediaItemQuery.NextUp>()

        coEvery { repo.refreshItem(itemId) } returns DataResult.Success(Unit)
        every { repo.observeItem(itemId) } returns flowOf(series)
        every { MediaItemQuery.Browse.childrenOf(series) } returns browseQuery
        every { MediaItemQuery.NextUp.forItem(series) } returns nextUpQuery
        coEvery { repo.refreshList(browseQuery) } returns DataResult.Success(Unit)
        coEvery { repo.refreshList(nextUpQuery) } returns DataResult.Success(Unit)

        val result = useCase(itemId)

        assertEquals(DataResult.Success(Unit), result)
        coVerify { repo.refreshItem(itemId) }
        coVerify { repo.refreshList(browseQuery) }
        coVerify { repo.refreshList(nextUpQuery) }
    }

    @Test
    fun `invoke returns success for season with children and next up`() = runTest {
        val itemId = "season-1"
        val season = MediaItem.Season(
            id = itemId,
            name = "Season 1",
            images = createMockImages(),
            userData = createMockUserData(),
            details = createMockDetails(),
            seriesId = "series-1",
            seasonNumber = 1
        )
        val browseQuery = mockk<MediaItemQuery.Browse>()
        val nextUpQuery = mockk<MediaItemQuery.NextUp>()

        coEvery { repo.refreshItem(itemId) } returns DataResult.Success(Unit)
        every { repo.observeItem(itemId) } returns flowOf(season)
        every { MediaItemQuery.Browse.childrenOf(season) } returns browseQuery
        every { MediaItemQuery.NextUp.forItem(season) } returns nextUpQuery
        coEvery { repo.refreshList(browseQuery) } returns DataResult.Success(Unit)
        coEvery { repo.refreshList(nextUpQuery) } returns DataResult.Success(Unit)

        val result = useCase(itemId)

        assertEquals(DataResult.Success(Unit), result)
        coVerify { repo.refreshItem(itemId) }
        coVerify { repo.refreshList(browseQuery) }
        coVerify { repo.refreshList(nextUpQuery) }
    }

    @Test
    fun `invoke returns success for episode without children`() = runTest {
        val itemId = "episode-1"
        val episode = MediaItem.Episode(
            id = itemId,
            name = "Episode 1",
            images = createMockImages(),
            userData = createMockUserData(),
            details = createMockDetails(),
            seriesName = "Test Series",
            seriesId = "series-1",
            seasonId = "season-1",
            seasonNumber = 1,
            episodeNumber = 1
        )

        coEvery { repo.refreshItem(itemId) } returns DataResult.Success(Unit)
        every { repo.observeItem(itemId) } returns flowOf(episode)
        every { MediaItemQuery.NextUp.forItem(episode) } returns null

        val result = useCase(itemId)

        assertEquals(DataResult.Success(Unit), result)
        coVerify { repo.refreshItem(itemId) }
        coVerify(exactly = 0) { repo.refreshList(any<MediaItemQuery.Browse>()) }
        coVerify(exactly = 0) { repo.refreshList(any<MediaItemQuery.NextUp>()) }
    }

    @Test
    fun `invoke returns error when refresh item fails`() = runTest {
        val itemId = "item-1"
        val error = DataLoadError.NetworkError("Network error")
        coEvery { repo.refreshItem(itemId) } returns DataResult.Error(error)

        val result = useCase(itemId)

        assertNotEquals(DataResult.Success(Unit), result)
        coVerify { repo.refreshItem(itemId) }
        coVerify(exactly = 0) { repo.observeItem(itemId) }
    }

    @Test
    fun `invoke returns error when item not found`() = runTest {
        val itemId = "item-1"
        coEvery { repo.refreshItem(itemId) } returns DataResult.Success(Unit)
        every { repo.observeItem(itemId) } returns flowOf(null)

        val result = useCase(itemId)

        assertNotEquals(DataResult.Success(Unit), result)
    }

    @Test
    fun `invoke returns error when refresh children fails for series`() = runTest {
        val itemId = "series-1"
        val series = MediaItem.Series(
            id = itemId,
            name = "Test Series",
            images = createMockImages(),
            userData = createMockUserData(),
            details = createMockDetails(),
            productionYear = 2023,
            status = "Continuing"
        )
        val browseQuery = mockk<MediaItemQuery.Browse>()
        val error = DataLoadError.HttpError(500, "Server error")

        coEvery { repo.refreshItem(itemId) } returns DataResult.Success(Unit)
        every { repo.observeItem(itemId) } returns flowOf(series)
        every { MediaItemQuery.Browse.childrenOf(series) } returns browseQuery
        every { MediaItemQuery.NextUp.forItem(series) } returns null
        coEvery { repo.refreshList(browseQuery) } returns DataResult.Error(error)

        val result = useCase(itemId)

        assertNotEquals(DataResult.Success(Unit), result)
    }

    @Test
    fun `invoke returns error when refresh next up fails`() = runTest {
        val itemId = "movie-1"
        val movie = MediaItem.Movie(
            id = itemId,
            name = "Test Movie",
            images = createMockImages(),
            userData = createMockUserData(),
            details = createMockDetails(),
            productionYear = 2023
        )
        val nextUpQuery = mockk<MediaItemQuery.NextUp>()
        val error = DataLoadError.DatabaseError("DB error")

        coEvery { repo.refreshItem(itemId) } returns DataResult.Success(Unit)
        every { repo.observeItem(itemId) } returns flowOf(movie)
        every { MediaItemQuery.NextUp.forItem(movie) } returns nextUpQuery
        coEvery { repo.refreshList(nextUpQuery) } returns DataResult.Error(error)

        val result = useCase(itemId)

        assertNotEquals(DataResult.Success(Unit), result)
    }
}