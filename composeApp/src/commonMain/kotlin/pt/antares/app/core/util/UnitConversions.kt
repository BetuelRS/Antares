package pt.antares.app.core.util

import pt.antares.app.core.model.UnitSystem
import kotlin.math.roundToInt

/**
 * Conversões de apresentação. A base guarda sempre quilos, centímetros e quilocalorias; a
 * preferência de unidades muda o que se vê e nunca o que está gravado, por isso mudá-la
 * não pode alterar nenhum registo.
 */
// Dezoito funções, e é para ter dezoito: são nove pares de ida e volta mais os três
// apresentadores. Parti-las por ficheiros faria alguém converter num sítio e esquecer o
// inverso no outro — que é precisamente o erro que uma conversão de unidades permite.
@Suppress("TooManyFunctions")
object UnitConversions {
    // Fatores exatos por definição, não arredondados: converter ida e volta tem de devolver
    // o mesmo número.
    const val KG_PER_LB = 0.45359237
    const val CM_PER_IN = 2.54
    const val KM_PER_MI = 1.609344
    const val G_PER_OZ = 28.349523125

    // A onça líquida americana, que é a que interessa a quem escolhe o imperial numa app de
    // nutrição. Não é a britânica, e não é 1/16 da onça de massa: são unidades diferentes com
    // o mesmo nome.
    const val ML_PER_FLOZ = 29.5735295625

    fun kgToLb(kg: Double): Double = kg / KG_PER_LB
    fun lbToKg(lb: Double): Double = lb * KG_PER_LB

    fun kmToMi(km: Double): Double = km / KM_PER_MI
    fun miToKm(mi: Double): Double = mi * KM_PER_MI

    fun gToOz(grams: Double): Double = grams / G_PER_OZ
    fun ozToG(oz: Double): Double = oz * G_PER_OZ

    fun mlToFlOz(ml: Double): Double = ml / ML_PER_FLOZ
    fun flOzToMl(flOz: Double): Double = flOz * ML_PER_FLOZ

    /**
     * A quantidade de uma porção como se escreve e como se lê.
     *
     * **A app guarda sempre gramas**, porque é em gramas que a nutrição está medida — cada
     * alimento traz os seus valores por 100 g, e a conta do dia é `gramas ÷ 100`. O que se
     * mostra é outra coisa: num líquido são mililitros, e num sistema imperial são onças.
     *
     * A [densidade] é o que fecha a diferença entre as duas. Sem ela, um mililitro valia uma
     * grama — certo para a água, e errado para tudo o resto: 200 ml de azeite pesam 182 g, e
     * a app contava-lhes 200. Nula quer dizer que ninguém a mediu, e aí volta a valer 1,00.
     */
    fun portionToDisplay(
        quantity: Double,
        system: UnitSystem,
        liquid: Boolean,
        densidade: Double? = null,
    ): Double {
        val emMl = if (liquid) quantity / densidadeUtil(densidade) else quantity
        return when {
            system != UnitSystem.IMPERIAL -> emMl
            liquid -> mlToFlOz(emMl)
            else -> gToOz(quantity)
        }
    }

    fun portionToStored(
        shown: Double,
        system: UnitSystem,
        liquid: Boolean,
        densidade: Double? = null,
    ): Double {
        val emMl = when {
            system != UnitSystem.IMPERIAL -> shown
            liquid -> flOzToMl(shown)
            else -> return ozToG(shown)
        }
        return if (liquid) emMl * densidadeUtil(densidade) else emMl
    }

    /**
     * A densidade a usar: a medida, ou 1,00.
     *
     * Uma densidade zero ou negativa não é uma medição — é um erro que dividia por zero — e
     * por isso também cai no valor da água.
     */
    private fun densidadeUtil(densidade: Double?): Double =
        densidade?.takeIf { it > 0 } ?: DENSIDADE_DA_AGUA

    /** Um grama por mililitro, que é a definição do grama. */
    const val DENSIDADE_DA_AGUA = 1.0

    // Arredonda para polegadas inteiras antes de separar pés: sem isso, 5 pés e 12
    // polegadas era um resultado possível.
    /**
     * Metros para pés. O desnível de uma corrida lê-se em pés para quem usa imperial, e não
     * havia por onde: era o último número da corrida ainda preso ao sistema métrico.
     */
    fun mToFt(m: Double): Double = m / M_PER_FT

    private const val M_PER_FT = 0.3048

    fun cmToFtIn(cm: Int): Pair<Int, Int> {
        val totalInches = (cm / CM_PER_IN).roundToInt()
        return Pair(totalInches / 12, totalInches % 12)
    }

    fun ftInToCm(feet: Int, inches: Int): Int =
        ((feet * 12 + inches) * CM_PER_IN).roundToInt()

    fun weightToDisplay(kg: Double, system: UnitSystem): Double =
        if (system == UnitSystem.IMPERIAL) kgToLb(kg) else kg

    fun distanceToDisplay(km: Double, system: UnitSystem): Double =
        if (system == UnitSystem.IMPERIAL) kmToMi(km) else km

    fun massToDisplay(grams: Double, system: UnitSystem): Double =
        if (system == UnitSystem.IMPERIAL) gToOz(grams) else grams

    /**
     * Um comprimento do corpo — cintura, braço, coxa — para ver. É a fita métrica, e não a
     * altura: a altura mostra-se em pés e polegadas, e uma cintura em pés não se lê.
     */
    fun lengthToDisplay(cm: Double, system: UnitSystem): Double =
        if (system == UnitSystem.IMPERIAL) cm / CM_PER_IN else cm

    /**
     * O ritmo é o inverso da distância: uma milha é mais longa do que um quilómetro, por isso
     * o mesmo ritmo dá **mais** segundos por milha. Dividir aqui em vez de multiplicar era um
     * erro que ainda daria um número plausível.
     */
    fun paceToDisplay(secPerKm: Int, system: UnitSystem): Int =
        if (system == UnitSystem.IMPERIAL) (secPerKm * KM_PER_MI).roundToInt() else secPerKm
}
