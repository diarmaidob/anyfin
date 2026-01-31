package io.github.diarmaidob.anyfin.feature.mediaitem.list

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.diarmaidob.anyfin.core.ui.components.AnyFinScaffold
import io.github.diarmaidob.anyfin.core.ui.components.AsyncContentLayout
import io.github.diarmaidob.anyfin.core.ui.components.MediaItemList
import io.github.diarmaidob.anyfin.core.ui.uimodel.MediaItemListItemUiModel
import io.github.diarmaidob.anyfin.navigation.MediaItemDetailsDestination

@Composable
fun MediaItemListRoute(
    viewModel: MediaItemListViewModel = hiltViewModel(),
    onNavigateToMediaItemDetails: (MediaItemDetailsDestination) -> Unit,
    onNavigateUp: () -> Unit
) {
    val uiModel by viewModel.uiModel.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onScreenVisible()
    }

    AnyFinScaffold(
        title = uiModel.title,
        onBack = onNavigateUp
    ) { paddingValues ->
        AsyncContentLayout(
            modifier = Modifier.padding(paddingValues),
            uiModel = uiModel.screenState,
            onRefresh = viewModel::onPullToRefresh,
            onEventConsumed = viewModel::onEventConsumed,
            onEvent = {
                when (it) {
                    is MediaItemListEvent.NavigateToMediaItemDetails -> onNavigateToMediaItemDetails(it.dest)
                }
            }
        ) {
            MediaItemListScreen(
                uiModel = uiModel,
                onItemClick = viewModel::onItemClick
            )
        }
    }


}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaItemListScreen(
    modifier: Modifier = Modifier,
    uiModel: MediaItemListScreenUiModel,
    onItemClick: (MediaItemListItemUiModel) -> Unit
) {
    MediaItemList(
        modifier = modifier,
        items = uiModel.items,
        onItemClick = onItemClick
    )
}