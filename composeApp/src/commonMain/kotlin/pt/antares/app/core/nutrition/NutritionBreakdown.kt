package pt.antares.app.core.nutrition

import pt.antares.app.core.model.LifeStage
import pt.antares.app.core.model.Sex
import kotlin.math.roundToInt

data class MicroValue(
    val key: String,

    val amount: Double,
    val unit: String,

    val pctDv: Int?,
)

object NutrientClaim {
    const val SOURCE_OF = 15
    const val HIGH_IN = 30
}

data class NutritionBreakdown(
    val labels: List<MicroValue> = emptyList(),
    val vitamins: List<MicroValue> = emptyList(),
    val minerals: List<MicroValue> = emptyList(),
    val others: List<MicroValue> = emptyList(),
) {

    val micronutrients: List<MicroValue> get() = vitamins + minerals

    val all: List<MicroValue> get() = labels + micronutrients + others

    val isEmpty: Boolean get() = all.isEmpty()

    val hasMicronutrients: Boolean get() = micronutrients.isNotEmpty()

    val highlights: List<MicroValue>
        get() = (micronutrients + labels.filter { it.key == Nutrients.FIBER })
            .filter { (it.pctDv ?: 0) >= NutrientClaim.SOURCE_OF }
            .sortedByDescending { it.pctDv ?: 0 }

    val overLimits: List<MicroValue>
        get() = labels.filter {
            (it.key == Nutrients.SODIUM || it.key == Nutrients.SAT_FAT) &&
                (it.pctDv ?: 0) >= OVER_LIMIT
        }

    companion object {

        const val OVER_LIMIT = 100
    }
}

object NutritionFacts {

    private val VITAMIN_ORDER = Nutrients.VITAMINS

    private val MINERAL_ORDER = Nutrients.MINERALS.filterNot { it == Nutrients.SODIUM }

    fun build(
        per100: Map<String, Double>,
        amountG: Double,
        reference: EfsaReference?,
        sex: Sex,
        stage: LifeStage? = null,
    ): NutritionBreakdown {
        val factor = amountG / 100.0
        fun valueFor(key: String): MicroValue? {
            val value = per100[key] ?: return null

            if (value <= 0.0) return null
            val amount = value * factor
            val drv = reference?.forKey(key)?.forPerson(sex, stage)
            val pct = if (drv != null && drv > 0) (amount / drv * 100).roundToInt() else null
            return MicroValue(key, amount, Nutrients.unitOf(key), pct)
        }
        return NutritionBreakdown(
            labels = Nutrients.LABEL.mapNotNull { valueFor(it) },
            vitamins = VITAMIN_ORDER.mapNotNull { valueFor(it) },
            minerals = MINERAL_ORDER.mapNotNull { valueFor(it) },
            others = Nutrients.OTHERS.mapNotNull { valueFor(it) },
        )
    }
}
