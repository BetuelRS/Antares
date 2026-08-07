package pt.antares.app.feature.workout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.antares.app.core.calc.PrDetector
import pt.antares.app.core.calc.SetEntry
import pt.antares.app.core.calc.VolumeCalc
import pt.antares.app.core.database.daos.ExerciseLibraryDao
import pt.antares.app.feature.workout.data.WorkoutSessionRepository

data class WorkoutSummaryState(
    val loading: Boolean = true,
    val durationMin: Int = 0,
    val volume: Double = 0.0,
    val setCount: Int = 0,
    val prLabels: List<String> = emptyList(),
)

class WorkoutSummaryViewModel(
    private val sessionRepository: WorkoutSessionRepository,
    private val exerciseDao: ExerciseLibraryDao,
) : ViewModel() {

    private val _state = MutableStateFlow(WorkoutSummaryState())
    val state: StateFlow<WorkoutSummaryState> = _state

    fun load(sessionId: String) {
        viewModelScope.launch {
            val session = sessionRepository.sessionById(sessionId)
            val sets = sessionRepository.setsForSession(sessionId)
            val working = sets.filter { !it.isWarmup }
            val volume = VolumeCalc.volume(sets.map { SetEntry(it.weightKg, it.reps, it.isWarmup) })
            val duration = session?.let {
                val end = it.endedAt ?: it.startedAt
                ((end - it.startedAt) / 60000L).toInt().coerceAtLeast(0)
            } ?: 0

            val byExercise = sets.groupBy { it.exerciseId }
            val prExerciseIds = byExercise.filter { (exId, exSets) ->
                val current = exSets.map { SetEntry(it.weightKg, it.reps, it.isWarmup) }
                val previousSets = sessionRepository.doneSetsForExercise(exId, sessionId)
                    .map { SetEntry(it.weightKg, it.reps, it.isWarmup) }
                PrDetector.detect(PrDetector.best(previousSets), current).any
            }.keys
            val names = exerciseDao.namesByIds(prExerciseIds.toList())
                .associate { it.id to it.namePt.ifBlank { it.nameEn } }
            val prLabels = prExerciseIds.mapNotNull { names[it] }

            _state.update {
                it.copy(
                    loading = false,
                    durationMin = duration,
                    volume = volume,
                    setCount = working.size,
                    prLabels = prLabels,
                )
            }
        }
    }
}
