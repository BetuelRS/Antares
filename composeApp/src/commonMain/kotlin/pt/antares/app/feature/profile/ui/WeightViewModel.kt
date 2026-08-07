package pt.antares.app.feature.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pt.antares.app.core.calc.WeightTrend
import pt.antares.app.core.database.entities.WeightLogEntity
import pt.antares.app.core.model.UnitSystem
import pt.antares.app.core.util.todayEpochDay
import pt.antares.app.feature.profile.data.ProfileRepository

data class WeightState(
    val loading: Boolean = true,
    val entries: List<WeightLogEntity> = emptyList(),

    val series: List<Pair<Long, Double>> = emptyList(),
    val trendSeries: List<Pair<Long, Double>> = emptyList(),
    val trend: Double? = null,

    val goalWeightKg: Double? = null,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
) {

    val daysWithEntry: Set<Long> get() = entries.map { it.epochDay }.toSet()
}

data class WeightRecalc(val oldKcal: Int, val newKcal: Int, val deltaWeightKg: Double)

data class PendingWeight(
    val epochDay: Long,
    val weightKg: Double,
    val note: String?,

    val referenceKg: Double,
)

class WeightViewModel(
    private val repository: ProfileRepository,
) : ViewModel() {

    val state: StateFlow<WeightState> = combine(
        repository.observeWeights(),
        repository.observeProfile(),
    ) { entries, profile ->
        val sorted = entries.sortedBy { it.epochDay }

        val dated = sorted.map { it.epochDay to it.weightKg }
        WeightState(
            loading = false,
            entries = entries,
            series = dated,
            trendSeries = WeightTrend.trendPairs(dated),
            trend = WeightTrend.trendNow(dated),
            goalWeightKg = profile?.goalWeightKg,
            unitSystem = profile?.unitSystem ?: UnitSystem.METRIC,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeightState())

    private val _recalc = MutableStateFlow<WeightRecalc?>(null)
    val recalc: StateFlow<WeightRecalc?> = _recalc

    private val _pendingTypo = MutableStateFlow<PendingWeight?>(null)
    val pendingTypo: StateFlow<PendingWeight?> = _pendingTypo

    fun submit(epochDay: Long = todayEpochDay(), weightKg: Double, note: String?) {
        viewModelScope.launch {
            val reference = repository.weightLooksLikeTypo(weightKg)
            if (reference != null) {
                _pendingTypo.value = PendingWeight(epochDay, weightKg, note, reference)
            } else {
                addOrUpdate(epochDay, weightKg, note)
            }
        }
    }

    fun confirmPending() {
        val p = _pendingTypo.value ?: return
        _pendingTypo.value = null
        addOrUpdate(p.epochDay, p.weightKg, p.note)
    }

    fun dismissPending() { _pendingTypo.value = null }

    fun addOrUpdate(epochDay: Long = todayEpochDay(), weightKg: Double, note: String?) {
        viewModelScope.launch {

            val before = repository.targetsFor(epochDay)
            val previousWeight = repository.observeLatestWeight().first()?.weightKg
            repository.upsertWeight(epochDay, weightKg, note)
            val after = repository.targetsFor(epochDay)

            if (before != null && after != null && before.kcal != after.kcal && previousWeight != null) {
                _recalc.value = WeightRecalc(
                    oldKcal = before.kcal,
                    newKcal = after.kcal,
                    deltaWeightKg = weightKg - previousWeight,
                )
            }
        }
    }

    fun consumeRecalc() { _recalc.value = null }

    fun delete(id: String) {
        viewModelScope.launch { repository.deleteWeight(id) }
    }
}
