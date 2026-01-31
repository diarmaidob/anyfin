package io.github.diarmaidob.anyfin.core.common


sealed class DataResult<out T> {
    data class Success<T>(val data: T) : DataResult<T>()
    data class Error(val error: DataLoadError) : DataResult<Nothing>()
}

inline fun <T> DataResult<T>.onSuccess(action: (T) -> Unit): DataResult<T> {
    if (this is DataResult.Success) action(data)
    return this
}

inline fun <T> DataResult<T>.onError(action: (DataLoadError) -> Unit): DataResult<T> {
    if (this is DataResult.Error) action(error)
    return this
}

fun <T> DataResult<T>.getOrNull(): T? = when (this) {
    is DataResult.Success -> data
    is DataResult.Error -> null
}

@PublishedApi
internal class DataResultControlException(val error: DataLoadError) : RuntimeException()

inline fun <T> resultScope(block: ResultScope.() -> T): DataResult<T> {
    val scope = ResultScope()
    return try {
        DataResult.Success(scope.block())
    } catch (e: DataResultControlException) {
        DataResult.Error(error = e.error)
    }
}

class ResultScope {

    /**
     * Unwraps a DataResult.
     * Use this when calling Repositories.
     */
    fun <T> DataResult<T>.getOrThrow(): T {
        return when (this) {
            is DataResult.Success -> this.data
            is DataResult.Error -> throw DataResultControlException(this.error)
        }
    }

    /**
     * Checks if a value is null.
     * Use this for validation logic (e.g. Session check, DB item check).
     */
    fun <T> T?.ensureNotNull(error: DataLoadError): T {
        return this ?: throw DataResultControlException(error)
    }
}