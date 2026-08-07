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
import pt.antares.app.core.calc.WeeklyAggregate
import pt.antares.app.core.network.supabase.SupabaseContainer
import pt.antares.app.core.util.AppError
import pt.antares.app.core.util.AppResult

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

    private suspend inline fun <reified B : Any, reified T> call(fn: String, body: B): AppResult<T> =
        withContext(io) {
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

internal fun Throwable.toAiError(): AppError {
    val status = when (this) {
        is RestException -> statusCode
        is ResponseException -> response.status.value
        else -> null
    }
    return when (status) {

        402, 429 -> AppError.QuotaExceeded
        401, 403 -> AppError.Unauthorized

        503 -> AppError.AiPaused
        null -> AppError.Network
        else -> AppError.Unknown("ai $status")
    }
}
