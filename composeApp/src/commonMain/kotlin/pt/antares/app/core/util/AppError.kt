package pt.antares.app.core.util

/**
 * Os erros que a app sabe explicar. É um conjunto fechado de propósito: cada um tem uma
 * frase e um caminho de saída no ecrã, e `Unknown` é o que sobra quando não há nem uma nem
 * outro. A mensagem que aqui viaja é para diagnóstico e nunca se mostra a ninguém.
 */
sealed interface AppError {
    data object Network : AppError
    data object NotFound : AppError
    data object Unauthorized : AppError
    data object QuotaExceeded : AppError

    data object AiPaused : AppError

    data class Parsing(val message: String) : AppError
    data class Unknown(val message: String) : AppError
}
