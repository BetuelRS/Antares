package pt.antares.app.feature.running.domain

import kotlin.math.roundToInt

/**
 * O ritmo médio de um conjunto de corridas.
 *
 * Sai da distância e do tempo **somados**, e não da média dos ritmos de cada corrida: uma
 * de 1 km a 6:00 e uma de 10 km a 5:00 dão 5:05 por quilómetro, e não 5:30. A média dos
 * ritmos daria peso igual a corridas de tamanhos diferentes.
 */
object RitmoMedio {

    /** Zero quando não há distância — é o valor que o `RunFormat.pace` já lê como «--:--». */
    fun secPorKm(metros: Double, segundos: Long): Int {
        if (metros < METROS_MINIMOS || segundos <= 0) return 0
        return (segundos / (metros / METROS_POR_KM)).roundToInt()
    }

    // Abaixo de dez metros o ritmo é o do arredondamento do GPS, e dava números de quatro
    // dígitos por quilómetro numa corrida que ainda não começou.
    private const val METROS_MINIMOS = 10.0
    private const val METROS_POR_KM = 1000.0
}
