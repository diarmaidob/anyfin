package io.github.diarmaidob.anyfin.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.diarmaidob.anyfin.core.ui.uimodel.MediaItemListItemUiModel
import io.github.diarmaidob.anyfin.feature.home.PosterCardUi

@Composable
fun MediaItemList(
    modifier: Modifier = Modifier,
    items: List<MediaItemListItemUiModel>,
    onItemClick: (MediaItemListItemUiModel) -> Unit,
    itemContent: @Composable (MediaItemListItemUiModel) -> Unit = {
        PosterCardUi(item = it, onClick = { onItemClick(it) })
    }
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 140.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(
            items = items,
            key = { it.id }
        ) { item ->
            itemContent(item)
        }
    }
}