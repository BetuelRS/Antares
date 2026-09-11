package pt.antares.app.feature.today

import kotlin.math.roundToInt

/**
 * A meta de passos e a distância que eles fazem.
 *
 * **A meta é 8 000, fixa**, escolhida sem o dono a 2026-09-10 e escrita no plano como tal. A
 * meta-análise de Paluch e colegas (*Lancet Public Health*, 2022, quinze coortes) vê o benefício
 * na mortalidade estabilizar entre 6 000 e 8 000 passos por dia acima dos 60 anos, e entre 8 000
 * e 10 000 abaixo — 8 000 é o ponto que as duas faixas partilham. Os 10 000 do costume vêm de um
 * pedómetro japonês dos anos 60, e não de um estudo.
 */
object PassosCalc {

    const val META = 8_000

    /** A fração da meta, sem travar em 1: um dia de 12 000 passos passa dela, e deve ver-se. */
    fun fracao(passos: Long): Float = passos.toFloat() / META

    /**
     * A distância estimada, em metros, pela passada de 0,415 × altura — a regra corrente para
     * andar. É uma estimativa, e o ecrã escreve-a com «≈». Nula sem altura: sem ela, qualquer
     * número seria inventado.
     */
    fun distanciaM(passos: Long, alturaCm: Double?): Double? {
        if (alturaCm == null || alturaCm <= 0.0) return null
        val passadaM = PASSADA_POR_ALTURA * alturaCm / CM_POR_M
        return (passos * passadaM * CASAS).roundToInt() / CASAS
    }

    private const val PASSADA_POR_ALTURA = 0.415
    private const val CM_POR_M = 100.0
    private const val CASAS = 1.0
}
