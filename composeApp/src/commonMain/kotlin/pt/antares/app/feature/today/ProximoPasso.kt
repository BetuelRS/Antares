package pt.antares.app.feature.today

import kotlin.math.roundToInt

/**
 * A linha de «a seguir» do Hoje: o que falta fazer, numa frase, com o botão que o resolve.
 *
 * É a proposta 4 da `estudo/areas/01-hoje.md` — *«a diferença entre um relatório e um
 * assistente»* — e sai toda de números que o ecrã já tinha. O ecrã dizia o estado de nove
 * coisas e em nenhum momento qual era a próxima.
 */
sealed interface ProximoPasso {

    /** O treino agendado para hoje, que ainda não se fez. O botão começa-o. */
    data class Treino(val routineId: String, val nome: String) : ProximoPasso

    data class Proteina(val faltamG: Int) : ProximoPasso

    data class Agua(val faltamMl: Int) : ProximoPasso
}

/**
 * A regra, que é uma frase só — a primeira que se aplicar:
 *
 * 1. **o treino agendado**, se hoje ainda não houve treino nem há um a decorrer;
 * 2. **a partir do meio-dia, a maior falta entre proteína e água**, se faltar pelo menos um
 *    quarto da meta. Maior em proporção e não em número: 40 g e 900 ml não se comparam.
 *
 * Antes do meio-dia só o treino: de manhã falta sempre tudo, e uma frase a dizê-lo todos os
 * dias deixava de se ler. Sem nada que se aplique, não há linha — um «está tudo bem» ocupava o
 * sítio da coisa mais importante do ecrã para não dizer nada.
 *
 * Escolhida sem o dono, a 2026-09-10, e escrita no plano como tal.
 */
object ProximoPassoCalc {

    fun escolher(
        hora: Int,
        treinoAgendado: ProximoPasso.Treino?,
        treinouHoje: Boolean,
        treinoActivo: Boolean,
        proteinaMetaG: Int,
        proteinaConsumidaG: Double,
        aguaMetaMl: Int,
        aguaBebidaMl: Int,
    ): ProximoPasso? {
        if (treinoAgendado != null && !treinouHoje && !treinoActivo) return treinoAgendado
        if (hora < HORA_DAS_FALTAS) return null

        val proteina = falta(proteinaMetaG.toDouble(), proteinaConsumidaG)
        val agua = falta(aguaMetaMl.toDouble(), aguaBebidaMl.toDouble())
        val maior = listOfNotNull(proteina?.let { it to true }, agua?.let { it to false })
            .maxByOrNull { (f, _) -> f.fracao } ?: return null

        val (f, eProteina) = maior
        return if (eProteina) ProximoPasso.Proteina(f.quanto.roundToInt())
        else ProximoPasso.Agua(f.quanto.roundToInt())
    }

    private class Falta(val quanto: Double, val fracao: Double)

    // Nula sem meta, ou quando falta menos de um quarto dela: a meio da tarde, 20 g de
    // proteína por comer é um lanche, e não uma coisa que precise de uma frase.
    private fun falta(meta: Double, feito: Double): Falta? {
        if (meta <= 0.0) return null
        val quanto = meta - feito
        val fracao = quanto / meta
        return if (fracao >= FRACAO_MINIMA) Falta(quanto, fracao) else null
    }

    private const val HORA_DAS_FALTAS = 12
    private const val FRACAO_MINIMA = 0.25
}
