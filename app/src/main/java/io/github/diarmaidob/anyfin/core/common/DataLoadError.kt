package io.github.diarmaidob.anyfin.core.common

sealed class DataLoadError : Exception() {
    data class AuthError(override val message: String) : DataLoadError()
    data class HttpError(val code: Int, override val message: String) : DataLoadError()
    data class NetworkError(override val message: String) : DataLoadError()
    data class DatabaseError(override val message: String) : DataLoadError()
    data class UnknownError(override val message: String) : DataLoadError()

    data class ItemNotFoundError(val itemId: String) : DataLoadError() {
        override val message: String = "Item not found: $itemId"
    }
}