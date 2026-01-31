package io.github.diarmaidob.anyfin.core.mediaitem

import io.github.diarmaidob.anyfin.core.common.DataResult
import io.github.diarmaidob.anyfin.core.entity.MediaItem
import io.github.diarmaidob.anyfin.core.entity.MediaItemQuery
import io.github.diarmaidob.anyfin.core.entity.MediaItemSource
import io.github.diarmaidob.anyfin.core.entity.MediaItemStreamOptions
import kotlinx.coroutines.flow.Flow

interface MediaItemRepo {
    fun observeItems(query: MediaItemQuery): Flow<List<MediaItem>>
    fun observeItem(id: String): Flow<MediaItem?>
    fun observeStreamOptions(itemId: String): Flow<MediaItemStreamOptions?>
    suspend fun refreshList(query: MediaItemQuery): DataResult<Unit>
    suspend fun refreshLists(queryList: List<MediaItemQuery>): DataResult<Unit>
    suspend fun refreshItem(id: String): DataResult<Unit>
    suspend fun getSource(sourceId: String): DataResult<MediaItemSource?>
}