package io.github.diarmaidob.anyfin.core.mediaitem.data.repo

import io.github.diarmaidob.anyfin.core.common.DataLoadError
import io.github.diarmaidob.anyfin.core.common.DataResult
import io.github.diarmaidob.anyfin.core.entity.MediaItem
import io.github.diarmaidob.anyfin.core.entity.MediaItemQuery
import io.github.diarmaidob.anyfin.core.entity.MediaItemRow
import io.github.diarmaidob.anyfin.core.entity.MediaItemSource
import io.github.diarmaidob.anyfin.core.entity.MediaItemStream
import io.github.diarmaidob.anyfin.core.entity.MediaItemStreamOptions
import io.github.diarmaidob.anyfin.core.mediaitem.data.api.MediaItemResponse
import io.github.diarmaidob.anyfin.core.mediaitem.data.source.MediaItemLocalDataSource
import io.github.diarmaidob.anyfin.core.mediaitem.data.source.MediaItemRemoteDataSource
import io.github.diarmaidob.anyfin.core.mediaitem.data.source.MediaItemRowConverter
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
class MediaItemRepoImplTest {

    private val localDataSource: MediaItemLocalDataSource = mockk()
    private val remoteDataSource: MediaItemRemoteDataSource = mockk()
    private val converter: MediaItemRowConverter = mockk()
    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: MediaItemRepoImpl

