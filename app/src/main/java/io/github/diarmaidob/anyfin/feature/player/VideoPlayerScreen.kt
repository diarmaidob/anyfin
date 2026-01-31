package io.github.diarmaidob.anyfin.feature.player

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView


@Composable
fun PlayerScreen(
    onNavigateUp: () -> Unit,
    viewModel: VideoPlayerViewModel = hiltViewModel()
) {
    val uiModel by viewModel.uiModel.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        viewModel.onPause()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onResume()
    }

    DisposableEffect(Unit) {
        val activity = context.findActivity() ?: return@DisposableEffect onDispose {}
        val window = activity.window
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)

        val originalSystemBarsBehavior = insetsController.systemBarsBehavior
        val originalCutoutMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode
        } else 0

        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        onDispose {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior = originalSystemBarsBehavior
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                @SuppressLint("WrongConstant")
                window.attributes.layoutInDisplayCutoutMode = originalCutoutMode
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { viewModel.toggleControls() },
                    onDoubleTap = { offset ->
                        if (offset.x < size.width / 2) viewModel.onSeekDelta(-10_000)
                        else viewModel.onSeekDelta(10_000)
                    }
                )
            }
    ) {
        VideoSurface(player = viewModel.playerInstance)

        PlayerOverlay(
            uiModel = uiModel,
            onBack = onNavigateUp,
            onPlayPause = viewModel::onPlayPause,
            onSeekStart = viewModel::onSeekStart,
            onSeekEnd = viewModel::onSeekEnd,
            onRewind = { viewModel.onSeekDelta(-10_000) },
            onForward = { viewModel.onSeekDelta(10_000) },
            onTrackClick = viewModel::showTrackSelector
        )

        if (uiModel.activeDialog == TrackType.AUDIO) {
            TrackSelectionDialog(
                title = "Audio",
                tracks = uiModel.audioTracks,
                onDismiss = viewModel::dismissDialog,
                onSelect = { id -> viewModel.onTrackSelected(TrackType.AUDIO, id) }
            )
        }
        if (uiModel.activeDialog == TrackType.TEXT) {
            TrackSelectionDialog(
                title = "Subtitles",
                tracks = uiModel.subtitleTracks,
                onDismiss = viewModel::dismissDialog,
                onSelect = { id -> viewModel.onTrackSelected(TrackType.TEXT, id) },
                onDisable = viewModel::onSubtitleOff
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoSurface(player: Any?) {
    val exoPlayer = player as? Player ?: return

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                this.player = exoPlayer
            }
        },
        update = { it.player = exoPlayer },
        onRelease = { it.player = null },
        modifier = Modifier.fillMaxSize()
    )
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerOverlay(
    uiModel: VideoPlayerScreenUiModel,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekStart: () -> Unit,
    onSeekEnd: (Float) -> Unit,
    onRewind: () -> Unit,
    onForward: () -> Unit,
    onTrackClick: (TrackType) -> Unit
) {
    if (uiModel.isBuffering) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier = Modifier.size(100.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    uiModel.errorMsg?.let {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleLarge)
        }
    }

    val topGradient = remember {
        Brush.verticalGradient(listOf(Color.Black.copy(0.8f), Color.Transparent))
    }
    val bottomGradient = remember {
        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.8f)))
    }

    AnimatedVisibility(
        visible = uiModel.showControls,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(topGradient)
                    .windowInsetsPadding(WindowInsets.displayCutout)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Column(Modifier.padding(start = 16.dp)) {
                    Text(uiModel.title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    uiModel.subtitle?.let {
                        Text(it, color = Color.White.copy(0.7f), style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.weight(1f))
                if (uiModel.audioTracks.size > 1) {
                    IconButton(onClick = { onTrackClick(TrackType.AUDIO) }) {
                        Icon(Icons.Default.Audiotrack, contentDescription = "Audio Tracks", tint = Color.White)
                    }
                }
                if (uiModel.subtitleTracks.isNotEmpty()) {
                    IconButton(onClick = { onTrackClick(TrackType.TEXT) }) {
                        Icon(Icons.Default.Subtitles, contentDescription = "Subtitles", tint = Color.White)
                    }
                }
            }

            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onRewind, modifier = Modifier.size(56.dp)) {
                    Icon(
                        Icons.Default.Replay10,
                        contentDescription = "Rewind 10 seconds",
                        tint = Color.White,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color.White.copy(0.2f), CircleShape)
                ) {
                    Icon(
                        if (uiModel.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (uiModel.isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(56.dp)
                    )
                }
                IconButton(onClick = onForward, modifier = Modifier.size(56.dp)) {
                    Icon(
                        Icons.Default.Forward10,
                        contentDescription = "Forward 10 seconds",
                        tint = Color.White,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(bottomGradient)
                    .windowInsetsPadding(WindowInsets.displayCutout)
                    .padding(16.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(uiModel.positionText, color = Color.White)
                    Text(uiModel.durationText, color = Color.White)
                }

                var isDragging by remember { mutableStateOf(false) }
                var sliderValue by remember { mutableFloatStateOf(0f) }

                LaunchedEffect(uiModel.progress) {
                    if (!isDragging) {
                        sliderValue = uiModel.progress
                    }
                }

                val sliderColors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Color.White.copy(0.5f)
                )

                Slider(
                    modifier = Modifier.fillMaxWidth(),
                    value = sliderValue,
                    onValueChange = {
                        isDragging = true
                        sliderValue = it
                        onSeekStart()
                    },
                    onValueChangeFinished = {
                        onSeekEnd(sliderValue)
                        isDragging = false
                    },
                    colors = sliderColors,
                    thumb = {},
                    track = { sliderState ->
                        SliderDefaults.Track(
                            sliderState = sliderState,
                            colors = sliderColors,
                            drawStopIndicator = null,
                            thumbTrackGapSize = 0.dp,
                            modifier = Modifier
                                .height(8.dp)
                                .clip(CircleShape)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun TrackSelectionDialog(
    title: String,
    tracks: List<PlayerTrack>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onDisable: (() -> Unit)? = null
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                LazyColumn(Modifier.heightIn(max = 300.dp)) {
                    if (onDisable != null) {
                        item {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onDisable() }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Error, contentDescription = null)
                                Spacer(Modifier.width(16.dp))
                                Text("Off")
                            }
                        }
                    }
                    items(tracks) { track ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(track.id) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = track.isSelected, onClick = null)
                            Spacer(Modifier.width(16.dp))
                            Text(track.name)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Close")
                }
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}