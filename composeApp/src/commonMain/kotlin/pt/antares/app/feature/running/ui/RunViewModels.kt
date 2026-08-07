package pt.antares.app.feature.running.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pt.antares.app.core.database.entities.RunEntity
import pt.antares.app.feature.running.RunController
import pt.antares.app.feature.running.RunLiveState
import pt.antares.app.feature.running.data.RunRepository
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
)

class RunHistoryViewModel(
    private val repository: RunRepository,
) : ViewModel() {

    val state: StateFlow<RunHistoryState> = repository.observeHistory()
        .map { runs ->
            val splitsPerRun = runs.map { repository.splitsOf(it) }
            RunHistoryState(
                runs = runs,
                pr1kMs = RunPrCalc.bestTimeMs(splitsPerRun, 1),
                pr5kMs = RunPrCalc.bestTimeMs(splitsPerRun, 5),
                pr10kMs = RunPrCalc.bestTimeMs(splitsPerRun, 10),
                totalRuns = runs.size,
                totalDistanceM = runs.sumOf { it.distanceM },
                totalMovingS = runs.sumOf { it.movingS },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RunHistoryState())
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
