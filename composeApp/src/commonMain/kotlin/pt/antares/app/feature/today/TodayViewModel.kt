package pt.antares.app.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import pt.antares.app.core.calc.AguaDaComida
import pt.antares.app.core.calc.DailyGoals
import pt.antares.app.core.calc.LoggingStreak
import pt.antares.app.core.calc.Targets
import pt.antares.app.core.calc.WeeklyBudget
import pt.antares.app.core.calc.WeightTrend
import pt.antares.app.core.database.daos.DayTotals
import pt.antares.app.core.database.entities.FastingSessionEntity
import pt.antares.app.core.database.entities.RunEntity
import pt.antares.app.core.datastore.AppPreferences
import pt.antares.app.core.designsystem.HeroStyle
import pt.antares.app.core.health.HealthPublisher
import pt.antares.app.core.model.Sex
import pt.antares.app.core.nutrition.DailyGap
import pt.antares.app.core.health.HealthRepository
import pt.antares.app.core.util.DayTicker
import pt.antares.app.core.util.epochDayToLocalDate
import pt.antares.app.core.util.weekStartEpochDay
import pt.antares.app.feature.diary.DiaryRepository
import pt.antares.app.feature.exercise.ExerciseRepository
import pt.antares.app.feature.fasting.data.FastingRepository
import pt.antares.app.core.model.UnitSystem
import pt.antares.app.feature.profile.data.ProfileRepository
import pt.antares.app.feature.running.data.RunRepository
import pt.antares.app.feature.workout.data.RoutineRepository
import pt.antares.app.feature.workout.data.WorkoutHistoryRepository
import pt.antares.app.feature.workout.data.WorkoutSessionRepository

data class TodayState(
    val loading: Boolean = true,
    val targets: Targets? = null,
    val latestWeightKg: Double? = null,

    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val trendWeightKg: Double? = null,
    val consumed: DayTotals = DayTotals(0, 0.0, 0.0, 0.0),
    val waterMl: Int = 0,
    val waterGoalMl: Int = 2000,
    val exerciseKcal: Int = 0,
)

data class TodayStreak(
    val current: Int = 0,
    val longest: Int = 0,
    val loggedToday: Boolean = false,

    val freezeThisWeek: Boolean = false,
)

private data class WaterRaw(val ml: Int, val override: Int?)

data class TodayWorkout(
    val hasActive: Boolean = false,
    val lastVolume: Double? = null,

    val scheduledRoutineName: String? = null,
)

