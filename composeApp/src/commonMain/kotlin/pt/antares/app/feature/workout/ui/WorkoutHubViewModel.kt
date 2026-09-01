package pt.antares.app.feature.workout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pt.antares.app.core.util.DayTicker
import pt.antares.app.feature.workout.data.CentroDeTreino
import pt.antares.app.feature.workout.data.RoutineRepository
import pt.antares.app.feature.workout.data.WorkoutHubRepository

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutHubViewModel(
    private val routineRepository: RoutineRepository,
    private val hubRepository: WorkoutHubRepository,
) : ViewModel() {

    /**
     * O `flatMapLatest` sobre o dia é o que faz o cartão de destaque virar à meia-noite: com
     * o dia lido uma vez na construção, uma app deixada aberta continuava a propor o treino
     * de ontem.
     */
    val state: StateFlow<CentroDeTreino> = DayTicker.today
        .flatMapLatest { hoje -> hubRepository.observe(hoje) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CentroDeTreino())

    fun createRoutine(name: String, onCreated: (String) -> Unit) {
        viewModelScope.launch { onCreated(routineRepository.createRoutine(name)) }
    }
}
