package pt.antares.app.core.nutrition

import pt.antares.app.core.model.LifeStage
import pt.antares.app.core.model.Sex
import kotlin.math.roundToInt

data class Drv(
    val key: String,
    val male: Double,
    val female: Double,
    val unit: String,
) {
    fun forSex(sex: Sex): Double = if (sex == Sex.MALE) male else female

    fun forPerson(sex: Sex, stage: LifeStage?): Double =
        LifeStageDrv.valueFor(key, stage, forSex(sex))
}

class EfsaReference(private val drvs: Map<String, Drv>) {

    fun all(): List<Drv> = drvs.values.toList()
    fun forKey(key: String): Drv? = drvs[key]

    companion object {

        fun parse(csv: String): EfsaReference {
            val map = csv.trim().lines()
                .drop(1)
                .mapNotNull { line ->
                    val c = line.split(",")
                    if (c.size < 4) return@mapNotNull null
                    val key = c[0].trim().ifBlank { return@mapNotNull null }
                    val male = c[1].trim().toDoubleOrNull() ?: return@mapNotNull null
                    val female = c[2].trim().toDoubleOrNull() ?: return@mapNotNull null
                    key to Drv(key, male, female, c[3].trim())
                }
                .toMap()
            return EfsaReference(map)
        }
    }
}

data class MicroTotals(
    val byKey: Map<String, Double>,

    val measuredKcalByKey: Map<String, Double>,
    val totalKcal: Double,

    val measuredAnyKcal: Double = 0.0,
) {

    val measuredAnyPct: Int
        get() = if (totalKcal > 0) {
            (measuredAnyKcal / totalKcal * 100).roundToInt().coerceIn(0, 100)
        } else {
            100
        }

    companion object {
        val EMPTY = MicroTotals(emptyMap(), emptyMap(), 0.0, 0.0)

        private val LABEL_ONLY_KEYS = setOf("fiber_g", "sugars_g", "satFat_g", Nutrients.SODIUM)

        fun hasRealMicros(keys: Set<String>): Boolean = keys.any { it !in LABEL_ONLY_KEYS }
    }
}

data class MicroCoverage(
    val key: String,
    val intake: Double,
    val drv: Double,
    val unit: String,
    val coveragePct: Int,

    val hasData: Boolean,

    val measuredPct: Int = 100,
) {

    val isPartial: Boolean get() = hasData && measuredPct < PARTIAL_BELOW

    companion object {
        const val PARTIAL_BELOW = 70
    }
}

object CoverageCalc {

    fun compute(
        totals: MicroTotals,
        sex: Sex,
        drvs: List<Drv>,
        stage: LifeStage? = null,
    ): List<MicroCoverage> =
        drvs.map { drv ->
            val intake = totals.byKey[drv.key]
            val ref = drv.forPerson(sex, stage)
            val measured = totals.measuredKcalByKey[drv.key] ?: 0.0
            MicroCoverage(
                key = drv.key,
                intake = intake ?: 0.0,
                drv = ref,
                unit = drv.unit,
                coveragePct = if (intake != null && ref > 0) (intake / ref * 100).roundToInt() else 0,
                hasData = intake != null,
                measuredPct = if (totals.totalKcal > 0) {
                    (measured / totals.totalKcal * 100).roundToInt().coerceIn(0, 100)
                } else {
                    100
                },
            )
        }
}
