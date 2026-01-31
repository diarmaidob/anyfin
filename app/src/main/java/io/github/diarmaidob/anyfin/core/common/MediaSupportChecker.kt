package io.github.diarmaidob.anyfin.core.common


import androidx.annotation.OptIn
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil

object MediaSupportChecker {

    /**
     * Checks if the device has a decoder for the given format.
     */
    @OptIn(UnstableApi::class)
    fun isCodecSupported(jellyfinCodec: String?, mimeType: String): Boolean {
        if (jellyfinCodec == null) return false

        val mime = mimeType.ifEmpty { mapJellyfinCodecToMime(jellyfinCodec) } ?: return false

        return try {
            val decoders = MediaCodecUtil.getDecoderInfos(
                mime,
                false,
                false
            )
            decoders.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Determines if we need to transcode based on container and codecs.
     */
    @OptIn(UnstableApi::class)
    fun canDirectPlay(
        container: String?,
        videoCodec: String?,
        audioCodec: String?
    ): Boolean {
        val safeContainers = listOf("mkv", "mp4", "m4v", "webm", "ts", "mpegts")
        if (container != null && !safeContainers.contains(container.lowercase())) {
            return false
        }

        val videoSupported = if (videoCodec != null) {
            isCodecSupported(videoCodec, MimeTypes.getMediaMimeType(videoCodec) ?: "")
        } else true

        val audioSupported = if (audioCodec != null) {
            isCodecSupported(audioCodec, MimeTypes.getMediaMimeType(audioCodec) ?: "")
        } else true

        return videoSupported && audioSupported
    }

    @OptIn(UnstableApi::class)
    private fun mapJellyfinCodecToMime(codec: String): String? {
        return when (codec.lowercase()) {
            "h264", "avc" -> MimeTypes.VIDEO_H264
            "hevc", "h265" -> MimeTypes.VIDEO_H265
            "vp9" -> MimeTypes.VIDEO_VP9
            "av1" -> MimeTypes.VIDEO_AV1
            "mpeg4" -> MimeTypes.VIDEO_MP4V
            "mpeg2", "mpeg2video" -> MimeTypes.VIDEO_MPEG2

            "aac" -> MimeTypes.AUDIO_AAC
            "ac3" -> MimeTypes.AUDIO_AC3
            "eac3" -> MimeTypes.AUDIO_E_AC3
            "mp3" -> MimeTypes.AUDIO_MPEG
            "flac" -> MimeTypes.AUDIO_FLAC
            "opus" -> MimeTypes.AUDIO_OPUS
            "dts" -> MimeTypes.AUDIO_DTS
            "dtshd", "dts-hd" -> MimeTypes.AUDIO_DTS_HD
            "truehd" -> MimeTypes.AUDIO_TRUEHD
            else -> null
        }
    }
}