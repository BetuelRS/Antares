package pt.antares.app.core.calc

import kotlin.math.roundToInt

object OneRepMax {
    fun epley(weightKg: Double, reps: Int): Double? {
        if (weightKg <= 0.0 || reps < 1 || reps > 12) return null
        return weightKg * (1.0 + reps / 30.0)
    }
}

data class SetEntry(
    val weightKg: Double,
    val reps: Int,
    val isWarmup: Boolean = false,
)

object VolumeCalc {
    fun volume(sets: List<SetEntry>): Double =
        sets.filter { !it.isWarmup }.sumOf { it.weightKg * it.reps }
}

data class ExercisePr(

    val bestOneRm: Double?,
    val bestWeightReps: Double,
)

object PrDetector {

    fun best(sets: List<SetEntry>): ExercisePr? {
        val work = sets.filter { !it.isWarmup && it.weightKg > 0 && it.reps > 0 }
        if (work.isEmpty()) return null
        val bestOneRm = work.mapNotNull { OneRepMax.epley(it.weightKg, it.reps) }.maxOrNull()
        val bestWr = work.maxOf { it.weightKg * it.reps }
        return ExercisePr(bestOneRm = bestOneRm, bestWeightReps = bestWr)
    }

    fun detect(previous: ExercisePr?, current: List<SetEntry>): PrResult {
        val now = best(current) ?: return PrResult(false, false)
        if (previous == null) {

            return PrResult(
                newOneRm = (now.bestOneRm ?: 0.0) > 0,
                newWeightReps = now.bestWeightReps > 0,
            )
        }

        val umRmNovo = when {
            now.bestOneRm == null -> false
            previous.bestOneRm == null -> true
            else -> now.bestOneRm > previous.bestOneRm + EPS
        }
        return PrResult(
            newOneRm = umRmNovo,
            newWeightReps = now.bestWeightReps > previous.bestWeightReps + EPS,
        )
    }

    private const val EPS = 1e-6
}

data class PrResult(val newOneRm: Boolean, val newWeightReps: Boolean) {
    val any: Boolean get() = newOneRm || newWeightReps
}

data class MuscleVolumeInput(
    val weightKg: Double,
    val reps: Int,
    val primaryMuscles: List<String>,
)

object MuscleVolume {
    const val OTHER = "other"

    fun aggregate(inputs: List<MuscleVolumeInput>): Map<String, Double> {
        val out = mutableMapOf<String, Double>()
        for (i in inputs) {
            val vol = i.weightKg * i.reps
            val targets = i.primaryMuscles.ifEmpty { listOf(OTHER) }
            for (m in targets) out[m] = (out[m] ?: 0.0) + vol
        }
        return out
    }
}

fun Double.round1(): Double = (this * 10).roundToInt() / 10.0
