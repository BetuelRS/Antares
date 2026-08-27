package pt.antares.app.core.calc

import kotlin.math.roundToInt

data class IngredientNutrition(
    val kcalPer100: Int,
    val proteinPer100: Double,
    val carbsPer100: Double,
    val fatPer100: Double,
    val grams: Double,

    val sugarsPer100: Double? = null,
    val satFatPer100: Double? = null,
    val fiberPer100: Double? = null,
    val sodiumMgPer100: Double? = null,
    val microsPer100: Map<String, Double> = emptyMap(),

    /**
     * Quanto de cada micronutriente **deste ingrediente** sobrevive ao lume, por chave do
     * vocabulário. Vazio quer dizer que nada se perde — que é o que a app assumia em todas
     * as receitas até aqui.
     *
     * Os factores são por ingrediente e não por receita porque a retenção é uma propriedade
     * da família do alimento: cozer espinafres e cozer arroz no mesmo tacho não destrói a
     * mesma fracção de vitamina C, e a tabela do USDA publica-os separados.
     */
    val retencoes: Map<String, Double> = emptyMap(),
)

/**
 * O resultado da receita. Os totais são da receita inteira; os `per100` derivam deles
 * pela base, que é o peso depois de cozinhar quando esse valor existe.
 */
data class RecipeNutrition(
    val totalKcal: Int,
    val totalProteinG: Double,
    val totalCarbsG: Double,
    val totalFatG: Double,
    val basisGrams: Double,

    val sugarsPer100: Double? = null,
    val satFatPer100: Double? = null,
    val fiberPer100: Double? = null,
    val sodiumMgPer100: Double? = null,

    val microsPer100: Map<String, Double> = emptyMap(),
) {
    val kcalPer100: Int get() = per100(totalKcal.toDouble()).roundToInt()
    val proteinPer100: Double get() = per100(totalProteinG)
    val carbsPer100: Double get() = per100(totalCarbsG)
    val fatPer100: Double get() = per100(totalFatG)

    private fun per100(total: Double): Double = if (basisGrams > 0) total / basisGrams * 100 else 0.0
}

object RecipeCalc {

    // Um nutriente só sai da receita se os ingredientes que o declaram pesarem pelo menos
    // 60% do total. Abaixo disto o valor descreve parte do prato e passaria por descrever
    // o prato todo — em micronutrientes, quase sempre por defeito.
    const val MIN_COVERAGE = 0.6

    /**
     * Soma os ingredientes e reduz a valores por 100 g. `yieldGrams` é o peso final: a
     * água que evapora concentra tudo, e sem esse valor a receita fica pelo peso cru.
     *
     * **A concentração sozinha mente para cima.** Uma receita que perde 200 g de água em
     * 1 000 g concentra tudo 1,25 vezes — e é assim que os macros se comportam, porque a
     * proteína não desaparece ao lume. A vitamina C desaparece: cozer destrói metade dela.
     * Contar só a concentração dava uma sopa de espinafres com 25 % mais vitamina C do que
     * a que estava crua no tacho, que é o contrário do que acontece.
     *
     * Por isso cada ingrediente traz as suas [IngredientNutrition.retencoes], e são elas que
     * entram na soma — a concentração continua a ser da receita inteira, através da base.
     */
    fun compute(ingredients: List<IngredientNutrition>, yieldGrams: Double?): RecipeNutrition {
        val macros = somarMacros(ingredients)
        val rawGrams = macros.gramas

        val totals = HashMap<String, Double>()
        val covered = HashMap<String, Double>()
        somarMicros(ingredients, totals, covered)

        val basis = yieldGrams?.takeIf { it > 0 } ?: rawGrams

        fun resolve(key: String): Double? {
            if (basis <= 0 || rawGrams <= 0) return null
            // A cobertura mede-se sobre o peso cru, que é o que os ingredientes somam. O
            // peso final só entra na divisão, para não penalizar receitas que perdem água.
            if ((covered[key] ?: 0.0) / rawGrams < MIN_COVERAGE) return null
            return (totals[key] ?: return null) / basis * GRAMAS_DE_REFERENCIA
        }

        val micros = buildMap {
            for (key in totals.keys) {
                // Fibra, açúcares, gordura saturada e sódio têm campo próprio no resultado;
                // deixá-los também no mapa duplicava-os no ecrã dos micronutrientes.
                if (key in SECONDARY_KEYS) continue
                resolve(key)?.let { if (it > 0) put(key, it) }
            }
        }
        return RecipeNutrition(
            totalKcal = macros.kcal.roundToInt(),
            totalProteinG = macros.proteina,
            totalCarbsG = macros.hidratos,
            totalFatG = macros.gordura,
            basisGrams = basis,
            sugarsPer100 = resolve(KEY_SUGARS),
            satFatPer100 = resolve(KEY_SATFAT),
            fiberPer100 = resolve(KEY_FIBER),
            sodiumMgPer100 = resolve(KEY_SODIUM),
            microsPer100 = micros,
        )
    }

