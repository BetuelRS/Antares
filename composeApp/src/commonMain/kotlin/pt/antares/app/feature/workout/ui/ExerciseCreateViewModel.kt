package pt.antares.app.feature.workout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.antares.app.feature.workout.data.ExerciseLibraryRepository

data class ExerciseCreateState(
    val namePt: String = "",
    val nameEn: String = "",
    val category: String = "strength",
    val primaryMuscle: String? = null,
    val equipment: String? = null,
    val createdId: String? = null,
) {
    val valid: Boolean get() = namePt.isNotBlank() || nameEn.isNotBlank()
}

class ExerciseCreateViewModel(
    private val repository: ExerciseLibraryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ExerciseCreateState())
    val state: StateFlow<ExerciseCreateState> = _state

    fun setNamePt(v: String) = _state.update { it.copy(namePt = v) }
    fun setNameEn(v: String) = _state.update { it.copy(nameEn = v) }
    fun setCategory(v: String) = _state.update { it.copy(category = v) }
    fun setPrimaryMuscle(v: String?) = _state.update { it.copy(primaryMuscle = v) }
    fun setEquipment(v: String?) = _state.update { it.copy(equipment = v) }

    fun save() {
        val s = _state.value
        if (!s.valid) return
        viewModelScope.launch {
            val id = repository.createCustom(
                nameEn = s.nameEn,
                namePt = s.namePt,
                category = s.category,
                primaryMuscles = listOfNotNull(s.primaryMuscle),
                equipment = s.equipment,
            )
            _state.update { it.copy(createdId = id) }
        }
    }
}
