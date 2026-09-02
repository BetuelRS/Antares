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

    /**
     * Grava a ordem que o dedo deixou. Quem chama guarda a ordem **anterior** para a poder
     * desfazer: mover era a única acção do editor sem desfazer, e é mais fácil de fazer por
     * engano do que apagar — as setas eram pequenas e estavam coladas ao menu.
     */
    fun reordenar(ordem: List<String>) {
        viewModelScope.launch { repository.reorderItems(ordem) }
    }

    /** A ordem em que os exercícios estão agora, para se poder voltar a ela. */
    fun ordemActual(): List<String> = detail.value?.items.orEmpty().map { it.item.id }

    fun duplicar(nome: String, aoTerminar: (String) -> Unit) {
        val id = routineId.value ?: return
        viewModelScope.launch { repository.duplicateRoutine(id, nome)?.let(aoTerminar) }
    }

    fun setSuperset(itemId: String, group: Int?) =
        viewModelScope.launch { repository.setSuperset(itemId, group) }

    fun deleteItem(itemId: String) = viewModelScope.launch { repository.deleteItem(itemId) }

    fun restoreItem(itemId: String) = viewModelScope.launch { repository.restoreItem(itemId) }

    fun deleteRoutine() {
        val id = routineId.value ?: return
        viewModelScope.launch {
            repository.deleteRoutine(id)
            _deleted.value = true
        }
    }

    /** O identificador é guardado pelo ecrã antes de ele fechar: aqui já não há estado. */
    fun restoreRoutine(id: String) = viewModelScope.launch { repository.restoreRoutine(id) }
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
