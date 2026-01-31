package io.github.diarmaidob.anyfin.feature.mediaitem.details

import io.github.diarmaidob.anyfin.core.entity.MediaItem
import io.github.diarmaidob.anyfin.core.entity.MediaItemQuery
import io.github.diarmaidob.anyfin.core.mediaitem.MediaItemRepo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveMediaItemDetailsUseCase @Inject constructor(
    private val repo: MediaItemRepo
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(itemId: String): Flow<MediaItemDetailsData> {
        return repo.observeItem(itemId).flatMapLatest { item ->
            if (item == null) return@flatMapLatest emptyFlow()

            val childrenFlow = if (item is MediaItem.Series || item is MediaItem.Season) {
                repo.observeItems(MediaItemQuery.Browse.childrenOf(item))
            } else {
                flowOf(emptyList())
            }

            val nextUpFlow = getNextUpFlow(item, childrenFlow)
            val streamsFlow = repo.observeStreamOptions(item.id)

            combine(childrenFlow, nextUpFlow, streamsFlow) { children, nextUp, streams ->
                MediaItemDetailsData(
                    item = item,
                    children = children,
                    nextUp = nextUp,
                    streamOptions = streams
                )
            }
        }
    }

    private fun getNextUpFlow(
        item: MediaItem,
        childrenFlow: Flow<List<MediaItem>>
    ): Flow<MediaItem?> {
        val query = MediaItemQuery.NextUp.forItem(item) ?: return flowOf(null)
        val nextUpRepoFlow = repo.observeItems(query)

        return when (item) {
            is MediaItem.Series -> nextUpRepoFlow.map { it.firstOrNull() }
            is MediaItem.Season -> {
                combine(nextUpRepoFlow, childrenFlow) { nextUpList, children ->
                    val seasonNextUp = nextUpList
                        .filterIsInstance<MediaItem.Episode>()
                        .firstOrNull { it.seasonId == item.id }

                    seasonNextUp ?: children.filterIsInstance<MediaItem.Episode>().firstOrNull()
                }
            }

            else -> flowOf(null)
        }
    }
}