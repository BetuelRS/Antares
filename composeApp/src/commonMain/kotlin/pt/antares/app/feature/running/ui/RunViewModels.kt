package pt.antares.app.feature.running.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pt.antares.app.core.calc.HistoryFilter
import pt.antares.app.core.calc.Mes
import pt.antares.app.core.database.entities.RunEntity
import pt.antares.app.feature.running.RunController
import pt.antares.app.feature.running.RunLiveState
import pt.antares.app.feature.running.data.RunRepository
import pt.antares.app.feature.running.domain.ActivityType
import pt.antares.app.feature.running.domain.RunPrCalc
import pt.antares.app.feature.running.domain.Split

class RunSummaryViewModel(
    private val controller: RunController,
    private val repository: RunRepository,
) : ViewModel() {

    val state: StateFlow<RunLiveState> = controller.state

    fun splits(): List<Split> = controller.lastResult.value?.splits ?: emptyList()

    fun save(name: String, note: String, onSaved: () -> Unit) {
        val s = state.value
        viewModelScope.launch {
            repository.save(
                type = s.type,
                metrics = s.metrics,
                path = s.path,
                splits = splits(),
                name = name.ifBlank { "" },
                note = note,
            )
            controller.discard()
            onSaved()
        }
    }

    fun discard(onDone: () -> Unit) {
        controller.discard()
        onDone()
    }
}

data class RunHistoryState(
    val runs: List<RunEntity> = emptyList(),
    val pr1kMs: Long? = null,
    val pr5kMs: Long? = null,
    val pr10kMs: Long? = null,

    val totalRuns: Int = 0,
    val totalDistanceM: Double = 0.0,
    val totalMovingS: Long = 0,

    val mes: Mes? = null,
    val tipo: ActivityType? = null,
) {
    val meses: List<Mes> get() = HistoryFilter.mesesDe(runs.map { it.startedAt })

    /** Os tipos que estão de facto no histórico: oferecer «bicicleta» a quem nunca pedalou é ruído. */
    val tipos: List<ActivityType> get() = runs.map { it.type }.distinct()

    val visiveis: List<RunEntity>
        get() = HistoryFilter.porMes(runs, mes) { it.startedAt }
            .filter { tipo == null || it.type == tipo }
}

class RunHistoryViewModel(
    private val repository: RunRepository,
) : ViewModel() {

    // Os filtros vivem à parte do histórico e cruzam-se com ele: assim uma corrida gravada
    // agora entra na lista sem apagar o filtro que estava escolhido.
    private val filtros = MutableStateFlow<Pair<Mes?, ActivityType?>>(null to null)

    val state: StateFlow<RunHistoryState> = combine(
        repository.observeHistory(),
        filtros,
    ) { runs, (mes, tipo) ->
        val splitsPerRun = runs.map { repository.splitsOf(it) }
        RunHistoryState(
            runs = runs,
            pr1kMs = RunPrCalc.bestTimeMs(splitsPerRun, 1),
            pr5kMs = RunPrCalc.bestTimeMs(splitsPerRun, 5),
            pr10kMs = RunPrCalc.bestTimeMs(splitsPerRun, 10),
            // Os totais e os recordes ficam de fora do filtro, de propósito: um recorde
            // pessoal é de sempre, e filtrá-lo por fevereiro tornava-o outra coisa.
            totalRuns = runs.size,
            totalDistanceM = runs.sumOf { it.distanceM },
            totalMovingS = runs.sumOf { it.movingS },
            mes = mes,
            tipo = tipo,
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RunHistoryState())

    fun setMes(mes: Mes?) { filtros.value = mes to filtros.value.second }

    fun setTipo(tipo: ActivityType?) { filtros.value = filtros.value.first to tipo }
}

data class RunDetailState(
    val run: RunEntity? = null,
    val path: List<Pair<Double, Double>> = emptyList(),
    val splits: List<Split> = emptyList(),
)

class RunDetailViewModel(
    private val repository: RunRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RunDetailState())
    val state: StateFlow<RunDetailState> = _state

    fun load(id: String) {
        viewModelScope.launch {
            val run = repository.byId(id) ?: return@launch
            _state.value = RunDetailState(
                run = run,
                path = repository.decodePath(run),
                splits = repository.splitsOf(run),
            )
        }
    }
}
