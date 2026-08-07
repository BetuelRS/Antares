package pt.antares.app.core.nutrition

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

    const val FIBER = "fiber_g"
    const val SUGARS = "sugars_g"
    const val SAT_FAT = "satFat_g"

    const val WATER = "water_g"
    const val ALCOHOL = "alcohol_g"
    const val CHOLESTEROL = "cholesterol_mg"
    const val FAT_MONO = "fatMono_g"
    const val FAT_POLY = "fatPoly_g"

    const val FAT_TRANS = "fatTrans_g"

    val VITAMINS = listOf(
        VIT_A, VIT_B1, VIT_B2, VIT_B3, VIT_B5, VIT_B6,
        VIT_B9, VIT_B12, VIT_C, VIT_D, VIT_E, VIT_K,
    )

    val MINERALS = listOf(
        CALCIUM, IRON, MAGNESIUM, PHOSPHORUS, POTASSIUM, SODIUM,
        ZINC, COPPER, MANGANESE, SELENIUM, IODINE,
    )

    val OTHERS = listOf(FAT_MONO, FAT_POLY, FAT_TRANS, CHOLESTEROL, WATER, ALCOHOL)

    val LABEL = listOf(SUGARS, FIBER, SAT_FAT, SODIUM)

    val ALL: List<String> = VITAMINS + MINERALS + OTHERS + listOf(FIBER, SUGARS, SAT_FAT)
    private val ALL_SET = ALL.toSet()

    fun isCanonical(key: String): Boolean = key in ALL_SET

    fun unitOf(key: String): String = when {
        key.endsWith("_ug") -> "µg"
        key.endsWith("_mg") -> "mg"
        key.endsWith("_g") -> "g"
        else -> ""
    }

    private val ALIASES: Map<String, String> = buildMap {

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
    }

    private fun squash(s: String): String =
        s.lowercase().filter { it.isLetterOrDigit() }

    fun canonical(sourceKey: String): String? = ALIASES[squash(sourceKey)]

    fun normalize(raw: Map<String, Double?>): Map<String, Double> {
        val out = mutableMapOf<String, Double>()
        for ((k, v) in raw) {
            val key = canonical(k) ?: continue
            val value = v ?: continue
            if (!value.isFinite() || value <= 0.0) continue
            val previous = out[key]
            if (previous == null || value > previous) out[key] = value
        }
        return out
    }

    fun merge(primary: Map<String, Double>, fallback: Map<String, Double>): Map<String, Double> {
        val out = primary.toMutableMap()
        for ((k, v) in fallback) if (out[k] == null) out[k] = v
        return out
    }
}
