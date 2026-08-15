package pt.antares.app.feature.fooddata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.antares.app.core.model.Sex
import pt.antares.app.core.nutrition.NutrientDensity
import pt.antares.app.core.nutrition.NutrientRich
import pt.antares.app.core.nutrition.Nutrients
import pt.antares.app.feature.profile.data.ProfileRepository
import pt.antares.app.feature.stats.NutritionStatsRepository
import pt.antares.app.core.nutrition.microsDeJson

data class RichInState(
    val loading: Boolean = false,

    val key: String? = null,
    val results: List<NutrientRich> = emptyList(),
)

class RichInViewModel(
    private val foodRepository: FoodRepository,
    private val profileRepository: ProfileRepository,
    private val statsRepository: NutritionStatsRepository,
) : ViewModel() {


    private val _state = MutableStateFlow(RichInState())
    val state: StateFlow<RichInState> = _state

    suspend fun searchableKeys(): List<String> =
        statsRepository.loadReference().all().map { it.key }

            .filter { (it in Nutrients.VITAMINS || it in Nutrients.MINERALS) && it != Nutrients.SODIUM }

    fun pick(key: String) {
        _state.update { it.copy(loading = true, key = key, results = emptyList()) }
        viewModelScope.launch {
            val perfil = profileRepository.observeProfile().first()
            val sex = perfil?.sex ?: Sex.MALE
            val drv = statsRepository.loadReference().forKey(key)?.forPerson(sex, perfil?.lifeStage) ?: 0.0
            val candidates = foodRepository.foodsWithNutrient(key, drv, NutrientDensity.LIST_LIMIT)

            val micros = candidates.associate { food ->
                food.id to microsDeJson(food.microsJson)
            }
            val ranked = NutrientDensity.rank(
                foods = candidates.map { Triple(it.id, it.namePt.ifBlank { it.nameEn }, it.kcal) },
                microsByFood = micros,
                key = key,
                drv = drv,
            )
            _state.update { it.copy(loading = false, results = ranked) }
        }
    }

    fun clear() = _state.update { RichInState() }
}
