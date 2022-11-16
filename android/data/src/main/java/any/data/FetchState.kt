package any.data

import androidx.compose.runtime.Stable

@Stable
sealed class FetchState<out T> {
    @Stable
    class Loading(val progress: Float, val message: String?) : FetchState<Nothing>()

    @Stable
    class Success<T>(val value: T, val isRemote: Boolean) : FetchState<T>()

    @Stable
    class Failure(val error: Throwable) : FetchState<Nothing>()

    fun valueOrThrow(): T {
        return (this as Success).value
    }

    fun valueOrNull(): T? {
        return (this as? Success)?.value
    }

    fun errorOrThrow(): Throwable {
        return (this as Failure).error
    }

    fun errorOrNull(): Throwable? {
        return (this as? Failure)?.error
    }

    companion object {
        fun <T> success(value: T, isRemote: Boolean) = Success(value, isRemote)

        fun failure(error: Throwable) = Failure(error)

        fun loading(progress: Float, message: String?) = Loading(progress, message)
    }
}
