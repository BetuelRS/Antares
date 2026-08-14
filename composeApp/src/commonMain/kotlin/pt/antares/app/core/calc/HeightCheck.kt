package pt.antares.app.core.calc

/**
 * Quando voltar a perguntar a altura. A altura entra no basal e é o único dado do perfil
 * que se introduz uma vez e nunca mais — e que na verdade muda.
 */
object HeightCheck {

    // Seis meses para menores, que ainda crescem depressa.
    const val MINOR_INTERVAL_DAYS = 182

    // Dois anos para adultos: a altura perde-se devagar com a idade, e perguntar mais vezes
    // seria só ruído.
    const val ADULT_INTERVAL_DAYS = 730

    fun isDue(
        ageYears: Int,
        confirmedEpochDay: Long?,
        profileUpdatedEpochDay: Long,
        todayEpochDay: Long,
    ): Boolean {
        // Quem nunca confirmou conta a partir da última mexida no perfil: introduzir a
        // altura no arranque vale como confirmação, e sem isto a app perguntava logo no
        // primeiro dia.
        val last = confirmedEpochDay ?: profileUpdatedEpochDay
        val interval = if (ageYears < NutritionCalc.ADULT_AGE) {
            MINOR_INTERVAL_DAYS
        } else {
            ADULT_INTERVAL_DAYS
        }
        return todayEpochDay - last >= interval
    }
}
