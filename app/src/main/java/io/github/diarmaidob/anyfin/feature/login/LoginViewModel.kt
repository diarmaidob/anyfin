package io.github.diarmaidob.anyfin.feature.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.diarmaidob.anyfin.core.common.DataResult
import io.github.diarmaidob.anyfin.core.common.JellyfinConstants
import io.github.diarmaidob.anyfin.core.common.ScreenStateDelegate
import io.github.diarmaidob.anyfin.core.entity.AuthParams
import io.github.diarmaidob.anyfin.core.ui.uimodel.converter.ScreenStateUiModelConverter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val screenDelegate: ScreenStateDelegate<LoginEvent>,
    private val screenStateUiModelConverter: ScreenStateUiModelConverter
) : ViewModel() {

    private val _formState = MutableStateFlow(LoginFormUiModel())

    val uiModel: StateFlow<LoginScreenUiModel> = combine(
        _formState,
        screenDelegate.bind(viewModelScope)
    ) { form, screenState ->
        LoginScreenUiModel(
            loginFormUiModel = form,
            screenStateUiModel = screenStateUiModelConverter.toUiModel(state = screenState, hasData = true)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LoginScreenUiModel()
    )

    fun onHostChange(value: String) {
        var clean = value.replace("http://", "").replace("https://", "")
        clean = clean.split("/").first()
        _formState.update { it.copy(host = clean, hostError = false, formError = null) }
    }

    fun onPortChange(value: String) {
        if (value.all { it.isDigit() }) {
            _formState.update { it.copy(port = value, portError = false, formError = null) }
        }
    }

    fun onUserChange(value: String) {
        _formState.update { it.copy(username = value, formError = null) }
    }

    fun onPasswordChange(value: String) {
        _formState.update { it.copy(password = value, formError = null) }
    }

    fun onProtocolToggle() {
        _formState.update { current ->
            val nextIsHttps = !current.isHttps
            val nextPort = if (nextIsHttps && current.port == JellyfinConstants.DEFAULT_PORT_HTTP_STR)
                JellyfinConstants.DEFAULT_PORT_HTTPS_STR
            else if (!nextIsHttps && current.port == JellyfinConstants.DEFAULT_PORT_HTTPS_STR)
                JellyfinConstants.DEFAULT_PORT_HTTP_STR
            else current.port

            current.copy(isHttps = nextIsHttps, port = nextPort)
        }
    }

    fun onTogglePasswordVisibility() {
        _formState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onLoginClick() {
        val form = _formState.value

        val isHostInvalid = form.host.isBlank()
        val isPortInvalid = form.port.isBlank()

        if (isHostInvalid || isPortInvalid || form.username.isBlank()) {
            _formState.update {
                it.copy(
                    hostError = isHostInvalid,
                    portError = isPortInvalid,
                    formError = "Please check all fields"
                )
            }
            return
        }

        viewModelScope.launch {
            screenDelegate.load(isManualRefresh = false) {
                val protocol = if (form.isHttps) "https" else "http"
                val fullUrl = "$protocol://${form.host}:${form.port}"
                val params = AuthParams(fullUrl, form.username, form.password)

                val result = loginUseCase.login(params)

                when (result) {
                    is DataResult.Success -> {
                        screenDelegate.sendEvent(LoginEvent.NavigateToDashboard)
                    }

                    is DataResult.Error -> {
                        _formState.update {
                            it.copy(formError = result.error.message ?: "Login failed")
                        }
                    }
                }
            }
        }
    }

    fun onEventConsumed(id: String) = screenDelegate.consumeEvent(id)
}