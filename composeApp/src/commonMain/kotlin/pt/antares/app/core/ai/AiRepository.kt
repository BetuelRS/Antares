package pt.antares.app.core.ai

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.serialization.json.Json
import pt.antares.app.core.database.entities.FoodLogEntity
import pt.antares.app.core.fooddata.DrinkClassifier
import pt.antares.app.core.model.LogOrigin
import pt.antares.app.core.model.MealSlot
import pt.antares.app.core.util.AppResult
import pt.antares.app.core.util.Ids
import pt.antares.app.core.util.onSuccess
import kotlin.math.roundToInt

class AiRepository(
    private val client: AiClient,

    private val ensureAccount: suspend () -> Unit,
    private val saveFoodLog: suspend (FoodLogEntity) -> Unit,
    private val latestWeightKg: suspend () -> Double?,

    private val persistUsage: suspend (AiUsage, String) -> Unit,
    private val io: CoroutineDispatcher,
    private val lang: () -> String = { "pt" },
    private val json: Json = Json,
    private val newId: () -> String = { Ids.newUuid() },
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {

    private val _usage = MutableStateFlow<AiUsage?>(null)

    val usage: StateFlow<AiUsage?> = _usage

    private fun today(): String = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()

    suspend fun analyzeText(text: String): AppResult<FoodAnalysis> = withContext(io) {
        ensureAccount()
        client.analyzeFoodText(text, lang(), today()).onSuccess { rememberUsage(it.usage) }
    }

    suspend fun analyzePhoto(imageBase64: String, mime: String = "image/jpeg"): AppResult<FoodAnalysis> =
        withContext(io) {
            ensureAccount()
            client.analyzeFoodPhoto(imageBase64, mime, lang(), today())
                .onSuccess { rememberUsage(it.usage) }
        }

    suspend fun readLabel(imageBase64: String, mime: String = "image/jpeg"): AppResult<LabelAnalysis> =
        withContext(io) {
            ensureAccount()
            client.readLabel(imageBase64, mime, lang(), today())
                .onSuccess { rememberUsage(it.usage) }
        }

    suspend fun analyzeExercise(text: String): AppResult<ExerciseAnalysis> = withContext(io) {
        ensureAccount()
        val weight = latestWeightKg() ?: DEFAULT_WEIGHT_KG
        client.analyzeExercise(text, weight, lang(), today())
            .onSuccess { rememberUsage(it.usage) }
    }

    private suspend fun rememberUsage(usage: AiUsage) {
        _usage.value = usage
        persistUsage(usage, today())
    }

    suspend fun confirmFood(
        items: List<AiFoodItem>,
        mealSlot: MealSlot,
        epochDay: Long,
        origin: LogOrigin,
    ) = withContext(io) {
        val timestamp = now()
        items.forEach { item ->

            val micros = item.micros
                ?.takeIf { it.isNotEmpty() && item.grams > 0 }
                ?.mapValues { (_, v) -> v * 100.0 / item.grams }
                ?.let { json.encodeToString(it) }

            saveFoodLog(
                FoodLogEntity(
                    id = newId(),
                    epochDay = epochDay,
                    mealSlot = mealSlot,
                    foodId = null,
                    nameSnapshot = item.name,
                    quantityGrams = item.grams,
                    kcalSnapshot = item.kcal,
                    proteinSnapshot = item.protein,
                    carbsSnapshot = item.carbs,
                    fatSnapshot = item.fat,
                    microsPer100Json = micros,
                    origin = origin,

                    isLiquid = DrinkClassifier.isLiquid(item.name, item.name),
                    updatedAt = timestamp,
                ),
            )
        }
    }

    companion object {
        const val DEFAULT_WEIGHT_KG = 70.0
    }
}

fun AiFoodItem.withGrams(newGrams: Double): AiFoodItem {
    if (grams <= 0 || newGrams <= 0) return this
    val f = newGrams / grams
    return copy(
        grams = newGrams,
        kcal = (kcal * f).roundToInt(),
        protein = protein * f,
        carbs = carbs * f,
        fat = fat * f,
        micros = micros?.mapValues { (_, v) -> v * f },
    )
}