/**
 * O ecrã de hoje. É o mais dependente da app — doze repositórios — porque é a única vista
 * que junta tudo o que se pode registar num dia.
 *
 * Cada bloco é um `StateFlow` independente em vez de um estado único gigante: assim
 * registar água não recompõe o cartão de treino, e cada parte do ecrã aparece assim que os
 * seus dados chegam, sem esperar pelas outras.
 *
 * Todos partem do [DayTicker], e é por isso que o ecrã vira sozinho à meia-noite.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(
    private val profileRepository: ProfileRepository,
    private val diaryRepository: DiaryRepository,
    exerciseRepository: ExerciseRepository,
    workoutSessionRepository: WorkoutSessionRepository,
    workoutHistoryRepository: WorkoutHistoryRepository,
    routineRepository: RoutineRepository,
    fastingRepository: FastingRepository,
    runRepository: RunRepository,
    preferences: AppPreferences,
    private val statsRepository: pt.antares.app.feature.stats.NutritionStatsRepository,
    private val health: HealthRepository,
    private val healthPublisher: HealthPublisher,
) : ViewModel() {

    private val todayFlow = DayTicker.today

    val steps: StateFlow<Long?> = todayFlow
        .map { today ->
            val zone = TimeZone.currentSystemDefault()
            val start = epochDayToLocalDate(today).atStartOfDayIn(zone).toEpochMilliseconds()
            health.stepsToday(start, start + DAY_MS)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun syncHealthConnect() {
        viewModelScope.launch {
            // Importar antes de publicar, sempre: assim um treino que veio de fora não é
            // devolvido ao Health Connect como se fosse da app.
            health.importNow()

            healthPublisher.publishNow(todayFlow.value)
        }
    }

    val fasting: StateFlow<FastingSessionEntity?> = fastingRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val lastRun: StateFlow<RunEntity?> = runRepository.observeHistory()
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val loggingStreak: StateFlow<TodayStreak> = todayFlow.flatMapLatest { today ->
        diaryRepository.observeLoggedDaysSince(today - STREAK_WINDOW_DAYS)
            .map { days ->
                val set = days.toSet()
                val streak = LoggingStreak.currentWithFreeze(set, today)
                val freezeThisWeek = streak.freezeUsedAtDay?.let {
                    weekStartEpochDay(it) == weekStartEpochDay(today)
                } ?: false
                TodayStreak(
                    current = streak.current,
                    longest = LoggingStreak.longest(set),
                    loggedToday = today in set,
                    freezeThisWeek = freezeThisWeek,
                )
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayStreak())

    private val _celebration = MutableStateFlow<Int?>(null)
    val celebration: StateFlow<Int?> = _celebration.asStateFlow()

    val heroStyle: StateFlow<HeroStyle> = preferences.heroStyle
        .map { runCatching { HeroStyle.valueOf(it) }.getOrDefault(HeroStyle.CLASSIC) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HeroStyle.CLASSIC)

    init {

        viewModelScope.launch {
            loggingStreak.collect { s ->
                // A comparação com o último marco celebrado é o que impede a animação de
                // voltar a disparar a cada abertura do ecrã com a mesma sequência.
                if (s.current in STREAK_MILESTONES && s.current > preferences.lastCelebratedStreak.first()) {
                    preferences.setLastCelebratedStreak(s.current)
                    _celebration.value = s.current
                }
            }
        }
    }

    fun consumeCelebration() {
        _celebration.value = null
    }

    val workout: StateFlow<TodayWorkout> = todayFlow.flatMapLatest { today ->

        // O `flatMapLatest` é o que faz virar o dia cancelar os fluxos antigos e recomeçar:
        // com `map`, o ecrã continuava a ouvir as consultas de ontem.
        val todayIsoDay = epochDayToLocalDate(today).dayOfWeek.isoDayNumber
        combine(
            workoutSessionRepository.observeActive(),
            workoutHistoryRepository.observeHistory(),
            routineRepository.observeSchedule(),
            routineRepository.observeRoutines(),
        ) { active, history, schedule, routines ->
            val routineId = schedule.firstOrNull { it.dayOfWeek == todayIsoDay }?.routineId
            val scheduledName = routineId?.let { id -> routines.firstOrNull { it.id == id }?.name }
            TodayWorkout(
                hasActive = active != null,
                lastVolume = history.firstOrNull()?.volume,
                scheduledRoutineName = scheduledName,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayWorkout())

    val weeklyBudget: StateFlow<WeeklyBudget?> = todayFlow.flatMapLatest { today ->
        val inicioDaSemana = weekStartEpochDay(today)
        val diaIso = epochDayToLocalDate(today).dayOfWeek.isoDayNumber
        combine(
            profileRepository.observeTargets(today),
            diaryRepository.observeDailyKcal(inicioDaSemana, today),
        ) { metas, dias ->
            metas?.let {
                WeeklyBudget.of(
                    targetPerDay = it.kcal,
                    isoDayOfWeek = diaIso,

                    loggedDays = dias.count { d -> d.kcal > 0 },
                    consumed = dias.sumOf { d -> d.kcal },
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val dailyGap: StateFlow<DailyGap?> = todayFlow.flatMapLatest { today ->
        diaryRepository.observeDayTotals(today).map {
            val perfil = profileRepository.observeProfile().first()
            val referencia = statsRepository.loadReference()
            val sexo = perfil?.sex ?: Sex.MALE
            DailyGap.worst(
                totals = statsRepository.totals(today, today),
                referenceFor = { chave ->
                    referencia.forKey(chave)?.forPerson(sexo, perfil?.lifeStage)
                },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * A água que veio da comida. Fica fora do [state] de propósito: não é para somar ao
     * contador, é para aparecer ao lado dele — a meta fala do que se bebe.
     */
    val aguaDaComidaMl: StateFlow<Int?> = todayFlow.flatMapLatest { today ->
        diaryRepository.observeDayTotals(today).map {
            AguaDaComida.mlDoDia(statsRepository.totals(today, today))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val state: StateFlow<TodayState> = todayFlow.flatMapLatest { today ->
        combine(

            combine(
                profileRepository.observeTargets(today),
                profileRepository.observeProfile(),
            ) { alvos, perfil -> alvos to perfil },
            profileRepository.observeWeights(),
            diaryRepository.observeDayTotals(today),
            combine(
                diaryRepository.observeWater(today),
                preferences.waterGoalOverrideMl,
            ) { water, override -> WaterRaw(water?.ml ?: 0, override) },
            exerciseRepository.observeDayKcal(today),
        ) { (targets, perfil), weights, consumed, waterRaw, exerciseKcal ->

            val chronologicalDated = weights.sortedBy { it.epochDay }.map { it.epochDay to it.weightKg }
            val chronological = weights.sortedBy { it.epochDay }.map { it.weightKg }
            val latest = chronological.lastOrNull()

            // A meta de água escolhida à mão manda sobre a calculada pelo peso; sem
            // pesagem nenhuma, um peso de recurso evita uma meta de zero.
            val waterGoal = waterRaw.override
                ?: DailyGoals.waterMl(latest ?: ProfileRepository.DEFAULT_WEIGHT_KG)
            TodayState(
                loading = false,
                targets = targets,
                latestWeightKg = latest,
                trendWeightKg = WeightTrend.trendNow(chronologicalDated),
                consumed = consumed,
                waterMl = waterRaw.ml,
                waterGoalMl = waterGoal,
                exerciseKcal = exerciseKcal,
                unitSystem = perfil?.unitSystem ?: UnitSystem.METRIC,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayState())

    private companion object {
        const val DAY_MS = 24L * 60 * 60 * 1000

        // Pouco mais de um ano: chega para a sequência mais longa possível caber na janela
        // sem carregar o histórico todo a cada abertura do ecrã.
        const val STREAK_WINDOW_DAYS = 400L

        val STREAK_MILESTONES = setOf(3, 7, 14, 30, 60, 100, 200, 365)
    }
}
