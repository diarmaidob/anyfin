package io.github.diarmaidob.anyfin.core.entity

sealed interface MediaItemQuery {
    val cacheKey: String
    val limit: Int

    enum class SortOrder(val apiValue: String) {
        ASCENDING("Ascending"),
        DESCENDING("Descending")
    }

    enum class Sort(val apiValue: String) {
        DATE_CREATED("DateCreated"),
        DATE_PLAYED("DatePlayed"),
        NAME("SortName"),
        PRODUCTION_YEAR("ProductionYear"),
        PREMIERE_DATE("PremiereDate"),
        INDEX_NUMBER("IndexNumber"),
        RANDOM("Random")
    }

    enum class Filter(val apiValue: String) {
        IS_PLAYED("IsPlayed"),
        IS_UNPLAYED("IsUnplayed"),
        IS_FAVORITE("IsFavorite")
    }

    enum class MediaType(val apiValue: String) {
        MOVIE("Movie"),
        EPISODE("Episode"),
        SERIES("Series"),
        SEASON("Season"),
        AUDIO("Audio"),
        VIDEO("Video"),
        COLLECTION_FOLDER("CollectionFolder"),
        UNKNOWN("Unknown");
    }


    data class Resume(
        override val cacheKey: String = "resume",
        override val limit: Int = 20,
        val mediaTypes: Set<MediaType> = setOf(MediaType.VIDEO)
    ) : MediaItemQuery

    data class NextUp(
        override val cacheKey: String = "nextup",
        val seriesId: String? = null,
        override val limit: Int = 20
    ) : MediaItemQuery {

        companion object {
            fun forItem(item: MediaItem): NextUp? {
                return when (item) {
                    is MediaItem.Series -> NextUp(
                        cacheKey = "next_up_of_${item.id}",
                        seriesId = item.id
                    )

                    is MediaItem.Season -> NextUp(
                        cacheKey = "next_up_of_${item.seriesId}",
                        seriesId = item.seriesId
                    )

                    else -> null
                }
            }
        }
    }

    data class Latest(
        override val cacheKey: String = "latest",
        override val limit: Int = 20,
        val mediaTypes: Set<MediaType> = setOf(MediaType.MOVIE, MediaType.SERIES),
        val parentId: String? = null
    ) : MediaItemQuery

    data class UserViews(
        override val cacheKey: String = "user_views",
        override val limit: Int = 100
    ) : MediaItemQuery

    data class Browse(
        override val cacheKey: String,
        override val limit: Int = 100,
        val parentId: String? = null,
        val isRecursive: Boolean = true,
        val sortOrder: SortOrder = SortOrder.ASCENDING,
        val sortBy: List<Sort> = listOf(Sort.NAME),
        val filters: Set<Filter> = emptySet(),
        val mediaTypes: Set<MediaType> = emptySet(),
        val excludeItemTypes: Set<String> = emptySet()
    ) : MediaItemQuery {

        companion object {
            fun libraries() = Browse(
                cacheKey = "libraries",
                isRecursive = false,
                mediaTypes = setOf(MediaType.COLLECTION_FOLDER),
                excludeItemTypes = setOf("ManualPlaylistsFolder")
            )

            fun folder(parentId: String, sort: Sort = Sort.NAME) = Browse(
                cacheKey = "lib_$parentId",
                parentId = parentId,
                sortBy = listOf(sort),
                isRecursive = false
            )

            fun childrenOf(item: MediaItem): Browse {
                val childTypes = if (item is MediaItem.Series) {
                    setOf(MediaType.SEASON)
                } else {
                    setOf(MediaType.EPISODE)
                }

                return Browse(
                    cacheKey = "children_of_${item.id}",
                    parentId = item.id,
                    mediaTypes = childTypes,
                    sortBy = listOf(Sort.INDEX_NUMBER),
                    sortOrder = SortOrder.ASCENDING
                )
            }
        }
    }
}