package pt.antares.app.feature.running.domain

import kotlinx.serialization.Serializable

@Serializable
enum class ActivityType { RUN, WALK, RIDE }

@Serializable
enum class RunStatus { DONE, DISCARDED }

data class GeoSample(
    val tMs: Long,
    val lat: Double,
    val lon: Double,
    val altM: Double? = null,
    val accM: Double = 0.0,
    val speedMps: Double? = null,
)

@Serializable
data class Split(
    val index: Int,
    val distanceM: Double,
    val movingMs: Long,
    val paceSecPerKm: Int,
    val kcal: Int,
)

data class RunMetrics(
    val distanceM: Double = 0.0,
    val elapsedMs: Long = 0L,
    val movingMs: Long = 0L,
    val avgPaceSecPerKm: Int = 0,
    val curSpeedMps: Double = 0.0,
    val kcal: Int = 0,
    val elevGainM: Double = 0.0,
    val paused: Boolean = false,
)

data class RunResult(
    val metrics: RunMetrics,
    val splits: List<Split>,
)
