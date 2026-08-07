package pt.antares.app.feature.workout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import pt.antares.app.feature.workout.data.ExerciseRecord
import pt.antares.app.feature.workout.data.MuscleVolumeStat
import pt.antares.app.feature.workout.data.SessionBreakdown
import pt.antares.app.feature.workout.data.SessionSummary
import pt.antares.app.feature.workout.data.WorkoutHistoryRepository

class WorkoutHistoryViewModel(
    repository: WorkoutHistoryRepository,
) : ViewModel() {
    val history: StateFlow<List<SessionSummary>> = repository.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
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
