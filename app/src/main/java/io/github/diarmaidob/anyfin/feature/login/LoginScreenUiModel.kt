package io.github.diarmaidob.anyfin.feature.login

import io.github.diarmaidob.anyfin.core.common.JellyfinConstants
import io.github.diarmaidob.anyfin.core.ui.uimodel.ScreenStateUiModel

sealed interface LoginEvent {
    data object NavigateToDashboard : LoginEvent
}

data class LoginFormUiModel(
    val isHttps: Boolean = false,
    val host: String = "",
    val port: String = JellyfinConstants.DEFAULT_PORT_HTTP_STR,
    val username: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val hostError: Boolean = false,
    val portError: Boolean = false,
    val formError: String? = null
) {
    val protocolString: String
        get() = if (isHttps) "https" else "http"
}

data class LoginScreenUiModel(
    val loginFormUiModel: LoginFormUiModel = LoginFormUiModel(),
    val screenStateUiModel: ScreenStateUiModel<LoginEvent> = ScreenStateUiModel()
) {
    val canInteract: Boolean
        get() = !screenStateUiModel.isSyncing && !screenStateUiModel.isInitialLoading
}