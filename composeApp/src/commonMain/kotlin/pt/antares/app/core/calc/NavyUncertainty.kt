package pt.antares.app.core.calc

/**
 * De quanto é o «cerca de» num basal calculado a partir de fita métrica.
 *
 * O método das circunferências da marinha americana tem um erro-padrão de **3,6 a 3,8 pontos
 * percentuais** de massa gorda contra a pesagem hidrostática, com viés por sexo. Isso não é
 * um defeito da app — é o que o método dá —, mas mostrar «1797,5 kcal» a partir dele é
 * afirmar uma precisão que o número não tem.
 *
 * A conta é directa. Uma incerteza de 3,6 pp sobre 80 kg são 2,88 kg de massa magra, e a
 * Katch-McArdle converte cada quilo de magra em 21,6 kcal: dá **±62 kcal**. É um valor que a
 * pessoa deve ver, não um que a app deva esconder.
 *
 * **Só se propaga quando a massa gorda veio da fita.** Uma medição por absorciometria não
 * tem este erro, e a estimativa pelo IMC nem sequer chega à Katch-McArdle — o
 * `usableLeanMassKg` recusa-a antes disso, e está certo assim.
 */
object NavyUncertainty {

    // O extremo baixo dos 3,6 a 3,8 publicados. Escolhe-se o menor porque o intervalo já é
    // largo, e um intervalo exagerado ensina a ignorá-lo.
    const val BODY_FAT_STANDARD_ERROR_PP = 3.6

    private const val PP_TO_FRACTION = 100.0

    /**
     * O «mais ou menos» em kcal, ou nulo quando não há incerteza a declarar — porque a
     * massa gorda não veio da fita, ou porque a fórmula usada não passa pela massa magra.
     */
    fun bmrKcal(formula: BmrFormula, weightKg: Double): Double? {
        if (weightKg <= 0.0) return null
        val leanKg = weightKg * BODY_FAT_STANDARD_ERROR_PP / PP_TO_FRACTION

        // Passa-se a diferença pela própria fórmula em vez de multiplicar pelo coeficiente
        // dela: assim, mudar a fórmula muda o intervalo junto, e não fica um número velho
        // a descrever uma conta nova.
        return when (formula) {
            BmrFormula.KATCH_MCARDLE ->
                NutritionCalc.bmrKatchMcArdle(leanKg) - NutritionCalc.bmrKatchMcArdle(0.0)
            BmrFormula.CUNNINGHAM ->
                NutritionCalc.bmrCunningham(leanKg) - NutritionCalc.bmrCunningham(0.0)
            // A Mifflin sai do peso e da altura, que a fita não mede. O erro dela é outro
            // e não é este — está no [MifflinUncertainty].
            BmrFormula.MIFFLIN_ST_JEOR -> null
        }
    }
}

/**
 * De quanto é o «cerca de» num basal calculado sem massa gorda nenhuma.
 *
 * **A app declarava a margem da estimativa boa e calava-se sobre a má**, que é o inverso do
 * que devia ser — é o achado principal do `estudo/motor/01-metabolismo-e-metas.md`. Quem
 * mediu a cintura via ±62 kcal; quem nunca mediu a massa gorda, e é a maioria, via um número
 * nu. E é esse que traz mais margem.
 *
 * A Mifflin-St Jeor erra tipicamente **10 %** do basal contra calorimetria indireta, e vai a
 * 36 % em obesidade. Num basal de 1 750 kcal são ±175 kcal — quase o triplo do que a app já
 * mostrava no outro caminho.
 *
 * **É percentagem e não um valor fixo** porque o erro da fórmula escala com o número que ela
 * produz: dizer «±175 kcal» a toda a gente seria exagerado num basal de 1 200 e curto num de
 * 2 400.
 */
object MifflinUncertainty {

    // Mifflin et al. (1990), contra calorimetria indireta. O extremo baixo do que a
    // literatura reporta, pela mesma razão do [NavyUncertainty]: um intervalo exagerado
    // ensina a ignorá-lo.
    const val RELATIVE_ERROR = 0.10

    /** O «mais ou menos» em kcal, ou nulo quando a fórmula usada não é esta. */
    fun bmrKcal(formula: BmrFormula, bmr: Double): Double? {
        if (bmr <= 0.0) return null
        return when (formula) {
            BmrFormula.MIFFLIN_ST_JEOR -> bmr * RELATIVE_ERROR
            // As duas de massa magra têm o erro da fita, e esse já é declarado.
            BmrFormula.KATCH_MCARDLE, BmrFormula.CUNNINGHAM -> null
        }
    }
}
