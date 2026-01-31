package io.github.diarmaidob.anyfin.feature.mediaitem.list

import io.github.diarmaidob.anyfin.core.entity.MediaItemQuery
import io.github.diarmaidob.anyfin.navigation.MediaItemListDestination
import javax.inject.Inject

class MediaItemListQueryFactory @Inject constructor() {
    fun create(destination: MediaItemListDestination): MediaItemQuery {
        return when (destination.type) {
            MediaItemListDestination.ListType.RESUME -> MediaItemQuery.Resume()
            MediaItemListDestination.ListType.NEXT_UP -> MediaItemQuery.NextUp()
            MediaItemListDestination.ListType.LATEST_MOVIES -> MediaItemQuery.Latest(
                mediaTypes = setOf(MediaItemQuery.MediaType.MOVIE)
            )

            MediaItemListDestination.ListType.LATEST_SHOWS -> MediaItemQuery.Latest(
                mediaTypes = setOf(MediaItemQuery.MediaType.SERIES)
            )

            MediaItemListDestination.ListType.LIBRARIES -> MediaItemQuery.Browse.libraries()
            MediaItemListDestination.ListType.LIBRARY_CONTENTS -> {
                requireNotNull(destination.listId) { "Library contents must have an ID" }
                MediaItemQuery.Browse.folder(destination.listId)
            }
        }
    }
}