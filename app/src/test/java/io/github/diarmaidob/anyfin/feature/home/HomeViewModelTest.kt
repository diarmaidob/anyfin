package io.github.diarmaidob.anyfin.feature.home

import io.github.diarmaidob.anyfin.common.test.ViewModelTestBase
import io.github.diarmaidob.anyfin.core.common.ScreenStateDelegate
import io.github.diarmaidob.anyfin.core.entity.MediaItem
import io.github.diarmaidob.anyfin.core.entity.ScreenState
import io.github.diarmaidob.anyfin.core.ui.uimodel.MediaItemListItemUiModel
import io.github.diarmaidob.anyfin.navigation.MediaItemDetailsDestination
import io.github.diarmaidob.anyfin.navigation.MediaItemListDestination
import io.github.diarmaidob.anyfin.navigation.PlayerDestination
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
class HomeViewModelTest : ViewModelTestBase() {

    @MockK
    lateinit var useCase: GetHomeSectionsUseCase

    @MockK(relaxed = true)
    lateinit var screenDelegate: ScreenStateDelegate<HomeEvent>

    @MockK
    lateinit var uiModelConverter: HomeScreenUiModelConverter

    private lateinit var viewModel: HomeViewModel

    private val sectionsFlow = MutableStateFlow<List<HomeSection>>(emptyList())
    private val screenStateFlow = MutableStateFlow(ScreenState<HomeEvent>())

    @Before
    fun setUp() {
        every { useCase.observe() } returns sectionsFlow
        every { screenDelegate.bind(any()) } returns screenStateFlow
        every { uiModelConverter.toUiModel(any()) } returns HomeScreenUiModel()

        mockScreenStateDelegate(screenDelegate)

        viewModel =
            HomeViewModel(
                useCase = useCase,
                screenStateDelegate = screenDelegate,
                homeScreenUiModelConverter = uiModelConverter
            )
    }

    @Test
    fun `uiModel combines sections and screen state via converter`() = runTest {
        val section =
            HomeSection(
                type = HomeSection.SectionType.RESUME,
                items = listOf(mockk<MediaItem>())
            )
        val screenState = ScreenState<HomeEvent>()
        val expectedUiModel = HomeScreenUiModel(title = "Test Home")

        val captured = mutableListOf<HomeData>()
        every { uiModelConverter.toUiModel(capture(captured)) } returns expectedUiModel

        startCollecting(viewModel.uiModel)

        sectionsFlow.value = listOf(section)
        screenStateFlow.value = screenState

        val actual = awaitValue(viewModel.uiModel)

        assertEquals(expectedUiModel, actual)
        val homeData = captured.single()
        assertEquals(listOf(section), homeData.sections)
        assertEquals(screenState, homeData.screenState)
    }

    @Test
    fun `onScreenVisible triggers refresh without manual flag`() = runTest {
        coEvery { useCase.refresh() } returns mockk()

        viewModel.onScreenVisible()

        coVerify { useCase.refresh() }
    }

    @Test
    fun `onPullToRefresh triggers manual refresh`() = runTest {
        coEvery { useCase.refresh() } returns mockk()

        viewModel.onPullToRefresh()

        coVerify { useCase.refresh() }
    }

    @Test
    fun `onEventConsumed delegates to screen delegate`() {
        val eventId = "event_1"

        viewModel.onEventConsumed(eventId)

        verify { screenDelegate.consumeEvent(eventId) }
    }

    @Test
    fun `onSectionHeaderClick with null navigation type does nothing`() {
        val section =
            HomeSectionUiModel(
                id = "id_1",
                title = "Section",
                items = emptyList(),
                posterType = HomeSectionUiModel.SectionPosterType.Poster,
                navigationListType = null
            )

        viewModel.onSectionHeaderClick(section)

        verify(exactly = 0) { screenDelegate.sendEvent(any()) }
    }

    @Test
    fun `onSectionHeaderClick sends navigation event when type present`() {
        val section =
            HomeSectionUiModel(
                id = "id_2",
                title = "Latest Movies",
                items = emptyList(),
                posterType = HomeSectionUiModel.SectionPosterType.Poster,
                navigationListType = MediaItemListDestination.ListType.LATEST_MOVIES
            )
        val expectedDest =
            MediaItemListDestination(
                listId = section.id,
                title = section.title,
                type = section.navigationListType!!
            )

        viewModel.onSectionHeaderClick(section)

        verify { screenDelegate.sendEvent(HomeEvent.NavigateToMediaItemList(expectedDest)) }
    }

    @Test
    fun `onItemClick sends NavigateToMediaItemDetails event`() {
        val item =
            mockk<MediaItemListItemUiModel> {
                every { id } returns "item_1"
                every { common.name } returns "Item Name"
            }
        val expectedDest = MediaItemDetailsDestination("item_1", "Item Name")

        viewModel.onItemClick(item)

        verify { screenDelegate.sendEvent(HomeEvent.NavigateToMediaItemDetails(expectedDest)) }
    }

    @Test
    fun `onResumeClick sends NavigateToPlayer event`() {
        val item = mockk<MediaItemListItemUiModel> { every { id } returns "item_2" }
        val expectedDest = PlayerDestination("item_2")

        viewModel.onResumeClick(item)

        verify { screenDelegate.sendEvent(HomeEvent.NavigateToPlayer(expectedDest)) }
    }
}
