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
import pt.antares.app.core.calc.EatingWindow
import pt.antares.app.core.calc.Janela
import kotlinx.datetime.Clock
import pt.antares.app.core.calc.FastingClash
import pt.antares.app.core.database.entities.FastingSessionEntity
import pt.antares.app.core.util.epochMillisAt
import pt.antares.app.core.util.minuteOfDayAt

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

    // Quando existe, houve comida registada depois de o jejum ter começado. É o único
    // sítio da app onde as duas funcionalidades se falam.
    val quebraDoJejum: QuebraDoJejum? = null,
) {

    /**
     * Da primeira à última refeição com hora. Nulo quando o dia não tem horas que cheguem
     * — todo o histórico anterior à coluna cai aqui, e o ecrã limita-se a não mostrar
     * a linha em vez de mostrar uma janela inventada.
     */
    val janela: Janela? get() = EatingWindow.doDia(logsBySlot.values.flatten().map { it.eatenAtMin })
}

/**
 * Comida registada depois de um jejum ter começado, e ele ainda a correr.
 *
 * Só conta registos com hora: sem ela não se sabe se foram antes ou depois, e acusar sem
 * prova é pior do que calar.
 */
data class QuebraDoJejum(val inicioMin: Int, val registos: Int)

data class NutritionRef(
    val reference: EfsaReference,
    val sex: Sex,
    val lifeStage: LifeStage? = null,
)

// A meta não se fecha aqui: falta saber se houve treino no dia, e isso chega noutro fluxo.
private data class WaterInfo(val ml: Int, val overrideMl: Int?, val sex: Sex, val weightKg: Double)
// Junta o exercício e o jejum do dia numa fonte só. O `combine` tipado do Flow leva cinco
// fontes, e sem este agrupamento a sexta não caberia.
private data class ExerciseInfo(
    val entries: List<ExerciseLogEntity>,
    val kcal: Int,
    val jejum: pt.antares.app.core.database.entities.FastingSessionEntity?,
)

@OptIn(ExperimentalCoroutinesApi::class)
class DiaryViewModel(
    private val diaryRepository: DiaryRepository,
    private val profileRepository: ProfileRepository,
    private val fastingRepository: pt.antares.app.feature.fasting.data.FastingRepository,
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
                profileRepository.observeProfile(),
            ) { water, override, weight, perfil ->
                WaterInfo(
                    ml = water?.ml ?: 0,
                    overrideMl = override,
                    sex = perfil?.sex ?: Sex.MALE,
                    weightKg = weight?.weightKg ?: ProfileRepository.DEFAULT_WEIGHT_KG,
                )
            },

            combine(
                exerciseRepository.observeDay(day),
                exerciseRepository.observeDayKcal(day),
                fastingRepository.observeActive(),
            ) { entries, kcal, jejum ->
                ExerciseInfo(entries, kcal, jejum)
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
                // A meta escolhida à mão manda sobre a calculada. A mesma função do ecrã de
                // hoje: repetir a conta fazia os dois discordarem assim que ela mudasse.
                waterGoalMl = waterInfo.overrideMl ?: DailyGoals.waterMl(
                    sex = waterInfo.sex,
                    weightKg = waterInfo.weightKg,
                    treinouHoje = exerciseInfo.kcal > 0,
                ),
                exerciseEntries = exerciseInfo.entries,
                exerciseKcal = exerciseInfo.kcal,
                quebraDoJejum = quebraDoJejum(day, logs, exerciseInfo.jejum),
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DiaryState(epochDay = todayEpochDay(), isToday = true),
    )

    /**
     * O aviso de que se comeu com o jejum a correr, ou `null` se não houver o que dizer.
     *
     * Cala-se em três casos, e todos são o mesmo cuidado: sem jejum ativo não há nada a
     * cruzar; num dia que não é hoje o jejum a decorrer não lhe diz respeito; e um registo
     * sem hora não prova nada, porque tanto pode ter sido antes como depois.
     */
    private fun quebraDoJejum(
        day: Long,
        logs: List<FoodLogEntity>,
        jejum: FastingSessionEntity?,
    ): QuebraDoJejum? {
        if (jejum == null || day != todayEpochDay()) return null
        val instantes = logs.mapNotNull { log ->
            log.eatenAtMin?.let { epochMillisAt(log.epochDay, it) }
        }
        val depois = FastingClash.dentroDoJejum(
            instantesMs = instantes,
            inicioMs = jejum.startedAt,
            agoraMs = Clock.System.now().toEpochMilliseconds(),
        )
        if (depois.isEmpty()) return null
        return QuebraDoJejum(
            inicioMin = minuteOfDayAt(jejum.startedAt),
            registos = depois.size,
        )
    }

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

    fun updateLogEatenAt(logId: String, eatenAtMin: Int?) =
        viewModelScope.launch { diaryRepository.updateEatenAt(logId, eatenAtMin) }

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
