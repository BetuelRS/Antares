package pt.antares.app.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import pt.antares.app.core.model.Sex
import org.jetbrains.compose.resources.StringResource
import pt.antares.app.core.nutrition.Nutrients
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*
import pt.antares.app.core.nutrition.CoverageCalc
import pt.antares.app.core.nutrition.EfsaReference
import pt.antares.app.core.nutrition.MicroCoverage
import pt.antares.app.core.util.todayEpochDay
import pt.antares.app.feature.profile.data.ProfileRepository

enum class StatsPeriod { DAY, WEEK }

data class StatsState(
    val loading: Boolean = true,
    val period: StatsPeriod = StatsPeriod.DAY,
    val coverage: List<MicroCoverage> = emptyList(),
    val hasAnyData: Boolean = false,

    val measuredAnyPct: Int = 100,

    val includesOldCatalogue: Boolean = false,
) {

    val groups: List<Pair<StringResource, List<MicroCoverage>>>
        get() = listOf(
            Res.string.nutrition_vitamins to coverage.filter { it.key in Nutrients.VITAMINS },
            Res.string.nutrition_minerals to coverage.filter { it.key in Nutrients.MINERALS },
            Res.string.nutrition_others to coverage.filter {
                it.key !in Nutrients.VITAMINS && it.key !in Nutrients.MINERALS
            },
        )
}

@OptIn(ExperimentalCoroutinesApi::class)
class NutritionStatsViewModel(
    private val statsRepository: NutritionStatsRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private var efsa: EfsaReference? = null
    private val period = MutableStateFlow(StatsPeriod.DAY)

    val state: StateFlow<StatsState> = period
        .mapLatest { p -> compute(p) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsState())

    fun setPeriod(p: StatsPeriod) { period.value = p }

    private suspend fun compute(p: StatsPeriod): StatsState {
        val reference = efsa ?: statsRepository.loadReference().also { efsa = it }
        val today = todayEpochDay()
        val from = if (p == StatsPeriod.WEEK) today - 6 else today
        val totals = statsRepository.totals(from, today)
        val perfil = profileRepository.observeProfile().first()
        val sex = perfil?.sex ?: Sex.MALE
        val coverage = CoverageCalc.compute(totals, sex, reference.all(), perfil?.lifeStage)

        val rebuiltDay = statsRepository.catalogueRebuiltDay()
        return StatsState(
            loading = false,
            period = p,
            coverage = coverage,
            hasAnyData = coverage.any { it.hasData },
            measuredAnyPct = totals.measuredAnyPct,
            includesOldCatalogue = rebuiltDay != null && from < rebuiltDay,
        )
    }
}
