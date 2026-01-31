package io.github.diarmaidob.anyfin.feature.player

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import io.github.diarmaidob.anyfin.common.test.ViewModelTestBase
import io.github.diarmaidob.anyfin.core.common.DataLoadError
import io.github.diarmaidob.anyfin.core.common.DataResult
import io.github.diarmaidob.anyfin.navigation.PlayerDestination
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VideoPlayerViewModelTest : ViewModelTestBase() {

    @MockK
    lateinit var savedStateHandle: SavedStateHandle

    @MockK
    lateinit var getPlaybackInfoUseCase: GetPlaybackInfoUseCase

    @MockK(relaxed = true)
    lateinit var playerController: VideoPlayerController

    @MockK
    lateinit var playbackInfo: PlaybackInfo

    private lateinit var viewModel: VideoPlayerViewModel
    private val destination = mockk<PlayerDestination>(relaxed = true)
    private val testItemId = "test_item_123"

    @Before
    fun setUp() {
        mockkStatic("androidx.navigation.SavedStateHandleKt")
        every { savedStateHandle.toRoute<PlayerDestination>() } returns destination
        every { destination.itemId } returns testItemId

        every { playbackInfo.title } returns "Test Title"
        every { playbackInfo.subtitle } returns "Test Subtitle"
        every { playbackInfo.streamUrl } returns "http://test.url"
        every { playbackInfo.startPositionMs } returns 1000L
        coEvery { getPlaybackInfoUseCase(testItemId) } returns DataResult.Success(playbackInfo)

        every { playerController.playerInstance } returns mockk()

        createViewModel()
    }

    private fun createViewModel() {
        viewModel = VideoPlayerViewModel(
            savedStateHandle,
            getPlaybackInfoUseCase,
            playerController
        )
    }

    @Test
    fun `init loads data successfully and initializes player`() = runTest {
        val uiModel = awaitValue(viewModel.uiModel)

        assertEquals("Test Title", uiModel.title)
        assertEquals("Test Subtitle", uiModel.subtitle)

        verify { playerController.setListener(viewModel) }
        verify { playerController.initialize("http://test.url", 1000L) }
    }

    @Test
    fun `init handles load failure`() = runTest {
        val errorMsg = "Network error"
        coEvery { getPlaybackInfoUseCase(testItemId) } returns DataResult.Error(DataLoadError.NetworkError(errorMsg))

        createViewModel()

        val uiModel = awaitValue(viewModel.uiModel)
        assertEquals(errorMsg, uiModel.errorMsg)
    }

    @Test
    fun `onPause pauses controller if playing`() {
        viewModel.onStateChanged(isPlaying = true, isBuffering = false, durationMs = 10000)

        viewModel.onPause()

        verify { playerController.pause() }
    }

    @Test
    fun `onPause does nothing if not playing`() {
        viewModel.onStateChanged(isPlaying = false, isBuffering = false, durationMs = 10000)

        viewModel.onPause()

        verify(exactly = 0) { playerController.pause() }
    }

    @Test
    fun `onPlayPause pauses when playing`() {
        viewModel.onStateChanged(isPlaying = true, isBuffering = false, durationMs = 10000)

        viewModel.onPlayPause()

        verify { playerController.pause() }
    }

    @Test
    fun `onPlayPause plays when paused`() {
        viewModel.onStateChanged(isPlaying = false, isBuffering = false, durationMs = 10000)

        viewModel.onPlayPause()

        verify { playerController.play() }
    }

    @Test
    fun `onSeekStart sets seeking state`() = runTest {
        viewModel.onSeekStart()

        val uiModel = awaitValue(viewModel.uiModel)
        assertEquals(true, uiModel.isSeeking)
    }

    @Test
    fun `onSeekEnd seeks to target and clears seeking state`() = runTest {
        viewModel.onStateChanged(isPlaying = true, isBuffering = false, durationMs = 100000)

        viewModel.onSeekEnd(0.5f)

        verify { playerController.seekTo(50000) }

        val uiModel = awaitValue(viewModel.uiModel)
        assertEquals(false, uiModel.isSeeking)
        assertEquals(50000L, uiModel.currentTimeMs)
    }

    @Test
    fun `onSeekDelta seeks to new position clamped to duration`() = runTest {
        viewModel.onStateChanged(isPlaying = true, isBuffering = false, durationMs = 100000)
        viewModel.onPositionChanged(50000)

        viewModel.onSeekDelta(10000)

        verify { playerController.seekTo(60000) }

        viewModel.onSeekDelta(-70000)
        verify { playerController.seekTo(0) }
    }

    @Test
    fun `toggleControls toggles visibility`() = runTest {
        val initial = awaitValue(viewModel.uiModel).showControls

        viewModel.toggleControls()
        assertEquals(!initial, awaitValue(viewModel.uiModel).showControls)

        viewModel.toggleControls()
        assertEquals(initial, awaitValue(viewModel.uiModel).showControls)
    }

    @Test
    fun `showTrackSelector updates state and hides controls`() = runTest {
        viewModel.showTrackSelector(TrackType.AUDIO)

        val uiModel = awaitValue(viewModel.uiModel)
        assertEquals(TrackType.AUDIO, uiModel.activeDialog)
        assertEquals(false, uiModel.showControls)
    }

    @Test
    fun `dismissDialog resets state`() = runTest {
        viewModel.showTrackSelector(TrackType.AUDIO)
        viewModel.dismissDialog()

        val uiModel = awaitValue(viewModel.uiModel)
        assertEquals(null, uiModel.activeDialog)
        assertEquals(true, uiModel.showControls)
    }

    @Test
    fun `onTrackSelected calls controller and dismisses dialog`() = runTest {
        viewModel.showTrackSelector(TrackType.AUDIO)

        viewModel.onTrackSelected(TrackType.AUDIO, "track_1")

        verify { playerController.selectTrack(TrackType.AUDIO, "track_1") }
        assertEquals(null, awaitValue(viewModel.uiModel).activeDialog)
    }

    @Test
    fun `onSubtitleOff clears text track and dismisses dialog`() = runTest {
        viewModel.showTrackSelector(TrackType.TEXT)

        viewModel.onSubtitleOff()

        verify { playerController.clearTrack(TrackType.TEXT) }
        assertEquals(null, awaitValue(viewModel.uiModel).activeDialog)
    }

    @Test
    fun `onStateChanged updates uiModel`() = runTest {
        viewModel.onStateChanged(isPlaying = true, isBuffering = true, durationMs = 5000)

        val uiModel = awaitValue(viewModel.uiModel)
        assertEquals(true, uiModel.isPlaying)
        assertEquals(true, uiModel.isBuffering)
        assertEquals(5000L, uiModel.durationMs)
    }

    @Test
    fun `onPositionChanged updates time only when not seeking`() = runTest {
        viewModel.onPositionChanged(1234)
        assertEquals(1234L, awaitValue(viewModel.uiModel).currentTimeMs)

        viewModel.onSeekStart()
        viewModel.onPositionChanged(5678)

        assertNotEquals(5678L, awaitValue(viewModel.uiModel).currentTimeMs)
        assertEquals(1234L, awaitValue(viewModel.uiModel).currentTimeMs)
    }

    @Test
    fun `onError updates error message`() = runTest {
        viewModel.onError("Fatal Error")
        assertEquals("Fatal Error", awaitValue(viewModel.uiModel).errorMsg)
    }

    @Test
    fun `onEnded resets playing state and shows controls`() = runTest {
        viewModel.toggleControls()

        viewModel.onEnded()

        val uiModel = awaitValue(viewModel.uiModel)
        assertEquals(false, uiModel.isPlaying)
        assertEquals(true, uiModel.showControls)
    }

    @Test
    fun `onTracksChanged updates track lists`() = runTest {
        val audioTracks = listOf(mockk<PlayerTrack>())
        val textTracks = listOf(mockk<PlayerTrack>())

        viewModel.onTracksChanged(audioTracks, textTracks)

        val uiModel = awaitValue(viewModel.uiModel)
        assertEquals(audioTracks, uiModel.audioTracks)
        assertEquals(textTracks, uiModel.subtitleTracks)
    }

    @Test
    fun `controls auto-hide after 4 seconds when playing`() = runTest {
        viewModel.onStateChanged(isPlaying = true, isBuffering = false, durationMs = 1000)

        assertEquals(true, viewModel.uiModel.value.showControls)

        advanceTimeBy(3000)
        assertEquals(true, viewModel.uiModel.value.showControls)

        advanceTimeBy(1001)
        assertEquals(false, viewModel.uiModel.value.showControls)
    }

    @Test
    fun `controls do not auto-hide if paused`() = runTest {
        viewModel.onStateChanged(isPlaying = false, isBuffering = false, durationMs = 1000)

        advanceTimeBy(5000)

        assertEquals(true, viewModel.uiModel.value.showControls)
    }

    @Test
    fun `user interaction resets auto-hide timer`() = runTest {
        viewModel.onStateChanged(isPlaying = true, isBuffering = false, durationMs = 1000)

        advanceTimeBy(3000)
        viewModel.toggleControls()
        if (!viewModel.uiModel.value.showControls) viewModel.toggleControls()

        advanceTimeBy(3000)
        assertEquals(true, viewModel.uiModel.value.showControls)

        advanceTimeBy(1001)
        assertEquals(false, viewModel.uiModel.value.showControls)
    }
}