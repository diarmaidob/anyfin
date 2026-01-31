package io.github.diarmaidob.anyfin.core.mediaitem.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.QueryMap

interface MediaItemApi {
    @GET("Users/{userId}/Items/Resume")
    suspend fun getResumeItems(
        @Path("userId") userId: String,
        @QueryMap params: Map<String, String>
    ): Response<MediaItemsResponse<MediaItemResponse>>

    @GET("Shows/NextUp")
    suspend fun getNextUpItems(
        @QueryMap params: Map<String, String>
    ): Response<MediaItemsResponse<MediaItemResponse>>

    @GET("Users/{userId}/Items/Latest")
    suspend fun getLatestItems(
        @Path("userId") userId: String,
        @QueryMap params: Map<String, String>
    ): Response<List<MediaItemResponse>>

    @GET("Users/{userId}/Items")
    suspend fun getItems(
        @Path("userId") userId: String,
        @QueryMap params: Map<String, String>
    ): Response<MediaItemsResponse<MediaItemResponse>>

    @GET("Users/{userId}/Items/{itemId}")
    suspend fun getItemDetails(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String
    ): Response<MediaItemResponse>

    @GET("Users/{userId}/Views")
    suspend fun getUserViews(
        @Path("userId") userId: String
    ): Response<MediaItemsResponse<MediaItemResponse>>
}