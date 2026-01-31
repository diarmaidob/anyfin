package io.github.diarmaidob.anyfin.core.mediaitem.data.api

import io.github.diarmaidob.anyfin.core.common.JellyfinUtils
import io.github.diarmaidob.anyfin.core.entity.MediaItemDetail
import io.github.diarmaidob.anyfin.core.entity.MediaItemImageTagInfo
import io.github.diarmaidob.anyfin.core.entity.MediaItemInfo
import io.github.diarmaidob.anyfin.core.entity.MediaItemSource
import io.github.diarmaidob.anyfin.core.entity.MediaItemStream
import io.github.diarmaidob.anyfin.core.entity.MediaItemUserData

fun MediaItemResponse.toInfo(): MediaItemInfo {
    val validName = this.name.takeIf { !it.isNullOrBlank() }
        ?: this.originalTitle
        ?: this.path?.substringAfterLast("/")
        ?: "Unknown Item"

    val validType = this.type ?: "BaseItem"

    val validSortName = this.sortName ?: this.forcedSortName ?: validName

    val premiereMillis = this.premiereDate?.let { JellyfinUtils.parseIsoDate(it).toEpochMilli() }
    val endMillis = this.endDate?.let { JellyfinUtils.parseIsoDate(it).toEpochMilli() }

    return MediaItemInfo(
        id = this.id,
        serverId = this.serverId ?: "",
        type = validType,
        name = validName,
        sortName = validSortName,
        originalTitle = this.originalTitle,

        path = this.path,
        parentId = this.parentId,
        seriesId = this.seriesId,
        seriesName = this.seriesName,
        seasonId = this.seasonId,
        seasonName = this.seasonName,

        indexNumber = this.indexNumber?.toLong(),
        parentIndexNumber = this.parentIndexNumber?.toLong(),

        productionYear = this.productionYear?.toLong(),
        premiereDate = premiereMillis,
        endDate = endMillis
    )
}

fun MediaItemResponse.toDetail(): MediaItemDetail {
    return MediaItemDetail(
        mediaId = this.id,
        overview = this.overview,
        tagline = this.taglines?.firstOrNull(),
        officialRating = this.officialRating,
        communityRating = this.communityRating,
        criticRating = this.criticRating?.toLong(),

        dateCreated = this.dateCreated?.let { JellyfinUtils.parseIsoDate(it).toEpochMilli() } ?: 0L,
        runTimeTicks = this.runTimeTicks ?: 0L,
        container = this.container
    )
}

fun MediaItemResponse.toUserData(): MediaItemUserData {
    return MediaItemUserData(
        mediaId = this.id,
        isPlayed = this.userData?.isPlayed == true,
        playbackPositionTicks = this.userData?.playbackPositionTicks ?: 0L,
        isFavorite = this.userData?.isFavorite == true,
        lastPlayedDate = this.userData?.lastPlayedDate?.let { JellyfinUtils.parseIsoDate(it).toEpochMilli() } ?: 0L,
        playCount = this.userData?.playCount?.toLong() ?: 0L
    )
}

fun MediaItemResponse.toImageTagInfo(): MediaItemImageTagInfo {
    return MediaItemImageTagInfo(
        mediaId = this.id,
        primary = this.imageTags?.get("Primary"),
        backdrop = this.imageTags?.get("Backdrop") ?: this.backdropImageTags?.firstOrNull(),
        logo = this.imageTags?.get("Logo"),
        thumb = this.imageTags?.get("Thumb"),
        banner = this.imageTags?.get("Banner"),
        seriesPrimary = this.seriesPrimaryImageTag,
        parentLogo = this.parentLogoImageTag,
        parentThumb = this.parentThumbImageTag,
        parentBackdrop = this.parentBackdropImageTags?.firstOrNull()
    )
}

fun MediaSourceResponse.toEntity(mediaId: String): MediaItemSource {
    return MediaItemSource(
        id = this.id,
        mediaId = mediaId,
        container = this.container,
        protocol = this.protocol,
        name = this.name,
        supportsDirectPlay = this.supportsDirectPlay == true,
        supportsTranscoding = this.supportsTranscoding == true
    )
}

fun MediaStreamResponse.toEntity(sourceId: String): MediaItemStream {
    val typeStr = this.type ?: "Unknown"

    return MediaItemStream(
        sourceId = sourceId,
        indexNumber = this.index?.toLong() ?: 0L,
        type = typeStr,
        codec = this.codec,
        language = this.language,
        displayTitle = this.displayTitle ?: this.language,
        isDefault = this.isDefault == true,
        isForced = this.isForced == true,
        channels = this.channels?.toLong(),
        width = this.width?.toLong(),
        height = this.height?.toLong()
    )
}