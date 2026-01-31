package io.github.diarmaidob.anyfin.feature.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.diarmaidob.anyfin.core.common.onError
import io.github.diarmaidob.anyfin.core.common.onSuccess
import io.github.diarmaidob.anyfin.navigation.PlayerDestination
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getPlaybackInfo: GetPlaybackInfoUseCase,
    private val playerController: VideoPlayerController
) : ViewModel(),
    VideoPlayerController.Listener {

    private val destination = savedStateHandle.toRoute<PlayerDestination>()

    private val _uiModel = MutableStateFlow(VideoPlayerScreenUiModel())
    val uiModel = _uiModel.asStateFlow()

    val playerInstance: Any? get() = playerController.playerInstance

    private var controlsJob: Job? = null

    init {
        playerController.setListener(this)
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            getPlaybackInfo(destination.itemId)
                .onSuccess { info ->
                    _uiModel.update { it.copy(title = info.title, subtitle = info.subtitle) }
                    playerController.initialize(info.streamUrl, info.startPositionMs)
                    resetControlsHideTimer()
                }
                .onError { error ->
                    _uiModel.update { it.copy(errorMsg = error.message) }
                }
        }
    }


    fun onPause() {
        if (_uiModel.value.isPlaying) {
            playerController.pause()
        }
    }

    fun onResume() {
    }


    fun onPlayPause() {
        if (_uiModel.value.isPlaying) playerController.pause() else playerController.play()
        resetControlsHideTimer()
    }

    fun onSeekStart() {
        cancelControlsTimer()
        _uiModel.update { it.copy(isSeeking = true) }
    }

    fun onSeekEnd(percent: Float) {
        val duration = _uiModel.value.durationMs
        val targetMs = (percent * duration).toLong()
        playerController.seekTo(targetMs)
        _uiModel.update { it.copy(isSeeking = false, currentTimeMs = targetMs) }
        resetControlsHideTimer()
    }

    fun onSeekDelta(deltaMs: Long) {
        val current = _uiModel.value.currentTimeMs
        val duration = _uiModel.value.durationMs
        val newPos = (current + deltaMs).coerceIn(0, duration)
        playerController.seekTo(newPos)
        resetControlsHideTimer()
    }

    fun toggleControls() {
        if (_uiModel.value.showControls) {
            _uiModel.update { it.copy(showControls = false) }
            cancelControlsTimer()
        } else {
            _uiModel.update { it.copy(showControls = true) }
            resetControlsHideTimer()
        }
    }


    fun showTrackSelector(type: TrackType) {
        cancelControlsTimer()
        _uiModel.update { it.copy(activeDialog = type, showControls = false) }
    }

    fun dismissDialog() {
        _uiModel.update { it.copy(activeDialog = null, showControls = true) }
        resetControlsHideTimer()
    }

    fun onTrackSelected(type: TrackType, trackId: String) {
        playerController.selectTrack(type, trackId)
        dismissDialog()
    }

    fun onSubtitleOff() {
        playerController.clearTrack(TrackType.TEXT)
        dismissDialog()
    }


    override fun onStateChanged(isPlaying: Boolean, isBuffering: Boolean, durationMs: Long) {
        _uiModel.update {
            it.copy(isPlaying = isPlaying, isBuffering = isBuffering, durationMs = durationMs)
        }
        if (isPlaying && !isBuffering) resetControlsHideTimer()
    }

    override fun onPositionChanged(currentMs: Long) {
        if (!_uiModel.value.isSeeking) {
            _uiModel.update { it.copy(currentTimeMs = currentMs) }
        }
    }

    override fun onError(message: String) {
        _uiModel.update { it.copy(errorMsg = message) }
    }

    override fun onEnded() {
        _uiModel.update { it.copy(isPlaying = false, showControls = true) }
        cancelControlsTimer()
    }

    override fun onTracksChanged(audio: List<PlayerTrack>, text: List<PlayerTrack>) {
        _uiModel.update { it.copy(audioTracks = audio, subtitleTracks = text) }
    }

    override fun onCleared() {
        playerController.release()
        super.onCleared()
    }

    private fun resetControlsHideTimer() {
        cancelControlsTimer()
        if (!_uiModel.value.isPlaying) return
        controlsJob = viewModelScope.launch {
            delay(4000)
            _uiModel.update { it.copy(showControls = false) }
        }
    }

    private fun cancelControlsTimer() {
        controlsJob?.cancel()
        controlsJob = null
    }
}