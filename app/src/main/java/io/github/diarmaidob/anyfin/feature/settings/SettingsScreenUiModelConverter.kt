package io.github.diarmaidob.anyfin.feature.settings

import io.github.diarmaidob.anyfin.core.entity.SessionState
import io.github.diarmaidob.anyfin.core.ui.uimodel.converter.ScreenStateUiModelConverter
import javax.inject.Inject

class SettingsScreenUiModelConverter @Inject constructor(
    private val screenStateUiModelConverter: ScreenStateUiModelConverter
) {

    fun toUiModel(data: SettingsScreenData): SettingsScreenUiModel {
        val screenStateUiModel = screenStateUiModelConverter.toUiModel(data.screenState, data.hasData)
        val loggedIn = data.sessionState as? SessionState.LoggedIn
        return SettingsScreenUiModel(
            screenStateUiModel = screenStateUiModel,
            serverUrl = loggedIn?.serverUrl.orEmpty(),
            userId = loggedIn?.userId.orEmpty(),
            version = data.versionName
        )
    }

}