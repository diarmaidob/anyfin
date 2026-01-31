package io.github.diarmaidob.anyfin.feature.mediaitem.details

import io.github.diarmaidob.anyfin.core.common.DataLoadError
import io.github.diarmaidob.anyfin.core.common.DataResult
import io.github.diarmaidob.anyfin.core.common.resultScope
import io.github.diarmaidob.anyfin.core.entity.MediaItem
import io.github.diarmaidob.anyfin.core.entity.MediaItemQuery
import io.github.diarmaidob.anyfin.core.mediaitem.MediaItemRepo
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import javax.inject.Inject

class RefreshMediaItemDetailsUseCase @Inject constructor(
    private val repo: MediaItemRepo
) {
    suspend operator fun invoke(itemId: String): DataResult<Unit> = resultScope {
        repo.refreshItem(itemId).getOrThrow()

        val item = repo.observeItem(itemId).firstOrNull()
            .ensureNotNull(DataLoadError.ItemNotFoundError(itemId))

        coroutineScope {
            val jobs = mutableListOf<Job>()

            if (item is MediaItem.Series || item is MediaItem.Season) {
                jobs += launch {
                    val query = MediaItemQuery.Browse.childrenOf(item)
                    repo.refreshList(query).getOrThrow()
                }
            }

            MediaItemQuery.NextUp.forItem(item)?.let { query ->
                jobs += launch {
                    repo.refreshList(query).getOrThrow()
                }
            }

            jobs.joinAll()
        }
    }
}