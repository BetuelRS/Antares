package pt.antares.app.feature.running

import kotlinx.coroutines.flow.MutableStateFlow
import pt.antares.app.feature.running.domain.ActivityType
import pt.antares.app.feature.running.domain.GeoSample
import pt.antares.app.feature.running.domain.RunEngine
import pt.antares.app.feature.running.domain.RunResult

internal object RunTrackingState {
    val live = MutableStateFlow(RunLiveState())
    val last = MutableStateFlow<RunResult?>(null)

    private var engine: RunEngine? = null
    private val path = mutableListOf<Pair<Double, Double>>()

    fun begin(type: ActivityType, weightKg: Double, autoPause: Boolean) {
        engine = RunEngine(type, weightKg, autoPause)
        path.clear()
        last.value = null
        live.value = RunLiveState(active = true, type = type, autoPause = autoPause)
    }

    fun onSample(sample: GeoSample) {
        val e = engine ?: return
        val metrics = e.onSample(sample)

        val usable = sample.accM <= 30.0
        if (usable) path.add(sample.lat to sample.lon)
        live.value = live.value.copy(
            metrics = metrics,
            path = path.toList(),
            hasFix = live.value.hasFix || usable,
        )
    }

    fun finish() {
        val e = engine ?: return
        last.value = e.finish()
        live.value = live.value.copy(active = false)
        engine = null
    }

    fun discard() {
        engine = null
        path.clear()
        last.value = null
        live.value = RunLiveState()
    }

    fun isActive(): Boolean = engine != null
}
