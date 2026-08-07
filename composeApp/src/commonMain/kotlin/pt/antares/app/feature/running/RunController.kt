package pt.antares.app.feature.running

import kotlinx.coroutines.flow.StateFlow
import pt.antares.app.feature.running.domain.ActivityType
import pt.antares.app.feature.running.domain.RunMetrics
import pt.antares.app.feature.running.domain.RunResult

data class RunLiveState(
    val active: Boolean = false,
    val type: ActivityType = ActivityType.RUN,
    val autoPause: Boolean = true,
    val metrics: RunMetrics = RunMetrics(),

    val path: List<Pair<Double, Double>> = emptyList(),

    val hasFix: Boolean = false,
)

interface RunController {
    val state: StateFlow<RunLiveState>

    val lastResult: StateFlow<RunResult?>

    fun start(type: ActivityType, autoPause: Boolean)
    fun stop()
    fun discard()
}

class NoopRunController : RunController {
    private val _state = kotlinx.coroutines.flow.MutableStateFlow(RunLiveState())
    override val state: StateFlow<RunLiveState> = _state
    private val _last = kotlinx.coroutines.flow.MutableStateFlow<RunResult?>(null)
    override val lastResult: StateFlow<RunResult?> = _last
    override fun start(type: ActivityType, autoPause: Boolean) {}
    override fun stop() {}
    override fun discard() {}
}
