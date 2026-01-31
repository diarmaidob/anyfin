package io.github.diarmaidob.anyfin.feature.settings

import io.github.diarmaidob.anyfin.BuildConfig
import io.github.diarmaidob.anyfin.common.test.ViewModelTestBase
import io.github.diarmaidob.anyfin.core.common.ScreenStateDelegate
import io.github.diarmaidob.anyfin.core.entity.ScreenState
import io.github.diarmaidob.anyfin.core.entity.SessionState
import io.github.diarmaidob.anyfin.core.session.SessionRepo
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
class SettingsViewModelTest : ViewModelTestBase() {

    @MockK
    lateinit var sessionRepo: SessionRepo

    @MockK
    lateinit var logoutUseCase: LogoutUseCase

    @MockK(relaxed = true)
    lateinit var screenStateDelegate: ScreenStateDelegate<SettingsEvent>

    @MockK
    lateinit var settingScreenUiModelConverter: SettingsScreenUiModelConverter

    private lateinit var viewModel: SettingsViewModel

    private val sessionStateFlow = MutableStateFlow<SessionState>(SessionState.LoggedOut)
    private val screenStateFlow = MutableStateFlow(ScreenState<SettingsEvent>())

    @Before
    fun setUp() {
        every {
            sessionRepo.sessionState
        } returns sessionStateFlow

        every { screenStateDelegate.bind(any()) } returns screenStateFlow
        every { settingScreenUiModelConverter.toUiModel(any()) } returns SettingsScreenUiModel(
            version = BuildConfig.VERSION_NAME
        )
        coEvery { logoutUseCase() } returns mockk()

        mockScreenStateDelegate(screenStateDelegate)

        viewModel = SettingsViewModel(
            sessionRepo = sessionRepo,
            logoutUseCase = logoutUseCase,
            screenStateDelegate = screenStateDelegate,
            settingScreenUiModelConverter = settingScreenUiModelConverter
        )
    }

    @Test
    fun `uiModel combines session state and screen state via converter`() = runTest {
        val sessionState = SessionState.LoggedIn("https://example.com", "authToken", "user123")
        val screenState = ScreenState<SettingsEvent>()
        val expectedUiModel = SettingsScreenUiModel(
            version = "1.0.0",
            serverUrl = "https://example.com",
            userId = "user123"
        )

        val captured = mutableListOf<SettingsScreenData>()
        every { settingScreenUiModelConverter.toUiModel(capture(captured)) } returns expectedUiModel

        startCollecting(viewModel.uiModel)

        sessionStateFlow.value = sessionState
        screenStateFlow.value = screenState

        val actual = awaitValue(viewModel.uiModel)

        assertEquals(expectedUiModel, actual)
        val screenData = captured.single()
        assertEquals(sessionState, screenData.sessionState)
        assertEquals(screenState, screenData.screenState)
    }

    @Test
    fun `onEventConsumed delegates to screen delegate`() {
        val eventId = "event_1"

        viewModel.onEventConsumed(eventId)

        verify { screenStateDelegate.consumeEvent(eventId) }
    }

    @Test
    fun `onCopyClick sends CopyToClipboard event when value is valid`() {
        val label = "Server"
        val value = "https://example.com"

        viewModel.onCopyClick(label, value)

        verify { screenStateDelegate.sendEvent(SettingsEvent.CopyToClipboard(label, value)) }
    }

    @Test
    fun `onCopyClick does nothing when value is blank`() {
        val label = "Server"
        val value = "   "

        viewModel.onCopyClick(label, value)

        verify(exactly = 0) { screenStateDelegate.sendEvent(any()) }
    }

    @Test
    fun `onCopyClick does nothing when value is empty`() {
        val label = "Server"
        val value = ""

        viewModel.onCopyClick(label, value)

        verify(exactly = 0) { screenStateDelegate.sendEvent(any()) }
    }

    @Test
    fun `onLogoutClick triggers logout and sends LoggedOut event`() = runTest {
        coEvery { logoutUseCase() } returns mockk()

        viewModel.onLogoutClick()

        coVerify { logoutUseCase() }
        verify { screenStateDelegate.sendEvent(SettingsEvent.LoggedOut) }
    }
}