package pt.antares.app.feature.workout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import pt.antares.app.core.calc.HistoryFilter
import pt.antares.app.core.calc.Mes
import pt.antares.app.feature.workout.data.ExerciseOption
import pt.antares.app.feature.workout.data.ExerciseRecord
import pt.antares.app.feature.workout.data.MuscleVolumeStat
import pt.antares.app.feature.workout.data.SessionBreakdown
import pt.antares.app.feature.workout.data.SessionSummary
import pt.antares.app.feature.workout.data.WorkoutHistoryRepository

data class WorkoutHistoryState(
    val todos: List<SessionSummary> = emptyList(),
    val exercicios: List<ExerciseOption> = emptyList(),
    val mes: Mes? = null,
    val exercicioId: String? = null,
) {
    val meses: List<Mes> get() = HistoryFilter.mesesDe(todos.map { it.startedAt })

    /**
     * A lista depois dos filtros. Os dois cruzam-se: mês **e** exercício, e não um ou outro —
     * quem procura os agachamentos de fevereiro procura os dois ao mesmo tempo.
     */
    val visiveis: List<SessionSummary>
        get() = HistoryFilter.porMes(todos, mes) { it.startedAt }
            .filter { exercicioId == null || exercicioId in it.exerciseIds }

    val filtrado: Boolean get() = mes != null || exercicioId != null
}

class WorkoutHistoryViewModel(
    private val repository: WorkoutHistoryRepository,
) : ViewModel() {

    private val filtros = MutableStateFlow(WorkoutHistoryState())
    val state: StateFlow<WorkoutHistoryState> = filtros

    init {
        repository.observeHistory()
            .onEach { linhas -> filtros.update { it.copy(todos = linhas) } }
            .launchIn(viewModelScope)
        viewModelScope.launch {
            filtros.update { it.copy(exercicios = repository.exerciseOptions()) }
        }
    }

    fun setMes(mes: Mes?) = filtros.update { it.copy(mes = mes) }

    fun setExercicio(id: String?) = filtros.update { it.copy(exercicioId = id) }

    fun limparFiltros() = filtros.update { it.copy(mes = null, exercicioId = null) }
}

class WorkoutDetailViewModel(
    private val repository: WorkoutHistoryRepository,
) : ViewModel() {
    private val _breakdown = MutableStateFlow<SessionBreakdown?>(null)
    val breakdown: StateFlow<SessionBreakdown?> = _breakdown

    fun load(sessionId: String) {
        viewModelScope.launch { _breakdown.update { repository.breakdown(sessionId) } }
    }
}

data class WorkoutStatsState(
    val loading: Boolean = true,
    val muscleVolume: List<MuscleVolumeStat> = emptyList(),
    val records: List<ExerciseRecord> = emptyList(),
)

class WorkoutStatsViewModel(
    private val repository: WorkoutHistoryRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(WorkoutStatsState())
    val state: StateFlow<WorkoutStatsState> = _state

    private val weekAgo = Clock.System.now().toEpochMilliseconds() - 7L * 24 * 60 * 60 * 1000

    init {
        viewModelScope.launch {
            repository.observeMuscleVolume(weekAgo).collect { volumes ->
                _state.update { it.copy(loading = false, muscleVolume = volumes) }
            }
        }
        viewModelScope.launch {
            val recs = repository.records()
            _state.update { it.copy(records = recs) }
        }
    }
}
