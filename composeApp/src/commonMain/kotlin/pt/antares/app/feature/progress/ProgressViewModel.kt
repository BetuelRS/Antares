package pt.antares.app.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pt.antares.app.core.calc.BeenHereCalc
import pt.antares.app.core.calc.EatingPatterns
import pt.antares.app.core.calc.GoalHistoryCalc
import pt.antares.app.core.calc.NutritionCalc
import pt.antares.app.core.calc.ProgressRange
import pt.antares.app.core.calc.ProgressCalc
import pt.antares.app.core.calc.WeightTrend
import pt.antares.app.core.database.entities.ProgressPhotoEntity
import pt.antares.app.core.datastore.AppPreferences
import pt.antares.app.core.model.GoalRates
import pt.antares.app.core.model.UnitSystem
import pt.antares.app.core.util.todayEpochDay
import pt.antares.app.feature.profile.data.ProfileRepository

data class ProgressState(
    val loading: Boolean = true,

    val weightSeries: List<Pair<Long, Double>> = emptyList(),

    val trendSeries: List<Pair<Long, Double>> = emptyList(),
    val goalWeightKg: Double? = null,

    val weeklyRateKg: Double? = null,

    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val consistency: List<ProgressCalc.DayCell> = emptyList(),
    val consistencyPct: Int = 0,

    val kcalComparison: ProgressCalc.Comparison? = null,
    val goals: List<GoalHistoryCalc.Goal> = emptyList(),
    val milestones: List<ProgressCalc.Milestone> = emptyList(),

    val patterns: List<EatingPatterns.Pattern> = emptyList(),

    val patternSuggestions: Boolean = false,

    val lastVisit: BeenHereCalc.Visit? = null,

    val waistNowCm: Double? = null,
    val bodyFatNowPct: Double? = null,

    val photoFirst: ProgressPhotoEntity? = null,
    val photoLast: ProgressPhotoEntity? = null,

    val range: ProgressRange = ProgressRange.DAYS_30,

    val today: Long = 0L,

    val desiredWeeklyRateKg: Double? = null,
) {
    val hasWeight: Boolean get() = weightSeries.isNotEmpty()

    val trendNowKg: Double? get() = trendSeries.lastOrNull()?.second

    val firstWeightKg: Double? get() = weightSeries.firstOrNull()?.second

    val rangeWeightSeries: List<Pair<Long, Double>> get() = range.clip(weightSeries, today)
    val rangeTrendSeries: List<Pair<Long, Double>> get() = range.clip(trendSeries, today)

    val rangeChangeKg: Double?
        get() {
            val serie = rangeTrendSeries
            if (serie.size < 2) return null
            return serie.last().second - serie.first().second
        }

    val hasAnything: Boolean
        get() = hasWeight || consistency.any { it.logged } || goals.isNotEmpty()
}

class ProgressViewModel(
    private val repository: ProgressRepository,
    private val photoRepository: ProgressPhotoRepository,
    private val profileRepository: ProfileRepository,
    private val preferences: AppPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(ProgressState())
    val state: StateFlow<ProgressState> = _state

    fun setRange(range: ProgressRange) {
        _state.value = _state.value.copy(range = range)
    }

    fun refresh() {
        viewModelScope.launch {
            val hoje = todayEpochDay()
            val pesagens = repository.weighIns()

            val dias = repository.loggedDays(days = ProgressCalc.CONSISTENCY_WEEKS * 7 + 7, today = hoje)
            val grelha = ProgressCalc.consistencyGrid(dias, hoje)

            val atual = repository.dailyKcal(hoje - 29, hoje)
            val anterior = repository.dailyKcal(hoje - 59, hoje - 30)

            val perfil = profileRepository.profileOnce()

            val fotos = photoRepository.observeAll().first()

            _state.value = ProgressState(
                loading = false,
                weightSeries = pesagens,
                trendSeries = WeightTrend.trendPairs(pesagens),
                goalWeightKg = perfil?.goalWeightKg,
                weeklyRateKg = WeightTrend.weeklyRateKg(pesagens),
                unitSystem = perfil?.unitSystem ?: UnitSystem.METRIC,
                consistency = grelha,
                consistencyPct = ProgressCalc.consistencyPct(grelha),
                kcalComparison = ProgressCalc.compare(atual, anterior),
                goals = repository.goalTimeline(),
                milestones = repository.milestones(hoje),
                patterns = EatingPatterns.detect(repository.patternDays(PATTERN_WINDOW_DAYS, hoje)),
                patternSuggestions = preferences.patternSuggestions.first(),
                photoFirst = fotos.firstOrNull(),
                photoLast = fotos.lastOrNull(),
                lastVisit = repository.lastVisitAtCurrentWeight(hoje),
                waistNowCm = perfil?.waistCm,
                bodyFatNowPct = perfil?.bodyFatPct,

                range = _state.value.range,
                today = hoje,
                desiredWeeklyRateKg = perfil?.goalRateKcal
                    ?.takeIf { it != GoalRates.MAINTAIN }
                    ?.let { NutritionCalc.weeklyKgFromKcalPerDay(it) },
            )
        }
    }
}

private const val PATTERN_WINDOW_DAYS = 56
