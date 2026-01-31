package io.github.diarmaidob.anyfin.feature.player

enum class TrackType { AUDIO, VIDEO, TEXT }

data class PlayerTrack(
    val id: String,
    val name: String,
    val language: String?,
    val isSelected: Boolean
)

interface VideoPlayerController {
    val playerInstance: Any?

    fun initialize(url: String, startPositionMs: Long)
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun release()

    fun selectTrack(type: TrackType, trackId: String)
    fun clearTrack(type: TrackType)

    fun setListener(listener: Listener)

    interface Listener {
        fun onStateChanged(isPlaying: Boolean, isBuffering: Boolean, durationMs: Long)
        fun onPositionChanged(currentMs: Long)
        fun onError(message: String)
        fun onEnded()
        fun onTracksChanged(audio: List<PlayerTrack>, text: List<PlayerTrack>)
    }
}