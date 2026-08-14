package pt.antares.app.core.calc

import kotlin.math.roundToInt

/**
 * Decide se vale a pena lembrar a proteína ao fim do dia. Devolve as gramas em falta, ou
 * null quando o lembrete não se justifica.
 */
object EndOfDayProtein {

    // Só se avisa abaixo de 60% da meta. Acima disto ainda dá para compensar amanhã, e um
    // lembrete que aparece quase todos os dias deixa de ser lido.
    const val THRESHOLD = 0.60

    fun gapToNotify(consumedG: Double, targetG: Int, hasLogs: Boolean): Int? {
        // Dia sem registo nenhum não é dia sem proteína: é dia sem app aberta, e avisar
        // seria dar a entender que a app viu o que a pessoa comeu.
        if (!hasLogs) return null
        if (targetG <= 0) return null
        if (consumedG >= targetG * THRESHOLD) return null
        val gap = (targetG - consumedG).roundToInt()
        // O arredondamento pode dar zero quando falta menos de meio grama; um lembrete a
        // pedir 0 g não faz sentido nenhum.
        return if (gap > 0) gap else null
    }
}
