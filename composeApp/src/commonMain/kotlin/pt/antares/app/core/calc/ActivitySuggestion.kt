package pt.antares.app.core.calc

import pt.antares.app.core.model.ActivityLevel

/**
 * Propõe um nível de atividade a partir dos passos contados pelo telemóvel. Propõe: quem
 * muda o perfil é a pessoa, porque os passos não veem a bicicleta nem o ginásio.
 */
object ActivitySuggestion {

    // Cortes convencionais da literatura de podometria, os mesmos que a maioria das apps
    // usa — o que importa é serem estáveis, não serem exatos.
    const val LOW_ACTIVE = 5_000
    const val SOMEWHAT_ACTIVE = 7_500
    const val ACTIVE = 10_000

    const val MIN_DAYS = 7

    // Dias a zero são telemóvel em casa ou sensor desligado, não dias parados; entravam na
    // média e empurravam toda a gente para sedentário.
    private fun usable(dailySteps: List<Long>): List<Long> = dailySteps.filter { it > 0 }

    /** Null com menos de uma semana de dias úteis — a média não descreveria um hábito. */
    fun averageDailySteps(dailySteps: List<Long>): Long? {
        val dias = usable(dailySteps)
        if (dias.size < MIN_DAYS) return null
        return dias.sum() / dias.size
    }

    fun levelForSteps(averageSteps: Long): ActivityLevel = when {
        averageSteps < LOW_ACTIVE -> ActivityLevel.SEDENTARY
        averageSteps < SOMEWHAT_ACTIVE -> ActivityLevel.LIGHT
        averageSteps < ACTIVE -> ActivityLevel.MODERATE

        // Não há degrau para atleta: nenhuma contagem de passos o justifica, e chegar lá
        // por caminhar dava um gasto muito acima do real.
        else -> ActivityLevel.HIGH
    }

    fun suggest(dailySteps: List<Long>, current: ActivityLevel?): Suggestion? {
        val media = averageDailySteps(dailySteps) ?: return null
        val sugerido = levelForSteps(media)
        if (sugerido == current) return null

        // Quem se declarou atleta nunca é contrariado: os passos não veem o treino, e
        // baixar-lhe o nível cortava-lhe centenas de calorias por engano.
        if (current == ActivityLevel.ATHLETE) return null
        return Suggestion(averageSteps = media, suggested = sugerido, current = current)
    }

    data class Suggestion(
        val averageSteps: Long,
        val suggested: ActivityLevel,
        val current: ActivityLevel?,
    )
}
