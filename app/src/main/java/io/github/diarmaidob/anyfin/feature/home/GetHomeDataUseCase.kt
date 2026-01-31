package io.github.diarmaidob.anyfin.feature.home

import io.github.diarmaidob.anyfin.core.common.DataResult
import io.github.diarmaidob.anyfin.core.entity.MediaItem
import io.github.diarmaidob.anyfin.core.entity.MediaItemQuery
import io.github.diarmaidob.anyfin.core.mediaitem.MediaItemRepo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

data class HomeSection(
    val type: SectionType,
    val items: List<MediaItem>,
) {

    val id = type.toString()

    val isEmpty = items.isEmpty()

    enum class SectionType {
        RESUME,
        NEXT_UP,
        LATEST_MOVIES,
        LATEST_SERIES
    }
}

class GetHomeSectionsUseCase @Inject constructor(
    private val repo: MediaItemRepo
) {
    private val resumeQuery = MediaItemQuery.Resume(
        cacheKey = "home_resume",
        limit = 12
    )
    private val nextUpQuery = MediaItemQuery.NextUp(
        cacheKey = "home_nextup",
        limit = 20
    )
    private val latestMoviesQuery = MediaItemQuery.Latest(
        cacheKey = "home_latest_movies",
        limit = 20,
        mediaTypes = setOf(MediaItemQuery.MediaType.MOVIE)
    )
    private val latestSeriesQuery = MediaItemQuery.Latest(
        cacheKey = "home_latest_series",
        limit = 20,
        mediaTypes = setOf(MediaItemQuery.MediaType.SERIES),
    )

    fun observe(): Flow<List<HomeSection>> {
        return combine(
            repo.observeItems(resumeQuery),
            repo.observeItems(nextUpQuery),
            repo.observeItems(latestMoviesQuery),
            repo.observeItems(latestSeriesQuery)
        ) { resume, nextUp, movies, series ->
            listOf(
                HomeSection(
                    type = HomeSection.SectionType.RESUME,
                    items = resume,
                ),
                HomeSection(
                    type = HomeSection.SectionType.NEXT_UP,
                    items = nextUp,
                ),
                HomeSection(
                    type = HomeSection.SectionType.LATEST_MOVIES,
                    items = movies,
                ),
                HomeSection(
                    type = HomeSection.SectionType.LATEST_SERIES,
                    items = series,
                )
            ).filterNot { it.isEmpty }
        }
    }

    suspend fun refresh(): DataResult<Unit> {
        return repo.refreshLists(
            listOf(
                resumeQuery,
                nextUpQuery,
                latestMoviesQuery,
                latestSeriesQuery
            )
        )
    }

}