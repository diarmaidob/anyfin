package io.github.diarmaidob.anyfin.feature.mediaitem.details

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import io.github.diarmaidob.anyfin.common.test.ViewModelTestBase
import io.github.diarmaidob.anyfin.core.common.DataResult
import io.github.diarmaidob.anyfin.core.common.ScreenStateDelegate
import io.github.diarmaidob.anyfin.core.entity.ScreenState
import io.github.diarmaidob.anyfin.core.ui.uimodel.MediaItemDetailsUiModel
import io.github.diarmaidob.anyfin.core.ui.uimodel.MediaItemListItemUiModel
import io.github.diarmaidob.anyfin.core.ui.uimodel.PlayAction
import io.github.diarmaidob.anyfin.core.ui.uimodel.PlayButtonUiModel
import io.github.diarmaidob.anyfin.navigation.MediaItemDetailsDestination
import io.github.diarmaidob.anyfin.navigation.PlayerDestination
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MediaItemDetailsViewModelTest : ViewModelTestBase() {

    @MockK
    lateinit var savedStateHandle: SavedStateHandle

    @MockK
    lateinit var observeMediaItemDetailsUseCase: ObserveMediaItemDetailsUseCase

    @MockK
    lateinit var refreshMediaItemDetailsUseCase: RefreshMediaItemDetailsUseCase

    @MockK
    lateinit var uiModelConverter: MediaItemDetailsScreenUiModelConverter

    @MockK(relaxed = true)
    lateinit var screenStateDelegate: ScreenStateDelegate<MediaItemDetailsEvent>

    private lateinit var viewModel: MediaItemDetailsViewModel

    private val dataFlow = MutableStateFlow(mockk<MediaItemDetailsData>(relaxed = true))
    private val screenStateFlow = MutableStateFlow(ScreenState<MediaItemDetailsEvent>())
    private val testDestination = MediaItemDetailsDestination("item_1", "Test Movie")

    @Before
    fun setUp() {
        mockkStatic("androidx.navigation.SavedStateHandleKt")
        every { savedStateHandle.toRoute<MediaItemDetailsDestination>() } returns testDestination

        every { observeMediaItemDetailsUseCase.invoke(any()) } returns dataFlow
        coEvery { refreshMediaItemDetailsUseCase.invoke(any()) } returns DataResult.Success(Unit)
        every { screenStateDelegate.bind(any()) } returns screenStateFlow
        every {
            uiModelConverter.toUiModel(any(), any(), any(), any(), any())
        } returns MediaItemDetailsScreenUiModel(title = "Default")

        mockScreenStateDelegate(screenStateDelegate)

        viewModel = MediaItemDetailsViewModel(
            savedStateHandle = savedStateHandle,
            observeItem = observeMediaItemDetailsUseCase,
            refreshItem = refreshMediaItemDetailsUseCase,
            uiModelConverter = uiModelConverter,
            screenStateDelegate = screenStateDelegate
        )
    }

    @After
    fun tearDown() {
        unmockkStatic("androidx.navigation.SavedStateHandleKt")
    }

    @Test
    fun `uiModel combines data, vm state, and screen state via converter`() = runTest {
        val data = mockk<MediaItemDetailsData>()
        val screenState = ScreenState<MediaItemDetailsEvent>()
        val expectedUiModel = MediaItemDetailsScreenUiModel(title = "Test Details")

        val captured = mutableListOf<MediaItemDetailsData>()
        every {
            uiModelConverter.toUiModel(
                data = capture(captured),
                audioIndex = any(),
                subtitleIndex = any(),
                screenState = any(),
                title = any()
            )
        } returns expectedUiModel

        startCollecting(viewModel.uiModel)

        dataFlow.value = data
        screenStateFlow.value = screenState

        val actual = awaitValue(viewModel.uiModel)

        assertEquals(expectedUiModel, actual)
        assertEquals(data, captured.single())
    }

    @Test
    fun `onScreenVisible triggers refresh without manual flag`() = runTest {
        coEvery { refreshMediaItemDetailsUseCase.invoke(any()) } returns mockk()

        viewModel.onScreenVisible()

        coVerify { refreshMediaItemDetailsUseCase.invoke("item_1") }
    }

    @Test
    fun `onPullToRefresh triggers manual refresh`() = runTest {
        coEvery { refreshMediaItemDetailsUseCase.invoke(any()) } returns mockk()

        viewModel.onPullToRefresh()

        coVerify { refreshMediaItemDetailsUseCase.invoke("item_1") }
    }

    @Test
    fun `onEventConsumed delegates to screen delegate`() {
        val eventId = "event_1"

        viewModel.onEventConsumed(eventId)

        verify { screenStateDelegate.consumeEvent(eventId) }
    }

    @Test
    fun `onAudioSelected updates ui model`() = runTest {
        val index = 2
        startCollecting(viewModel.uiModel)

        viewModel.onAudioSelected(index)

        awaitValue(viewModel.uiModel)
        verify {
            uiModelConverter.toUiModel(any(), eq(index), any(), any(), any())
        }
    }

    @Test
    fun `onSubtitleSelected updates ui model`() = runTest {
        val index = 3
        startCollecting(viewModel.uiModel)

        viewModel.onSubtitleSelected(index)

        awaitValue(viewModel.uiModel)
        verify {
            uiModelConverter.toUiModel(any(), any(), eq(index), any(), any())
        }
    }

    @Test
    fun `onPlayClick sends NavigateToPlayer with selections`() = runTest {
        val playAction = PlayAction.PlayItem("item_1")
        val playButton = mockk<PlayButtonUiModel> { every { action } returns playAction }
        val details = mockk<MediaItemDetailsUiModel> { every { this@mockk.playButton } returns playButton }
        val uiModel = MediaItemDetailsScreenUiModel(title = "T", details = details)

        every { uiModelConverter.toUiModel(any(), any(), any(), any(), any()) } returns uiModel
        startCollecting(viewModel.uiModel)

        viewModel.onAudioSelected(1)
        viewModel.onSubtitleSelected(2)
        awaitValue(viewModel.uiModel)

        viewModel.onPlayClick()

        val slot = slot<MediaItemDetailsEvent>()
        verify { screenStateDelegate.sendEvent(capture(slot)) }

        val expectedEvent = MediaItemDetailsEvent.NavigateToPlayer(
            PlayerDestination("item_1", 1, 2)
        )
        assertEquals(expectedEvent, slot.captured)
    }

    @Test
    fun `onPlayClick uses default audio track if selection is null`() = runTest {
        val playAction = PlayAction.PlayItem("item_1")
        val playButton = mockk<PlayButtonUiModel> { every { action } returns playAction }
        val defaultAudio = mockk<TrackUiModel> {
            every { isSelected } returns true
            every { index } returns 5
        }
        val details = mockk<MediaItemDetailsUiModel> { every { this@mockk.playButton } returns playButton }
        val uiModel = MediaItemDetailsScreenUiModel(
            title = "T",
            details = details,
            audioTracks = listOf(defaultAudio)
        )

        every { uiModelConverter.toUiModel(any(), any(), any(), any(), any()) } returns uiModel
        startCollecting(viewModel.uiModel)
        awaitValue(viewModel.uiModel)

        viewModel.onPlayClick()

        val slot = slot<MediaItemDetailsEvent>()
        verify { screenStateDelegate.sendEvent(capture(slot)) }

        val expectedEvent = MediaItemDetailsEvent.NavigateToPlayer(
            PlayerDestination("item_1", 5, null)
        )
        assertEquals(expectedEvent, slot.captured)
    }

    @Test
    fun `onPlayClick navigates to child ignoring selections`() = runTest {
        val playAction = PlayAction.NavigateToChild("child_id")
        val playButton = mockk<PlayButtonUiModel> { every { action } returns playAction }
        val details = mockk<MediaItemDetailsUiModel> { every { this@mockk.playButton } returns playButton }
        val uiModel = MediaItemDetailsScreenUiModel(title = "T", details = details)

        every { uiModelConverter.toUiModel(any(), any(), any(), any(), any()) } returns uiModel
        startCollecting(viewModel.uiModel)

        viewModel.onAudioSelected(1)
        awaitValue(viewModel.uiModel)

        viewModel.onPlayClick()

        val slot = slot<MediaItemDetailsEvent>()
        verify { screenStateDelegate.sendEvent(capture(slot)) }

        val expectedEvent = MediaItemDetailsEvent.NavigateToPlayer(
            PlayerDestination("child_id", null, null)
        )
        assertEquals(expectedEvent, slot.captured)
    }

    @Test
    fun `onPlayClick does nothing for None action`() = runTest {
        val playButton = mockk<PlayButtonUiModel> { every { action } returns PlayAction.None }
        val details = mockk<MediaItemDetailsUiModel> { every { this@mockk.playButton } returns playButton }
        val uiModel = MediaItemDetailsScreenUiModel(title = "T", details = details)

        every { uiModelConverter.toUiModel(any(), any(), any(), any(), any()) } returns uiModel
        startCollecting(viewModel.uiModel)
        awaitValue(viewModel.uiModel)

        viewModel.onPlayClick()

        verify(exactly = 0) { screenStateDelegate.sendEvent(any()) }
    }

    @Test
    fun `onPlayClick does nothing when details are null`() = runTest {
        val uiModel = MediaItemDetailsScreenUiModel(title = "T", details = null)
        every { uiModelConverter.toUiModel(any(), any(), any(), any(), any()) } returns uiModel
        startCollecting(viewModel.uiModel)
        awaitValue(viewModel.uiModel)

        viewModel.onPlayClick()

        verify(exactly = 0) { screenStateDelegate.sendEvent(any()) }
    }

    @Test
    fun `onChildClick sends NavigateToChild event`() {
        val child = mockk<MediaItemListItemUiModel> {
            every { id } returns "child_1"
            every { common.name } returns "Child Title"
        }

        viewModel.onChildClick(child)

        val slot = slot<MediaItemDetailsEvent>()
        verify { screenStateDelegate.sendEvent(capture(slot)) }

        val expectedEvent = MediaItemDetailsEvent.NavigateToChild(
            MediaItemDetailsDestination("child_1", "Child Title")
        )
        assertEquals(expectedEvent, slot.captured)
    }
}