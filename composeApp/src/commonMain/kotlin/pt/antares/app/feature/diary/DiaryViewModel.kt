package pt.antares.app.feature.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pt.antares.app.core.calc.DailyGoals
import pt.antares.app.core.calc.Targets
import pt.antares.app.core.database.daos.DayTotals
import pt.antares.app.core.database.entities.ExerciseLogEntity
import pt.antares.app.core.database.entities.FoodLogEntity
import pt.antares.app.core.datastore.AppPreferences
import pt.antares.app.core.model.MealSlot
import pt.antares.app.core.util.DayTicker
import pt.antares.app.core.util.followDayChange
import pt.antares.app.core.util.todayEpochDay
import pt.antares.app.feature.exercise.ExerciseRepository
import pt.antares.app.core.model.Sex
import pt.antares.app.core.model.LifeStage
import pt.antares.app.core.nutrition.EfsaReference
import pt.antares.app.feature.profile.data.ProfileRepository

data class DiaryState(
    val epochDay: Long,
    val isToday: Boolean,
    val loading: Boolean = true,
    val logsBySlot: Map<MealSlot, List<FoodLogEntity>> = emptyMap(),
    val totals: DayTotals = DayTotals(0, 0.0, 0.0, 0.0),
    val targets: Targets? = null,
    val waterMl: Int = 0,
    val waterGoalMl: Int = 2000,
    val exerciseEntries: List<ExerciseLogEntity> = emptyList(),
    val exerciseKcal: Int = 0,
)

data class NutritionRef(
    val reference: EfsaReference,
    val sex: Sex,
    val lifeStage: LifeStage? = null,
)

private data class WaterInfo(val ml: Int, val goalMl: Int)
private data class ExerciseInfo(val entries: List<ExerciseLogEntity>, val kcal: Int)

