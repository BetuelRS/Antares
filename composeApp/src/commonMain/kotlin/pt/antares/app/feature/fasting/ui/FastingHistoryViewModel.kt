package pt.antares.app.feature.fasting.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import pt.antares.app.core.calc.FastingStats
import pt.antares.app.core.calc.FastingStatsResult
import pt.antares.app.core.database.entities.FastingSessionEntity
import pt.antares.app.feature.fasting.data.FastingRepository
import pt.antares.app.feature.fasting.data.toFinishedFast

data class FastingHistoryState(
    val sessions: List<FastingSessionEntity> = emptyList(),
    val stats: FastingStatsResult = FastingStatsResult(0, 0, 0, 0, 0f, 0L),
)

class FastingHistoryViewModel(
    repository: FastingRepository,
) : ViewModel() {

    val state: StateFlow<FastingHistoryState> = repository.observeFinished()
        .map { finished ->

            val now = Clock.System.now().toEpochMilliseconds()
            val stats = FastingStats.compute(
                sessions = finished.map { it.toFinishedFast() },
                now = now,
                zone = TimeZone.currentSystemDefault(),
            )

            FastingHistoryState(sessions = finished.sortedByDescending { it.startedAt }, stats = stats)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FastingHistoryState())
}
