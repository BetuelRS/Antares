package pt.antares.app.core.nutrition

/**
 * Os nomes canónicos dos nutrientes. A unidade vai colada à chave — `_ug`, `_mg`, `_g` —
 * e é essa a razão de as chaves serem strings e não uma enumeração: elas viajam para
 * dentro do JSON guardado na base, e mudar um nome partiria os dados já escritos.
 */
object Nutrients {

    const val VIT_A = "vitA_ug"
    const val VIT_B1 = "vitB1_mg"
    const val VIT_B2 = "vitB2_mg"
    const val VIT_B3 = "vitB3_mg"
    const val VIT_B5 = "vitB5_mg"
    const val VIT_B6 = "vitB6_mg"
    const val VIT_B9 = "vitB9_ug"
    const val VIT_B12 = "vitB12_ug"
    const val VIT_C = "vitC_mg"
    const val VIT_D = "vitD_ug"
    const val VIT_E = "vitE_mg"
    const val VIT_K = "vitK_ug"

    const val CALCIUM = "calcium_mg"
    const val IRON = "iron_mg"
    const val MAGNESIUM = "magnesium_mg"
    const val ZINC = "zinc_mg"
    const val POTASSIUM = "potassium_mg"
    const val COPPER = "copper_mg"
    const val SELENIUM = "selenium_ug"
    const val SODIUM = "sodium_mg"
    const val PHOSPHORUS = "phosphorus_mg"
    const val MANGANESE = "manganese_mg"
    const val IODINE = "iodine_ug"
    const val CHLORIDE = "chloride_mg"

    const val FIBER = "fiber_g"
    const val SUGARS = "sugars_g"
    const val SAT_FAT = "satFat_g"

    const val WATER = "water_g"
    const val ALCOHOL = "alcohol_g"
    const val CHOLESTEROL = "cholesterol_mg"
    const val FAT_MONO = "fatMono_g"
    const val FAT_POLY = "fatPoly_g"

    const val FAT_TRANS = "fatTrans_g"

    // Os ácidos gordos ficam em gramas, e não em miligramas como se lê nos rótulos, porque é
    // assim que as três tabelas os publicam. Converter aqui era abrir a porta ao erro de mil
    // vezes que não dá erro nenhum: 0,1 g de EPA e 100 mg de EPA são o mesmo número escrito
    // ao contrário, e nenhum teste distingue um do outro depois de gravado.
    const val OMEGA3 = "omega3_g"
    const val OMEGA6 = "omega6_g"
    const val EPA = "epa_g"
    const val DHA = "dha_g"

    const val STARCH = "starch_g"
    const val LACTOSE = "lactose_g"
    const val POLYOLS = "polyols_g"

    // Retinol e beta-caroteno já entram no `vitA_ug`, que é o equivalente de retinol. Ficam à
    // parte porque a conversão não tem volta: quem quer saber se a vitamina A veio de fígado
    // ou de cenoura não o consegue tirar do total.
    const val RETINOL = "retinol_ug"
    const val BETA_CAROTENE = "betaCarotene_ug"

    val VITAMINS = listOf(
        VIT_A, VIT_B1, VIT_B2, VIT_B3, VIT_B5, VIT_B6,
        VIT_B9, VIT_B12, VIT_C, VIT_D, VIT_E, VIT_K,
    )

    val MINERALS = listOf(
        CALCIUM, IRON, MAGNESIUM, PHOSPHORUS, POTASSIUM, SODIUM,
        ZINC, COPPER, MANGANESE, SELENIUM, IODINE, CHLORIDE,
    )

    val OTHERS = listOf(
        FAT_MONO, FAT_POLY, FAT_TRANS, OMEGA3, OMEGA6, EPA, DHA,
        CHOLESTEROL, STARCH, LACTOSE, POLYOLS, RETINOL, BETA_CAROTENE, WATER, ALCOHOL,
    )

    // O que vem no rótulo das embalagens. O sódio aparece aqui e nos minerais: é a mesma
    // substância, mas no rótulo lê-se como limite a não passar e nos minerais como dose a
    // atingir.
    val LABEL = listOf(SUGARS, FIBER, SAT_FAT, SODIUM)

    /**
     * Minerais cuja referência da EFSA é um teto e não uma meta. Faltar não é uma lacuna a
     * preencher, e por isso ficam de fora tanto do aviso do dia como da procura por
     * alimentos ricos — sugerir a alguém que coma mais sal seria o contrário do que se quer.
     *
     * O cloro entra por vir sempre acompanhado: os 3,1 g da EFSA são a outra metade dos 5 g
     * de sal, e quem os atinge atingiu o sódio pelo mesmo caminho.
     */
    val TETOS = setOf(SODIUM, CHLORIDE)

    // A ordem das listas é a ordem em que os nutrientes aparecem no ecrã, e por isso são
    // listas e não conjuntos: as vitaminas seguem a ordem convencional dos rótulos, A a K.
    val ALL: List<String> = VITAMINS + MINERALS + OTHERS + listOf(FIBER, SUGARS, SAT_FAT)
    private val ALL_SET = ALL.toSet()

    fun isCanonical(key: String): Boolean = key in ALL_SET

    fun unitOf(key: String): String = when {
        key.endsWith("_ug") -> "µg"
        key.endsWith("_mg") -> "mg"
        key.endsWith("_g") -> "g"
        else -> ""
    }

