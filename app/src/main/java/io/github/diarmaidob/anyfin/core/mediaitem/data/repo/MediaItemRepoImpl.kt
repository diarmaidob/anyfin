@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.diarmaidob.anyfin.core.mediaitem.data.repo

import io.github.diarmaidob.anyfin.core.common.DataLoadError
import io.github.diarmaidob.anyfin.core.common.DataResult
import io.github.diarmaidob.anyfin.core.entity.MediaItem
import io.github.diarmaidob.anyfin.core.entity.MediaItemQuery
import io.github.diarmaidob.anyfin.core.entity.MediaItemStreamOptions
import io.github.diarmaidob.anyfin.core.mediaitem.MediaItemRepo
import io.github.diarmaidob.anyfin.core.mediaitem.data.source.MediaItemLocalDataSource
import io.github.diarmaidob.anyfin.core.mediaitem.data.source.MediaItemRemoteDataSource
import io.github.diarmaidob.anyfin.core.mediaitem.data.source.MediaItemRowConverter
import io.github.diarmaidob.anyfin.injection.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class MediaItemRepoImpl @Inject constructor(
    private val remoteDataSource: MediaItemRemoteDataSource,
    private val localDataSource: MediaItemLocalDataSource,
    private val mediaItemRowConverter: MediaItemRowConverter,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : MediaItemRepo {

    override fun observeItems(query: MediaItemQuery): Flow<List<MediaItem>> =
        localDataSource.observeList(query.cacheKey)
            .map { mediaItemRowConverter.toDomainList(it) }
            .distinctUntilChanged()

    override fun observeItem(id: String): Flow<MediaItem?> =
        localDataSource.observeItem(id)
            .map { row -> row?.let { mediaItemRowConverter.toDomain(it) } }
            .distinctUntilChanged()

    override fun observeStreamOptions(itemId: String): Flow<MediaItemStreamOptions?> {
        return localDataSource.observePrimarySource(itemId).transformLatest { source ->
            if (source == null) {
                emit(null)
            } else {
                emitAll(
                    localDataSource.observeStreams(source.id).map { streams ->
                        mediaItemRowConverter.toStreamOptions(source, streams)
                    }
                )
            }
        }
    }

    override suspend fun getSource(sourceId: String) = safeCall(dispatcher) {
        val source = localDataSource.getSourceById(sourceId)
        DataResult.Success(source)
    }

    override suspend fun refreshList(query: MediaItemQuery): DataResult<Unit> =
        refreshLists(listOf(query))

    override suspend fun refreshLists(queryList: List<MediaItemQuery>) = safeCall(dispatcher) {
        val resultsMap = remoteDataSource.fetchBatch(queryList)

        resultsMap.forEach { (query, items) ->
            localDataSource.replaceMediaList(query.cacheKey, items)
        }

        DataResult.Success(Unit)
    }

    override suspend fun refreshItem(id: String) = safeCall(dispatcher) {
        val item = remoteDataSource.fetchItemDetails(id)
        localDataSource.updateItemDetails(id, item)
        DataResult.Success(Unit)
    }

    private suspend fun <T> safeCall(
        dispatcher: CoroutineDispatcher,
        block: suspend () -> DataResult<T>
    ): DataResult<T> = withContext(dispatcher) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            DataResult.Error(DataLoadError.NetworkError(e.message ?: "Network error"))
        } catch (e: Exception) {
            DataResult.Error(DataLoadError.UnknownError(e.message ?: "Unknown error"))
        }
    }
}