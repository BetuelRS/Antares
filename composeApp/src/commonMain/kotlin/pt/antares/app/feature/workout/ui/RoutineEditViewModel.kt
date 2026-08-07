package pt.antares.app.feature.workout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import pt.antares.app.feature.workout.data.RoutineRepository
import pt.antares.app.feature.workout.data.RoutineWithItems

@OptIn(ExperimentalCoroutinesApi::class)
class RoutineEditViewModel(
    private val repository: RoutineRepository,
) : ViewModel() {

    private val routineId = MutableStateFlow<String?>(null)

    val detail: StateFlow<RoutineWithItems?> = routineId
        .filterNotNull()
        .flatMapLatest { repository.observeDetail(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted

    fun start(id: String) { routineId.value = id }

    fun rename(name: String) {
        val id = routineId.value ?: return
        viewModelScope.launch { repository.rename(id, name) }
    }

    fun updateTargets(itemId: String, sets: Int, repsMin: Int, repsMax: Int, weightKg: Double?, restSec: Int) =
        viewModelScope.launch { repository.updateTargets(itemId, sets, repsMin, repsMax, weightKg, restSec) }

    fun move(itemId: String, up: Boolean) {
        val id = routineId.value ?: return
        viewModelScope.launch { repository.move(id, itemId, up) }
    }

    fun setSuperset(itemId: String, group: Int?) =
        viewModelScope.launch { repository.setSuperset(itemId, group) }

    fun deleteItem(itemId: String) = viewModelScope.launch { repository.deleteItem(itemId) }

    fun deleteRoutine() {
        val id = routineId.value ?: return
        viewModelScope.launch {
            repository.deleteRoutine(id)
            _deleted.value = true
        }
    }
}

class RoutineItemPickViewModel(
    private val repository: RoutineRepository,
) : ViewModel() {
    fun add(routineId: String, exerciseId: String, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.addItem(routineId, exerciseId)
            onDone()
        }
    }
}