    /**
     * Nomes alternativos vindos das bases de alimentos, que escrevem o mesmo nutriente de
     * maneiras diferentes: tiamina e vitamina B1, folatos e ácido fólico, `Fe` e ferro.
     * Sem isto, metade dos micronutrientes importados era descartada em silêncio.
     */
    private val ALIASES: Map<String, String> = buildMap {

        // As próprias chaves canónicas entram como alias de si mesmas, para o mapa também
        // reconhecer os dados que a app escreveu.
        ALL.forEach { put(squash(it), it) }

        fun alias(canonical: String, vararg names: String) {
            names.forEach { put(squash(it), canonical) }
        }

        alias(VIT_A, "vitamina", "vitaminaactivity", "retinolequivalent", "vita")
        alias(VIT_B1, "vitaminb1", "thiamin", "thiamine", "vitb1")
        alias(VIT_B2, "vitaminb2", "riboflavin", "vitb2")
        alias(VIT_B3, "vitaminb3", "niacin", "vitb3", "niacinequivalent")
        alias(VIT_B5, "vitaminb5", "pantothenicacid", "vitb5")
        alias(VIT_B6, "vitaminb6", "vitb6", "pyridoxine")
        alias(VIT_B9, "vitaminb9", "folates", "folate", "totalfolates", "folicacid", "vitb9")
        alias(VIT_B12, "vitaminb12", "cobalamin", "vitb12")
        alias(VIT_C, "vitaminc", "ascorbicacid", "vitc")
        alias(VIT_D, "vitamind", "vitd", "cholecalciferol")
        alias(VIT_E, "vitamine", "vite", "alphatocopherol", "tocopherol")
        alias(VIT_K, "vitamink", "vitk", "vitamink1", "phylloquinone")
        alias(CALCIUM, "calcium", "ca")
        alias(IRON, "iron", "fe")
        alias(MAGNESIUM, "magnesium", "mg")
        alias(ZINC, "zinc", "zn")
        alias(POTASSIUM, "potassium", "k")
        alias(COPPER, "copper", "cu")
        alias(SELENIUM, "selenium", "se")
        alias(SODIUM, "sodium", "na")
        alias(PHOSPHORUS, "phosphorus", "p")
        alias(MANGANESE, "manganese", "mn")
        alias(IODINE, "iodine", "iodide", "i")
        alias(CHLORIDE, "chloride", "chlorine", "cld")

        // As tabelas nomeiam os ácidos gordos pela cadeia — «18:3 n-3» — e os rótulos pelo
        // nome comum. O `squash` já apaga os dois pontos e os hífenes, por isso «FA 18:3
        // c9,c12,c15 (n-3)» chega aqui como `fa183c9c12c15n3` e não como uma das entradas.
        // As siglas de três letras ficam de fora de propósito: `ALA` também é alanina e `LA`
        // não é nada em particular. Um alias errado é pior do que nenhum — atribui o número
        // à substância errada em vez de o descartar.
        alias(OMEGA3, "omega3", "omega3fat", "alphalinolenicacid", "f18d3n3")
        alias(OMEGA6, "omega6", "omega6fat", "linoleicacid", "f18d2cn6")
        alias(EPA, "epa", "eicosapentaenoicacid", "f20d5n3")
        alias(DHA, "dha", "docosahexaenoicacid", "f22d6n3")
        alias(STARCH, "starch", "amido")
        alias(LACTOSE, "lactose")
        alias(POLYOLS, "polyols", "polyol", "sugaralcohols")
        alias(RETINOL, "retinol", "retol")
        alias(BETA_CAROTENE, "betacarotene", "carotene", "cartb")
    }

    // Reduz a comparação ao essencial: "Vitamin B-12", "vitamin_b12" e "VitaminB12" ficam
    // todas iguais, e o mapa de aliases não precisa de uma entrada por pontuação.
    private fun squash(s: String): String =
        s.lowercase().filter { it.isLetterOrDigit() }

    fun canonical(sourceKey: String): String? = ALIASES[squash(sourceKey)]

    /**
     * Passa um mapa vindo de fora para as chaves canónicas. Descarta em silêncio o que não
     * reconhece: é preferível mostrar menos nutrientes do que atribuir um número à
     * substância errada.
     */
    fun normalize(raw: Map<String, Double?>): Map<String, Double> {
        val out = mutableMapOf<String, Double>()
        for ((k, v) in raw) {
            val key = canonical(k) ?: continue
            val value = v ?: continue
            // Zero conta como ausência, não como medição: as bases enchem de zeros os
            // campos que nunca analisaram, e mostrá-los diria que o alimento não tem nada.
            if (!value.isFinite() || value <= 0.0) continue
            val previous = out[key]
            // Dois nomes diferentes podem cair na mesma chave — equivalentes de retinol e
            // vitamina A, por exemplo. Fica o maior, que é o valor total.
            if (previous == null || value > previous) out[key] = value
        }
        return out
    }

    /** Junta duas fontes sem sobrepor: o `fallback` só preenche o que falta ao primeiro. */
    fun merge(primary: Map<String, Double>, fallback: Map<String, Double>): Map<String, Double> {
        val out = primary.toMutableMap()
        for ((k, v) in fallback) if (out[k] == null) out[k] = v
        return out
    }
}
