package io.github.diarmaidob.anyfin.core.ui.uimodel.converter

import io.github.diarmaidob.anyfin.core.entity.JellyfinImage
import io.github.diarmaidob.anyfin.core.entity.MediaItem
import io.github.diarmaidob.anyfin.core.ui.uimodel.MediaItemCommonUiModel
import io.github.diarmaidob.anyfin.core.ui.uimodel.MediaItemDetailsUiModel
import io.github.diarmaidob.anyfin.core.ui.uimodel.MediaItemImageSetUiModel
import io.github.diarmaidob.anyfin.core.ui.uimodel.MediaItemListItemUiModel
import io.github.diarmaidob.anyfin.core.ui.uimodel.PlayAction
import io.github.diarmaidob.anyfin.core.ui.uimodel.PlayButtonUiModel
import javax.inject.Inject


class MediaItemCommonUiModelConverter @Inject constructor() {
    fun toUiModel(item: MediaItem): MediaItemCommonUiModel = with(item) {
        val resolvedPrimary: JellyfinImage = when {
            this.images.primary != null ->
                JellyfinImage.Primary(this.id, this.images.primary)

            this is MediaItem.Season && this.images.seriesPrimary != null ->
                JellyfinImage.Primary(this.seriesId, this.images.seriesPrimary)

            this is MediaItem.Episode && this.images.seriesPrimary != null ->
                JellyfinImage.Primary(this.seriesId, this.images.seriesPrimary)

            else -> JellyfinImage.Primary(this.id, "")
        }

        val resolvedLogo: JellyfinImage? = when {
            this.images.logo != null ->
                JellyfinImage.Logo(this.id, this.images.logo)

            this is MediaItem.Episode && this.images.parentLogo != null ->
                JellyfinImage.Logo(this.seriesId, this.images.parentLogo)

            this is MediaItem.Season && this.images.parentLogo != null ->
                JellyfinImage.Logo(this.seriesId, this.images.parentLogo)

            else -> null
        }

        val resolvedThumb: JellyfinImage? = when {
            this.images.thumb != null ->
                JellyfinImage.Thumb(this.id, this.images.thumb)

            this is MediaItem.Episode && this.images.parentThumb != null ->
                JellyfinImage.Thumb(this.seriesId, this.images.parentThumb)

            this is MediaItem.Season && this.images.parentThumb != null ->
                JellyfinImage.Thumb(this.seriesId, this.images.parentThumb)

            else -> resolvedPrimary
        }

        val resolvedBackdrop: JellyfinImage = when {
            this.images.backdrop != null ->
                JellyfinImage.Backdrop(this.id, this.images.backdrop)

            this is MediaItem.Episode && this.images.primary != null ->
                JellyfinImage.Primary(this.id, this.images.primary)

            this is MediaItem.Season && this.images.parentBackdrop != null && this.seriesId.isNotEmpty() ->
                JellyfinImage.Backdrop(this.seriesId, this.images.parentBackdrop)

            this is MediaItem.Episode && this.images.parentBackdrop != null && this.seriesId.isNotEmpty() ->
                JellyfinImage.Backdrop(this.seriesId, this.images.parentBackdrop)

            else -> resolvedPrimary
        }

        val imageSet = MediaItemImageSetUiModel(
            primary = resolvedPrimary,
            backdrop = resolvedBackdrop,
            logo = resolvedLogo,
            thumb = resolvedThumb
        )
        val type = when (this) {
            is MediaItem.Episode -> MediaItemCommonUiModel.MediaItemUiType.EPISODE
            is MediaItem.Movie -> MediaItemCommonUiModel.MediaItemUiType.MOVIE
            is MediaItem.Season -> MediaItemCommonUiModel.MediaItemUiType.SEASON
            is MediaItem.Series -> MediaItemCommonUiModel.MediaItemUiType.SERIES
            is MediaItem.Unknown -> MediaItemCommonUiModel.MediaItemUiType.UNKNOWN
        }

        return MediaItemCommonUiModel(
            id = this.id,
            name = this.name,
            type = type,
            images = imageSet
        )
    }
}

class MediaItemListItemUiModelConverter @Inject constructor(
    private val commonUiModelConverter: MediaItemCommonUiModelConverter
) {
    fun toUiModel(item: MediaItem): MediaItemListItemUiModel {
        val common = commonUiModelConverter.toUiModel(item)

        return MediaItemListItemUiModel(
            common = common,
            subtitle = item.resolveSubtitle(),
            isPlayed = item.userData.isPlayed,
            isFavorite = item.userData.isFavorite,
            playbackProgress = calculateProgress(
                position = item.userData.playbackPositionTicks,
                runtime = item.details?.runTimeTicks ?: 0L
            )
        )
    }
}

class MediaItemDetailsUiModelConverter @Inject constructor(
    private val commonUiModelConverter: MediaItemCommonUiModelConverter
) {
    fun toUiModel(item: MediaItem, nextUp: MediaItem?): MediaItemDetailsUiModel = with(item) {
        val common = commonUiModelConverter.toUiModel(item)

        val starRating = this.details?.communityRating?.let { "★ %.1f".format(it) }

        val specs = mutableListOf<String>()
        this.details?.container?.let { specs.add(it.uppercase()) }
        if (this.details?.officialRating != null) specs.add(this.details!!.officialRating!!)

        return MediaItemDetailsUiModel(
            common = common,
            overview = this.details?.overview ?: "",
            tagline = this.details?.tagline,
            officialRating = this.details?.officialRating,
            starRating = starRating,
            specs = specs,
            isFavorite = this.userData.isFavorite,
            playButton = resolvePlayButton(this, nextUp)
        )
    }
}



private fun MediaItem.resolveSubtitle(): String? {
    return when (this) {
        is MediaItem.Movie -> productionYear?.toString()
        is MediaItem.Series -> {
            val year = productionYear?.toString() ?: ""
            val status = if (status == "Ended") "Ended" else ""
            listOf(year, status).filter { it.isNotEmpty() }.joinToString(" • ")
        }

        is MediaItem.Episode -> "$seriesName - S$seasonNumber:E$episodeNumber"
        is MediaItem.Season -> "Season $seasonNumber"
        else -> null
    }
}

private fun calculateProgress(position: Long, runtime: Long): Float {
    if (position <= 0 || runtime <= 0) return 0f
    val progress = position.toFloat() / runtime.toFloat()
    return progress.coerceIn(0f, 1f)
}

private fun resolvePlayButton(item: MediaItem, nextUp: MediaItem?): PlayButtonUiModel {
    if (item is MediaItem.Series || item is MediaItem.Season) {
        if (nextUp == null || nextUp !is MediaItem.Episode) {
            return PlayButtonUiModel("Play", PlayAction.None, isEnabled = false)
        }

        val nameSummary = if (item is MediaItem.Series) {
            "S${nextUp.seasonNumber}:E${nextUp.episodeNumber}"
        } else {
            "E${nextUp.episodeNumber}"
        }
        val isResuming = nextUp.userData.playbackPositionTicks > 0
        val label = if (isResuming) {
            "Resume $nameSummary"
        } else {
            "Play $nameSummary"
        }

        return PlayButtonUiModel(label, PlayAction.NavigateToChild(nextUp.id), isEnabled = true)
    }

    val isResuming = item.userData.playbackPositionTicks > (60 * 10_000 * 1000)
    val label = if (isResuming) "Resume" else "Play"

    return PlayButtonUiModel(label, PlayAction.PlayItem(item.id), isEnabled = true)
}