package pt.antares.app.core.nutrition

import pt.antares.app.core.model.LifeStage
import pt.antares.app.core.model.Sex
import kotlin.math.roundToInt

/**
 * Valor de referência diário da EFSA para um nutriente. Separado por sexo, e ajustado
 * depois pela fase da vida — gravidez e amamentação sobem várias destas referências.
 */
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

        /**
         * Lê a tabela de referências de um CSV empacotado com a app. Toda a linha
         * malformada é saltada em silêncio: uma referência a menos esconde um nutriente
         * do ecrã, enquanto rebentar aqui impedia a app inteira de arrancar.
         */
        fun parse(csv: String): EfsaReference {
            val map = csv.trim().lines()
                // A primeira linha é o cabeçalho.
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

/**
 * Micronutrientes somados de um período, com a honestidade a acompanhar cada número.
 *
 * A soma sozinha mentiria: metade da comida do dia pode não declarar ferro nenhum, e o
 * total daria a entender que a pessoa comeu pouco. Por isso guardam-se também as calorias
 * que traziam medição de cada nutriente — é a fração do prato sobre a qual o número fala.
 */
data class MicroTotals(
    val byKey: Map<String, Double>,

    val measuredKcalByKey: Map<String, Double>,
    val totalKcal: Double,

    // Calorias que traziam pelo menos um micronutriente. Serve para o aviso geral do ecrã,
    // antes de se olhar para qualquer nutriente em concreto.
    val measuredAnyKcal: Double = 0.0,
) {

    val measuredAnyPct: Int
        get() = if (totalKcal > 0) {
            (measuredAnyKcal / totalKcal * 100).roundToInt().coerceIn(0, 100)
        } else {
            // Sem comida registada não há cobertura em falta para avisar.
            100
        }

    companion object {
        val EMPTY = MicroTotals(emptyMap(), emptyMap(), 0.0, 0.0)

        private val LABEL_ONLY_KEYS = setOf("fiber_g", "sugars_g", "satFat_g", Nutrients.SODIUM)

        // Estes quatro vêm no rótulo de qualquer embalagem: tê-los não prova que o alimento
        // foi analisado, e sem esta distinção a app anunciava micronutrientes que não tem.
        fun hasRealMicros(keys: Set<String>): Boolean = keys.any { it !in LABEL_ONLY_KEYS }
    }
}

data class MicroCoverage(
    val key: String,
    val intake: Double,
    val drv: Double,
    val unit: String,
    val coveragePct: Int,

    // Distingue zero por não se ter comido de zero por não se saber. O ecrã diz coisas
    // diferentes em cada caso.
    val hasData: Boolean,

    // Sobre que fração das calorias do período este número fala.
    val measuredPct: Int = 100,
) {

    // Abaixo de 70% de cobertura o valor é apresentado como parcial: falta demasiada
    // comida por analisar para o comparar com a referência sem ressalva.
    val isPartial: Boolean get() = hasData && measuredPct < PARTIAL_BELOW

    companion object {
        const val PARTIAL_BELOW = 70
    }
}

object CoverageCalc {

    /**
     * @param days quantos dias os [totals] cobrem. As referências da EFSA são
     *   **diárias**, por isso somar uma semana e compará-la com elas dá sete
     *   vezes o valor certo: quem cumprisse 100% todos os dias lia 700%. Com
     *   `days` a ingestão é reduzida a média por dia antes de comparar.
     */
    fun compute(
        totals: MicroTotals,
        sex: Sex,
        drvs: List<Drv>,
        stage: LifeStage? = null,
        days: Int = 1,
    ): List<MicroCoverage> {
        val porDia = days.coerceAtLeast(1)
        // Percorre as referências e não os totais: um nutriente sem nenhum registo tem de
        // aparecer na lista com `hasData` a falso, e não desaparecer do ecrã.
        return drvs.map { drv ->
            val intake = totals.byKey[drv.key]?.div(porDia)
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
}
