package pt.antares.app.feature.running.ui

import pt.antares.app.core.model.UnitSystem
import pt.antares.app.core.util.UnitConversions

/**
 * Os números da corrida como se leem.
 *
 * A distância e o ritmo recebem o sistema de unidades e **não têm valor por omissão**: um
 * `= METRIC` aqui deixava cada ecrã esquecido a mostrar quilómetros a quem escolheu milhas,
 * sem erro nenhum a avisar. Era exatamente esse o estado da app.
 */
object RunFormat {

    fun distance(distanceM: Double, system: UnitSystem): String {
        val valor = UnitConversions.distanceToDisplay(distanceM / 1000.0, system)
        val whole = valor.toInt()
        val dec = ((valor - whole) * 100).toInt()
        return "$whole,${if (dec < 10) "0$dec" else "$dec"}"
    }

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
}
