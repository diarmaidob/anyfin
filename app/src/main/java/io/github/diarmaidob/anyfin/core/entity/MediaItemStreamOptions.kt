package io.github.diarmaidob.anyfin.core.entity

data class MediaItemStreamOptions(
    val sourceId: String,
    val videoContainer: String?,
    val supportsTranscoding: Boolean,
    val supportsDirectPlay: Boolean,
    val video: List<MediaItemStream>,
    val audio: List<MediaItemStream>,
    val subtitles: List<MediaItemStream>
)