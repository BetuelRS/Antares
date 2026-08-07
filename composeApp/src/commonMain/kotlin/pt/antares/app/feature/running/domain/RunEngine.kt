package pt.antares.app.feature.running.domain

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class RunEngine(
    private val type: ActivityType,
    private val weightKg: Double,
    private val autoPauseEnabled: Boolean = true,
) {
    private companion object {
        const val MAX_ACC_M = 30.0
        const val MIN_MOVE_M = 3.0
        const val AUTO_PAUSE_SPEED = 0.5
        const val AUTO_PAUSE_HOLD_MS = 10_000L
        const val ELEV_WINDOW = 5
        const val EARTH_R = 6_371_000.0
    }

    private val maxSpeed = if (type == ActivityType.RIDE) 25.0 else 12.0

    private var anchorLat = 0.0
    private var anchorLon = 0.0
    private var anchorT = 0L
    private var firstT = 0L
    private var lastT = 0L
    private var started = false

    private var distanceM = 0.0
    private var movingMs = 0L
    private var curSpeedMps = 0.0
    private var kcalRide = 0.0
    private var paused = false

    private val elevWindow = ArrayDeque<Double>()
    private var smoothedAlt: Double? = null
    private var elevGainM = 0.0

    private val splits = mutableListOf<Split>()
    private var lastSplitDist = 0.0
    private var lastSplitMoving = 0L

    fun onSample(s: GeoSample): RunMetrics {

        if (s.accM > MAX_ACC_M) return metrics()

        if (!started) {
            started = true
            firstT = s.tMs; lastT = s.tMs
            anchorLat = s.lat; anchorLon = s.lon; anchorT = s.tMs
            pushElevation(s.altM)
            return metrics()
        }

        if (s.tMs <= lastT) return metrics()

        val dFromAnchor = haversine(anchorLat, anchorLon, s.lat, s.lon)
        val dtFromAnchorMs = s.tMs - anchorT
        val segSpeed = if (dtFromAnchorMs > 0) dFromAnchor / (dtFromAnchorMs / 1000.0) else 0.0

        if (segSpeed > maxSpeed) {

            return metrics()
        }

        val dtSample = s.tMs - lastT
        val movingBefore = movingMs
        if (!autoPauseEnabled || !paused) movingMs += dtSample
        lastT = s.tMs

        if (type == ActivityType.RIDE && (!autoPauseEnabled || !paused)) {
            kcalRide += metForCycling(curSpeedMps) * weightKg * (dtSample / 3_600_000.0)
        }

        if (dFromAnchor >= MIN_MOVE_M) {

            addDistanceWithSplits(
                fromDist = distanceM,
                segDist = dFromAnchor,
                segMovingStart = movingBefore,
                dtMovingMs = movingMs - movingBefore,
            )
            distanceM += dFromAnchor
            curSpeedMps = segSpeed
            pushElevation(s.altM)
            anchorLat = s.lat; anchorLon = s.lon; anchorT = s.tMs
            paused = false
        } else {

            curSpeedMps = segSpeed
            val idleMs = s.tMs - anchorT
            if (autoPauseEnabled && idleMs >= AUTO_PAUSE_HOLD_MS && segSpeed < AUTO_PAUSE_SPEED) {
                paused = true
            }
        }
        return metrics()
    }

    fun finish(): RunResult {
        val partial = distanceM - lastSplitDist
        if (partial > 1.0) {
            val dt = movingMs - lastSplitMoving
            splits += Split(
                index = splits.size + 1,
                distanceM = partial,
                movingMs = dt,
                paceSecPerKm = paceSecPerKm(partial, dt),
                kcal = splitKcal(partial, dt).roundToInt(),
            )
        }
        return RunResult(metrics = metrics(), splits = splits.toList())
    }

    private fun addDistanceWithSplits(fromDist: Double, segDist: Double, segMovingStart: Long, dtMovingMs: Long) {
        var boundary = ((fromDist / 1000.0).toInt() + 1) * 1000.0
        while (fromDist + segDist >= boundary) {
            val distInto = boundary - fromDist
            val f = distInto / segDist
            val movingAtCross = segMovingStart + (f * dtMovingMs).toLong()
            val splitDist = boundary - lastSplitDist
            val splitMoving = movingAtCross - lastSplitMoving
            splits += Split(
                index = splits.size + 1,
                distanceM = splitDist,
                movingMs = splitMoving,
                paceSecPerKm = paceSecPerKm(splitDist, splitMoving),
                kcal = splitKcal(splitDist, splitMoving).roundToInt(),
            )
            lastSplitDist = boundary
            lastSplitMoving = movingAtCross
            boundary += 1000.0
        }
    }

    private fun pushElevation(alt: Double?) {
        if (alt == null) return
        elevWindow.addLast(alt)
        if (elevWindow.size > ELEV_WINDOW) elevWindow.removeFirst()
        val avg = elevWindow.average()
        val prev = smoothedAlt
        if (prev != null) elevGainM += max(0.0, avg - prev)
        smoothedAlt = avg
    }

    private fun splitKcal(distM: Double, movingMsSeg: Long): Double = when (type) {
        ActivityType.RUN -> 1.0 * weightKg * (distM / 1000.0)
        ActivityType.WALK -> 0.53 * weightKg * (distM / 1000.0)

        ActivityType.RIDE -> {
            val speed = if (movingMsSeg > 0) distM / (movingMsSeg / 1000.0) else 0.0
            metForCycling(speed) * weightKg * (movingMsSeg / 3_600_000.0)
        }
    }

    private fun totalKcal(): Int = when (type) {
        ActivityType.RUN -> (1.0 * weightKg * (distanceM / 1000.0)).roundToInt()
        ActivityType.WALK -> (0.53 * weightKg * (distanceM / 1000.0)).roundToInt()
        ActivityType.RIDE -> kcalRide.roundToInt()
    }

    private fun metrics(): RunMetrics {
        val elapsed = if (started) lastT - firstT else 0L
        val moving = if (autoPauseEnabled) movingMs else elapsed
        return RunMetrics(
            distanceM = distanceM,
            elapsedMs = elapsed,
            movingMs = moving,
            avgPaceSecPerKm = paceSecPerKm(distanceM, moving),
            curSpeedMps = curSpeedMps,
            kcal = totalKcal(),
            elevGainM = elevGainM,
            paused = paused,
        )
    }

    private fun paceSecPerKm(distM: Double, movingMsVal: Long): Int {
        if (distM < 1.0) return 0
        val km = distM / 1000.0
        val sec = movingMsVal / 1000.0
        return (sec / km).roundToInt()
    }
}

internal fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6_371_000.0
    val p1 = lat1 * PI / 180.0
    val p2 = lat2 * PI / 180.0
    val dp = (lat2 - lat1) * PI / 180.0
    val dl = (lon2 - lon1) * PI / 180.0
    val a = sin(dp / 2) * sin(dp / 2) + cos(p1) * cos(p2) * sin(dl / 2) * sin(dl / 2)
    return r * 2 * atan2(sqrt(a), sqrt(1 - a))
}

internal fun metForCycling(speedMps: Double): Double {
    val kmh = speedMps * 3.6
    return when {
        kmh < 16 -> 4.0
        kmh < 19 -> 6.8
        kmh < 22 -> 8.0
        kmh < 25 -> 10.0
        kmh < 30 -> 12.0
        else -> 15.8
    }
}
