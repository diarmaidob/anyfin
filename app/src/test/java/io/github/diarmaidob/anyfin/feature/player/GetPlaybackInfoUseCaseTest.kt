package io.github.diarmaidob.anyfin.feature.player

import io.github.diarmaidob.anyfin.core.common.DataLoadError
import io.github.diarmaidob.anyfin.core.common.DataResult
import io.github.diarmaidob.anyfin.core.common.MediaSupportChecker
import io.github.diarmaidob.anyfin.core.entity.MediaItem
import io.github.diarmaidob.anyfin.core.entity.MediaItemSource
import io.github.diarmaidob.anyfin.core.entity.MediaItemStream
import io.github.diarmaidob.anyfin.core.entity.MediaItemStreamOptions
import io.github.diarmaidob.anyfin.core.entity.MediaItemUserData
import io.github.diarmaidob.anyfin.core.entity.SessionState
import io.github.diarmaidob.anyfin.core.mediaitem.MediaItemRepo
import io.github.diarmaidob.anyfin.core.session.SessionRepo
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GetPlaybackInfoUseCaseTest {

    @MockK
    private lateinit var repo: MediaItemRepo

    @MockK
    private lateinit var sessionRepo: SessionRepo

    private lateinit var useCase: GetPlaybackInfoUseCase

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        coEvery { repo.refreshItem(any()) } returns DataResult.Success(Unit)
        mockkObject(MediaSupportChecker)

        useCase = GetPlaybackInfoUseCase(repo, sessionRepo)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `invoke with logged out session returns auth error`() = runTest {
        every { sessionRepo.getCurrentSessionState() } returns SessionState.LoggedOut

        val result = useCase("itemId")

        val expected = DataResult.Error(DataLoadError.AuthError("User must be logged in"))
        assertEquals(expected, result)
    }

    @Test
    fun `invoke with item not found returns unknown error`() = runTest {
        every { sessionRepo.getCurrentSessionState() } returns createSessionState()
        every { repo.observeItem("itemId") } returns emptyFlow()

        val result = useCase("itemId")

        val expected = DataResult.Error(DataLoadError.UnknownError("Item not found"))
        assertEquals(expected, result)
    }

    @Test
    fun `invoke with stream options not found returns unknown error`() = runTest {
        val item = createEpisode()
        every { sessionRepo.getCurrentSessionState() } returns createSessionState()
        every { repo.observeItem("itemId") } returns flowOf(item)
        every { repo.observeStreamOptions("itemId") } returns emptyFlow()

        val result = useCase("itemId")

        val expected = DataResult.Error(DataLoadError.UnknownError("No stream options found"))
        assertEquals(expected, result)
    }

    @Test
    fun `invoke with source details error returns error`() = runTest {
        val item = createEpisode()
        val streamOptions = createMediaItemStreamOptions()
        every { sessionRepo.getCurrentSessionState() } returns createSessionState()
        every { repo.observeItem("itemId") } returns flowOf(item)
        every { repo.observeStreamOptions("itemId") } returns flowOf(streamOptions)
        coEvery { repo.getSource("sourceId") } returns DataResult.Error(DataLoadError.UnknownError("Network error"))

        val result = useCase("itemId")

        val expected = DataResult.Error(DataLoadError.UnknownError("Network error"))
        assertEquals(expected, result)
    }

    @Test
    fun `invoke with transcoding not supported by server returns error`() = runTest {
        val item = createEpisode()
        val videoStream = createMediaItemStream(codec = "h265")
        val streamOptions = createMediaItemStreamOptions(video = listOf(videoStream))
        val source = createMediaItemSource(supportsTranscoding = false)
        every { sessionRepo.getCurrentSessionState() } returns createSessionState()
        every { repo.observeItem("itemId") } returns flowOf(item)
        every { repo.observeStreamOptions("itemId") } returns flowOf(streamOptions)
        coEvery { repo.getSource("sourceId") } returns DataResult.Success(source)
        every { MediaSupportChecker.isCodecSupported("h265", "") } returns false

        val result = useCase("itemId")

        val expected = DataResult.Error(DataLoadError.UnknownError("Transcoding required but not supported by the server"))
        assertEquals(expected, result)
    }

    @Test
    fun `invoke with episode returns correct playback info with direct play`() = runTest {
        val item = createEpisode(
            seriesName = "Series Name",
            name = "Episode Name",
            seasonNumber = 1,
            episodeNumber = 2,
            playbackPositionTicks = 100000L
        )
        val videoStream = createMediaItemStream(codec = "h264")
        val audioStream = createAudioStream(indexNumber = 2L)
        val streamOptions = createMediaItemStreamOptions(
            video = listOf(videoStream),
            audio = listOf(audioStream)
        )
        val source = createMediaItemSource()
        every { sessionRepo.getCurrentSessionState() } returns createSessionState()
        every { repo.observeItem("itemId") } returns flowOf(item)
        every { repo.observeStreamOptions("itemId") } returns flowOf(streamOptions)
        coEvery { repo.getSource("sourceId") } returns DataResult.Success(source)
        every { MediaSupportChecker.isCodecSupported("h264", "") } returns true

        val result = useCase("itemId")

        val expected = PlaybackInfo(
            title = "Series Name",
            subtitle = "S1:E2 - Episode Name",
            streamUrl = "http://server.com/Videos/itemId/stream?static=true&mediaSourceId=sourceId&api_key=token123",
            startPositionMs = 10L,
            isTranscoding = false
        )
        assertEquals(DataResult.Success(expected), result)
    }

    @Test
    fun `invoke with movie returns correct playback info with direct play`() = runTest {
        val item = createMovie(
            name = "Movie Title",
            productionYear = 2020,
            playbackPositionTicks = 200000L
        )
        val videoStream = createMediaItemStream(codec = "h264")
        val streamOptions = createMediaItemStreamOptions(
            videoContainer = "mp4",
            video = listOf(videoStream)
        )
        val source = createMediaItemSource(container = "mp4")
        every { sessionRepo.getCurrentSessionState() } returns createSessionState()
        every { repo.observeItem("movieId") } returns flowOf(item)
        every { repo.observeStreamOptions("movieId") } returns flowOf(streamOptions)
        coEvery { repo.getSource("sourceId") } returns DataResult.Success(source)
        every { MediaSupportChecker.isCodecSupported("h264", "") } returns true

        val result = useCase("movieId")

        val expected = PlaybackInfo(
            title = "Movie Title",
            subtitle = "2020",
            streamUrl = "http://server.com/Videos/movieId/stream?static=true&mediaSourceId=sourceId&api_key=token123",
            startPositionMs = 20L,
            isTranscoding = false
        )
        assertEquals(DataResult.Success(expected), result)
    }

    @Test
    fun `invoke with movie without production year returns null subtitle`() = runTest {
        val item = createMovie(productionYear = null)
        val videoStream = createMediaItemStream()
        val streamOptions = createMediaItemStreamOptions(video = listOf(videoStream))
        val source = createMediaItemSource()
        every { sessionRepo.getCurrentSessionState() } returns createSessionState()
        every { repo.observeItem("movieId") } returns flowOf(item)
        every { repo.observeStreamOptions("movieId") } returns flowOf(streamOptions)
        coEvery { repo.getSource("sourceId") } returns DataResult.Success(source)
        every { MediaSupportChecker.isCodecSupported("h264", "") } returns true

        val result = useCase("movieId")

        val playbackInfo = (result as DataResult.Success).data
        assertEquals(null, playbackInfo.subtitle)
    }

    @Test
    fun `invoke with unsupported container uses transcoding`() = runTest {
        val item = createMovie()
        val streamOptions = createMediaItemStreamOptions(videoContainer = "avi")
        val source = createMediaItemSource(container = "avi")
        every { sessionRepo.getCurrentSessionState() } returns createSessionState()
        every { repo.observeItem("itemId") } returns flowOf(item)
        every { repo.observeStreamOptions("itemId") } returns flowOf(streamOptions)
        coEvery { repo.getSource("sourceId") } returns DataResult.Success(source)
        every { MediaSupportChecker.isCodecSupported("h264", "") } returns true

        val result = useCase("itemId")

        val playbackInfo = (result as DataResult.Success).data
        assertEquals(true, playbackInfo.isTranscoding)
        assertEquals(true, playbackInfo.streamUrl.contains("master.m3u8"))
    }

    @Test
    fun `invoke with unsupported video codec uses transcoding`() = runTest {
        val item = createMovie()
        val videoStream = createMediaItemStream(codec = "h265")
        val streamOptions = createMediaItemStreamOptions(video = listOf(videoStream))
        val source = createMediaItemSource()
        every { sessionRepo.getCurrentSessionState() } returns createSessionState()
        every { repo.observeItem("itemId") } returns flowOf(item)
        every { repo.observeStreamOptions("itemId") } returns flowOf(streamOptions)
        coEvery { repo.getSource("sourceId") } returns DataResult.Success(source)
        every { MediaSupportChecker.isCodecSupported("h265", "") } returns false

        val result = useCase("itemId")

        val playbackInfo = (result as DataResult.Success).data
        assertEquals(true, playbackInfo.isTranscoding)
        assertEquals(true, playbackInfo.streamUrl.contains("VideoCodec=h264"))
    }

    @Test
    fun `invoke with specific audio stream index selects correct audio`() = runTest {
        val item = createMovie()
        val videoStream = createMediaItemStream(codec = "h265")
        val audioStream1 = createAudioStream(indexNumber = 1L, language = "eng")
        val audioStream2 = createAudioStream(indexNumber = 2L, language = "fre")
        val streamOptions = createMediaItemStreamOptions(
            video = listOf(videoStream),
            audio = listOf(audioStream1, audioStream2)
        )
        val source = createMediaItemSource()
        every { sessionRepo.getCurrentSessionState() } returns createSessionState()
        every { repo.observeItem("itemId") } returns flowOf(item)
        every { repo.observeStreamOptions("itemId") } returns flowOf(streamOptions)
        coEvery { repo.getSource("sourceId") } returns DataResult.Success(source)
        every { MediaSupportChecker.isCodecSupported("h265", "") } returns false

        val result = useCase("itemId", audioStreamIndex = 2, subtitleStreamIndex = null)

        val playbackInfo = (result as DataResult.Success).data
        assertEquals(true, playbackInfo.streamUrl.contains("AudioStreamIndex=2"))
    }

    @Test
    fun `invoke with null audio stream index selects first audio`() = runTest {
        val item = createMovie()
        val videoStream = createMediaItemStream(codec = "h265")
        val audioStream = createAudioStream(indexNumber = 3L)
        val streamOptions = createMediaItemStreamOptions(
            video = listOf(videoStream),
            audio = listOf(audioStream)
        )
        val source = createMediaItemSource()
        every { sessionRepo.getCurrentSessionState() } returns createSessionState()
        every { repo.observeItem("itemId") } returns flowOf(item)
        every { repo.observeStreamOptions("itemId") } returns flowOf(streamOptions)
        coEvery { repo.getSource("sourceId") } returns DataResult.Success(source)
        every { MediaSupportChecker.isCodecSupported("h265", "") } returns false

        val result = useCase("itemId", audioStreamIndex = null, subtitleStreamIndex = null)

        val playbackInfo = (result as DataResult.Success).data
        assertEquals(true, playbackInfo.streamUrl.contains("AudioStreamIndex=3"))
    }

    @Test
    fun `invoke with subtitle stream index includes in transcoding url`() = runTest {
        val item = createMovie()
        val videoStream = createMediaItemStream(codec = "h265")
        val streamOptions = createMediaItemStreamOptions(video = listOf(videoStream))
        val source = createMediaItemSource()
        every { sessionRepo.getCurrentSessionState() } returns createSessionState()
        every { repo.observeItem("itemId") } returns flowOf(item)
        every { repo.observeStreamOptions("itemId") } returns flowOf(streamOptions)
        coEvery { repo.getSource("sourceId") } returns DataResult.Success(source)
        every { MediaSupportChecker.isCodecSupported("h265", "") } returns false

        val result = useCase("itemId", audioStreamIndex = null, subtitleStreamIndex = 5)

        val playbackInfo = (result as DataResult.Success).data
        assertEquals(true, playbackInfo.streamUrl.contains("SubtitleStreamIndex=5"))
    }

    @Test
    fun `invoke with direct play does not include audio or subtitle indices in url`() = runTest {
        val item = createMovie()
        val videoStream = createMediaItemStream(codec = "h264")
        val streamOptions = createMediaItemStreamOptions(
            videoContainer = "mp4",
            video = listOf(videoStream)
        )
        val source = createMediaItemSource(container = "mp4")
        every { sessionRepo.getCurrentSessionState() } returns createSessionState()
        every { repo.observeItem("itemId") } returns flowOf(item)
        every { repo.observeStreamOptions("itemId") } returns flowOf(streamOptions)
        coEvery { repo.getSource("sourceId") } returns DataResult.Success(source)
        every { MediaSupportChecker.isCodecSupported("h264", "") } returns true

        val result = useCase("itemId", audioStreamIndex = 2, subtitleStreamIndex = 5)

        val playbackInfo = (result as DataResult.Success).data
        assertEquals(false, playbackInfo.streamUrl.contains("AudioStreamIndex"))
        assertEquals(false, playbackInfo.streamUrl.contains("SubtitleStreamIndex"))
        assertEquals(false, playbackInfo.streamUrl.contains("master.m3u8"))
    }

    @Test
    fun `invoke calculates start position correctly`() = runTest {
        val item = createMovie(playbackPositionTicks = 150000L)
        val videoStream = createMediaItemStream()
        val streamOptions = createMediaItemStreamOptions(video = listOf(videoStream))
        val source = createMediaItemSource()
        every { sessionRepo.getCurrentSessionState() } returns createSessionState()
        every { repo.observeItem("itemId") } returns flowOf(item)
        every { repo.observeStreamOptions("itemId") } returns flowOf(streamOptions)
        coEvery { repo.getSource("sourceId") } returns DataResult.Success(source)
        every { MediaSupportChecker.isCodecSupported("h264", "") } returns true

        val result = useCase("itemId")

        val playbackInfo = (result as DataResult.Success).data
        assertEquals(15L, playbackInfo.startPositionMs)
    }

    @Test
    fun `invoke with different server url builds correct url`() = runTest {
        val session = createSessionState(
            serverUrl = "https://jellyfin.example.com",
            authToken = "authToken456"
        )
        val item = createMovie(id = "movie123")
        val videoStream = createMediaItemStream()
        val streamOptions = createMediaItemStreamOptions(
            sourceId = "source456",
            videoContainer = "mp4",
            video = listOf(videoStream)
        )
        val source = createMediaItemSource(id = "source456", mediaId = "movie123", container = "mp4")
        every { sessionRepo.getCurrentSessionState() } returns session
        every { repo.observeItem("movie123") } returns flowOf(item)
        every { repo.observeStreamOptions("movie123") } returns flowOf(streamOptions)
        coEvery { repo.getSource("source456") } returns DataResult.Success(source)
        every { MediaSupportChecker.isCodecSupported("h264", "") } returns true

        val result = useCase("movie123")

        val playbackInfo = (result as DataResult.Success).data
        assertEquals(
            "https://jellyfin.example.com/Videos/movie123/stream?static=true&mediaSourceId=source456&api_key=authToken456",
            playbackInfo.streamUrl
        )
    }

    @Test
    fun `invoke with transcoding includes correct codecs in url`() = runTest {
        val item = createMovie()
        val videoStream = createMediaItemStream(codec = "h265")
        val streamOptions = createMediaItemStreamOptions(video = listOf(videoStream))
        val source = createMediaItemSource()
        every { sessionRepo.getCurrentSessionState() } returns createSessionState()
        every { repo.observeItem("itemId") } returns flowOf(item)
        every { repo.observeStreamOptions("itemId") } returns flowOf(streamOptions)
        coEvery { repo.getSource("sourceId") } returns DataResult.Success(source)
        every { MediaSupportChecker.isCodecSupported("h265", "") } returns false

        val result = useCase("itemId")

        val playbackInfo = (result as DataResult.Success).data
        assertEquals(true, playbackInfo.streamUrl.contains("VideoCodec=h264"))
        assertEquals(true, playbackInfo.streamUrl.contains("AudioCodec=copy"))
    }

    @Test
    fun `invoke with no video streams selects first audio when available`() = runTest {
        val item = createMovie()
        val audioStream = createAudioStream(indexNumber = 5L)
        val streamOptions = createMediaItemStreamOptions(
            video = emptyList(),
            audio = listOf(audioStream)
        )
        val source = createMediaItemSource()
        every { sessionRepo.getCurrentSessionState() } returns createSessionState()
        every { repo.observeItem("itemId") } returns flowOf(item)
        every { repo.observeStreamOptions("itemId") } returns flowOf(streamOptions)
        coEvery { repo.getSource("sourceId") } returns DataResult.Success(source)
        every { MediaSupportChecker.isCodecSupported(null, "") } returns true

        val result = useCase("itemId")

        assertEquals(DataResult.Success::class.java, result::class.java)
    }

    @Test
    fun `invoke with empty audio list uses null for audio selection`() = runTest {
        val item = createMovie()
        val videoStream = createMediaItemStream(codec = "h264")
        val streamOptions = createMediaItemStreamOptions(
            video = listOf(videoStream),
            audio = emptyList()
        )
        val source = createMediaItemSource()
        every { sessionRepo.getCurrentSessionState() } returns createSessionState()
        every { repo.observeItem("itemId") } returns flowOf(item)
        every { repo.observeStreamOptions("itemId") } returns flowOf(streamOptions)
        coEvery { repo.getSource("sourceId") } returns DataResult.Success(source)
        every { MediaSupportChecker.isCodecSupported("h264", "") } returns true

        val result = useCase("itemId")

        val playbackInfo = (result as DataResult.Success).data
        assertEquals(false, playbackInfo.isTranscoding)
    }

    private fun createSessionState(
        serverUrl: String = "http://server.com",
        authToken: String = "token123",
        userId: String = "userId"
    ) = SessionState.LoggedIn(serverUrl, authToken, userId)

    private fun createMediaItemUserData(
        playbackPositionTicks: Long = 0L
    ) = MediaItemUserData(
        mediaId = "mediaId",
        isPlayed = false,
        playbackPositionTicks = playbackPositionTicks,
        isFavorite = false,
        lastPlayedDate = 0,
        playCount = 0
    )

    private fun createEpisode(
        id: String = "episodeId",
        name: String = "Episode Name",
        seriesName: String = "Series Name",
        seasonNumber: Int = 1,
        episodeNumber: Int = 1,
        playbackPositionTicks: Long = 0L
    ) = MediaItem.Episode(
        id = id,
        name = name,
        images = mockk(),
        userData = createMediaItemUserData(playbackPositionTicks),
        details = null,
        seriesName = seriesName,
        seriesId = "seriesId",
        seasonId = "seasonId",
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber
    )

    private fun createMovie(
        id: String = "movieId",
        name: String = "Movie Title",
        productionYear: Int? = 2020,
        playbackPositionTicks: Long = 0L
    ) = MediaItem.Movie(
        id = id,
        name = name,
        images = mockk(),
        userData = createMediaItemUserData(playbackPositionTicks),
        details = null,
        productionYear = productionYear
    )

    private fun createMediaItemSource(
        id: String = "sourceId",
        mediaId: String = "itemId",
        container: String? = "mkv",
        supportsDirectPlay: Boolean = true,
        supportsTranscoding: Boolean = true
    ) = MediaItemSource(
        id = id,
        mediaId = mediaId,
        container = container,
        protocol = null,
        name = "Source",
        supportsDirectPlay = supportsDirectPlay,
        supportsTranscoding = supportsTranscoding
    )

    private fun createMediaItemStream(
        sourceId: String = "sourceId",
        indexNumber: Long = 1L,
        type: String = "Video",
        codec: String? = "h264"
    ) = MediaItemStream(
        sourceId = sourceId,
        indexNumber = indexNumber,
        type = type,
        codec = codec,
        language = null,
        displayTitle = null,
        isDefault = null,
        isForced = null,
        channels = null,
        width = null,
        height = null
    )

    private fun createAudioStream(
        sourceId: String = "sourceId",
        indexNumber: Long = 2L,
        codec: String? = "aac",
        language: String = "eng"
    ) = MediaItemStream(
        sourceId = sourceId,
        indexNumber = indexNumber,
        type = "Audio",
        codec = codec,
        language = language,
        displayTitle = language,
        isDefault = true,
        isForced = false,
        channels = 2L,
        width = null,
        height = null
    )

    private fun createMediaItemStreamOptions(
        sourceId: String = "sourceId",
        videoContainer: String? = "mkv",
        supportsTranscoding: Boolean = true,
        supportsDirectPlay: Boolean = true,
        video: List<MediaItemStream> = emptyList(),
        audio: List<MediaItemStream> = emptyList(),
        subtitles: List<MediaItemStream> = emptyList()
    ) = MediaItemStreamOptions(
        sourceId = sourceId,
        videoContainer = videoContainer,
        supportsTranscoding = supportsTranscoding,
        supportsDirectPlay = supportsDirectPlay,
        video = video,
        audio = audio,
        subtitles = subtitles
    )
}