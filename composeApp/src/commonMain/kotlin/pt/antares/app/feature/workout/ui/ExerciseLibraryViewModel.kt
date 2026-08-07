package pt.antares.app.feature.workout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import pt.antares.app.feature.workout.data.ExerciseLibraryRepository
import pt.antares.app.feature.workout.model.Exercise

data class LibraryFilters(
    val query: String = "",
    val muscle: String? = null,
    val equipment: String? = null,
    val level: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseLibraryViewModel(
    private val repository: ExerciseLibraryRepository,
) : ViewModel() {

    private val _filters = MutableStateFlow(LibraryFilters())
    val filters: StateFlow<LibraryFilters> = _filters

    val results: StateFlow<List<Exercise>> = _filters
        .flatMapLatest { f -> repository.observeFiltered(f.query, f.muscle, f.equipment, f.level) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(q: String) { _filters.value = _filters.value.copy(query = q) }
    fun setMuscle(m: String?) { _filters.value = _filters.value.copy(muscle = m) }
    fun setEquipment(e: String?) { _filters.value = _filters.value.copy(equipment = e) }
    fun setLevel(l: String?) { _filters.value = _filters.value.copy(level = l) }
}
