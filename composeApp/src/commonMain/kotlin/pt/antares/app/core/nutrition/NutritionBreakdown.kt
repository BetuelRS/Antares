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

/**
 * Os limiares do regulamento europeu das alegações nutricionais: 15% da dose de referência
 * para "fonte de", 30% para "rico em". São os mesmos que as embalagens usam, e é por isso
 * que a app pode destacar um nutriente sem inventar critério próprio.
 */
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

    // A fibra entra nos destaques apesar de ser um valor de rótulo: é a única das quatro
    // que é uma dose a atingir e não um limite a evitar.
    val highlights: List<MicroValue>
        get() = (micronutrients + labels.filter { it.key == Nutrients.FIBER })
            .filter { (it.pctDv ?: 0) >= NutrientClaim.SOURCE_OF }
            .sortedByDescending { it.pctDv ?: 0 }

    // O reverso: sódio e gordura saturada são os dois cujo valor de referência é um teto.
    // Passar 100% deles é o contrário de um destaque.
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

    // O sódio sai dos minerais porque já aparece nos valores de rótulo; listá-lo duas
    // vezes na mesma ficha daria a entender que são coisas diferentes.
    private val MINERAL_ORDER = Nutrients.MINERALS.filterNot { it == Nutrients.SODIUM }

    /**
     * A ficha nutricional de uma porção. Recebe os valores por 100 g e a quantidade; a
     * referência pode faltar, e nesse caso os valores saem sem percentagem em vez de
     * desaparecerem.
     */
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

            // Zero é tratado como ausente, tal como na normalização: as bases preenchem a
            // zero os campos que nunca mediram.
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
