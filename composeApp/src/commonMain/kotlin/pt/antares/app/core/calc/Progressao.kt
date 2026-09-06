package pt.antares.app.core.calc

import pt.antares.app.core.model.RegraDeProgressao
import pt.antares.app.core.model.UnitSystem
import pt.antares.app.core.util.UnitConversions

/** O que a rotina planeia para um exercício. */
data class AlvoDoExercicio(
    val series: Int,
    val repsMin: Int,
    val repsMax: Int,
    val pesoKg: Double?,
)

/** Uma série de trabalho da última vez que se fez este exercício. O aquecimento não entra. */
data class SerieDaUltimaVez(val weightKg: Double, val reps: Int)

/**
 * O que a app propõe para a próxima vez. **É uma proposta e não uma escrita:** o
 * `targetWeightKg` que a pessoa escreveu na rotina fica intocado, e este número só aparece —
 * no editor e no campo da sessão. Foi decisão do dono a 2026-09-06, contra as duas hipóteses
 * que reescreviam a rotina no fim do treino.
 */
data class ProximoAlvo(
    val pesoKg: Double,
    val reps: Int,

    /** Se a proposta é uma subida ou a repetição do mesmo peso. Só isto merece uma seta. */
    val subiu: Boolean,
)

object Progressao {

    /** Dois discos de 1,25 kg, um de cada lado — o menor salto que o conjunto métrico permite. */
    const val DEGRAU_KG = 2.5

    /** Dois discos de 2,5 lb, um de cada lado. É o menor salto do conjunto imperial. */
    const val DEGRAU_LB = 5.0

    /**
     * O degrau de omissão, que é o **menor disco a dobrar** do conjunto que o [PlateMath]
     * conhece: 1,25 kg de cada lado dão 2,5 kg no métrico, e 2,5 lb de cada lado dão 5 lb no
     * imperial. Não são o mesmo número, e não podiam ser — 2,5 kg são 5,51 lb, um peso que
     * não se monta com os discos que existem.
     */
    fun incrementoPorOmissao(sistema: UnitSystem): Double = when (sistema) {
        UnitSystem.METRIC -> DEGRAU_KG
        UnitSystem.IMPERIAL -> UnitConversions.lbToKg(DEGRAU_LB)
    }

    /**
     * O que fazer da próxima vez, ou `null` quando a pergunta não tem resposta.
     *
     * Devolve `null` — e não um alvo igual ao de hoje — em quatro casos, porque «não sei» e
     * «fica na mesma» são coisas diferentes e só a segunda merece ser escrita no ecrã:
     * sem regra, sem última vez, com o peso a zero, e **quando as séries da última vez não
     * foram todas ao mesmo peso**. Este último é o caso das séries descendentes e do dia em
     * que se baixou a meio: não houve um peso que se tenha aguentado, e inventar um a partir
     * da média ou do máximo era decidir por quem treinou.
     */
    fun proximo(
        alvo: AlvoDoExercicio,
        ultima: List<SerieDaUltimaVez>,
        regra: RegraDeProgressao,
        incrementoKg: Double,
    ): ProximoAlvo? {
        if (ultima.isEmpty()) return null

        val peso = ultima.first().weightKg
        if (peso <= 0.0 || ultima.any { it.weightKg != peso }) return null

        // Faltar uma das séries planeadas conta como não ter completado: o alvo é «três séries
        // de doze», e duas séries de doze são outra coisa.
        val completou = ultima.size >= alvo.series && ultima.all { it.reps >= alvo.repsMax }
        val sobe = completou && incrementoKg > 0.0

        return when (regra) {
            // Dito **aqui e em mais lado nenhum**: uma segunda guarda lá em cima era a mesma
            // decisão escrita duas vezes, e as duas podiam passar a discordar.
            RegraDeProgressao.NENHUMA -> null

            RegraDeProgressao.LINEAR -> ProximoAlvo(
                pesoKg = if (sobe) peso + incrementoKg else peso,
                reps = alvo.repsMax,
                subiu = sobe,
            )

            RegraDeProgressao.DUPLA -> ProximoAlvo(
                pesoKg = if (sobe) peso + incrementoKg else peso,
                // Uma repetição a mais do que a **pior** série, e não do que a melhor: o alvo
                // é o número que todas têm de alcançar.
                reps = if (sobe) alvo.repsMin else proximasReps(alvo, ultima),
                subiu = sobe,
            )
        }
    }

    private fun proximasReps(alvo: AlvoDoExercicio, ultima: List<SerieDaUltimaVez>): Int =
        (ultima.minOf { it.reps } + 1).coerceIn(alvo.repsMin, alvo.repsMax)
}

/**
 * Como se resume o que se fez da última vez, numa linha por baixo do alvo.
 *
 * Três formas e não uma: «3×10 a 60 kg» é o caso comum e o que o
 * `estudo/esbocos/07-treino-rotinas.html` desenha, mas nem todos os treinos são assim. Escrever
 * sempre a forma comum obrigava a mentir sobre as séries que não bateram certo, e escrever
 * sempre a forma completa dava «60 kg×10 · 60 kg×10 · 60 kg×10» no caso em que três números
 * chegavam.
 */
sealed interface UltimaVez {

    /** Todas as séries ao mesmo peso e com as mesmas repetições. */
    data class Uniforme(val series: Int, val reps: Int, val pesoKg: Double) : UltimaVez

    /** Mesmo peso, repetições diferentes — o que acontece quando a última série cede. */
    data class MesmoPeso(val reps: List<Int>, val pesoKg: Double) : UltimaVez

    /** Pesos diferentes: séries descendentes, ou o dia em que se baixou a meio. */
    data class Mista(val series: List<SerieDaUltimaVez>) : UltimaVez
}

/** `null` quando não há última vez nenhuma — e a ausência é a resposta, não um zero. */
fun resumoDaUltimaVez(series: List<SerieDaUltimaVez>): UltimaVez? {
    if (series.isEmpty()) return null

    val peso = series.first().weightKg
    if (series.any { it.weightKg != peso }) return UltimaVez.Mista(series)

    val reps = series.first().reps
    if (series.all { it.reps == reps }) return UltimaVez.Uniforme(series.size, reps, peso)

    return UltimaVez.MesmoPeso(series.map { it.reps }, peso)
}
