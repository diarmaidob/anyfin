package io.github.diarmaidob.anyfin.core.entity

sealed interface MediaItem {
    val id: String
    val name: String

    val images: MediaItemImageTagInfo
    val userData: MediaItemUserData
    val details: MediaItemDetail?

    data class Movie(
        override val id: String,
        override val name: String,
        override val images: MediaItemImageTagInfo,
        override val userData: MediaItemUserData,
        override val details: MediaItemDetail?,
        val productionYear: Int?
    ) : MediaItem

    data class Series(
        override val id: String,
        override val name: String,
        override val images: MediaItemImageTagInfo,
        override val userData: MediaItemUserData,
        override val details: MediaItemDetail?,
        val productionYear: Int?,
        val status: String
    ) : MediaItem

    data class Episode(
        override val id: String,
        override val name: String,
        override val images: MediaItemImageTagInfo,
        override val userData: MediaItemUserData,
        override val details: MediaItemDetail?,
        val seriesName: String,
        val seriesId: String,
        val seasonId: String,
        val seasonNumber: Int,
        val episodeNumber: Int
    ) : MediaItem

    data class Season(
        override val id: String,
        override val name: String,
        override val images: MediaItemImageTagInfo,
        override val userData: MediaItemUserData,
        override val details: MediaItemDetail?,
        val seriesId: String,
        val seasonNumber: Int
    ) : MediaItem

    data class Unknown(
        override val id: String,
        override val name: String,
        override val images: MediaItemImageTagInfo,
        override val userData: MediaItemUserData,
        override val details: MediaItemDetail?
    ) : MediaItem
}