package pt.antares.app.feature.fooddata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.antares.app.core.database.entities.FoodEntity
import pt.antares.app.core.model.MealSlot
import pt.antares.app.core.model.LifeStage
import pt.antares.app.core.model.Sex
import pt.antares.app.core.model.UnitSystem
import pt.antares.app.core.util.UnitConversions
import pt.antares.app.core.nutrition.EfsaReference
import pt.antares.app.core.nutrition.Nutrients
import pt.antares.app.core.nutrition.NutritionBreakdown
import pt.antares.app.core.nutrition.NutritionFacts
import pt.antares.app.feature.diary.DiaryRepository
import pt.antares.app.feature.profile.data.ProfileRepository
import pt.antares.app.feature.stats.NutritionStatsRepository
import kotlin.math.roundToInt
import pt.antares.app.core.nutrition.microsDeJson

data class PortionState(
    val loading: Boolean = true,
    val food: FoodEntity? = null,

    // O favorito e a última porção deixaram de viver na linha do alimento na v27. Vêm à
    // parte porque são da pessoa, e o alimento é do catálogo.
    val favorito: Boolean = false,
    val ultimaPorcaoG: Double? = null,
    val quantityText: String = "100",
    val saved: Boolean = false,

    val microsPer100: Map<String, Double> = emptyMap(),
    val reference: EfsaReference? = null,
    val sex: Sex = Sex.MALE,

    val lifeStage: LifeStage? = null,

    val usualG: Double? = null,

    /** Só muda o que se escreve e o que se lê. O que se grava está sempre em gramas. */
    val unitSystem: UnitSystem = UnitSystem.METRIC,
) {
    val quantityGrams: Double?
        get() = quantityText.replace(',', '.').toDoubleOrNull()
            ?.let { UnitConversions.portionToStored(it, unitSystem, food?.isLiquid == true) }
            ?.takeIf { it in 1.0..5000.0 }

    val previewKcal: Int get() = scale { it.kcal.toDouble() }.roundToInt()
    val previewP: Double get() = scale { it.proteinG }
    val previewC: Double get() = scale { it.carbsG }
    val previewF: Double get() = scale { it.fatG }

    // O sódio e a fibra vêm do mapa desde a v28; o açúcar e a gordura saturada continuam
    // em coluna, por não terem meta diária.
    val previewFiber: Double? get() = escalarDoMapa(Nutrients.FIBER)
    val previewSugar: Double? get() = scaleOrNull { it.sugarsG }
    val previewSatFat: Double? get() = scaleOrNull { it.satFatG }
    val previewSodiumMg: Double? get() = escalarDoMapa(Nutrients.SODIUM)

    /**
     * A energia em quilojoules e o sal, que é o que está escrito na embalagem.
     *
     * **Não são nutrientes novos, e não se guardam:** os quilojoules são as calorias vezes o
     * factor com que a própria unidade se define, e o sal é o sódio vezes 2,5. Guardá-los era
     * ter o mesmo número duas vezes, e dois sítios onde ele pode passar a discordar. Aparecem
     * aqui porque é aqui que a pessoa está a comparar com o rótulo.
     */
    val previewKj: Int get() = (previewKcal * KJ_POR_KCAL).roundToInt()
    val previewSalG: Double? get() = previewSodiumMg?.let { it * SAL_POR_SODIO / MG_POR_G }

    private fun escalarDoMapa(chave: String): Double? {
        val por100 = microsPer100[chave] ?: return null
        val q = quantityGrams ?: return null
        return por100 * q / POR_CEM_GRAMAS
    }

    val breakdown: NutritionBreakdown?
        get() {
            val f = food ?: return null
            val q = quantityGrams ?: return null
            val per100 = buildMap {
                putAll(microsPer100)
                f.sugarsG?.let { put(Nutrients.SUGARS, it) }
                f.satFatG?.let { put(Nutrients.SAT_FAT, it) }
            }
            if (per100.isEmpty()) return null
            return NutritionFacts.build(per100, q, reference, sex, lifeStage).takeIf { !it.isEmpty }
        }

    private fun scale(field: (FoodEntity) -> Double): Double {
        val f = food ?: return 0.0
        val q = quantityGrams ?: return 0.0
        return field(f) * q / 100.0
    }

    private fun scaleOrNull(field: (FoodEntity) -> Double?): Double? {
        val f = food ?: return null
        val q = quantityGrams ?: return null
        val per100 = field(f) ?: return null
        return per100 * q / 100.0
    }
}

