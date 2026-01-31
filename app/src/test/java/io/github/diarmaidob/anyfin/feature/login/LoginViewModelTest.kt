package io.github.diarmaidob.anyfin.feature.login

import io.github.diarmaidob.anyfin.common.test.ViewModelTestBase
import io.github.diarmaidob.anyfin.core.common.DataLoadError
import io.github.diarmaidob.anyfin.core.common.DataResult
import io.github.diarmaidob.anyfin.core.common.JellyfinConstants
import io.github.diarmaidob.anyfin.core.common.ScreenStateDelegate
import io.github.diarmaidob.anyfin.core.entity.AuthParams
import io.github.diarmaidob.anyfin.core.entity.ScreenState
import io.github.diarmaidob.anyfin.core.ui.uimodel.converter.ScreenStateUiModelConverter
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest : ViewModelTestBase() {

    @MockK
    lateinit var loginUseCase: LoginUseCase

    @MockK(relaxed = true)
    lateinit var screenDelegate: ScreenStateDelegate<LoginEvent>

    @MockK
    lateinit var screenStateUiModelConverter: ScreenStateUiModelConverter

    private lateinit var viewModel: LoginViewModel

    private val screenStateFlow = MutableStateFlow(ScreenState<LoginEvent>())

    @Before
    fun setUp() {
        every { screenDelegate.bind(any()) } returns screenStateFlow
        every { screenStateUiModelConverter.toUiModel<LoginEvent>(any(), any()) } returns mockk(relaxed = true)
        mockScreenStateDelegate(screenDelegate)
        viewModel = LoginViewModel(
            loginUseCase = loginUseCase,
            screenDelegate = screenDelegate,
            screenStateUiModelConverter = screenStateUiModelConverter
        )
    }

    @Test
    fun `uiModel combines form state and screen state via converter`() = runTest {
        val formState = LoginFormUiModel(host = "example.com", username = "user")
        val screenState = ScreenState<LoginEvent>()
        val expectedUiModel = LoginScreenUiModel(
            loginFormUiModel = formState,
            screenStateUiModel = mockk(relaxed = true)
        )

        every { screenStateUiModelConverter.toUiModel(screenState, true) } returns expectedUiModel.screenStateUiModel

        startCollecting(viewModel.uiModel)

        viewModel.onHostChange("example.com")
        viewModel.onUserChange("user")
        screenStateFlow.value = screenState

        val actual = awaitValue(viewModel.uiModel)

        assertEquals("example.com", actual.loginFormUiModel.host)
        assertEquals("user", actual.loginFormUiModel.username)
    }

    @Test
    fun `onPortChange updates state only for digits`() = runTest {
        startCollecting(viewModel.uiModel)

        viewModel.onPortChange("8097")
        var state = awaitValue(viewModel.uiModel).loginFormUiModel
        assertEquals("8097", state.port)
        assertFalse(state.portError)

        viewModel.onPortChange("80a")
        state = awaitValue(viewModel.uiModel).loginFormUiModel
        assertEquals("8097", state.port)
    }

    @Test
    fun `onUserChange updates username`() = runTest {
        startCollecting(viewModel.uiModel)

        viewModel.onUserChange("testUser")

        val state = awaitValue(viewModel.uiModel).loginFormUiModel
        assertEquals("testUser", state.username)
        assertNull(state.formError)
    }

    @Test
    fun `onPasswordChange updates password`() = runTest {
        startCollecting(viewModel.uiModel)

        viewModel.onPasswordChange("secret123")

        val state = awaitValue(viewModel.uiModel).loginFormUiModel
        assertEquals("secret123", state.password)
        assertNull(state.formError)
    }

    @Test
    fun `onTogglePasswordVisibility toggles boolean flag`() = runTest {
        startCollecting(viewModel.uiModel)

        assertFalse(awaitValue(viewModel.uiModel).loginFormUiModel.isPasswordVisible)

        viewModel.onTogglePasswordVisibility()
        assertTrue(awaitValue(viewModel.uiModel).loginFormUiModel.isPasswordVisible)

        viewModel.onTogglePasswordVisibility()
        assertFalse(awaitValue(viewModel.uiModel).loginFormUiModel.isPasswordVisible)
    }

    @Test
    fun `onProtocolToggle switches defaults correctly`() = runTest {
        startCollecting(viewModel.uiModel)

        viewModel.onPortChange(JellyfinConstants.DEFAULT_PORT_HTTP_STR)

        viewModel.onProtocolToggle()

        var state = awaitValue(viewModel.uiModel).loginFormUiModel
        assertTrue(state.isHttps)
        assertEquals(JellyfinConstants.DEFAULT_PORT_HTTPS_STR, state.port)

        viewModel.onProtocolToggle()

        state = awaitValue(viewModel.uiModel).loginFormUiModel
        assertFalse(state.isHttps)
        assertEquals(JellyfinConstants.DEFAULT_PORT_HTTP_STR, state.port)
    }

    @Test
    fun `onProtocolToggle preserves custom port`() = runTest {
        startCollecting(viewModel.uiModel)

        viewModel.onPortChange("5000")

        viewModel.onProtocolToggle()

        val state = awaitValue(viewModel.uiModel).loginFormUiModel
        assertTrue(state.isHttps)
        assertEquals("5000", state.port)
    }

    @Test
    fun `onLoginClick validates empty fields and sets errors`() = runTest {
        startCollecting(viewModel.uiModel)

        viewModel.onHostChange("")
        viewModel.onPortChange("")
        viewModel.onUserChange("")

        viewModel.onLoginClick()

        val state = awaitValue(viewModel.uiModel).loginFormUiModel
        assertTrue(state.hostError)
        assertTrue(state.portError)
        assertEquals("Please check all fields", state.formError)

        coVerify(exactly = 0) { loginUseCase.login(any()) }
    }

    @Test
    fun `onLoginClick success navigates to dashboard`() = runTest {
        startCollecting(viewModel.uiModel)

        viewModel.onHostChange("192.168.1.50")
        viewModel.onPortChange("8096")
        viewModel.onUserChange("user")
        viewModel.onPasswordChange("pass")

        coEvery { loginUseCase.login(any()) } returns DataResult.Success(Unit)

        viewModel.onLoginClick()

        coVerify {
            loginUseCase.login(AuthParams("http://192.168.1.50:8096", "user", "pass"))
        }

        verify { screenDelegate.sendEvent(LoginEvent.NavigateToDashboard) }
    }

    @Test
    fun `onLoginClick success with HTTPS constructs correct URL`() = runTest {
        startCollecting(viewModel.uiModel)

        viewModel.onHostChange("example.com")
        viewModel.onPortChange("443")
        viewModel.onUserChange("user")
        viewModel.onProtocolToggle()

        coEvery { loginUseCase.login(any()) } returns DataResult.Success(Unit)

        viewModel.onLoginClick()

        coVerify {
            loginUseCase.login(AuthParams("https://example.com:443", "user", ""))
        }
    }

    @Test
    fun `onLoginClick failure updates form error`() = runTest {
        startCollecting(viewModel.uiModel)

        viewModel.onHostChange("192.168.1.50")
        viewModel.onPortChange("8096")
        viewModel.onUserChange("user")

        val errorMessage = "Invalid credentials"
        val mockError = mockk<DataLoadError> {
            every { message } returns errorMessage
        }
        coEvery { loginUseCase.login(any()) } returns DataResult.Error(mockError)

        viewModel.onLoginClick()

        val state = awaitValue(viewModel.uiModel).loginFormUiModel
        assertEquals(errorMessage, state.formError)

        verify(exactly = 0) { screenDelegate.sendEvent(any()) }
    }

    @Test
    fun `onEventConsumed delegates to screen delegate`() {
        val eventId = "event_1"

        viewModel.onEventConsumed(eventId)

        verify { screenDelegate.consumeEvent(eventId) }
    }
}