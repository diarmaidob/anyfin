package io.github.diarmaidob.anyfin.feature.mediaitem.list

import io.github.diarmaidob.anyfin.core.entity.MediaItem
import io.github.diarmaidob.anyfin.core.entity.MediaItemQuery
import io.github.diarmaidob.anyfin.core.mediaitem.MediaItemRepo
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMediaItemListDataUseCase @Inject constructor(
    private val repo: MediaItemRepo
) {

    fun observe(query: MediaItemQuery): Flow<List<MediaItem>> = repo.observeItems(query)

    suspend fun refresh(query: MediaItemQuery) = repo.refreshList(query)

}