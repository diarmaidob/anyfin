package io.github.diarmaidob.anyfin.core.mediaitem.data.source

import io.github.diarmaidob.anyfin.core.common.DataLoadError
import io.github.diarmaidob.anyfin.core.entity.MediaItemQuery
import io.github.diarmaidob.anyfin.core.entity.userIdOrNull
import io.github.diarmaidob.anyfin.core.mediaitem.data.api.MediaItemApi
import io.github.diarmaidob.anyfin.core.mediaitem.data.api.MediaItemResponse
import io.github.diarmaidob.anyfin.core.session.SessionRepo
import io.github.diarmaidob.anyfin.injection.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MediaItemRemoteDataSource @Inject constructor(
    private val api: MediaItemApi,
    private val sessionRepo: SessionRepo,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    suspend fun fetchBatch(queries: List<MediaItemQuery>): Map<MediaItemQuery, List<MediaItemResponse>> =
        withContext(ioDispatcher) {
            val userId = getUserIdOrThrow()

            val results = queries.map { query ->
                async {
                    val responseItems = fetchSingleQuery(query, userId)
                    query to responseItems
                }
            }.awaitAll()

            results.toMap()
        }

    suspend fun fetchItemDetails(id: String): MediaItemResponse = withContext(ioDispatcher) {
        val userId = getUserIdOrThrow()
        val response = api.getItemDetails(userId, id)
        response.body() ?: throw IllegalStateException("Empty response body from API for item $id")
    }

    private suspend fun fetchSingleQuery(query: MediaItemQuery, userId: String): List<MediaItemResponse> {
        return when (query) {
            is MediaItemQuery.Resume -> {
                val params = mapOf(
                    "Limit" to query.limit.toString(),
                    "MediaTypes" to query.mediaTypes.joinToString(",") { it.apiValue },
                    "SortBy" to "DatePlayed",
                    "SortOrder" to "Descending"
                )
                api.getResumeItems(userId, params).body()?.items
            }

            is MediaItemQuery.NextUp -> {
                val params = buildMap {
                    put("Limit", query.limit.toString())
                    put("UserId", userId)
                    query.seriesId?.let { put("SeriesId", it) }
                }
                api.getNextUpItems(params).body()?.items
            }

            is MediaItemQuery.Latest -> {
                val params = mapOf(
                    "Limit" to query.limit.toString(),
                    "IncludeItemTypes" to query.mediaTypes.joinToString(",") { it.apiValue },
                    "ParentId" to (query.parentId ?: "")
                )
                api.getLatestItems(userId, params).body()
            }

            is MediaItemQuery.Browse -> {
                val params = buildMap {
                    put("Limit", query.limit.toString())
                    put("Recursive", query.isRecursive.toString())
                    put("SortOrder", query.sortOrder.apiValue)
                    if (query.sortBy.isNotEmpty()) put("SortBy", query.sortBy.joinToString(",") { it.apiValue })
                    if (query.parentId != null) put("ParentId", query.parentId)
                    if (query.mediaTypes.isNotEmpty()) put("IncludeItemTypes", query.mediaTypes.joinToString(",") { it.apiValue })
                    if (query.filters.isNotEmpty()) put("Filters", query.filters.joinToString(",") { it.apiValue })
                    if (query.excludeItemTypes.isNotEmpty()) put("ExcludeItemTypes", query.excludeItemTypes.joinToString(","))
                }
                api.getItems(userId, params).body()?.items
            }

            is MediaItemQuery.UserViews -> {
                api.getUserViews(userId).body()?.items
            }
        } ?: emptyList()
    }

    private fun getUserIdOrThrow(): String {
        return sessionRepo.getCurrentSessionState().userIdOrNull()
            ?: throw DataLoadError.AuthError("User not authenticated")
    }
}