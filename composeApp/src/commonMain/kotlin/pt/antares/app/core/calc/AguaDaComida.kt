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

    /**
     * A água que veio da comida, **com a razão de não haver número**.
     *
     * As duas ausências não são a mesma coisa e não se dizem com a mesma frase: quem não
     * registou nada não tem comida por medir, tem um dia por registar. O ecrã dizia «menos
     * de metade do que comeste hoje traz o teor de água medido» a quem não tinha comido
     * nada — uma afirmação sobre comida que não existe, no primeiro ecrã de quem instala a
     * app.
     *
     * É a distinção que o [EndOfDayProtein] já faz, e com as mesmas palavras: *«dia sem
     * registo nenhum não é dia sem proteína: é dia sem app aberta, e avisar seria dar a
     * entender que a app viu o que a pessoa comeu.»*
     */
    fun doDia(totals: MicroTotals): Resultado {
        if (totals.totalKcal <= 0) return Resultado.SemRegisto

        val medido = totals.measuredKcalByKey[Nutrients.WATER] ?: 0.0
        if (medido / totals.totalKcal < COBERTURA_MINIMA) return Resultado.SemCobertura

        // Um grama de água é um mililitro. A densidade só se afasta disso às décimas com a
        // temperatura, e a conta seguinte é sobre litros bebidos ao longo de um dia.
        return Resultado.Medida((totals.byKey[Nutrients.WATER] ?: 0.0).roundToInt())
    }

    sealed interface Resultado {

        /** Sabe-se, e são estes mililitros. */
        data class Medida(val ml: Int) : Resultado

        /** Não se comeu nada. Não há o que medir, e não é o mesmo que não se saber. */
        data object SemRegisto : Resultado

        /** Comeu-se, e menos de metade dessas calorias trouxe teor de água medido. */
        data object SemCobertura : Resultado
    }
}
