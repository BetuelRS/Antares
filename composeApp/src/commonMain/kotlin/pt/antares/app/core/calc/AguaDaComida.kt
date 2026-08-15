package pt.antares.app.core.calc

import pt.antares.app.core.nutrition.MicroTotals
import pt.antares.app.core.nutrition.Nutrients
import kotlin.math.roundToInt

/**
 * A água que veio da comida, em mililitros.
 *
 * Não entra no contador do dia nem na meta. A meta são 35 ml por quilo, uma regra prática
 * sobre o que se **bebe**, e somar-lhe a sopa dava-a por cumprida sem ninguém beber mais.
 * Este número existe para aparecer ao lado, dito como o que é.
 *
 * Sai nulo quando não há como saber. Menos de metade dos alimentos do catálogo declara
 * teor de água, e um total somado sobre um quarto do prato diria «bebeste 300 ml da comida»
 * quando o que se sabe é que não se mediu o resto.
 */
object AguaDaComida {

    // A mesma fração que o [pt.antares.app.core.nutrition.DailyGap] exige para avisar de uma
    // lacuna, e pela mesma razão: metade do prato analisado é o mínimo para o número falar
    // do dia e não da parte do dia que por acaso tinha análise.
    const val COBERTURA_MINIMA = 0.5

    fun mlDoDia(totals: MicroTotals): Int? {
        if (totals.totalKcal <= 0) return null

        val medido = totals.measuredKcalByKey[Nutrients.WATER] ?: 0.0
        if (medido / totals.totalKcal < COBERTURA_MINIMA) return null

        // Um grama de água é um mililitro. A densidade só se afasta disso às décimas com a
        // temperatura, e a conta seguinte é sobre litros bebidos ao longo de um dia.
        return (totals.byKey[Nutrients.WATER] ?: 0.0).roundToInt()
    }
}
