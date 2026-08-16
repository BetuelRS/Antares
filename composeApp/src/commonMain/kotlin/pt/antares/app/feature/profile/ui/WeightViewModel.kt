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

/**
 * Já havia uma pesagem hoje, e há outra à espera de resposta. Os dois valores viajam juntos
 * porque a pergunta não se pode fazer sem os mostrar.
 */
data class SegundaPesagem(
    val epochDay: Long,
    val novoKg: Double,
    val anteriorKg: Double,
    val note: String?,
)

/**
 * O que fazer com a segunda pesagem do dia.
 *
 * **Não há uma quarta opção que guarde as duas em linhas separadas.** O `weight_log` tem um
 * índice único por dia — é o que impede uma lápide de colidir com uma escrita nova, e está
 * escrito na decisão sobre lápides e índices únicos. Guardar duas medições do mesmo dia
 * obrigava a desfazê-lo, e isso é uma decisão de esquema com consequências para o restauro
 * de cópias, não uma opção de diálogo.
 */
enum class EscolhaDaSegundaPesagem {

    SUBSTITUIR,

    MEDIA,

    MANTER_A_ANTERIOR,
}

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

    private val _segundaPesagem = MutableStateFlow<SegundaPesagem?>(null)
    val segundaPesagem: StateFlow<SegundaPesagem?> = _segundaPesagem

    fun submit(epochDay: Long = todayEpochDay(), weightKg: Double, note: String?) {
        viewModelScope.launch {
            val reference = repository.weightLooksLikeTypo(weightKg)
            if (reference != null) {
                _pendingTypo.value = PendingWeight(epochDay, weightKg, note, reference)
            } else {
                perguntarOuGravar(epochDay, weightKg, note)
            }
        }
    }

    fun confirmPending() {
        val p = _pendingTypo.value ?: return
        _pendingTypo.value = null
        viewModelScope.launch { perguntarOuGravar(p.epochDay, p.weightKg, p.note) }
    }

    fun dismissPending() { _pendingTypo.value = null }

    /**
     * A segunda pesagem do dia não se resolve sozinha. Quem se pesa, acha o valor estranho
     * e repete ficava com a média dos dois em silêncio — sem saber que houve média, e sem
     * poder dizer qual das medições valia.
     */
    private suspend fun perguntarOuGravar(epochDay: Long, weightKg: Double, note: String?) {
        val jaExiste = repository.weightOnDay(epochDay)
        if (jaExiste == null) {
            addOrUpdate(epochDay, weightKg, note)
            return
        }
        _segundaPesagem.value = SegundaPesagem(
            epochDay = epochDay,
            novoKg = weightKg,
            anteriorKg = jaExiste.weightKg,
            note = note,
        )
    }

    fun resolverSegundaPesagem(escolha: EscolhaDaSegundaPesagem) {
        val p = _segundaPesagem.value ?: return
        _segundaPesagem.value = null
        // A escolha vale para esta vez e mais nada: não se guarda como preferência, porque
        // a razão de repetir a pesagem muda de dia para dia.
        when (escolha) {
            EscolhaDaSegundaPesagem.SUBSTITUIR ->
                addOrUpdate(p.epochDay, p.novoKg, p.note, media = false)
            EscolhaDaSegundaPesagem.MEDIA ->
                addOrUpdate(p.epochDay, p.novoKg, p.note, media = true)
            // Não escrever é ficar com a que lá está. Nada a fazer, e é essa a questão.
            EscolhaDaSegundaPesagem.MANTER_A_ANTERIOR -> Unit
        }
    }

    fun dispensarSegundaPesagem() { _segundaPesagem.value = null }

    fun addOrUpdate(
        epochDay: Long = todayEpochDay(),
        weightKg: Double,
        note: String?,
        media: Boolean = false,
    ) {
        viewModelScope.launch {

            val before = repository.targetsFor(epochDay)
            val previousWeight = repository.observeLatestWeight().first()?.weightKg
            repository.upsertWeight(epochDay, weightKg, note, averageWithExisting = media)
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

    fun restore(id: String) {
        viewModelScope.launch { repository.restoreWeight(id) }
    }
}
