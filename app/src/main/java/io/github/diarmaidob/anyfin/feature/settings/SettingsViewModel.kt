package io.github.diarmaidob.anyfin.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.diarmaidob.anyfin.BuildConfig
import io.github.diarmaidob.anyfin.core.common.ScreenStateDelegate
import io.github.diarmaidob.anyfin.core.entity.ScreenState
import io.github.diarmaidob.anyfin.core.entity.SessionState
import io.github.diarmaidob.anyfin.core.session.SessionRepo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SettingsEvent {
    data class CopyToClipboard(val label: String, val value: String) : SettingsEvent
    data object LoggedOut : SettingsEvent
}

data class SettingsScreenData(
    val sessionState: SessionState,
    val screenState: ScreenState<SettingsEvent>,
    val versionName: String
) {
    val hasData = true
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sessionRepo: SessionRepo,
    private val logoutUseCase: LogoutUseCase,
    private val screenStateDelegate: ScreenStateDelegate<SettingsEvent>,
    private val settingScreenUiModelConverter: SettingsScreenUiModelConverter
) : ViewModel() {

    val uiModel: StateFlow<SettingsScreenUiModel> = combine(
        sessionRepo.sessionState,
        screenStateDelegate.bind(viewModelScope)
    ) { sessionState, screenState ->
        SettingsScreenData(
            sessionState = sessionState,
            screenState = screenState,
            versionName = BuildConfig.VERSION_NAME // TODO: move to repo
        )
    }.map {
        settingScreenUiModelConverter.toUiModel(it)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsScreenUiModel(version = BuildConfig.VERSION_NAME)
    )


    fun onEventConsumed(eventId: String) {
        screenStateDelegate.consumeEvent(eventId)
    }

    fun onCopyClick(label: String, value: String) {
        if (value.isBlank()) return
        screenStateDelegate.performThrottledAction {
            screenStateDelegate.sendEvent(SettingsEvent.CopyToClipboard(label = label, value = value))
        }
    }

    fun onLogoutClick() {
        screenStateDelegate.performThrottledAction {
            viewModelScope.launch {
                logoutUseCase()
                screenStateDelegate.sendEvent(SettingsEvent.LoggedOut)
            }
        }
    }

}