    /** A soma dos macros e do peso cru. Sem retenção: os macros só se concentram. */
    private data class Macros(
        val kcal: Double,
        val proteina: Double,
        val hidratos: Double,
        val gordura: Double,
        val gramas: Double,
    )

    private fun somarMacros(ingredients: List<IngredientNutrition>): Macros {
        var kcal = 0.0
        var proteina = 0.0
        var hidratos = 0.0
        var gordura = 0.0
        var gramas = 0.0

        for (i in ingredients) {
            val factor = i.grams / GRAMAS_DE_REFERENCIA
            kcal += i.kcalPer100 * factor
            proteina += i.proteinPer100 * factor
            hidratos += i.carbsPer100 * factor
            gordura += i.fatPer100 * factor
            gramas += i.grams
        }
        return Macros(kcal, proteina, hidratos, gordura, gramas)
    }

    /**
     * Soma os micronutrientes, já com a retenção de cada ingrediente aplicada.
     *
     * O [covered] acompanha o [totals] para saber sobre que peso da receita cada nutriente
     * foi de facto declarado — um ingrediente sem o campo não entra em nenhum dos dois.
     *
     * A retenção é 1 para quem não tem factor publicado, e é essa a diferença entre «não se
     * perde» e «não se sabe». A gordura saturada não tem factor nenhum na tabela do USDA, e
     * por isso passa sempre inteira.
     */
    private fun somarMicros(
        ingredients: List<IngredientNutrition>,
        totals: HashMap<String, Double>,
        covered: HashMap<String, Double>,
    ) {
        fun add(key: String, per100: Double?, grams: Double, retencao: Double) {
            if (per100 == null) return
            totals[key] = (totals[key] ?: 0.0) + per100 * retencao * grams / GRAMAS_DE_REFERENCIA
            covered[key] = (covered[key] ?: 0.0) + grams
        }

        for (i in ingredients) {
            fun retencaoDe(chave: String) = i.retencoes[chave] ?: 1.0

            add(KEY_SUGARS, i.sugarsPer100, i.grams, retencaoDe(SUGARS_KEY))
            add(KEY_SATFAT, i.satFatPer100, i.grams, 1.0)
            add(KEY_FIBER, i.fiberPer100, i.grams, retencaoDe(FIBER_KEY))
            add(KEY_SODIUM, i.sodiumMgPer100, i.grams, retencaoDe(SODIUM_KEY))
            for ((k, v) in i.microsPer100) add(k, v, i.grams, retencaoDe(k))
        }
    }

    private const val GRAMAS_DE_REFERENCIA = 100.0

    // As chaves reais dos quatro que têm campo próprio no resultado. São elas que se
    // procuram na tabela de retenção — a interna, com prefixo duplo, não existe lá.
    private const val SUGARS_KEY = "sugars_g"
    private const val FIBER_KEY = "fiber_g"
    private const val SODIUM_KEY = "sodium_mg"

    // Prefixo duplo para não colidirem com nenhum código de micronutriente real, já que
    // partilham o mesmo mapa durante a soma.
    private const val KEY_SUGARS = "__sugars"
    private const val KEY_SATFAT = "__satfat"
    private const val KEY_FIBER = "__fiber"
    private const val KEY_SODIUM = "__sodium"
    private val SECONDARY_KEYS = setOf(KEY_SUGARS, KEY_SATFAT, KEY_FIBER, KEY_SODIUM)
}
