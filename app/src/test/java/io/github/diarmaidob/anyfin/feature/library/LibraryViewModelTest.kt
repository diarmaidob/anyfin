package io.github.diarmaidob.anyfin.feature.library

import io.github.diarmaidob.anyfin.common.test.ViewModelTestBase
import io.github.diarmaidob.anyfin.core.common.ScreenStateDelegate
import io.github.diarmaidob.anyfin.core.entity.MediaItem
import io.github.diarmaidob.anyfin.core.entity.MediaItemQuery
import io.github.diarmaidob.anyfin.core.entity.ScreenState
import io.github.diarmaidob.anyfin.core.ui.uimodel.MediaItemListItemUiModel
import io.github.diarmaidob.anyfin.feature.mediaitem.list.GetMediaItemListDataUseCase
import io.github.diarmaidob.anyfin.navigation.MediaItemListDestination
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest : ViewModelTestBase() {

    @MockK
    lateinit var getMediaListUseCase: GetMediaItemListDataUseCase

    @MockK(relaxed = true)
    lateinit var screenStateDelegate: ScreenStateDelegate<LibraryEvent>

    @MockK
    lateinit var libraryScreenUiModelConverter: LibraryScreenUiModelConverter

    private lateinit var viewModel: LibraryViewModel

    private val mediaListFlow = MutableStateFlow<List<MediaItem>>(emptyList())
    private val screenStateFlow = MutableStateFlow(ScreenState<LibraryEvent>())

    @Before
    fun setUp() {
        every { getMediaListUseCase.observe(any()) } returns mediaListFlow
        every { screenStateDelegate.bind(any()) } returns screenStateFlow
        every { libraryScreenUiModelConverter.toUiModel(any()) } returns LibraryScreenUiModel()

        mockScreenStateDelegate(screenStateDelegate)

        viewModel = LibraryViewModel(
            getMediaListUseCase = getMediaListUseCase,
            screenStateDelegate = screenStateDelegate,
            libraryScreenUiModelConverter = libraryScreenUiModelConverter
        )
    }


    @Test
    fun `uiModel combines media items and screen state via converter`() = runTest {
        val mediaItems = listOf(mockk<MediaItem>())
        val screenState = ScreenState<LibraryEvent>()
        val expectedUiModel = LibraryScreenUiModel(title = "Test Library")
        val query = MediaItemQuery.UserViews()

        val captured = mutableListOf<LibraryScreenData>()
        every { libraryScreenUiModelConverter.toUiModel(capture(captured)) } returns expectedUiModel

        startCollecting(viewModel.uiModel)

        mediaListFlow.value = mediaItems
        screenStateFlow.value = screenState

        val actual = awaitValue(viewModel.uiModel)

        assertEquals(expectedUiModel, actual)
        val libraryData = captured.single()
        assertEquals(mediaItems, libraryData.items)
        assertEquals(screenState, libraryData.screenState)
    }

    @Test
    fun `onScreenVisible triggers refresh without manual flag`() = runTest {
        coEvery { getMediaListUseCase.refresh(any()) } returns mockk()

        viewModel.onScreenVisible()

        coVerify { getMediaListUseCase.refresh(MediaItemQuery.UserViews()) }
    }

    @Test
    fun `onPullToRefresh triggers manual refresh`() = runTest {
        coEvery { getMediaListUseCase.refresh(any()) } returns mockk()

        viewModel.onPullToRefresh()

        coVerify { getMediaListUseCase.refresh(MediaItemQuery.UserViews()) }
    }


    @Test
    fun `onItemClick sends NavigateToLibraryContents event`() {
        val item = mockk<MediaItemListItemUiModel> {
            every { id } returns "item_1"
            every { common.name } returns "Item Name"
        }
        val expectedDest = MediaItemListDestination(
            listId = "item_1",
            title = "Item Name",
            type = MediaItemListDestination.ListType.LIBRARY_CONTENTS
        )

        viewModel.onItemClick(item)

        verify { screenStateDelegate.sendEvent(LibraryEvent.NavigateToLibraryContents(expectedDest)) }
    }

    @Test
    fun `onEventConsumed delegates to screen delegate`() {
        val eventId = "event_1"

        viewModel.onEventConsumed(eventId)

        verify { screenStateDelegate.consumeEvent(eventId) }
    }
}