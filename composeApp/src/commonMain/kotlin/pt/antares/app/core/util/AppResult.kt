package pt.antares.app.core.util

sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

inline fun <T> AppResult<T>.onSuccess(block: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) block(value)
    return this
}

inline fun <T> AppResult<T>.onFailure(block: (AppError) -> Unit): AppResult<T> {
    if (this is AppResult.Failure) block(error)
    return this
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(value))
    is AppResult.Failure -> this
}

fun Throwable.toAppError(): AppError = when (this) {
    is NoSuchElementException -> AppError.NotFound
    is IllegalStateException -> if (message?.contains("quota", ignoreCase = true) == true) {
        AppError.QuotaExceeded
    } else {
        AppError.Unknown(message ?: "Unknown state error")
    }
    is SecurityException -> AppError.Unauthorized
    is kotlinx.serialization.SerializationException -> AppError.Parsing(message ?: "Parsing error")
    else -> {
        val name = this::class.simpleName.orEmpty()
        if (name.contains("IO") || name.contains("Connect") || name.contains("Timeout")) {
            AppError.Network
        } else {
            AppError.Unknown(message ?: name)
        }
    }
}

suspend fun <T> runCatchingAppResult(block: suspend () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (t: Throwable) {
    AppResult.Failure(t.toAppError())
}
