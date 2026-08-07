package pt.antares.app.feature.workout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pt.antares.app.core.database.entities.RoutineEntity
import pt.antares.app.feature.workout.data.RoutineRepository
import pt.antares.app.feature.workout.data.RoutineWithItems

data class DayPlan(
    val dayOfWeek: Int,
    val routineId: String?,
    val routineName: String?,
)

data class ScheduleConfirm(
    val dayOfWeek: Int,
    val detail: RoutineWithItems,
)

data class WeeklyScheduleState(
    val days: List<DayPlan> = (1..7).map { DayPlan(it, null, null) },
    val routines: List<RoutineEntity> = emptyList(),
)

class WeeklyScheduleViewModel(
    private val routineRepository: RoutineRepository,
) : ViewModel() {

    val state: StateFlow<WeeklyScheduleState> = combine(
        routineRepository.observeSchedule(),
        routineRepository.observeRoutines(),
    ) { schedule, routines ->
        val byDay = schedule.associateBy { it.dayOfWeek }
        val names = routines.associate { it.id to it.name }
        WeeklyScheduleState(
            days = (1..7).map { day ->
                val routineId = byDay[day]?.routineId

                val name = routineId?.let { names[it] }
                DayPlan(day, if (name != null) routineId else null, name)
            },
            routines = routines,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeeklyScheduleState())

    private val _picking = MutableStateFlow<Int?>(null)
    val picking: StateFlow<Int?> = _picking

    private val _confirm = MutableStateFlow<ScheduleConfirm?>(null)
    val confirm: StateFlow<ScheduleConfirm?> = _confirm

    fun openPicker(dayOfWeek: Int) { _picking.value = dayOfWeek }
    fun closePicker() { _picking.value = null }

    fun chooseRoutine(dayOfWeek: Int, routineId: String) {
        _picking.value = null
        viewModelScope.launch {
            val detail = routineRepository.routineDetailOnce(routineId) ?: return@launch
            _confirm.value = ScheduleConfirm(dayOfWeek, detail)
        }
    }

    fun cancelConfirm() { _confirm.value = null }

    fun confirmSchedule() {
        val c = _confirm.value ?: return
        _confirm.value = null
        viewModelScope.launch {
            routineRepository.setScheduleDay(c.dayOfWeek, c.detail.routine.id)
        }
    }

    fun clearDay(dayOfWeek: Int) {
        _picking.value = null
        viewModelScope.launch { routineRepository.clearScheduleDay(dayOfWeek) }
    }
}
