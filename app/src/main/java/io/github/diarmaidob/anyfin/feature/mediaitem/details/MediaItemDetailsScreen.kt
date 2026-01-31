package io.github.diarmaidob.anyfin.feature.mediaitem.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.github.diarmaidob.anyfin.core.ui.components.AnyFinScaffold
import io.github.diarmaidob.anyfin.core.ui.components.AsyncContentLayout
import io.github.diarmaidob.anyfin.core.ui.uimodel.MediaItemCommonUiModel
import io.github.diarmaidob.anyfin.core.ui.uimodel.MediaItemDetailsUiModel
import io.github.diarmaidob.anyfin.core.ui.uimodel.MediaItemListItemUiModel
import io.github.diarmaidob.anyfin.navigation.MediaItemDetailsDestination
import io.github.diarmaidob.anyfin.navigation.PlayerDestination
import kotlinx.coroutines.launch

@Composable
fun MediaItemDetailsRoute(
    onNavigateToPlayer: (PlayerDestination) -> Unit,
    onNavigateToChild: (MediaItemDetailsDestination) -> Unit,
    onNavigateUp: () -> Unit,
    viewModel: MediaItemDetailsViewModel = hiltViewModel()
) {
    val uiModel by viewModel.uiModel.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onScreenVisible() }

    AnyFinScaffold(title = uiModel.title, onBack = onNavigateUp) { paddingValues ->
        AsyncContentLayout(
            modifier = Modifier.padding(paddingValues),
            uiModel = uiModel.screenStateUiModel,
            onRefresh = viewModel::onPullToRefresh,
            onEventConsumed = viewModel::onEventConsumed,
            onEvent = { event ->
                when (event) {
                    is MediaItemDetailsEvent.NavigateToPlayer -> onNavigateToPlayer(event.dest)
                    is MediaItemDetailsEvent.NavigateToChild -> onNavigateToChild(event.dest)
                }
            }
        ) {
            MediaItemDetailsScreen(
                uiModel = uiModel,
                onPlayClick = viewModel::onPlayClick,
                onChildClick = viewModel::onChildClick,
                onAudioSelect = viewModel::onAudioSelected,
                onSubSelect = viewModel::onSubtitleSelected
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaItemDetailsScreen(
    modifier: Modifier = Modifier,
    uiModel: MediaItemDetailsScreenUiModel,
    onPlayClick: () -> Unit,
    onChildClick: (MediaItemListItemUiModel) -> Unit,
    onAudioSelect: (Int) -> Unit,
    onSubSelect: (Int?) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            uiModel.details?.let { details ->
                HeaderSection(
                    details = details,
                    trackState = TrackState(
                        hasMedia = uiModel.hasMediaSources,
                        audioName = uiModel.selectedAudioName,
                        subName = uiModel.selectedSubtitleName,
                        audioTracks = uiModel.audioTracks,
                        subTracks = uiModel.subtitleTracks
                    ),
                    onPlayClick = onPlayClick,
                    onAudioSelect = onAudioSelect,
                    onSubSelect = onSubSelect
                )
            }
        }

        if (uiModel.children.isNotEmpty()) {
            item {
                Text(
                    text = uiModel.childrenTitle,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )
            }
            items(
                items = uiModel.children,
                key = { it.id }
            ) { child ->
                val onClick = { onChildClick(child) }
                if (uiModel.childLayoutType == ChildLayoutType.Seasons) {
                    SeasonRow(child, onClick)
                } else {
                    EpisodeRow(child, onClick)
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

data class TrackState(
    val hasMedia: Boolean,
    val audioName: String,
    val subName: String,
    val audioTracks: List<TrackUiModel>,
    val subTracks: List<TrackUiModel>
)

@Composable
private fun HeaderSection(
    details: MediaItemDetailsUiModel,
    trackState: TrackState,
    onPlayClick: () -> Unit,
    onAudioSelect: (Int) -> Unit,
    onSubSelect: (Int?) -> Unit
) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        ) {
            AsyncImage(
                model = details.common.images.backdrop,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background.copy(alpha = 0.4f),
                                MaterialTheme.colorScheme.background
                            ),
                            startY = 100f
                        )
                    )
            )

            if (details.common.type != MediaItemCommonUiModel.MediaItemUiType.EPISODE && details.common.images.logo != null) {
                AsyncImage(
                    model = details.common.images.logo,
                    contentDescription = "Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(0.8f)
                        .height(100.dp)
                        .padding(bottom = 16.dp)
                )
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {

            if (details.common.images.logo == null) {
                Text(
                    text = details.common.name,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (details.overview.isNotEmpty()) {
                Text(
                    text = details.overview,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (trackState.hasMedia) {
                PlaybackTrackSelector(
                    audioName = trackState.audioName,
                    subName = trackState.subName,
                    audioTracks = trackState.audioTracks,
                    subTracks = trackState.subTracks,
                    onAudioSelect = onAudioSelect,
                    onSubSelect = onSubSelect
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                enabled = details.playButton.isEnabled,
                onClick = onPlayClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text(details.playButton.label)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaybackTrackSelector(
    audioName: String,
    subName: String,
    audioTracks: List<TrackUiModel>,
    subTracks: List<TrackUiModel>,
    onAudioSelect: (Int) -> Unit,
    onSubSelect: (Int?) -> Unit
) {
    var activeSheetType by remember { mutableStateOf<SheetType?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    val closeSheet: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) activeSheetType = null
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (audioTracks.size > 1) {
            TrackButton(
                text = audioName,
                icon = Icons.Default.Audiotrack,
                modifier = Modifier.weight(1f),
                onClick = { activeSheetType = SheetType.Audio }
            )
        }

        if (subTracks.isNotEmpty()) {
            TrackButton(
                text = subName,
                icon = Icons.Default.ClosedCaption,
                modifier = Modifier.weight(1f),
                onClick = { activeSheetType = SheetType.Subtitle }
            )
        }
    }

    if (activeSheetType != null) {
        ModalBottomSheet(
            onDismissRequest = { activeSheetType = null },
            sheetState = sheetState,
        ) {
            val isAudio = activeSheetType == SheetType.Audio
            val title = if (isAudio) "Select Audio" else "Select Subtitles"
            val items = if (isAudio) audioTracks else subTracks

            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )
                HorizontalDivider()
                LazyColumn {
                    if (!isAudio) {
                        item {
                            TrackSelectionRow(
                                name = "Off",
                                isSelected = items.none { it.isSelected },
                                onClick = { onSubSelect(null); closeSheet() }
                            )
                        }
                    }
                    items(items) { track ->
                        TrackSelectionRow(
                            name = track.name,
                            isSelected = track.isSelected,
                            onClick = {
                                if (isAudio) onAudioSelect(track.index) else onSubSelect(track.index)
                                closeSheet()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Icon(icon, null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(Modifier.weight(1f))
        Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun TrackSelectionRow(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(name) },
        trailingContent = {
            RadioButton(selected = isSelected, onClick = null)
        },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        )
    )
}

private enum class SheetType { Audio, Subtitle }

@Composable
private fun SeasonRow(item: MediaItemListItemUiModel, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            modifier = Modifier
                .width(60.dp)
                .aspectRatio(2f / 3f)
        ) {
            AsyncImage(
                model = item.common.images.primary,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.width(16.dp))
        Text(item.common.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EpisodeRow(item: MediaItemListItemUiModel, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Card(
            modifier = Modifier
                .width(120.dp)
                .aspectRatio(16f / 9f)
        ) {
            AsyncImage(
                model = item.common.images.primary,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                item.common.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}