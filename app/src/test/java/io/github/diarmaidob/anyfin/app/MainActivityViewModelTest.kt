package io.github.diarmaidob.anyfin.app

import io.github.diarmaidob.anyfin.common.test.ViewModelTestBase
import io.github.diarmaidob.anyfin.core.entity.SessionState
import io.github.diarmaidob.anyfin.core.session.SessionRepo
import io.github.diarmaidob.anyfin.navigation.DashboardDestination
import io.github.diarmaidob.anyfin.navigation.LoginDestination
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainActivityViewModelTest : ViewModelTestBase() {

    @MockK
    lateinit var sessionRepo: SessionRepo

    private lateinit var viewModel: MainActivityViewModel

    private val sessionStateFlow = MutableStateFlow<SessionState>(SessionState.LoggedOut)

    @Before
    fun setup() {
        every { sessionRepo.sessionState } returns sessionStateFlow
        viewModel = MainActivityViewModel(sessionRepo)
    }

    @Test
    fun `initial uiState is Success LoginDestination when session is LoggedOut`() = runTest {
        startCollecting(viewModel.uiState)

        val actual = awaitValue(viewModel.uiState)
        val expected = MainActivityUiModel.Success(LoginDestination)
        assertEquals(expected, actual)
    }

    @Test
    fun `uiState is Success DashboardDestination when session becomes LoggedIn`() = runTest {
        startCollecting(viewModel.uiState)

        sessionStateFlow.value = SessionState.LoggedIn("serverUrl", "authToken", userId = "userId")
        val actual = awaitValue(viewModel.uiState)
        val expected = MainActivityUiModel.Success(DashboardDestination)
        assertEquals(expected, actual)
    }

    @Test
    fun `uiState is Success LoginDestination when session becomes LoggedOut`() = runTest {
        startCollecting(viewModel.uiState)

        sessionStateFlow.update { SessionState.LoggedIn("serverUrl", "authToken", userId = "userId") }
        awaitValue(viewModel.uiState)

        sessionStateFlow.value = SessionState.LoggedOut
        val actual = awaitValue(viewModel.uiState)
        val expected = MainActivityUiModel.Success(LoginDestination)
        assertEquals(expected, actual)
    }
}