    private val query: MediaItemQuery = mockk {
        every { cacheKey } returns "cacheKey"
    }
    private val mediaItemResponse: MediaItemResponse = mockk()
    private val mediaItem: MediaItem = mockk()
    private val row: MediaItemRow = mockk()
    private val source: MediaItemSource = mockk()
    private val stream: MediaItemStream = mockk()
    private val streamOptions: MediaItemStreamOptions = mockk()

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        repo = MediaItemRepoImpl(
            remoteDataSource = remoteDataSource,
            localDataSource = localDataSource,
            mediaItemRowConverter = converter,
            dispatcher = dispatcher
        )
    }

    @Test
    fun `observe items maps rows to domain list`() = runTest {
        every { localDataSource.observeList("cacheKey") } returns flowOf(listOf(row))
        every { converter.toDomainList(listOf(row)) } returns listOf(mediaItem)

        val result = repo.observeItems(query).first()

        assertEquals(listOf(mediaItem), result)
    }

    @Test
    fun `observe items applies distinct until changed`() = runTest {
        val row1 = mockk<MediaItemRow>()
        val row2 = mockk<MediaItemRow>()
        val item1 = mockk<MediaItem>()
        val item2 = mockk<MediaItem>()

        every { localDataSource.observeList("cacheKey") } returns flowOf(listOf(row1), listOf(row1), listOf(row2))
        every { converter.toDomainList(listOf(row1)) } returns listOf(item1)
        every { converter.toDomainList(listOf(row2)) } returns listOf(item2)

        val results = repo.observeItems(query).take(2).toList()

        assertEquals(2, results.size)
        assertEquals(listOf(item1), results[0])
        assertEquals(listOf(item2), results[1])
        assertNotEquals(results[0], results[1])
    }

    @Test
    fun `observe item maps row to domain`() = runTest {
        every { localDataSource.observeItem("id") } returns flowOf(row)
        every { converter.toDomain(row) } returns mediaItem

        val result = repo.observeItem("id").first()

        assertEquals(mediaItem, result)
    }

    @Test
    fun `observe item emits null when row is null`() = runTest {
        every { localDataSource.observeItem("id") } returns flowOf(null)

        val result = repo.observeItem("id").first()

        assertEquals(null, result)
    }

    @Test
    fun `observe item applies distinct until changed`() = runTest {
        val row1 = mockk<MediaItemRow>()
        val row2 = mockk<MediaItemRow>()
        val item1 = mockk<MediaItem>()
        val item2 = mockk<MediaItem>()

        every { localDataSource.observeItem("id") } returns flowOf(row1, row1, row2)
        every { converter.toDomain(row1) } returns item1
        every { converter.toDomain(row2) } returns item2

        val results = repo.observeItem("id").take(2).toList()

        assertEquals(2, results.size)
        assertEquals(item1, results[0])
        assertEquals(item2, results[1])
        assertNotEquals(results[0], results[1])
    }

    @Test
    fun `observe stream options emits null when source is null`() = runTest {
        every { localDataSource.observePrimarySource("itemId") } returns flowOf(null)

        val result = repo.observeStreamOptions("itemId").first()

        assertEquals(null, result)
    }

    @Test
    fun `observe stream options emits converted options`() = runTest {
        every { source.id } returns "sid"
        every { localDataSource.observePrimarySource("itemId") } returns flowOf(source)
        every { localDataSource.observeStreams("sid") } returns flowOf(listOf(stream))
        every { converter.toStreamOptions(source, listOf(stream)) } returns streamOptions

        val result = repo.observeStreamOptions("itemId").first()

        assertEquals(streamOptions, result)
    }

    @Test
    fun `observe stream options emits null then options when source becomes available`() = runTest {
        every { source.id } returns "sid"
        every { localDataSource.observePrimarySource("itemId") } returns flowOf(null, source)
        every { localDataSource.observeStreams("sid") } returns flowOf(listOf(stream))
        every { converter.toStreamOptions(source, listOf(stream)) } returns streamOptions

        val results = repo.observeStreamOptions("itemId").take(2).toList()

        assertEquals(null, results[0])
        assertEquals(streamOptions, results[1])
        assertNotEquals(results[0], results[1])
    }

    @Test
    fun `observe stream options switches when source changes`() = runTest {
        val source2 = mockk<MediaItemSource>()
        val stream2 = mockk<MediaItemStream>()
        val options2 = mockk<MediaItemStreamOptions>()

        every { source.id } returns "sid"
        every { source2.id } returns "sid2"
        every { localDataSource.observePrimarySource("itemId") } returns flowOf(source, source2)
        every { localDataSource.observeStreams("sid") } returns flowOf(listOf(stream))
        every { localDataSource.observeStreams("sid2") } returns flowOf(listOf(stream2))
        every { converter.toStreamOptions(source, listOf(stream)) } returns streamOptions
        every { converter.toStreamOptions(source2, listOf(stream2)) } returns options2

        val results = repo.observeStreamOptions("itemId").take(2).toList()

        assertEquals(streamOptions, results[0])
        assertEquals(options2, results[1])
        assertNotEquals(results[0], results[1])
    }

    @Test
    fun `get source returns success`() = runTest {
        coEvery { localDataSource.getSourceById("sid") } returns source

        val result = repo.getSource("sid")

        assertEquals(DataResult.Success(source), result)
    }

    @Test
    fun `get source returns network error on io exception`() = runTest {
        coEvery { localDataSource.getSourceById("sid") } throws IOException("network failure")

        val result = repo.getSource("sid")

        assertEquals(DataResult.Error(DataLoadError.NetworkError("network failure")), result)
    }

    @Test
    fun `get source returns unknown error on exception`() = runTest {
        coEvery { localDataSource.getSourceById("sid") } throws RuntimeException("unknown failure")

        val result = repo.getSource("sid")

        assertEquals(DataResult.Error(DataLoadError.UnknownError("unknown failure")), result)
    }

    @Test(expected = CancellationException::class)
    fun `get source propagates cancellation exception`() = runTest {
        coEvery { localDataSource.getSourceById("sid") } throws CancellationException()
        repo.getSource("sid")
    }

    @Test
    fun `refresh list fetches batch with single query`() = runTest {
        coEvery { remoteDataSource.fetchBatch(listOf(query)) } returns mapOf(query to listOf(mediaItemResponse))
        coEvery { localDataSource.replaceMediaList("cacheKey", listOf(mediaItemResponse)) } just Runs

        val result = repo.refreshList(query)

        assertEquals(DataResult.Success(Unit), result)
        coVerify { localDataSource.replaceMediaList("cacheKey", listOf(mediaItemResponse)) }
    }

    @Test
    fun `refresh lists returns success after replacing all lists`() = runTest {
        val query2 = mockk<MediaItemQuery> { every { cacheKey } returns "key2" }
        val response2 = mockk<MediaItemResponse>()
        val map = mapOf(query to listOf(mediaItemResponse), query2 to listOf(response2))

        coEvery { remoteDataSource.fetchBatch(listOf(query, query2)) } returns map
        coEvery { localDataSource.replaceMediaList("cacheKey", listOf(mediaItemResponse)) } just Runs
        coEvery { localDataSource.replaceMediaList("key2", listOf(response2)) } just Runs

        val result = repo.refreshLists(listOf(query, query2))

        assertEquals(DataResult.Success(Unit), result)
        coVerify { localDataSource.replaceMediaList("cacheKey", listOf(mediaItemResponse)) }
        coVerify { localDataSource.replaceMediaList("key2", listOf(response2)) }
    }

    @Test
    fun `refresh lists returns network error on io exception`() = runTest {
        coEvery { remoteDataSource.fetchBatch(listOf(query)) } throws IOException("net error")

        val result = repo.refreshLists(listOf(query))

        assertEquals(DataResult.Error(DataLoadError.NetworkError("net error")), result)
    }

    @Test
    fun `refresh lists returns unknown error on exception`() = runTest {
        coEvery { remoteDataSource.fetchBatch(listOf(query)) } throws RuntimeException("runtime error")

        val result = repo.refreshLists(listOf(query))

        assertEquals(DataResult.Error(DataLoadError.UnknownError("runtime error")), result)
    }

    @Test
    fun `refresh item fetches and updates details`() = runTest {
        coEvery { remoteDataSource.fetchItemDetails("id") } returns mediaItemResponse
        coEvery { localDataSource.updateItemDetails("id", mediaItemResponse) } just Runs

        val result = repo.refreshItem("id")

        assertEquals(DataResult.Success(Unit), result)
        coVerify { localDataSource.updateItemDetails("id", mediaItemResponse) }
    }

    @Test
    fun `refresh item returns network error on io exception`() = runTest {
        coEvery { remoteDataSource.fetchItemDetails("id") } throws IOException("network")

        val result = repo.refreshItem("id")

        assertEquals(DataResult.Error(DataLoadError.NetworkError("network")), result)
    }

    @Test
    fun `refresh item returns unknown error on exception`() = runTest {
        coEvery { remoteDataSource.fetchItemDetails("id") } throws RuntimeException("error")

        val result = repo.refreshItem("id")

        assertEquals(DataResult.Error(DataLoadError.UnknownError("error")), result)
    }
}