package pt.antares.app.feature.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import pt.antares.app.core.calc.ActivitySuggestion
import pt.antares.app.core.calc.BodyComposition
import pt.antares.app.core.calc.BodyStats
import pt.antares.app.core.calc.GoalProjection
import pt.antares.app.core.calc.MeasurementProgress
import pt.antares.app.core.calc.MeasurementProgressCalc
import pt.antares.app.core.calc.AdaptiveTdee
import pt.antares.app.core.calc.NutritionCalc
import pt.antares.app.core.calc.Projection
import pt.antares.app.core.calc.Targets
import pt.antares.app.core.calc.WeightTrend
import pt.antares.app.core.database.entities.UserProfileEntity
import pt.antares.app.core.model.Sex
import pt.antares.app.core.health.HealthRepository
import pt.antares.app.core.util.epochDayToLocalDate
import pt.antares.app.core.util.todayEpochDay
import kotlin.math.roundToInt
import pt.antares.app.feature.profile.data.BodyMeasurementRepository
import pt.antares.app.feature.profile.data.ProfileRepository

data class HealthProfileState(
    val loading: Boolean = true,
    val profile: UserProfileEntity? = null,
    val targets: Targets? = null,
    val latestWeightKg: Double? = null,
    val trendWeightKg: Double? = null,

    val weeklyRateKg: Double? = null,

    val weightSeries: List<Double> = emptyList(),
    val trendSeries: List<Double> = emptyList(),
    val body: BodyStats? = null,

    val startWeightKg: Double? = null,

    val learnedTdeeKcal: Int? = null,

    val projection: Projection? = null,

    val stallWeeks: Int = 0,

    val measurementProgress: MeasurementProgress? = null,

    val adaptiveWeeks: Int = 0,

    val loggedDaysPerWeek: Int = 0,

    val activitySuggestion: ActivitySuggestion.Suggestion? = null,
) {

    val maintenanceKcal: Int? get() = targets?.energy?.tdee?.roundToInt()

    val adaptiveIsConfident: Boolean
        get() = learnedTdeeKcal != null && adaptiveWeeks >= CONFIDENT_WEEKS

    val isStalled: Boolean get() = stallWeeks >= AdaptiveTdee.PLATEAU_WEEKS

    val showsCycleNote: Boolean
        get() = profile?.sex == Sex.FEMALE && latestWeightKg != null
    val goalWeightKg: Double? get() = profile?.goalWeightKg

    val hasOwnComposition: Boolean
        get() = profile?.bodyFatPct != null ||
            (profile?.waistCm != null && profile?.neckCm != null)

    val remainingToGoalKg: Double?
        get() {
            val goal = goalWeightKg ?: return null
            val now = latestWeightKg ?: return null
            return now - goal
        }

    companion object {

        const val CONFIDENT_WEEKS = 4
    }
}

class HealthProfileViewModel(
    private val repository: ProfileRepository,
    private val measurements: BodyMeasurementRepository,
    private val health: HealthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HealthProfileState())
    val state: StateFlow<HealthProfileState> = _state

    init {
        combine(
            repository.observeProfile(),
            repository.observeWeights(),
            repository.observeTargets(),
        ) { profile, weights, targets ->

            val chronological = weights.sortedBy { it.epochDay }
            val values = chronological.map { it.weightKg }
            val latest = values.lastOrNull()

            val rate = WeightTrend.weeklyRateKg(chronological.map { it.epochDay to it.weightKg })

            HealthProfileState(
                loading = false,
                profile = profile,
                targets = targets,
                latestWeightKg = latest,
                trendWeightKg = WeightTrend.trendNow(chronological.map { it.epochDay to it.weightKg }),
                weeklyRateKg = rate,
                weightSeries = values,
                trendSeries = WeightTrend.trendSeries(chronological.map { it.epochDay to it.weightKg }),
                body = bodyStats(profile, latest),
                startWeightKg = values.firstOrNull(),

                projection = profile?.goalWeightKg?.let { goal ->
                    latest?.let { now ->
                        GoalProjection.project(now, goal, rate, todayEpochDay())
                    }
                },
                stallWeeks = WeightTrend.consecutiveStallWeeks(
                    chronological.map { it.epochDay to it.weightKg },
                ),
            )
        }
            .onEach { fresh ->
                _state.update {
                    fresh.copy(
                        learnedTdeeKcal = it.learnedTdeeKcal,
                        adaptiveWeeks = it.adaptiveWeeks,
                        measurementProgress = it.measurementProgress,
                        loggedDaysPerWeek = it.loggedDaysPerWeek,
                        activitySuggestion = it.activitySuggestion,
                    )
                }

                if (fresh.stallWeeks > 0) {
                    val dias = repository.loggedDaysPerWeek(fresh.stallWeeks)
                    _state.update { it.copy(loggedDaysPerWeek = dias) }
                }
            }
            .launchIn(viewModelScope)

        measurements.observeAll()
            .onEach { entries ->
                _state.update { it.copy(measurementProgress = MeasurementProgressCalc.compute(entries)) }
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            val learned = repository.learnedTdee()
            val weeks = repository.adaptiveWeeks()
            _state.update { it.copy(learnedTdeeKcal = learned, adaptiveWeeks = weeks) }
        }

        viewModelScope.launch {
            val zone = TimeZone.currentSystemDefault()
            val inicioDeHoje = epochDayToLocalDate(todayEpochDay())
                .atStartOfDayIn(zone)
                .toEpochMilliseconds()
            val passos = health.stepsPerDay(inicioDeHoje, HealthRepository.ACTIVITY_WINDOW_DAYS)
            val atual = repository.profileOnce()?.activityLevel
            _state.update {
                it.copy(activitySuggestion = ActivitySuggestion.suggest(passos, atual))
            }
        }
    }

    fun acceptActivitySuggestion() {
        val sugestao = _state.value.activitySuggestion ?: return
        viewModelScope.launch {
            repository.profileOnce()?.let { perfil ->
                repository.saveProfile(perfil.copy(activityLevel = sugestao.suggested))
            }
            _state.update { it.copy(activitySuggestion = null) }
        }
    }

    fun dismissActivitySuggestion() = _state.update { it.copy(activitySuggestion = null) }

    private fun bodyStats(profile: UserProfileEntity?, weightKg: Double?): BodyStats? {
        if (profile == null || weightKg == null) return null
        return BodyComposition.stats(
            sex = profile.sex,
            weightKg = weightKg,
            heightCm = profile.heightCm,
            ageYears = NutritionCalc.ageYears(profile.birthEpochDay, todayEpochDay()),
            bodyFatPct = profile.bodyFatPct,
            bodyFatSource = profile.bodyFatSource,
            waistCm = profile.waistCm,
            neckCm = profile.neckCm,
            hipCm = profile.hipCm,
        )
    }

    companion object {

        const val MIN_POINTS_FOR_CHART = 2
    }
}
