package io.github.diarmaidob.anyfin.core.mediaitem.data.source

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import io.github.diarmaidob.anyfin.core.entity.MediaItemQueries
import io.github.diarmaidob.anyfin.core.entity.MediaItemRow
import io.github.diarmaidob.anyfin.core.entity.MediaItemStream
import io.github.diarmaidob.anyfin.core.entity.MediaListEntry
import io.github.diarmaidob.anyfin.core.mediaitem.data.api.MediaItemResponse
import io.github.diarmaidob.anyfin.core.mediaitem.data.api.toDetail
import io.github.diarmaidob.anyfin.core.mediaitem.data.api.toEntity
import io.github.diarmaidob.anyfin.core.mediaitem.data.api.toImageTagInfo
import io.github.diarmaidob.anyfin.core.mediaitem.data.api.toInfo
import io.github.diarmaidob.anyfin.core.mediaitem.data.api.toUserData
import io.github.diarmaidob.anyfin.injection.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MediaItemLocalDataSource @Inject constructor(
    private val queries: MediaItemQueries,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    fun observeList(cacheKey: String): Flow<List<MediaItemRow>> =
        queries.getListItems(cacheKey)
            .asFlow()
            .mapToList(ioDispatcher)

    fun observeItem(id: String): Flow<MediaItemRow?> =
        queries.getItem(id)
            .asFlow()
            .mapToOneOrNull(ioDispatcher)

    suspend fun replaceMediaList(cacheKey: String, items: List<MediaItemResponse>) = withContext(ioDispatcher) {
        queries.transaction {
            items.forEachIndexed { index, item ->
                upsertCoreData(item)
                queries.insertListEntry(
                    MediaListEntry(cacheKey, item.id, index.toLong())
                )
            }
            queries.deleteStaleEntries(
                listKey = cacheKey,
                currentIds = items.map { it.id }
            )
        }
    }

    suspend fun updateItemDetails(id: String, item: MediaItemResponse) = withContext(ioDispatcher) {
        queries.transaction {
            upsertCoreData(item)
            queries.upsertDetail(item.toDetail())

            queries.deleteSourcesForMedia(id)
            item.mediaSourceResponses?.forEach { sourceDto ->
                val sourceEntity = sourceDto.toEntity(id)
                queries.upsertSource(sourceEntity)

                queries.deleteStreamsForSource(sourceEntity.id)
                sourceDto.mediaStreamResponses?.forEach { streamDto ->
                    queries.upsertStream(streamDto.toEntity(sourceEntity.id))
                }
            }
        }
    }

    suspend fun getPrimarySource(itemId: String) = withContext(ioDispatcher) {
        queries.getSourcesForMedia(itemId).executeAsList().firstOrNull()
    }

    fun observePrimarySource(itemId: String) = queries.getSourcesForMedia(itemId).asFlow().mapToOneOrNull(ioDispatcher)

    fun observeStreams(sourceId: String): Flow<List<MediaItemStream>> =
        queries.getStreamsForSource(sourceId).asFlow().mapToList(ioDispatcher)

    suspend fun getStreams(sourceId: String) = withContext(ioDispatcher) {
        queries.getStreamsForSource(sourceId).executeAsList()
    }

    suspend fun getSourceById(sourceId: String) = withContext(ioDispatcher) {
        queries.getSourceById(sourceId).executeAsOneOrNull()
    }

    private fun upsertCoreData(response: MediaItemResponse) {
        queries.upsertInfo(response.toInfo())
        queries.upsertImageTags(response.toImageTagInfo())
        queries.upsertUserData(response.toUserData())
    }
}