class FoodDetailViewModel(
    private val foodRepository: FoodRepository,
    private val diaryRepository: DiaryRepository,
    private val profileRepository: ProfileRepository,
    private val statsRepository: NutritionStatsRepository,
) : ViewModel() {


    private val _state = MutableStateFlow(PortionState())
    val state: StateFlow<PortionState> = _state

    fun load(foodId: String) {
        viewModelScope.launch {
            val food = foodRepository.byId(foodId)

            val micros = microsDeJson(food?.microsJson)
            val reference = statsRepository.loadReference()
            val perfil = profileRepository.observeProfile().first()
            val sex = perfil?.sex ?: Sex.MALE
            val stage = perfil?.lifeStage

            val marca = food?.let { foodRepository.marcaDe(it.id) }
            val usual = food?.let { diaryRepository.usualPortionOf(it.id) }
            val unidades = perfil?.unitSystem ?: UnitSystem.METRIC
            val inicial = (usual ?: marca?.lastAmountG ?: food?.servingGrams) ?: PORCAO_DE_RECURSO_G
            _state.update {
                it.copy(
                    loading = false,
                    food = food,
                    favorito = marca?.isFavorite == true,
                    ultimaPorcaoG = marca?.lastAmountG,
                    unitSystem = unidades,
                    quantityText = paraCampo(inicial, unidades, food?.isLiquid == true),
                    usualG = usual,
                    microsPer100 = micros,
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

    fun setQuick(grams: Double) = _state.update {
        it.copy(quantityText = paraCampo(grams, it.unitSystem, it.food?.isLiquid == true))
    }

    fun toggleFavorite() {
        val food = _state.value.food ?: return
        viewModelScope.launch {
            foodRepository.toggleFavorite(food.id)
            _state.update { s -> s.copy(favorito = !s.favorito) }
        }
    }

    fun save(slot: MealSlot, epochDay: Long) {
        val s = _state.value
        val food = s.food ?: return
        val grams = s.quantityGrams ?: return
        viewModelScope.launch {
            diaryRepository.logFood(food, grams, slot, epochDay)
            foodRepository.touchLastUsed(food.id, amountG = grams)
            _state.update { it.copy(saved = true) }
        }
    }
}

/**
 * O valor guardado para o que o campo mostra. Em onças fica com uma casa decimal: uma onça são
 * quase trinta gramas, e arredondar ao inteiro dava saltos de trinta gramas por toque.
 */
fun paraCampo(quantidade: Double, system: UnitSystem, liquido: Boolean = false): String =
    if (system == UnitSystem.IMPERIAL) {
        val v = UnitConversions.portionToDisplay(quantidade, system, liquido)
        ((v * UMA_CASA).roundToInt() / UMA_CASA.toDouble()).toString()
    } else {
        quantidade.roundToInt().toString()
    }

private const val UMA_CASA = 10

// A definição da caloria: uma quilocaloria são 4,184 quilojoules, e os rótulos da UE são
// obrigados a trazer os dois. Não é uma medição nova — é a mesma, noutra escala.
private const val KJ_POR_KCAL = 4.184

// O sal é cloreto de sódio: 2,5 gramas de sal por cada grama de sódio. É o número que está
// na embalagem, e o sódio é o que as fontes medem.
private const val SAL_POR_SODIO = 2.5
private const val MG_POR_G = 1000.0

// Os micronutrientes ficam por 100 g e escalam na leitura, ao contrário dos macros.
private const val POR_CEM_GRAMAS = 100.0
private const val PORCAO_DE_RECURSO_G = 100.0
