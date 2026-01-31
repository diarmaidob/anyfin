package io.github.diarmaidob.anyfin.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.github.diarmaidob.anyfin.core.ui.components.AnyFinScaffold
import io.github.diarmaidob.anyfin.core.ui.components.AsyncContentLayout
import io.github.diarmaidob.anyfin.core.ui.components.MediaItemList
import io.github.diarmaidob.anyfin.core.ui.uimodel.MediaItemListItemUiModel
import io.github.diarmaidob.anyfin.navigation.MediaItemListDestination

@Composable
fun LibraryRoute(
    onNavigateToMediaItemList: (MediaItemListDestination) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiModel by viewModel.uiModel.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onScreenVisible()
    }

    AnyFinScaffold(
        title = uiModel.title
    ) { paddingValues ->
        AsyncContentLayout(
            uiModel = uiModel.screenStateUiModel,
            modifier = Modifier.padding(paddingValues),
            onEventConsumed = viewModel::onEventConsumed,
            onRefresh = viewModel::onPullToRefresh,
            onEvent = {
                when (it) {
                    is LibraryEvent.NavigateToLibraryContents -> onNavigateToMediaItemList(it.dest)
                }
            }
        ) {
            LibraryScreen(
                items = uiModel.items,
                onItemClick = { viewModel.onItemClick(it) }
            )
        }
    }
}

@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    items: List<MediaItemListItemUiModel>,
    onItemClick: (MediaItemListItemUiModel) -> Unit
) {
    MediaItemList(
        modifier = modifier,
        items = items,
        onItemClick = onItemClick
    ) { item ->
        LibraryCard(
            item = item,
            onClick = onItemClick
        )
    }
}

@Composable
fun LibraryCard(
    item: MediaItemListItemUiModel,
    onClick: (MediaItemListItemUiModel) -> Unit
) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .aspectRatio(16f / 9f)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = { onClick(item) }),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = item.common.images.primary,
                contentDescription = item.common.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}