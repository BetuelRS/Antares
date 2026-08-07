package pt.antares.app.feature.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import pt.antares.app.core.model.FastingStatus
import pt.antares.app.feature.fasting.data.FastingRepository
import pt.antares.app.feature.profile.data.BodyMeasurementRepository
import pt.antares.app.feature.profile.data.ProfileRepository
import pt.antares.app.feature.running.data.RunRepository
import pt.antares.app.feature.workout.data.WorkoutHistoryRepository

data class AchievementsState(
    val achievements: List<Achievement> = emptyList(),
    val unlocked: Int = 0,
    val total: Int = AchievementCalc.total(),
)

class AchievementsViewModel(
    workoutHistoryRepository: WorkoutHistoryRepository,
    runRepository: RunRepository,
    fastingRepository: FastingRepository,
    profileRepository: ProfileRepository,
    measurementRepository: BodyMeasurementRepository,
) : ViewModel() {

    val state: StateFlow<AchievementsState> = combine(
        workoutHistoryRepository.observeHistory(),
        runRepository.observeHistory(),
        fastingRepository.observeHistory(),
        profileRepository.observeWeights(),
        measurementRepository.observeAll(),
    ) { workouts, runs, fasts, weights, measurements ->

        val cronologico = measurements.sortedBy { it.epochDay }
        val stats = AchievementStats(
            workouts = workouts.size,
            runKm = (runs.sumOf { it.distanceM } / 1000.0).toInt(),
            fastsCompleted = fasts.count { it.status == FastingStatus.COMPLETED },
            weighIns = weights.size,
            waistCmLost = AchievementCalc.bestDrop(cronologico.mapNotNull { it.waistCm }),
            bodyFatPctLost = AchievementCalc.bestDrop(cronologico.mapNotNull { it.bodyFatPct }),
        )
        val list = AchievementCalc.build(stats)
        AchievementsState(
            achievements = list.sortedByDescending { it.unlocked },
            unlocked = list.count { it.unlocked },
            total = AchievementCalc.total(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AchievementsState())
}
