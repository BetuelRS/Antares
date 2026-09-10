package pt.antares.app.feature.running.ui

import kotlin.math.roundToInt
import pt.antares.app.core.model.UnitSystem
import pt.antares.app.core.designsystem.oneDecimal
import pt.antares.app.core.designsystem.twoDecimals
import pt.antares.app.core.util.UnitConversions

/**
 * Os números da corrida como se leem.
 *
 * A distância e o ritmo recebem o sistema de unidades e **não têm valor por omissão**: um
 * `= METRIC` aqui deixava cada ecrã esquecido a mostrar quilómetros a quem escolheu milhas,
 * sem erro nenhum a avisar. Era exatamente esse o estado da app.
 */
object RunFormat {

    /**
     * O separador decimal vem de fora, como no resto da app: a vírgula estava escrita à mão
     * aqui, e em inglês lia-se «6,87 mi» ao lado de «153.9 lb» no mesmo cartão.
     */
    fun distance(distanceM: Double, system: UnitSystem, comma: Boolean): String =
        twoDecimals(UnitConversions.distanceToDisplay(distanceM / 1000.0, system), comma)

    /**
     * A distância de uma semana, com uma casa decimal.
     *
     * Um total lê-se arredondado: «8.30 km esta semana» dizia um centésimo que ninguém
     * procura num total. A 2.20.1 deixou-o escrito para decidir com o hub, porque mudá-lo só
     * num sítio dava duas maneiras de escrever a mesma semana — por isso o hub e o painel de
     * treino passam os dois por aqui. As corridas soltas continuam a duas casas.
     */
    fun distanciaDaSemana(distanceM: Double, system: UnitSystem, comma: Boolean): String =
        oneDecimal(UnitConversions.distanceToDisplay(distanceM / 1000.0, system), comma)

    /**
     * O desnível acumulado, arredondado à unidade: uma subida não se lê às décimas. Em
     * imperial vai em pés, como o resto da corrida.
     */
    fun elevation(m: Double, system: UnitSystem): String =
        if (system == UnitSystem.IMPERIAL) {
            UnitConversions.mToFt(m).roundToInt().toString()
        } else {
            m.roundToInt().toString()
        }

    /**
     * Se o tempo total merece linha própria ao lado do tempo em movimento.
     *
     * Sem pausa automática os dois são o mesmo número, e parar a meio inflaciona o tempo sem
     * o ecrã o dizer — era o defeito concreto 3 da área 11. Abaixo de um minuto de
     * diferença é o arredondamento das amostras, e uma segunda linha a repetir o mesmo
     * relógio é ruído.
     */
    fun tempoTotalAParte(elapsedMs: Long, movingMs: Long): Boolean =
        elapsedMs - movingMs >= MS_POR_MINUTO

    fun clock(ms: Long): String {
        val total = ms / 1000
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        fun p(n: Long) = if (n < 10) "0$n" else "$n"
        return if (h > 0) "$h:${p(m)}:${p(s)}" else "${p(m)}:${p(s)}"
    }

    fun pace(secPerKm: Int, system: UnitSystem): String {
        if (secPerKm <= 0) return "--:--"
        val seg = UnitConversions.paceToDisplay(secPerKm, system)
        val m = seg / 60
        val s = seg % 60
        return "$m:${if (s < 10) "0$s" else "$s"}"
    }

    fun paceFromSpeed(speedMps: Double, system: UnitSystem): String {
        if (speedMps < MIN_SPEED_MPS) return "--:--"
        return pace((1000.0 / speedMps).toInt(), system)
    }

    // Abaixo disto é passo parado, e o ritmo dava números de três dígitos por minuto.
    private const val MIN_SPEED_MPS = 0.3
    private const val MS_POR_MINUTO = 60_000L
}
