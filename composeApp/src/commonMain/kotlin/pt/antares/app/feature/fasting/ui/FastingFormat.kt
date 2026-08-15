package pt.antares.app.feature.fasting.ui

import pt.antares.app.core.util.formatDurationMin
import kotlin.math.abs

object FastingFormat {

    // Delega no formatador comum: a janela alimentar do diário mostra durações ao lado
    // destas, e dois formatadores acabariam a divergir num acerto qualquer.
    fun hm(ms: Long): String = formatDurationMin((abs(ms) / MS_POR_MINUTO).toInt())

    fun hours(ms: Long): String = "${abs(ms) / MS_POR_HORA}h"

    private const val MS_POR_MINUTO = 60_000L
    private const val MS_POR_HORA = 3_600_000L
}
