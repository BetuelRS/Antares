package pt.antares.app.core.ai

import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import pt.antares.app.core.network.supabase.SupabaseContainer
import pt.antares.app.core.util.AppError
import pt.antares.app.core.util.AppResult

/**
 * As quatro únicas coisas que a app pede a um modelo de linguagem: descrever comida a
 * partir de texto ou de foto, ler um rótulo, e estimar o gasto de um exercício descrito
 * por palavras. Nada de metas, treinos ou conselhos — essas contas correm no telemóvel.
 *
 * O `day` viaja em todas as chamadas porque a contagem de utilizações é diária e é feita
 * do lado do servidor, onde o cliente não a pode enganar.
 */
interface AiClient {
    suspend fun analyzeFoodText(text: String, lang: String, day: String): AppResult<FoodAnalysis>

    suspend fun analyzeFoodPhoto(
        imageBase64: String,
        mime: String,
        lang: String,
        day: String,
    ): AppResult<FoodAnalysis>

    suspend fun readLabel(
        imageBase64: String,
        mime: String,
        lang: String,
        day: String,
    ): AppResult<LabelAnalysis>

    suspend fun analyzeExercise(
        text: String,
        weightKg: Double,
        lang: String,
        day: String,
    ): AppResult<ExerciseAnalysis>

}

class SupabaseAiClient(
    private val container: SupabaseContainer,
    private val io: CoroutineDispatcher,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : AiClient {

    override suspend fun analyzeFoodText(text: String, lang: String, day: String) =
        call<FoodAnalysisRequest, FoodAnalysis>(
            fn = FN_FOOD,
            body = FoodAnalysisRequest(mode = "text", text = text, lang = lang, day = day),
        )

    override suspend fun analyzeFoodPhoto(imageBase64: String, mime: String, lang: String, day: String) =
        call<FoodAnalysisRequest, FoodAnalysis>(
            fn = FN_FOOD,
            body = FoodAnalysisRequest(
                mode = "photo",
                imageBase64 = imageBase64,
                imageMime = mime,
                lang = lang,
                day = day,
            ),
        )

    override suspend fun readLabel(imageBase64: String, mime: String, lang: String, day: String) =
        call<FoodAnalysisRequest, LabelAnalysis>(
            fn = FN_FOOD,
            body = FoodAnalysisRequest(
                mode = "label",
                imageBase64 = imageBase64,
                imageMime = mime,
                lang = lang,
                day = day,
            ),
        )

    override suspend fun analyzeExercise(text: String, weightKg: Double, lang: String, day: String) =
        call<ExerciseAnalysisRequest, ExerciseAnalysis>(
            fn = FN_EXERCISE,
            body = ExerciseAnalysisRequest(text = text, weightKg = weightKg, lang = lang, day = day),
        )

    /**
     * A chamada em si. Nunca lança: qualquer falha sai como [AppResult.Failure], porque
     * quem chama está sempre a meio de um ecrã e tem de mostrar uma mensagem, não fechar.
     */
    private suspend inline fun <reified B : Any, reified T> call(fn: String, body: B): AppResult<T> =
        withContext(io) {
            // Sem cliente é porque não há sessão. A chave da AI vive no servidor e nunca no
            // telemóvel, por isso sem conta não há como pedir nada.
            val client = container.client
                ?: return@withContext AppResult.Failure(AppError.Unauthorized)
            try {
                val response: HttpResponse = client.functions(
                    function = fn,
                    body = body,
                    headers = Headers.build {
                        append(HttpHeaders.ContentType, "application/json")
                    },
                )
                AppResult.Success(json.decodeFromString<T>(response.body<String>()))
            } catch (e: Exception) {
                AppResult.Failure(e.toAiError())
            }
        }

    companion object {
        const val FN_FOOD = "analyze-food"
        const val FN_EXERCISE = "analyze-exercise"
    }
}

/**
 * Traduz a falha para algo que o ecrã saiba dizer. Cada estado leva a app a um caminho
 * diferente, e é por isso que não se agrupam num erro só.
 */
internal fun Throwable.toAiError(): AppError {
    // As duas bibliotecas embrulham o código de estado em sítios diferentes.
    val status = when (this) {
        is RestException -> statusCode
        is ResponseException -> response.status.value
        else -> null
    }
    return when (status) {

        // Limite atingido, seja o diário da pessoa ou o do serviço. Em ambos os casos a
        // resposta ao utilizador é a mesma: hoje não dá, amanhã dá.
        402, 429 -> AppError.QuotaExceeded
        401, 403 -> AppError.Unauthorized

        // O dono desligou a AI do lado do servidor. Distingue-se de uma avaria para o ecrã
        // não convidar a tentar outra vez.
        503 -> AppError.AiPaused
        // Sem código de estado nunca houve resposta: é rede, e vale a pena repetir.
        null -> AppError.Network
        else -> AppError.Unknown("ai $status")
    }
}
