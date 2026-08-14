package pt.antares.app.core.calc

/**
 * Uma pausa de dieta em manutenção. Existe para dar à app uma resposta possível quando o
 * peso pára — a única que não é mandar comer menos ainda.
 */
data class DietBreakSuggestion(

    val maintenanceKcal: Int,

    val weeks: Int,

    val assessment: AdaptiveTdee.Assessment,
) {

    // Só se propõe quando a leitura é adaptação metabólica. A quem provavelmente regista a
    // menos, mandar comer mais confirmaria o registo errado.
    val isWorthSuggesting: Boolean
        get() = assessment == AdaptiveTdee.Assessment.METABOLIC_ADAPTATION
}

object DietBreak {

    // Duas semanas: menos não chega para as hormonas responderem, mais e a pessoa perde o
    // fio ao plano.
    const val DEFAULT_WEEKS = 2

    /**
     * Constrói sempre a sugestão, mesmo quando não se deve mostrar; quem decide é o
     * [DietBreakSuggestion.isWorthSuggesting], para o ecrã poder explicar as duas leituras.
     */
    fun suggest(
        currentTdee: Double,
        consecutiveStallWeeks: Int,
        loggedDays: Int,
    ): DietBreakSuggestion = DietBreakSuggestion(

        // Manutenção é o gasto sem parcela nenhuma: é precisamente tirar o défice que faz
        // a pausa ser uma pausa.
        maintenanceKcal = currentTdee.toInt(),
        weeks = DEFAULT_WEEKS,
        assessment = AdaptiveTdee.assessPlateau(consecutiveStallWeeks, loggedDays),
    )
}
