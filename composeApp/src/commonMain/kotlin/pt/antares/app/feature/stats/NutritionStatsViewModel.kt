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

    // Avisa que o período abrange dias registados antes de o catálogo ter sido revisto: os
    // valores desses dias vieram de uma versão anterior dos alimentos, e comparar a semana
    // toda sem o dizer misturava duas origens.
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

    // A tabela da EFSA lê-se de um ficheiro e não muda: fica em memória depois da primeira
    // leitura, ao contrário do catálogo de METs, que só se usa uma vez por ecrã.
    private var efsa: EfsaReference? = null
    private val period = MutableStateFlow(StatsPeriod.DAY)

    val state: StateFlow<StatsState> = period
        .mapLatest { p -> compute(p) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsState())

    fun setPeriod(p: StatsPeriod) { period.value = p }

    private suspend fun compute(p: StatsPeriod): StatsState {
        val reference = efsa ?: statsRepository.loadReference().also { efsa = it }
        val today = todayEpochDay()
        val dias = if (p == StatsPeriod.WEEK) DAYS_IN_WEEK else 1
        val from = today - (dias - 1)
        val totals = statsRepository.totals(from, today)
        val perfil = profileRepository.observeProfile().first()
        val sex = perfil?.sex ?: Sex.MALE

        // As referências da EFSA são diárias: sem o `days`, uma semana somada
        // compara-se com um só dia e lê sete vezes o valor certo.
        val coverage = CoverageCalc.compute(totals, sex, reference.all(), perfil?.lifeStage, dias)

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

    private companion object {
        const val DAYS_IN_WEEK = 7
    }
}
