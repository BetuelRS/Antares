package pt.antares.app.feature.workout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.antares.app.feature.workout.data.ExerciseLibraryRepository
import pt.antares.app.feature.workout.data.WorkoutHistoryRepository
import pt.antares.app.feature.workout.model.Exercise

data class ExerciseDetailState(
    val loading: Boolean = true,
    val exercise: Exercise? = null,
    val progress: List<Float> = emptyList(),
    val deleted: Boolean = false,
)

class ExerciseDetailViewModel(
    private val repository: ExerciseLibraryRepository,
    private val historyRepository: WorkoutHistoryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ExerciseDetailState())
    val state: StateFlow<ExerciseDetailState> = _state

    fun load(id: String) {
        viewModelScope.launch {
            val ex = repository.byId(id)
            val progress = historyRepository.exerciseVolumeSeries(id)
            _state.update { it.copy(loading = false, exercise = ex, progress = progress) }
        }
    }

    fun deleteCustom() {
        val ex = _state.value.exercise ?: return
        if (!ex.isCustom) return
        viewModelScope.launch {
            repository.deleteCustom(ex.id)
            _state.update { it.copy(deleted = true) }
        }
    }
}
