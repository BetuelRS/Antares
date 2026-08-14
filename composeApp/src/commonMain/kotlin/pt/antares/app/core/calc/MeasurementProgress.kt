package pt.antares.app.core.calc

import pt.antares.app.core.database.entities.BodyMeasurementEntity
import kotlin.math.abs

data class MeasurementProgress(
    val firstEpochDay: Long,
    val lastEpochDay: Long,
    val waistFrom: Double?,
    val waistTo: Double?,
    val fatFrom: Double?,
    val fatTo: Double?,
) {
    val waistDelta: Double? get() = both(waistFrom, waistTo) { a, b -> b - a }
    val fatDelta: Double? get() = both(fatFrom, fatTo) { a, b -> b - a }

    val spanDays: Long get() = lastEpochDay - firstEpochDay

    // Um centímetro e um ponto percentual são o limite de repetibilidade da fita e do
    // método: abaixo disso o que mudou foi a forma de medir, não o corpo.
    val isMeaningful: Boolean
        get() = (waistDelta?.let { abs(it) >= MIN_WAIST_CHANGE_CM } ?: false) ||
            (fatDelta?.let { abs(it) >= MIN_FAT_CHANGE_PCT } ?: false)

    private inline fun both(a: Double?, b: Double?, f: (Double, Double) -> Double): Double? =
        if (a != null && b != null) f(a, b) else null

    companion object {

        const val MIN_WAIST_CHANGE_CM = 1.0
        const val MIN_FAT_CHANGE_PCT = 1.0
    }
}

object MeasurementProgressCalc {

    /** Espera as medições por ordem cronológica; as datas vêm da lista, não são procuradas. */
    fun compute(entries: List<BodyMeasurementEntity>): MeasurementProgress? {
        if (entries.size < 2) return null
        val first = entries.first()
        val last = entries.last()

        // Cada medida procura as suas próprias pontas: quem mediu a cintura em janeiro e a
        // massa gorda em março tem duas comparações válidas com datas diferentes.
        val waistFirst = entries.firstOrNull { it.waistCm != null }
        val waistLast = entries.lastOrNull { it.waistCm != null }
        val fatFirst = entries.firstOrNull { it.bodyFatPct != null }
        val fatLast = entries.lastOrNull { it.bodyFatPct != null }
        return MeasurementProgress(
            firstEpochDay = first.epochDay,
            lastEpochDay = last.epochDay,
            // Comparação por identidade: quando só existe uma medição de cintura, ela é ao
            // mesmo tempo a primeira e a última, e ficariam as duas iguais a fingir uma
            // evolução de zero em vez de dizerem que ainda não há comparação.
            waistFrom = waistFirst?.takeIf { it !== waistLast }?.waistCm,
            waistTo = waistLast?.takeIf { it !== waistFirst }?.waistCm,
            fatFrom = fatFirst?.takeIf { it !== fatLast }?.bodyFatPct,
            fatTo = fatLast?.takeIf { it !== fatFirst }?.bodyFatPct,
        )
    }
}
