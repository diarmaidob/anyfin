package io.github.diarmaidob.anyfin.core.mediaitem.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MediaItemResponse(
    @Json(name = "Id") val id: String,
    @Json(name = "Name") val name: String?,
    @Json(name = "OriginalTitle") val originalTitle: String?,
    @Json(name = "ServerId") val serverId: String?,
    @Json(name = "Etag") val etag: String?,
    @Json(name = "Type") val type: String?,
    @Json(name = "MediaType") val mediaType: String?,
    @Json(name = "IsFolder") val isFolder: Boolean?,

    @Json(name = "Overview") val overview: String?,
    @Json(name = "Taglines") val taglines: List<String>?,
    @Json(name = "Genres") val genres: List<String>?,
    @Json(name = "ProductionYear") val productionYear: Int?,
    @Json(name = "PremiereDate") val premiereDate: String?,
    @Json(name = "EndDate") val endDate: String?,
    @Json(name = "DateCreated") val dateCreated: String?,
    @Json(name = "OfficialRating") val officialRating: String?,
    @Json(name = "CriticRating") val criticRating: Int?,
    @Json(name = "CommunityRating") val communityRating: Double?,
    @Json(name = "CustomRating") val customRating: String?,
    @Json(name = "ProductionLocations") val productionLocations: List<String>?,
    @Json(name = "Path") val path: String?,

    @Json(name = "SeriesName") val seriesName: String?,
    @Json(name = "SeriesId") val seriesId: String?,
    @Json(name = "SeasonName") val seasonName: String?,
    @Json(name = "SeasonId") val seasonId: String?,
    @Json(name = "IndexNumber") val indexNumber: Int?,
    @Json(name = "ParentIndexNumber") val parentIndexNumber: Int?,
    @Json(name = "IndexNumberEnd") val indexNumberEnd: Int?,
    @Json(name = "ParentId") val parentId: String?,
    @Json(name = "CollectionType") val collectionType: String?,

    @Json(name = "Album") val album: String?,
    @Json(name = "AlbumId") val albumId: String?,
    @Json(name = "AlbumArtist") val albumArtist: String?,
    @Json(name = "Artists") val artists: List<String>?,
    @Json(name = "ArtistItems") val artistItemResponses: List<ArtistItemResponse>?,

    @Json(name = "RunTimeTicks") val runTimeTicks: Long?,
    @Json(name = "Container") val container: String?,
    @Json(name = "Video3DFormat") val video3DFormat: String?,
    @Json(name = "AspectRatio") val aspectRatio: String?,
    @Json(name = "MediaSources") val mediaSourceResponses: List<MediaSourceResponse>?,
    @Json(name = "PlayAccess") val playAccess: String?,
    @Json(name = "CanDownload") val canDownload: Boolean?,

    @Json(name = "People") val people: List<PersonResponse>?,

    @Json(name = "ImageTags") val imageTags: Map<String, String>?,
    @Json(name = "BackdropImageTags") val backdropImageTags: List<String>?,
    @Json(name = "ParentLogoImageTag") val parentLogoImageTag: String?,
    @Json(name = "ParentThumbImageTag") val parentThumbImageTag: String?,
    @Json(name = "SeriesPrimaryImageTag") val seriesPrimaryImageTag: String?,
    @Json(name = "ParentBackdropImageTags") val parentBackdropImageTags: List<String>?,

    @Json(name = "SortName") val sortName: String?,
    @Json(name = "ForcedSortName") val forcedSortName: String?,
    @Json(name = "DisplayPreferencesId") val displayPreferencesId: String?,

    @Json(name = "UserData") val userData: UserDataResponse?,

    @Json(name = "ProviderIds") val providerIds: Map<String, String>?,
    @Json(name = "Studios") val studios: List<StudioItemResponse>?,

    @Json(name = "Chapters") val chapterResponses: List<ChapterResponse>?,
    @Json(name = "PartCount") val partCount: Int?,
    @Json(name = "ChildCount") val childCount: Int?,
    @Json(name = "RecursiveItemCount") val recursiveItemCount: Int?,
    @Json(name = "SpecialFeatureCount") val specialFeatureCount: Int?,
    @Json(name = "LocalTrailerCount") val localTrailerCount: Int?
)

