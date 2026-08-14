package pt.antares.app.core.calc

import kotlin.math.roundToInt

/**
 * Metas diárias que não dependem das calorias. Vivem à parte das dos macros porque não
 * saem do gasto energético nem se ajustam com ele.
 */
object DailyGoals {

    // 35 ml por quilo é a regra prática comum para adultos. Não conta a água dos alimentos
    // nem o calor nem o treino, por isso é um alvo e não uma prescrição.
    const val WATER_ML_PER_KG = 35
    const val WATER_ROUNDING_ML = 50

    // 25 g é a referência da EFSA para adultos. Não escala com o peso: a fibra existe para
    // alimentar o intestino, e esse não é maior em quem pesa mais.
    const val FIBRE_G_ADULT = 25

    fun waterMl(weightKg: Double): Int {
        if (weightKg <= 0) return 0
        val raw = weightKg * WATER_ML_PER_KG
        // Arredonda-se a 50 ml para a meta ser um número que se lê e se enche com copos —
        // 2 650 ml em vez de 2 677.
        return (raw / WATER_ROUNDING_ML).roundToInt() * WATER_ROUNDING_ML
    }

    fun fibreG(): Int = FIBRE_G_ADULT
}
