package io.github.diarmaidob.anyfin.feature.mediaitem.details

import io.github.diarmaidob.anyfin.core.entity.MediaItem
import io.github.diarmaidob.anyfin.core.entity.MediaItemDetail
import io.github.diarmaidob.anyfin.core.entity.MediaItemImageTagInfo
import io.github.diarmaidob.anyfin.core.entity.MediaItemStreamOptions
import io.github.diarmaidob.anyfin.core.entity.MediaItemUserData
import io.github.diarmaidob.anyfin.core.mediaitem.MediaItemRepo
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class ObserveMediaItemDetailsUseCaseTest {

    @MockK
    private lateinit var repo: MediaItemRepo

    private lateinit var useCase: ObserveMediaItemDetailsUseCase

    private val testDispatcher = StandardTestDispatcher()

    private val mockImages = mockk<MediaItemImageTagInfo>(relaxed = true)
    private val mockUserData = mockk<MediaItemUserData>(relaxed = true)
    private val mockDetails = mockk<MediaItemDetail>(relaxed = true)
    private val mockStreamOptions = mockk<MediaItemStreamOptions>(relaxed = true)

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
        useCase = ObserveMediaItemDetailsUseCase(repo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `invoke returns empty flow when item is null`() = runTest {
        val itemId = "missing_id"
        every { repo.observeItem(itemId) } returns flowOf(null)

        val results = useCase(itemId).toList()

        assertEquals(0, results.size)
    }

    @Test
    fun `invoke returns correct data for Movie`() = runTest {
        val itemId = "movie_1"
        val movie = createMovie(itemId)

        every { repo.observeItem(itemId) } returns flowOf(movie)
        every { repo.observeStreamOptions(itemId) } returns flowOf(mockStreamOptions)

        val result = useCase(itemId).toList().first()

        assertEquals(movie, result.item)
        assertEquals(emptyList<MediaItem>(), result.children)
        assertEquals(null, result.nextUp)
        assertEquals(mockStreamOptions, result.streamOptions)
    }

    @Test
    fun `invoke returns correct data for Series`() = runTest {
        val seriesId = "series_1"
        val series = createSeries(seriesId)
        val seasons = listOf(createSeason("season_1", seriesId))
        val nextUpEpisode = createEpisode("ep_1", seriesId, "season_1")

        every { repo.observeItem(seriesId) } returns flowOf(series)
        every { repo.observeStreamOptions(seriesId) } returns flowOf(mockStreamOptions)

        every {
            repo.observeItems(match { it.cacheKey.contains("children_of_$seriesId") })
        } returns flowOf(seasons)

        every {
            repo.observeItems(match { it.cacheKey.contains("next_up_of_$seriesId") })
        } returns flowOf(listOf(nextUpEpisode))

        val result = useCase(seriesId).toList().first()

        assertEquals(series, result.item)
        assertEquals(seasons, result.children)
        assertEquals(nextUpEpisode, result.nextUp)
        assertEquals(mockStreamOptions, result.streamOptions)
    }

    @Test
    fun `invoke returns correct data for Season when NextUp contains matching episode`() = runTest {
        val seriesId = "series_1"
        val seasonId = "season_1"
        val season = createSeason(seasonId, seriesId)

        val childEpisode1 = createEpisode("ep_1", seriesId, seasonId)
        val childEpisode2 = createEpisode("ep_2", seriesId, seasonId)
        val children = listOf(childEpisode1, childEpisode2)

        val nextUpEpisode = createEpisode("ep_next", seriesId, seasonId) 
        val otherNextUpEpisode = createEpisode("ep_other", seriesId, "season_2")
        val nextUpList = listOf(otherNextUpEpisode, nextUpEpisode)

        every { repo.observeItem(seasonId) } returns flowOf(season)
        every { repo.observeStreamOptions(seasonId) } returns flowOf(mockStreamOptions)

        every {
            repo.observeItems(match { it.cacheKey.contains("children_of_$seasonId") })
        } returns flowOf(children)

        every {
            repo.observeItems(match { it.cacheKey.contains("next_up_of_$seriesId") })
        } returns flowOf(nextUpList)

        val result = useCase(seasonId).toList().first()

        assertEquals(season, result.item)
        assertEquals(children, result.children)
        assertEquals(nextUpEpisode, result.nextUp)
        assertEquals(mockStreamOptions, result.streamOptions)
    }

    @Test
    fun `invoke returns correct data for Season fallback to first child when NextUp has no match`() = runTest {
        val seriesId = "series_1"
        val seasonId = "season_1"
        val season = createSeason(seasonId, seriesId)

        val childEpisode1 = createEpisode("ep_1", seriesId, seasonId)
        val children = listOf(childEpisode1)

        val otherNextUpEpisode = createEpisode("ep_other", seriesId, "season_2") 
        val nextUpList = listOf(otherNextUpEpisode)

        every { repo.observeItem(seasonId) } returns flowOf(season)
        every { repo.observeStreamOptions(seasonId) } returns flowOf(mockStreamOptions)

        every {
            repo.observeItems(match { it.cacheKey.contains("children_of_$seasonId") })
        } returns flowOf(children)

        every {
            repo.observeItems(match { it.cacheKey.contains("next_up_of_$seriesId") })
        } returns flowOf(nextUpList)

        val result = useCase(seasonId).toList().first()

        assertEquals(season, result.item)
        assertEquals(childEpisode1, result.nextUp)
    }

    @Test
    fun `invoke returns null nextUp for Season when both sources are empty`() = runTest {
        val seriesId = "series_1"
        val seasonId = "season_1"
        val season = createSeason(seasonId, seriesId)

        every { repo.observeItem(seasonId) } returns flowOf(season)
        every { repo.observeStreamOptions(seasonId) } returns flowOf(mockStreamOptions)

        every {
            repo.observeItems(match { it.cacheKey.contains("children_of_$seasonId") })
        } returns flowOf(emptyList())

        every {
            repo.observeItems(match { it.cacheKey.contains("next_up_of_$seriesId") })
        } returns flowOf(emptyList())

        val result = useCase(seasonId).toList().first()

        assertEquals(season, result.item)
        assertEquals(null, result.nextUp)
    }

    @Test
    fun `invoke returns correct data for Episode`() = runTest {
        val seriesId = "series_1"
        val seasonId = "season_1"
        val episodeId = "ep_1"
        val episode = createEpisode(episodeId, seriesId, seasonId)

        every { repo.observeItem(episodeId) } returns flowOf(episode)
        every { repo.observeStreamOptions(episodeId) } returns flowOf(mockStreamOptions)

        val result = useCase(episodeId).toList().first()

        assertEquals(episode, result.item)
        assertEquals(emptyList<MediaItem>(), result.children)
        assertEquals(null, result.nextUp)
        assertEquals(mockStreamOptions, result.streamOptions)
    }

    @Test
    fun `invoke updates when repo emits new values`() = runTest {
        val seriesId = "series_1"
        val series = createSeries(seriesId)

        val streams1 = mockk<MediaItemStreamOptions>(relaxed = true)
        val streams2 = mockk<MediaItemStreamOptions>(relaxed = true)

        every { repo.observeItem(seriesId) } returns flowOf(series)
        every { repo.observeItems(any()) } returns flowOf(emptyList())
        every { repo.observeStreamOptions(seriesId) } returns flowOf(streams1, streams2)

        val results = useCase(seriesId).toList()

        assertEquals(2, results.size)
        assertEquals(streams1, results[0].streamOptions)
        assertEquals(streams2, results[1].streamOptions)
        assertNotEquals(streams1, streams2)
    }


    private fun createMovie(id: String): MediaItem.Movie {
        return MediaItem.Movie(
            id = id,
            name = "Movie Name",
            images = mockImages,
            userData = mockUserData,
            details = mockDetails,
            productionYear = 2024
        )
    }

    private fun createSeries(id: String): MediaItem.Series {
        return MediaItem.Series(
            id = id,
            name = "Series Name",
            images = mockImages,
            userData = mockUserData,
            details = mockDetails,
            productionYear = 2024,
            status = "Continuing"
        )
    }

    private fun createSeason(id: String, seriesId: String): MediaItem.Season {
        return MediaItem.Season(
            id = id,
            name = "Season Name",
            images = mockImages,
            userData = mockUserData,
            details = mockDetails,
            seriesId = seriesId,
            seasonNumber = 1
        )
    }

    private fun createEpisode(id: String, seriesId: String, seasonId: String): MediaItem.Episode {
        return MediaItem.Episode(
            id = id,
            name = "Episode Name",
            images = mockImages,
            userData = mockUserData,
            details = mockDetails,
            seriesName = "Series Name",
            seriesId = seriesId,
            seasonId = seasonId,
            seasonNumber = 1,
            episodeNumber = 1
        )
    }
}