package io.github.diarmaidob.anyfin.core.mediaitem.data.source

import io.github.diarmaidob.anyfin.core.entity.MediaItem
import io.github.diarmaidob.anyfin.core.entity.MediaItemDetail
import io.github.diarmaidob.anyfin.core.entity.MediaItemImageTagInfo
import io.github.diarmaidob.anyfin.core.entity.MediaItemRow
import io.github.diarmaidob.anyfin.core.entity.MediaItemSource
import io.github.diarmaidob.anyfin.core.entity.MediaItemStream
import io.github.diarmaidob.anyfin.core.entity.MediaItemStreamOptions
import io.github.diarmaidob.anyfin.core.entity.MediaItemUserData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaItemRowConverter @Inject constructor() {

    fun toDomainList(rows: List<MediaItemRow>): List<MediaItem> =
        rows.map { toDomain(it) }


    fun toDomain(row: MediaItemRow): MediaItem {
        val images = MediaItemImageTagInfo(
            mediaId = row.id,
            primary = row.primary,
            backdrop = row.backdrop,
            logo = row.logo,
            thumb = row.thumb,
            banner = row.banner,
            seriesPrimary = row.seriesPrimary,
            parentLogo = row.parentLogo,
            parentThumb = row.parentThumb,
            parentBackdrop = row.parentBackdrop
        )

        val userData = MediaItemUserData(
            mediaId = row.id,
            isPlayed = row.isPlayed ?: false,
            playbackPositionTicks = row.playbackPositionTicks ?: 0L,
            isFavorite = row.isFavorite ?: false,
            lastPlayedDate = row.lastPlayedDate ?: 0L,
            playCount = row.playCount ?: 0L
        )

        val details = row.dateCreated?.let {
            MediaItemDetail(
                mediaId = row.id,
                overview = row.overview,
                tagline = row.tagline,
                officialRating = row.officialRating,
                communityRating = row.communityRating,
                criticRating = row.criticRating,
                dateCreated = it,
                runTimeTicks = row.runTimeTicks ?: 0L,
                container = row.container
            )
        }

        return when (row.type) {
            "Movie" -> MediaItem.Movie(
                id = row.id,
                name = row.name,
                images = images,
                userData = userData,
                details = details,
                productionYear = row.productionYear?.toInt()
            )

            "Series" -> MediaItem.Series(
                id = row.id,
                name = row.name,
                images = images,
                userData = userData,
                details = details,
                productionYear = row.productionYear?.toInt(),
                status = if (row.endDate != null) "Ended" else "Continuing"
            )

            "Episode" -> MediaItem.Episode(
                id = row.id,
                name = row.name,
                images = images,
                userData = userData,
                details = details,
                seriesName = row.seriesName.orEmpty(),
                seriesId = row.seriesId.orEmpty(),
                seasonId = row.seasonId.orEmpty(),
                seasonNumber = row.parentIndexNumber?.toInt() ?: 1,
                episodeNumber = row.indexNumber?.toInt() ?: 0
            )

            "Season" -> MediaItem.Season(
                id = row.id,
                name = row.name,
                images = images,
                userData = userData,
                details = details,
                seriesId = row.seriesId.orEmpty(),
                seasonNumber = row.indexNumber?.toInt() ?: 1
            )

            else -> MediaItem.Unknown(row.id, row.name, images, userData, details)
        }
    }

    fun toStreamOptions(source: MediaItemSource, streams: List<MediaItemStream>): MediaItemStreamOptions {
        return MediaItemStreamOptions(
            sourceId = source.id,
            videoContainer = source.container,
            supportsTranscoding = source.supportsTranscoding,
            supportsDirectPlay = source.supportsDirectPlay,
            video = streams.filter { it.type == "Video" },
            audio = streams.filter { it.type == "Audio" },
            subtitles = streams.filter { it.type == "Subtitle" }
        )
    }
}