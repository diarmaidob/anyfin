package io.github.diarmaidob.anyfin.feature.mediaitem.list

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import io.github.diarmaidob.anyfin.common.test.ViewModelTestBase
import io.github.diarmaidob.anyfin.core.common.ScreenStateDelegate
import io.github.diarmaidob.anyfin.core.entity.MediaItem
import io.github.diarmaidob.anyfin.core.entity.MediaItemQuery
import io.github.diarmaidob.anyfin.core.entity.ScreenState
import io.github.diarmaidob.anyfin.core.ui.uimodel.MediaItemListItemUiModel
import io.github.diarmaidob.anyfin.navigation.MediaItemDetailsDestination
import io.github.diarmaidob.anyfin.navigation.MediaItemListDestination
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MediaItemListViewModelTest : ViewModelTestBase() {
    @MockK
    lateinit var savedStateHandle: SavedStateHandle

    @MockK
    lateinit var useCase: GetMediaItemListDataUseCase

    @MockK
    lateinit var uiModelConverter: MediaItemListScreenUiModelConverter

    @MockK
    lateinit var queryFactory: MediaItemListQueryFactory

    @MockK(relaxed = true)
    lateinit var screenDelegate: ScreenStateDelegate<MediaItemListEvent>

    private lateinit var viewModel: MediaItemListViewModel

    private val itemsFlow = MutableStateFlow<List<MediaItem>>(emptyList())
    private val stateFlow = MutableStateFlow(ScreenState<MediaItemListEvent>())
    private val destination = MediaItemListDestination("id_1", "TV Shows", MediaItemListDestination.ListType.LATEST_SHOWS)
    private val query = mockk<MediaItemQuery>()

    @Before
    fun setUp() {
        mockkStatic("androidx.navigation.SavedStateHandleKt")
        every { savedStateHandle.toRoute<MediaItemListDestination>() } returns destination

        every { queryFactory.create(destination) } returns query
        every { useCase.observe(query) } returns itemsFlow
        every { screenDelegate.bind(any()) } returns stateFlow
        every { uiModelConverter.toUiModel(any()) } returns MediaItemListScreenUiModel(
            title = "Test List",
            items = emptyList(),
            screenState = mockk(relaxed = true)
        )

        mockScreenStateDelegate(screenDelegate)

        viewModel = MediaItemListViewModel(
            savedStateHandle,
            useCase,
            uiModelConverter,
            queryFactory,
            screenDelegate
        )
    }

    @Test
    fun `uiModel combines items and screen state via converter`() = runTest {
        val items = listOf(mockk<MediaItem>())
        val screenState = ScreenState<MediaItemListEvent>()
        val expectedUiModel = MediaItemListScreenUiModel(
            title = "Test List",
            items = emptyList(),
            screenState = mockk(relaxed = true)
        )

        val captured = mutableListOf<MediaItemListScreenData>()
        every { uiModelConverter.toUiModel(capture(captured)) } returns expectedUiModel

        startCollecting(viewModel.uiModel)

        itemsFlow.value = items
        stateFlow.value = screenState

        val actual = awaitValue(viewModel.uiModel)

        assertEquals(expectedUiModel, actual)
        val screenData = captured.single()
        assertEquals("TV Shows", screenData.title)
        assertEquals(items, screenData.items)
        assertEquals(screenState, screenData.screenState)
    }

    @Test
    fun `onScreenVisible triggers refresh without manual flag`() = runTest {
        coEvery { useCase.refresh(any()) } returns mockk()

        viewModel.onScreenVisible()

        coVerify { useCase.refresh(query) }
    }

    @Test
    fun `onPullToRefresh triggers manual refresh`() = runTest {
        coEvery { useCase.refresh(any()) } returns mockk()

        viewModel.onPullToRefresh()

        coVerify { useCase.refresh(query) }
    }

    @Test
    fun `onItemClick sends NavigateToMediaItemDetails event`() {
        val item = mockk<MediaItemListItemUiModel> {
            every { id } returns "movie_123"
            every { common.name } returns "Inception"
        }
        val expectedDest = MediaItemDetailsDestination("movie_123", "Inception")

        viewModel.onItemClick(item)

        verify { screenDelegate.sendEvent(MediaItemListEvent.NavigateToMediaItemDetails(expectedDest)) }
    }

    @Test
    fun `onEventConsumed delegates to screen delegate`() {
        val eventId = "event_1"

        viewModel.onEventConsumed(eventId)

        verify { screenDelegate.consumeEvent(eventId) }
    }

    @Test
    fun `initialization creates query from factory`() {
        verify { queryFactory.create(destination) }
    }

}