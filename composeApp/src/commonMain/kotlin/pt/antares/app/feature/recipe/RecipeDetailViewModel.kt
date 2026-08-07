package pt.antares.app.feature.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import pt.antares.app.core.calc.RecipeNutrition
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
) {
    val quantityGrams: Double? get() = quantityText.replace(',', '.').toDoubleOrNull()?.takeIf { it in 1.0..5000.0 }
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
            _state.update {
                it.copy(
                    loading = false,
                    name = recipe?.name.orEmpty(),
                    nutrition = nutrition,
                    quantityText = recipe?.yieldGrams?.roundToInt()?.toString() ?: "100",
                    reference = reference,
                    sex = sex,
                    lifeStage = stage,
                )
            }
        }
    }

    fun setQuantity(text: String) = _state.update {
        it.copy(quantityText = text.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }.take(6))
    }

    fun save(slot: MealSlot, epochDay: Long) {
        val id = recipeId ?: return
        val grams = _state.value.quantityGrams ?: return
        viewModelScope.launch {
            repository.logRecipe(id, grams, slot, epochDay)
            _state.update { it.copy(saved = true) }
        }
    }
}
