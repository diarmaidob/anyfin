package io.github.diarmaidob.anyfin.feature.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExoPlayerController @Inject constructor(
    @ApplicationContext private val context: Context
) : VideoPlayerController {

    private var exoPlayer: ExoPlayer? = null
    private var listener: VideoPlayerController.Listener? = null

    private val handler = Handler(Looper.getMainLooper())
    private val progressRunnable = object : Runnable {
        override fun run() {
            exoPlayer?.let { player ->
                if (player.isPlaying) {
                    listener?.onPositionChanged(player.currentPosition)
                }
            }
            handler.postDelayed(this, 500)
        }
    }

    override val playerInstance: Any? get() = exoPlayer

    override fun setListener(listener: VideoPlayerController.Listener) {
        this.listener = listener
    }

    override fun initialize(url: String, startPositionMs: Long) {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context).build().apply {
                addListener(exoListener)
                playWhenReady = true
            }
        }

        val item = MediaItem.fromUri(url)
        exoPlayer?.apply {
            setMediaItem(item)
            seekTo(startPositionMs)
            prepare()
        }
    }

    override fun play() {
        exoPlayer?.play()
    }

    override fun pause() {
        exoPlayer?.pause()
    }

    override fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }

    override fun selectTrack(type: TrackType, trackId: String) {
        val player = exoPlayer ?: return

        val cType = if (type == TrackType.AUDIO) C.TRACK_TYPE_AUDIO else C.TRACK_TYPE_TEXT

        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(cType, false)
            .build()

        val split = trackId.split(":")
        val groupIndex = split[0].toIntOrNull() ?: return
        val trackIndex = split[1].toIntOrNull() ?: return

        val groups = player.currentTracks.groups
        if (groupIndex < groups.size) {
            val trackGroup = groups[groupIndex].mediaTrackGroup

            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .clearOverridesOfType(cType)
                .setOverrideForType(TrackSelectionOverride(trackGroup, trackIndex))
                .build()
        }
    }

    override fun clearTrack(type: TrackType) {
        val player = exoPlayer ?: return
        val cType = if (type == TrackType.AUDIO) C.TRACK_TYPE_AUDIO else C.TRACK_TYPE_TEXT

        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(cType, true)
            .build()
    }

    override fun release() {
        handler.removeCallbacks(progressRunnable)
        exoPlayer?.removeListener(exoListener)
        exoPlayer?.release()
        exoPlayer = null
    }

    private val exoListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val player = exoPlayer ?: return
            val isBuffering = playbackState == Player.STATE_BUFFERING
            val duration = if (player.duration != C.TIME_UNSET) player.duration else 0L

            listener?.onStateChanged(
                isPlaying = player.isPlaying,
                isBuffering = isBuffering,
                durationMs = duration
            )

            if (playbackState == Player.STATE_ENDED) {
                listener?.onEnded()
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val player = exoPlayer ?: return
            listener?.onStateChanged(
                isPlaying = isPlaying,
                isBuffering = player.playbackState == Player.STATE_BUFFERING,
                durationMs = player.duration
            )
            if (isPlaying) handler.post(progressRunnable) else handler.removeCallbacks(progressRunnable)
        }

        override fun onPlayerError(error: PlaybackException) {
            listener?.onError(error.localizedMessage ?: "Unknown Error")
        }

        override fun onTracksChanged(tracks: Tracks) {
            val audioList = ArrayList<PlayerTrack>()
            val textList = ArrayList<PlayerTrack>()

            for ((groupIndex, group) in tracks.groups.withIndex()) {
                if (group.type != C.TRACK_TYPE_AUDIO && group.type != C.TRACK_TYPE_TEXT) continue

                for (trackIndex in 0 until group.length) {
                    if (!group.isTrackSupported(trackIndex)) continue

                    val format = group.getTrackFormat(trackIndex)
                    val isSelected = group.isTrackSelected(trackIndex)

                    var name = format.label
                    if (name.isNullOrEmpty()) name = format.language
                    if (name.isNullOrEmpty()) name = "Track $trackIndex"

                    val track = PlayerTrack(
                        id = "$groupIndex:$trackIndex",
                        name = name!!,
                        language = format.language,
                        isSelected = isSelected
                    )

                    if (group.type == C.TRACK_TYPE_AUDIO) audioList.add(track)
                    else textList.add(track)
                }
            }

            listener?.onTracksChanged(audioList, textList)
        }
    }
}