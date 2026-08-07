package pt.antares.app.core.util

sealed interface AppError {
    data object Network : AppError
    data object NotFound : AppError
    data object Unauthorized : AppError
    data object QuotaExceeded : AppError

    data object AiPaused : AppError

    data class Parsing(val message: String) : AppError
    data class Unknown(val message: String) : AppError
}
