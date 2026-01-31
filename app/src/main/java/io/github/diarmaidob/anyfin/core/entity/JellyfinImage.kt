package io.github.diarmaidob.anyfin.core.entity

sealed interface JellyfinImage {
    val itemId: String
    val tag: String?

    data class Primary(override val itemId: String, override val tag: String?) : JellyfinImage
    data class Backdrop(override val itemId: String, override val tag: String?) : JellyfinImage
    data class Logo(override val itemId: String, override val tag: String?) : JellyfinImage
    data class Thumb(override val itemId: String, override val tag: String?) : JellyfinImage
}