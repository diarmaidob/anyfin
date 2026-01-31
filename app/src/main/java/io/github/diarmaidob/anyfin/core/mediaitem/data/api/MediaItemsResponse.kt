package io.github.diarmaidob.anyfin.core.mediaitem.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MediaItemsResponse<T>(
    @Json(name = "Items") val items: List<T>,
    @Json(name = "TotalRecordCount") val totalRecordCount: Int,
    @Json(name = "StartIndex") val startIndex: Int? = 0
)