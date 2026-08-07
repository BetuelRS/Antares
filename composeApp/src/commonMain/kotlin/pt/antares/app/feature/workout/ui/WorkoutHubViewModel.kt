package pt.antares.app.feature.workout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pt.antares.app.core.database.entities.RoutineEntity
import pt.antares.app.feature.workout.data.RoutineRepository
import pt.antares.app.feature.workout.data.WorkoutSessionRepository

class WorkoutHubViewModel(
    private val routineRepository: RoutineRepository,
    sessionRepository: WorkoutSessionRepository,
) : ViewModel() {

    val routines: StateFlow<List<RoutineEntity>> = routineRepository.observeRoutines()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val hasActiveSession: StateFlow<Boolean> = sessionRepository.observeActive()
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun createRoutine(name: String, onCreated: (String) -> Unit) {
        viewModelScope.launch { onCreated(routineRepository.createRoutine(name)) }
    }
}
