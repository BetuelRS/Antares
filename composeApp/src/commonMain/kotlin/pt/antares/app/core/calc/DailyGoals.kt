package pt.antares.app.core.calc

import pt.antares.app.core.model.Sex
import kotlin.math.roundToInt

/**
 * Metas diárias que não dependem das calorias. Vivem à parte das dos macros porque não
 * saem do gasto energético nem se ajustam com ele.
 */
object DailyGoals {

    /**
     * Ingestão adequada de **água total** da EFSA para adultos: 2,5 L nos homens e 2,0 L
     * nas mulheres. Total quer dizer tudo — bebida, sopa, fruta, o que vem na comida.
     *
     * A app pedia 35 ml por quilo só de bebida, o que dá 2800 ml a quem tem 80 kg: cerca de
     * 40% acima da referência que ela própria usa para todos os micronutrientes, e igual
     * para homens e mulheres. A EFSA é a fonte do `seed_efsa_drv.csv`; não fazia sentido
     * segui-la em tudo menos aqui.
     */
    const val WATER_EFSA_MALE_ML = 2500
    const val WATER_EFSA_FEMALE_ML = 2000

    const val WATER_ROUNDING_ML = 50

    /**
     * Peso a que a referência da EFSA corresponde, e a partir do qual a app a escala.
     *
     * **Isto é da app e não da EFSA**, que dá um número por adulto e não por quilo. Mas
     * pedir o mesmo a quem tem 50 kg e a quem tem 110 é pior do que escalar, e por isso a
     * app escala — e diz que escala.
     */
    const val WATER_REFERENCE_WEIGHT_KG = 70.0

    /**
     * O que se acrescenta num dia com treino registado. Também é da app: a EFSA reconhece
     * que a necessidade sobe com a atividade física e com o calor, mas não põe número. Meio
     * litro é a ordem de grandeza da perda por suor numa hora de exercício moderado.
     *
     * O calor fica de fora por falta de informação: a app não sabe a temperatura, e inventar
     * um valor era pior do que não o ter.
     */
    const val WATER_TRAINING_BONUS_ML = 500

    // 25 g é a referência da EFSA para adultos. Não escala com o peso: a fibra existe para
    // alimentar o intestino, e esse não é maior em quem pesa mais.
    const val FIBRE_G_ADULT = 25

    /**
     * A meta de água **total** do dia. Conta a que se bebe e a que vem na comida — ver
     * `AguaDaComida`, que calcula a segunda.
     */
    fun waterMl(sex: Sex, weightKg: Double, treinouHoje: Boolean = false): Int {
        if (weightKg <= 0) return 0
        val base = when (sex) {
            Sex.MALE -> WATER_EFSA_MALE_ML
            Sex.FEMALE -> WATER_EFSA_FEMALE_ML
        }
        val comPeso = base * (weightKg / WATER_REFERENCE_WEIGHT_KG)
        val raw = comPeso + if (treinouHoje) WATER_TRAINING_BONUS_ML else 0
        // Arredonda-se a 50 ml para a meta ser um número que se lê e se enche com copos —
        // 2 650 ml em vez de 2 677.
        return (raw / WATER_ROUNDING_ML).roundToInt() * WATER_ROUNDING_ML
    }

    fun fibreG(): Int = FIBRE_G_ADULT
}
