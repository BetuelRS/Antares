package pt.antares.app.feature.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import pt.antares.app.core.calc.RecipeNutrition
import pt.antares.app.core.database.entities.RecipeStepEntity
import pt.antares.app.core.model.MealSlot
import pt.antares.app.core.model.LifeStage
import pt.antares.app.core.model.Sex
import pt.antares.app.core.nutrition.EfsaReference
import pt.antares.app.core.nutrition.Nutrients
import pt.antares.app.core.nutrition.NutritionBreakdown
import pt.antares.app.core.nutrition.NutritionFacts
import pt.antares.app.feature.profile.data.ProfileRepository
import pt.antares.app.feature.stats.NutritionStatsRepository
import kotlin.math.roundToInt

data class RecipePortionState(
    val loading: Boolean = true,
    val name: String = "",
    val nutrition: RecipeNutrition = RecipeNutrition(0, 0.0, 0.0, 0.0, 0.0),
    val quantityText: String = "100",
    val saved: Boolean = false,
    val reference: EfsaReference? = null,
    val sex: Sex = Sex.MALE,

    val lifeStage: LifeStage? = null,

    /** Quanto pesa uma dose, ou `null` na receita que não diz em quantas se divide. */
    val gramsPerServing: Double? = null,

    /** Com doses, o campo conta doses. Sem elas, conta gramas — como sempre contou. */
    val byServings: Boolean = false,

    /**
     * Os passos de preparação, pela ordem em que se fazem.
     *
     * Estão aqui e não só no ecrã de edição porque é aqui que se lê a receita com o tacho ao
     * lume — o ecrã de edição é para a escrever, e ninguém cozinha com um formulário aberto.
     */
    val passos: List<RecipeStepEntity> = emptyList(),
) {
    val quantityGrams: Double?
        get() {
            val escrito = quantityText.replace(',', '.').toDoubleOrNull() ?: return null
            val gramas = if (byServings) escrito * (gramsPerServing ?: return null) else escrito
            return gramas.takeIf { it in 1.0..MAX_GRAMAS }
        }
    val previewKcal: Int get() = scale(nutrition.kcalPer100.toDouble()).roundToInt()
    val previewP: Double get() = scale(nutrition.proteinPer100)
    val previewC: Double get() = scale(nutrition.carbsPer100)
    val previewF: Double get() = scale(nutrition.fatPer100)

    val previewFiber: Double? get() = scaleOrNull(nutrition.fiberPer100)
    val previewSugar: Double? get() = scaleOrNull(nutrition.sugarsPer100)
    val previewSatFat: Double? get() = scaleOrNull(nutrition.satFatPer100)
    val previewSodiumMg: Double? get() = scaleOrNull(nutrition.sodiumMgPer100)

    val breakdown: NutritionBreakdown?
        get() {
            val q = quantityGrams ?: return null
            val per100 = buildMap {
                putAll(nutrition.microsPer100)
                nutrition.sugarsPer100?.let { put(Nutrients.SUGARS, it) }
                nutrition.fiberPer100?.let { put(Nutrients.FIBER, it) }
                nutrition.satFatPer100?.let { put(Nutrients.SAT_FAT, it) }
                nutrition.sodiumMgPer100?.let { put(Nutrients.SODIUM, it) }
            }
            if (per100.isEmpty()) return null
            return NutritionFacts.build(per100, q, reference, sex, lifeStage).takeIf { !it.isEmpty }
        }

    private fun scale(per100: Double): Double = (quantityGrams ?: 0.0) * per100 / 100.0

    private fun scaleOrNull(per100: Double?): Double? {
        val q = quantityGrams ?: return null
        return (per100 ?: return null) * q / 100.0
    }
}

/**
 * Quanto pesa uma dose, ou `null` na receita que não diz em quantas se divide.
 *
 * Fora do ViewModel porque o `load` não é testável: lê a referência da EFSA pelos recursos
 * do Compose, que não existem num teste de unidade. A conta é que interessa guardar.
 */
fun gramasPorDose(doses: Int?, basisGrams: Double): Double? =
    doses?.takeIf { it > 0 }?.let { n -> basisGrams.takeIf { it > 0 }?.div(n) }

/**
 * Trocar entre doses e gramas leva o valor consigo: quem escreveu duas doses e muda para
 * gramas vê as gramas dessas duas doses, e não o campo a começar do zero.
 */
fun RecipePortionState.trocarUnidade(): RecipePortionState {
    val porDose = gramsPerServing ?: return this
    val gramas = quantityGrams
    val paraDoses = !byServings
    return copy(
        byServings = paraDoses,
        quantityText = when {
            gramas == null -> quantityText
            paraDoses -> formatDoses(gramas / porDose)
            else -> gramas.roundToInt().toString()
        },
    )
}

// Cinco quilos de uma vez não é uma refeição; é um engano a caminho do histórico.
private const val MAX_GRAMAS = 5000.0

// Meia dose conta, um quarto de dose é precisão inventada.
private const val MEIA_DOSE = 2

private fun formatDoses(doses: Double): String {
    val metades = (doses * MEIA_DOSE).roundToInt().coerceAtLeast(1)
    val valor = metades.toDouble() / MEIA_DOSE
    return if (valor == valor.toInt().toDouble()) valor.toInt().toString() else valor.toString()
}

class RecipeDetailViewModel(
    private val repository: RecipeRepository,
    private val profileRepository: ProfileRepository,
    private val statsRepository: NutritionStatsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RecipePortionState())
    val state: StateFlow<RecipePortionState> = _state

    private var recipeId: String? = null

    fun load(recipeId: String) {
        this.recipeId = recipeId
        viewModelScope.launch {
            val recipe = repository.recipeById(recipeId)
            val nutrition = repository.nutrition(recipeId)
            val reference = statsRepository.loadReference()
            val perfil = profileRepository.observeProfile().first()
            val sex = perfil?.sex ?: Sex.MALE
            val stage = perfil?.lifeStage
            // Com doses declaradas, o campo abre em **uma** dose. Antes abria com o peso
            // inteiro da receita: registar uma lasanha propunha comê-la toda.
            val porDose = gramasPorDose(recipe?.servings, nutrition.basisGrams)

            _state.update {
                it.copy(
                    loading = false,
                    name = recipe?.name.orEmpty(),
                    nutrition = nutrition,
                    gramsPerServing = porDose,
                    byServings = porDose != null,
                    quantityText = if (porDose != null) {
                        "1"
                    } else {
                        recipe?.yieldGrams?.roundToInt()?.toString() ?: "100"
                    },
                    reference = reference,
                    sex = sex,
                    lifeStage = stage,
                    passos = repository.stepsOf(recipeId),
                )
            }
        }
    }

    fun setQuantity(text: String) = _state.update {
        it.copy(quantityText = text.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }.take(6))
    }

    fun toggleByServings() = _state.update { it.trocarUnidade() }

    fun save(slot: MealSlot, epochDay: Long) {
        val id = recipeId ?: return
        val grams = _state.value.quantityGrams ?: return
        viewModelScope.launch {
            repository.logRecipe(id, grams, slot, epochDay)
            _state.update { it.copy(saved = true) }
        }
    }
}
