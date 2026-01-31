package io.github.diarmaidob.anyfin.feature.player

import io.github.diarmaidob.anyfin.core.common.DataLoadError
import io.github.diarmaidob.anyfin.core.common.DataResult
import io.github.diarmaidob.anyfin.core.common.DataResultControlException
import io.github.diarmaidob.anyfin.core.common.MediaSupportChecker
import io.github.diarmaidob.anyfin.core.common.resultScope
import io.github.diarmaidob.anyfin.core.entity.MediaItem
import io.github.diarmaidob.anyfin.core.entity.MediaItemSource
import io.github.diarmaidob.anyfin.core.entity.MediaItemStream
import io.github.diarmaidob.anyfin.core.entity.SessionState
import io.github.diarmaidob.anyfin.core.mediaitem.MediaItemRepo
import io.github.diarmaidob.anyfin.core.session.SessionRepo
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

data class PlaybackInfo(
    val title: String,
    val subtitle: String?,
    val streamUrl: String,
    val startPositionMs: Long,
    val isTranscoding: Boolean
)

class GetPlaybackInfoUseCase @Inject constructor(
    private val repo: MediaItemRepo,
    private val sessionRepo: SessionRepo
) {

    suspend operator fun invoke(
        itemId: String,
        audioStreamIndex: Int? = null,
        subtitleStreamIndex: Int? = null
    ): DataResult<PlaybackInfo> = resultScope {
        val session = (sessionRepo.getCurrentSessionState() as? SessionState.LoggedIn)
            .ensureNotNull(DataLoadError.AuthError("User must be logged in"))

        // Critical: trigger full fetch of item in case we have only partial data
        repo.refreshItem(itemId)

        val item = repo.observeItem(itemId).firstOrNull()
            .ensureNotNull(DataLoadError.UnknownError("Item not found"))

        val options = repo.observeStreamOptions(itemId).firstOrNull()
            .ensureNotNull(DataLoadError.UnknownError("No stream options found"))

        val sourceEntity = repo.getSource(options.sourceId).getOrThrow()
            .ensureNotNull(DataLoadError.UnknownError("Source details missing"))

        val selectedAudio = options.audio.find { it.indexNumber.toInt() == audioStreamIndex }
            ?: options.audio.firstOrNull()

        val selectedVideo = options.video.firstOrNull()

        val decision = calculatePlaybackMethod(
            source = sourceEntity,
            video = selectedVideo,
            audio = selectedAudio
        )

        val (finalUrl, isTranscoding) = when (decision) {
            is PlaybackMethod.DirectPlay -> {
                session.buildDirectUrl(itemId, sourceEntity.id) to false
            }

            is PlaybackMethod.Transcode -> {
                if (!sourceEntity.supportsTranscoding) {
                    throw DataResultControlException(DataLoadError.UnknownError("Transcoding required but not supported by the server"))
                }
                session.buildHlsUrl(
                    itemId = itemId,
                    mediaSourceId = sourceEntity.id,
                    videoCodec = decision.videoCodec,
                    audioCodec = decision.audioCodec,
                    audioStreamIndex = selectedAudio?.indexNumber?.toInt(),
                    subtitleStreamIndex = subtitleStreamIndex
                ) to true
            }
        }

        val (displayTitle, displaySub) = getDisplayTitles(item)

        PlaybackInfo(
            title = displayTitle,
            subtitle = displaySub,
            streamUrl = finalUrl,
            startPositionMs = item.userData.playbackPositionTicks / 10_000,
            isTranscoding = isTranscoding
        )
    }


    private sealed interface PlaybackMethod {
        object DirectPlay : PlaybackMethod
        data class Transcode(val videoCodec: String, val audioCodec: String) : PlaybackMethod
    }

    private fun calculatePlaybackMethod(
        source: MediaItemSource,
        video: MediaItemStream?,
        audio: MediaItemStream?
    ): PlaybackMethod {
        val isContainerSupported = source.container?.lowercase() in SAFE_CONTAINERS
        val isVideoSupported = MediaSupportChecker.isCodecSupported(video?.codec, "")
        // TODO: fix audio codec check
        val isAudioSupported = true

        return if (source.supportsDirectPlay && isContainerSupported && isVideoSupported && isAudioSupported) {
            PlaybackMethod.DirectPlay
        } else {
            PlaybackMethod.Transcode(
                videoCodec = if (isVideoSupported) "copy" else "h264",
                audioCodec = if (isAudioSupported) "copy" else "aac"
            )
        }
    }

    private fun getDisplayTitles(item: MediaItem): Pair<String, String?> {
        return when (item) {
            is MediaItem.Episode -> item.seriesName to "S${item.seasonNumber}:E${item.episodeNumber} - ${item.name}"
            is MediaItem.Movie -> item.name to item.productionYear?.toString()
            else -> item.name to null
        }
    }

    companion object {
        private val SAFE_CONTAINERS = setOf("mkv", "mp4", "m4v", "webm", "ts", "mpegts", "mov")
    }
}

private fun SessionState.LoggedIn.buildDirectUrl(itemId: String, sourceId: String): String {
    return "$serverUrl/Videos/$itemId/stream?static=true&mediaSourceId=$sourceId&api_key=$authToken"
}

private fun SessionState.LoggedIn.buildHlsUrl(
    itemId: String,
    mediaSourceId: String,
    videoCodec: String,
    audioCodec: String,
    audioStreamIndex: Int?,
    subtitleStreamIndex: Int?
): String {
    return buildString {
        append("$serverUrl/Videos/$itemId/master.m3u8")
        append("?MediaSourceId=$mediaSourceId")
        append("&api_key=$authToken")
        append("&VideoCodec=$videoCodec")
        append("&AudioCodec=$audioCodec")
        append("&TranscodingContainer=ts")
        append("&TranscodingProtocol=hls")
        if (audioStreamIndex != null) append("&AudioStreamIndex=$audioStreamIndex")
        if (subtitleStreamIndex != null) append("&SubtitleStreamIndex=$subtitleStreamIndex")
    }
}