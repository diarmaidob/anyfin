package io.github.diarmaidob.anyfin.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.diarmaidob.anyfin.core.entity.SessionState
import io.github.diarmaidob.anyfin.core.session.SessionRepo
import io.github.diarmaidob.anyfin.navigation.DashboardDestination
import io.github.diarmaidob.anyfin.navigation.LoginDestination
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface MainActivityUiModel {
    data object Loading : MainActivityUiModel
    data class Success(val startDestination: Any) : MainActivityUiModel
}

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    sessionRepo: SessionRepo
) : ViewModel() {

    val uiState: StateFlow<MainActivityUiModel> = sessionRepo.sessionState
        .map { sessionState ->
            val destination = if (sessionState is SessionState.LoggedIn) {
                DashboardDestination
            } else {
                LoginDestination
            }
            MainActivityUiModel.Success(destination)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MainActivityUiModel.Loading
        )
}