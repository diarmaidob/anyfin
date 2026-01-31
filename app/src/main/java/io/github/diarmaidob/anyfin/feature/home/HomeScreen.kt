package io.github.diarmaidob.anyfin.feature.home

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import io.github.diarmaidob.anyfin.core.ui.uimodel.MediaItemListItemUiModel
import io.github.diarmaidob.anyfin.navigation.MediaItemDetailsDestination
import io.github.diarmaidob.anyfin.navigation.MediaItemListDestination
import io.github.diarmaidob.anyfin.navigation.PlayerDestination

@Composable
fun HomeRoute(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToDetails: (MediaItemDetailsDestination) -> Unit,
    onNavigateToPlayer: (PlayerDestination) -> Unit,
    onNavigateToSection: (MediaItemListDestination) -> Unit
) {
    val uiModel by viewModel.uiModel.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onScreenVisible()
    }

    AnyFinScaffold(
        title = uiModel.title,
    ) { paddingValues ->
        AsyncContentLayout(
            modifier = Modifier.padding(paddingValues),
            uiModel = uiModel.screenStateUiModel,
            onRefresh = viewModel::onPullToRefresh,
            onEventConsumed = viewModel::onEventConsumed,
            onEvent = { event ->
                when (event) {
                    is HomeEvent.NavigateToMediaItemDetails -> onNavigateToDetails(event.dest)
                    is HomeEvent.NavigateToPlayer -> onNavigateToPlayer(event.dest)
                    is HomeEvent.NavigateToMediaItemList -> onNavigateToSection(event.dest)
                }
            }
        ) {
            HomeScreen(
                state = uiModel,
                onItemClick = viewModel::onItemClick,
                onResumeClick = viewModel::onResumeClick,
                onSectionHeaderClick = viewModel::onSectionHeaderClick
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    state: HomeScreenUiModel,
    onItemClick: (MediaItemListItemUiModel) -> Unit,
    onResumeClick: (MediaItemListItemUiModel) -> Unit,
    onSectionHeaderClick: (HomeSectionUiModel) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        items(state.sectionUiModels, key = { it.id }) { section ->
            HomeSection(
                section = section,
                onItemClick = onItemClick,
                onResumeClick = onResumeClick,
                onHeaderClick = onSectionHeaderClick
            )
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }

}

@Composable
fun HomeSection(
    section: HomeSectionUiModel,
    onItemClick: (MediaItemListItemUiModel) -> Unit,
    onResumeClick: (MediaItemListItemUiModel) -> Unit,
    onHeaderClick: (HomeSectionUiModel) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.clickable { onHeaderClick(section) },
                text = section.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (section.items.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(section.items, key = { it.id }) { item ->
                    when (section.posterType) {
                        HomeSectionUiModel.SectionPosterType.Resume -> {
                            ResumeCardUi(
                                item = item,
                                onClick = { onResumeClick(item) }
                            )
                        }

                        HomeSectionUiModel.SectionPosterType.Poster -> {
                            PosterCardUi(
                                item = item,
                                onClick = { onItemClick(item) }
                            )
                        }

                        HomeSectionUiModel.SectionPosterType.NextUp -> {
                            NextUpCard(item, onClick = { onItemClick(item) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResumeCardUi(
    item: MediaItemListItemUiModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .aspectRatio(16f / 9f)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = item.common.images.backdrop,
                contentDescription = item.common.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
            )

            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp)
                    .background(Color.Black.copy(0.5f), CircleShape)
                    .padding(8.dp)
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .fillMaxWidth()
            ) {
                if (!item.subtitle.isNullOrEmpty()) {
                    Text(
                        text = item.subtitle,
                        color = Color.LightGray,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Text(
                    text = item.common.name,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.playbackProgress > 0) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { item.playbackProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(0.3f),
                        strokeCap = StrokeCap.Butt,
                        gapSize = 0.dp,
                        drawStopIndicator = {}
                    )
                }
            }
        }
    }
}

@Composable
fun PosterCardUi(
    item: MediaItemListItemUiModel,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick)
    ) {
        Card(
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(MaterialTheme.shapes.medium),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            AsyncImage(

                model = item.common.images.primary,
                contentDescription = item.common.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = item.common.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun NextUpCard(
    item: MediaItemListItemUiModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .aspectRatio(16f / 9f)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = item.common.images.thumb,
                contentDescription = item.common.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .fillMaxWidth()
            ) {
                if (!item.subtitle.isNullOrEmpty()) {
                    Text(
                        text = item.subtitle,
                        color = Color.LightGray,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Text(
                    text = item.common.name,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.playbackProgress > 0) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { item.playbackProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(0.3f),
                        strokeCap = StrokeCap.Butt,
                        gapSize = 0.dp,
                        drawStopIndicator = {}
                    )
                }
            }
        }
    }
}