@JsonClass(generateAdapter = true)
data class UserDataResponse(
    @Json(name = "Played") val isPlayed: Boolean?,
    @Json(name = "PlaybackPositionTicks") val playbackPositionTicks: Long?,
    @Json(name = "PlayedPercentage") val playedPercentage: Double?,
    @Json(name = "IsFavorite") val isFavorite: Boolean?,
    @Json(name = "LastPlayedDate") val lastPlayedDate: String?,
    @Json(name = "PlayCount") val playCount: Int?,
    @Json(name = "UnplayedItemCount") val unplayedItemCount: Int?,
    @Json(name = "Key") val key: String?
)

@JsonClass(generateAdapter = true)
data class MediaSourceResponse(
    @Json(name = "Id") val id: String,
    @Json(name = "Container") val container: String?,
    @Json(name = "Protocol") val protocol: String?,
    @Json(name = "Path") val path: String?,
    @Json(name = "Name") val name: String?,
    @Json(name = "IsRemote") val isRemote: Boolean?,
    @Json(name = "RunTimeTicks") val runTimeTicks: Long?,
    @Json(name = "Bitrate") val bitrate: Long?,
    @Json(name = "MediaStreams") val mediaStreamResponses: List<MediaStreamResponse>?,
    @Json(name = "TranscodingUrl") val transcodingUrl: String?,
    @Json(name = "TranscodingSubProtocol") val transcodingSubProtocol: String?,
    @Json(name = "TranscodingContainer") val transcodingContainer: String?,
    @Json(name = "SupportsTranscoding") val supportsTranscoding: Boolean?,
    @Json(name = "SupportsDirectStream") val supportsDirectStream: Boolean?,
    @Json(name = "SupportsDirectPlay") val supportsDirectPlay: Boolean?
)

@JsonClass(generateAdapter = true)
data class MediaStreamResponse(
    @Json(name = "Codec") val codec: String?,
    @Json(name = "Language") val language: String?,
    @Json(name = "TimeBase") val timeBase: String?,
    @Json(name = "VideoRange") val videoRange: String?,
    @Json(name = "DisplayTitle") val displayTitle: String?,
    @Json(name = "IsInterlaced") val isInterlaced: Boolean?,
    @Json(name = "BitRate") val bitRate: Long?,
    @Json(name = "Channels") val channels: Int?,
    @Json(name = "SampleRate") val sampleRate: Int?,
    @Json(name = "Width") val width: Int?,
    @Json(name = "Height") val height: Int?,
    @Json(name = "AspectRatio") val aspectRatio: String?,
    @Json(name = "Index") val index: Int?,
    @Json(name = "IsDefault") val isDefault: Boolean?,
    @Json(name = "IsForced") val isForced: Boolean?,
    @Json(name = "Type") val type: String?
)

@JsonClass(generateAdapter = true)
data class PersonResponse(
    @Json(name = "Name") val name: String,
    @Json(name = "Id") val id: String,
    @Json(name = "Role") val role: String?,
    @Json(name = "Type") val type: String?,
    @Json(name = "PrimaryImageTag") val primaryImageTag: String?
)

@JsonClass(generateAdapter = true)
data class ChapterResponse(
    @Json(name = "StartPositionTicks") val startPositionTicks: Long,
    @Json(name = "Name") val name: String?,
    @Json(name = "ImageTag") val imageTag: String?
)

@JsonClass(generateAdapter = true)
data class StudioItemResponse(
    @Json(name = "Id") val id: String,
    @Json(name = "Name") val name: String?
)

@JsonClass(generateAdapter = true)
data class ArtistItemResponse(
    @Json(name = "Id") val id: String,
    @Json(name = "Name") val name: String?
)