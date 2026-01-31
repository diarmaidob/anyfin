package io.github.diarmaidob.anyfin.feature.settings

import io.github.diarmaidob.anyfin.core.ui.uimodel.ScreenStateUiModel

data class SettingsScreenUiModel(
    val screenStateUiModel: ScreenStateUiModel<SettingsEvent> = ScreenStateUiModel(),
    val serverUrl: String = "",
    val userId: String = "",
    val version: String = ""
)
