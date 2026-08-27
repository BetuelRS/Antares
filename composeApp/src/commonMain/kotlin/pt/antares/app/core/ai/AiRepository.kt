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
import pt.antares.app.core.util.currentMinuteOfDay
import pt.antares.app.core.util.todayEpochDay

/**
 * Entre os ecrãs e o [AiClient]. Recebe funções em vez de repositórios para não depender
 * da base de dados: o que a AI devolve tem de poder ser mostrado e descartado sem nunca
 * chegar a ser gravado — só o [confirmFood] escreve, e só depois de a pessoa confirmar.
 */
class AiRepository(
    private val client: AiClient,

    // Garante que há sessão antes de cada chamada. A conta é anónima e cria-se sozinha:
    // existe para o servidor poder contar utilizações, não para identificar ninguém.
    private val ensureAccount: suspend () -> Unit,
    private val saveFoodLog: suspend (FoodLogEntity) -> Unit,
    private val latestWeightKg: suspend () -> Double?,

    private val persistUsage: suspend (AiUsage, String) -> Unit,

    // Grava a fotografia do prato e devolve o caminho, ou nulo se não deu. Nulo não é erro
    // nem cancela o registo: os números são o registo, e a foto é só o retrato dele.
    private val savePhoto: suspend (String, String) -> String? = { _, _ -> null },
    private val io: CoroutineDispatcher,
    private val lang: () -> String = { "pt" },
    private val json: Json = Json,
    private val newId: () -> String = { Ids.newUuid() },
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {

    // A contagem que o servidor devolve em cada resposta. Nula até à primeira chamada do
    // arranque: a app não adivinha o que resta, mostra o que lhe foi dito.
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
        // O peso vai junto porque o gasto de um exercício depende dele. Sem pesagem usa-se
        // um valor de recurso: uma estimativa aproximada é melhor do que recusar o pedido.
        val weight = latestWeightKg() ?: DEFAULT_WEIGHT_KG
        client.analyzeExercise(text, weight, lang(), today())
            .onSuccess { rememberUsage(it.usage) }
    }

    private suspend fun rememberUsage(usage: AiUsage) {
        _usage.value = usage
        persistUsage(usage, today())
    }

    /**
     * Grava o que a pessoa confirmou. Único ponto deste ficheiro que escreve na base, e
     * corre depois de o ecrã ter deixado rever e corrigir cada item.
     */
    suspend fun confirmFood(
        items: List<AiFoodItem>,
        mealSlot: MealSlot,
        epochDay: Long,
        origin: LogOrigin,

        // A fotografia analisada, em base64, quando veio de uma. Grava-se **uma vez** e o
        // caminho repete-se em todos os registos: uma foto de um prato dá tantos registos
        // quantos os alimentos que o modelo viu, e são todos a mesma imagem.
        photoBase64: String? = null,
    ) = withContext(io) {
        // Um instante só para todos os itens, para a refeição ficar junta na ordem do dia.
        val timestamp = now()

        val photoPath = photoBase64?.let { savePhoto(newId(), it) }
        items.forEach { item ->

            // A AI devolve os micronutrientes da porção; a base guarda-os por 100 g. Sem
            // esta conversão, editar as gramas depois escalava valores já escalados.
            val micros = item.micros
                ?.takeIf { it.isNotEmpty() && item.grams > 0 }
                ?.mapValues { (_, v) -> v * 100.0 / item.grams }
                ?.let { json.encodeToString(it) }

            saveFoodLog(
                FoodLogEntity(
                    id = newId(),
                    epochDay = epochDay,
                    mealSlot = mealSlot,
                    // Nulo enquanto for o que o modelo adivinhou; preenchido quando alguém
                    // trocou o item por um alimento do catálogo no ecrã de revisão.
                    foodId = item.foodId,
                    nameSnapshot = item.name,
                    quantityGrams = item.grams,
                    kcalSnapshot = item.kcal,
                    proteinSnapshot = item.protein,
                    carbsSnapshot = item.carbs,
                    fatSnapshot = item.fat,
                    microsPer100Json = micros,
                    origin = origin,

                    isLiquid = DrinkClassifier.isLiquid(item.name, item.name),
                    eatenAtMin = currentMinuteOfDay().takeIf { epochDay == todayEpochDay() },
                    photoPath = photoPath,
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
