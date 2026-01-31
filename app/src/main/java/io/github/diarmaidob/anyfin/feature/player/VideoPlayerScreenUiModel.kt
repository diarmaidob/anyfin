package io.github.diarmaidob.anyfin.feature.player

import java.util.Locale

data class VideoPlayerScreenUiModel(
    val title: String = "",
    val subtitle: String? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = true,
    val currentTimeMs: Long = 0L,
    val durationMs: Long = 0L,
    val errorMsg: String? = null,
    val showControls: Boolean = true,
    val isSeeking: Boolean = false,
    val audioTracks: List<PlayerTrack> = emptyList(),
    val subtitleTracks: List<PlayerTrack> = emptyList(),
    val activeDialog: TrackType? = null
) {
    val progress: Float
        get() = if (durationMs > 0) currentTimeMs.toFloat() / durationMs else 0f

    val positionText: String get() = formatTime(currentTimeMs)
    val durationText: String get() = formatTime(durationMs)

    companion object Companion {
        fun formatTime(ms: Long): String {
            if (ms <= 0) return "00:00"
            val totalSecs = ms / 1000
            val h = totalSecs / 3600
            val m = (totalSecs % 3600) / 60
            val s = totalSecs % 60
            return if (h > 0) {
                String.format(Locale.US, "%d:%02d:%02d", h, m, s)
            } else {
                String.format(Locale.US, "%02d:%02d", m, s)
            }
        }
    }
}