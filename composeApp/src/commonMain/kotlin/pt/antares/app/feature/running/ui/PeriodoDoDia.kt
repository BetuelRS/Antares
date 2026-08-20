package pt.antares.app.feature.running.ui

import org.jetbrains.compose.resources.StringResource
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.run_name_afternoon
import pt.antares.app.generated.resources.run_name_evening
import pt.antares.app.generated.resources.run_name_morning
import pt.antares.app.generated.resources.run_name_night

/**
 * A altura do dia a que uma corrida se lê. Não são fusos nem astronomia: são as quatro
 * palavras com que uma pessoa nomeia o que fez.
 */
enum class PeriodoDoDia { MADRUGADA, MANHA, TARDE, NOITE }

/**
 * O período a que uma hora pertence.
 *
 * A madrugada é o intervalo que dá a volta à meia-noite, e por isso é o ramo `else`: escrito
 * como `hora in 22..4` seria um intervalo vazio, e todas as horas cairiam noutro ramo sem
 * erro nenhum a avisar. É o que o `NomeDaCorridaTest` guarda.
 */
fun periodoDoDia(hora: Int): PeriodoDoDia = when (hora) {
    in 5..11 -> PeriodoDoDia.MANHA
    in 12..17 -> PeriodoDoDia.TARDE
    in 18..21 -> PeriodoDoDia.NOITE
    else -> PeriodoDoDia.MADRUGADA
}

fun rotuloDoPeriodo(periodo: PeriodoDoDia): StringResource = when (periodo) {
    PeriodoDoDia.MANHA -> Res.string.run_name_morning
    PeriodoDoDia.TARDE -> Res.string.run_name_afternoon
    PeriodoDoDia.NOITE -> Res.string.run_name_evening
    PeriodoDoDia.MADRUGADA -> Res.string.run_name_night
}