@OptIn(ExperimentalCoroutinesApi::class)
class DiaryViewModel(
    private val diaryRepository: DiaryRepository,
    private val profileRepository: ProfileRepository,
    private val exerciseRepository: ExerciseRepository,
    private val preferences: AppPreferences,
    private val templateRepository: pt.antares.app.feature.templates.MealTemplateRepository,
    private val statsRepository: pt.antares.app.feature.stats.NutritionStatsRepository,
) : ViewModel() {

    val nutritionRef: StateFlow<NutritionRef?> = profileRepository.observeProfile()
        .map { perfil ->
            NutritionRef(
                reference = statsRepository.loadReference(),
                sex = perfil?.sex ?: Sex.MALE,
                lifeStage = perfil?.lifeStage,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val selectedDay = MutableStateFlow(DayTicker.today.value)

    private val _templateSaved = MutableStateFlow<String?>(null)
    val templateSaved: StateFlow<String?> = _templateSaved

    init {

        // O diário acompanha a viragem do dia só se estiver em hoje: quem foi rever a
        // semana passada não pode ver o ecrã saltar debaixo dos olhos à meia-noite.
        viewModelScope.launch {
            var lastToday = DayTicker.today.value
            DayTicker.today.collect { newToday ->
                selectedDay.value = followDayChange(selectedDay.value, lastToday, newToday)
                lastToday = newToday
            }
        }
    }

    val state: StateFlow<DiaryState> = selectedDay.flatMapLatest { day ->
        combine(
            diaryRepository.observeDay(day),
            diaryRepository.observeDayTotals(day),
            profileRepository.observeTargets(day),

            combine(
                diaryRepository.observeWater(day),
                preferences.waterGoalOverrideMl,
                profileRepository.observeLatestWeight(),
            ) { water, override, weight ->
                // A mesma regra do ecrã de hoje, e pela mesma função: repetir a conta aqui
                // fazia os dois ecrãs discordarem assim que a constante mudasse.
                val goal = override
                    ?: DailyGoals.waterMl(weight?.weightKg ?: ProfileRepository.DEFAULT_WEIGHT_KG)
                WaterInfo(ml = water?.ml ?: 0, goalMl = goal)
            },

            combine(
                exerciseRepository.observeDay(day),
                exerciseRepository.observeDayKcal(day),
            ) { entries, kcal ->
                ExerciseInfo(entries, kcal)
            },
        ) { logs, totals, targets, waterInfo, exerciseInfo ->
            DiaryState(
                epochDay = day,
                isToday = day == todayEpochDay(),
                loading = false,
                logsBySlot = logs.groupBy { it.mealSlot },
                totals = totals,
                targets = targets,
                waterMl = waterInfo.ml,
                waterGoalMl = waterInfo.goalMl,
                exerciseEntries = exerciseInfo.entries,
                exerciseKcal = exerciseInfo.kcal,
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DiaryState(epochDay = todayEpochDay(), isToday = true),
    )

    fun goToDay(epochDay: Long) {
        selectedDay.value = epochDay
    }

    fun previousDay() = goToDay(selectedDay.value - 1)
    fun nextDay() = goToDay(selectedDay.value + 1)
    fun goToToday() = goToDay(todayEpochDay())

    // Todas as ações leem o dia selecionado antes de lançar a corrotina. Lê-lo lá dentro
    // deixava a escrita cair no dia errado se a pessoa mudasse de data entretanto.
    fun addWater(deltaMl: Int) {
        val day = selectedDay.value
        viewModelScope.launch { diaryRepository.addWater(day, deltaMl) }
    }

    fun deleteExercise(id: String) = viewModelScope.launch { exerciseRepository.delete(id) }

    fun deleteLog(logId: String) = viewModelScope.launch { diaryRepository.delete(logId) }
    fun updateLogQuantity(logId: String, grams: Double) =
        viewModelScope.launch { diaryRepository.updateQuantity(logId, grams) }

    fun duplicateLog(logId: String) = viewModelScope.launch { diaryRepository.duplicate(logId) }
    fun moveLog(logId: String, slot: MealSlot) = viewModelScope.launch { diaryRepository.move(logId, slot) }

    fun copyYesterday() {
        val day = selectedDay.value
        viewModelScope.launch { diaryRepository.copyDay(day - 1, day) }
    }

    val repeatable: StateFlow<Map<MealSlot, pt.antares.app.feature.diary.RepeatableMeal>> =
        selectedDay.flatMapLatest { day ->
            diaryRepository.observeDay(day).map { logs ->
                // Só se oferece repetir as refeições que ainda não têm nada: a sugestão
                // desaparece assim que a pessoa regista alguma coisa nesse período.
                val jaRegistadas = logs.map { it.mealSlot }.toSet()
                MealSlot.entries
                    .filter { it !in jaRegistadas }
                    .mapNotNull { diaryRepository.lastMealBefore(it, day) }
                    .associateBy { it.slot }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun repeatMeal(slot: MealSlot) {
        val day = selectedDay.value
        viewModelScope.launch { diaryRepository.repeatLastMeal(slot, day) }
    }

    private val _copyCandidates = MutableStateFlow<List<RepeatableMeal>?>(null)
    val copyCandidates: StateFlow<List<RepeatableMeal>?> = _copyCandidates

    fun loadCopyCandidates(slot: MealSlot) {
        val day = selectedDay.value
        viewModelScope.launch {
            _copyCandidates.value = diaryRepository.recentMeals(slot, day)
        }
    }

    fun closeCopyCandidates() {
        _copyCandidates.value = null
    }

    fun copyMealFrom(fromEpochDay: Long, slot: MealSlot) {
        val day = selectedDay.value
        viewModelScope.launch {
            diaryRepository.copyMeal(fromEpochDay, day, slot)
            _copyCandidates.value = null
        }
    }

    fun moveMeal(from: MealSlot, to: MealSlot) {
        val day = selectedDay.value
        viewModelScope.launch { diaryRepository.moveMeal(day, from, to) }
    }

    fun clearMeal(slot: MealSlot) {
        val day = selectedDay.value
        viewModelScope.launch { diaryRepository.clearMeal(day, slot) }
    }

    fun quickAddCalories(kcal: Int, name: String, slot: MealSlot) {
        // Zero calorias não é um registo; recusa-se em silêncio em vez de gravar uma linha
        // vazia que a pessoa depois tem de apagar.
        if (kcal <= 0) return
        val day = selectedDay.value
        viewModelScope.launch { diaryRepository.logQuickCalories(kcal, name, slot, day) }
    }

    fun saveMealAsTemplate(name: String, slot: MealSlot) {
        if (name.isBlank()) return
        val day = selectedDay.value
        viewModelScope.launch {
            val id = templateRepository.saveMealAsTemplate(name, slot, day)
            if (id != null) _templateSaved.value = name.trim()
        }
    }

    fun consumeTemplateSaved() {
        _templateSaved.value = null
    }
}
