package pt.antares.app.core.network.off

import kotlinx.serialization.json.Json
import pt.antares.app.core.database.entities.FoodEntity
import pt.antares.app.core.model.FoodSource
import pt.antares.app.core.nutrition.Nutrients
import kotlin.math.roundToInt

object OffMapper {

    private const val KJ_PER_KCAL = 4.184

    private const val SALT_TO_SODIUM = 2.5

    fun toFood(product: OffProduct, barcode: String, now: Long): FoodEntity {
        val n = product.nutriments

        val kcal = n?.energyKcal100g?.roundToInt()
            ?: n?.energy100g?.let { (it / KJ_PER_KCAL).roundToInt() }
            ?: 0

        val sodiumMg = n?.sodium100g?.let { (it * 1000).roundToInt() }
            ?: n?.salt100g?.let { (it / SALT_TO_SODIUM * 1000).roundToInt() }

        val name = product.productNamePt?.takeIf { it.isNotBlank() }
            ?: product.productName?.takeIf { it.isNotBlank() }
            ?: barcode
        val nameEn = product.productName?.takeIf { it.isNotBlank() } ?: name

        val porcao = ServingParse.from(product.servingQuantity, product.servingSize)

        return FoodEntity(
            id = "off_$barcode",
            source = FoodSource.OFF,
            sourceRef = barcode,
            namePt = name,
            nameEn = nameEn,
            brand = product.brands?.split(",")?.firstOrNull()?.trim()?.takeIf { it.isNotBlank() },
            kcal = kcal,
            proteinG = n?.proteins100g ?: 0.0,
            carbsG = n?.carbohydrates100g ?: 0.0,
            sugarsG = n?.sugars100g,
            fatG = n?.fat100g ?: 0.0,
            satFatG = n?.saturatedFat100g,
            fiberG = n?.fiber100g,
            sodiumMg = sodiumMg,
            microsJson = microsJson(n),

            servingName = porcao.name,
            servingGrams = porcao.grams,
            verified = true,
            updatedAt = now,
            dirty = false,
        )
    }

    private fun microsJson(n: OffNutriments?): String? {
        if (n == null) return null

        fun scaled(key: String, grams: Double?): Pair<String, Double>? {
            val g = grams ?: return null
            val factor = when {
                key.endsWith("_ug") -> 1_000_000.0
                key.endsWith("_mg") -> 1_000.0
                else -> 1.0
            }
            return key to g * factor
        }

        val micros = listOfNotNull(
            scaled(Nutrients.CALCIUM, n.calcium100g),
            scaled(Nutrients.IRON, n.iron100g),
            scaled(Nutrients.MAGNESIUM, n.magnesium100g),
            scaled(Nutrients.PHOSPHORUS, n.phosphorus100g),
            scaled(Nutrients.POTASSIUM, n.potassium100g),
            scaled(Nutrients.ZINC, n.zinc100g),
            scaled(Nutrients.COPPER, n.copper100g),
            scaled(Nutrients.MANGANESE, n.manganese100g),
            scaled(Nutrients.SELENIUM, n.selenium100g),
            scaled(Nutrients.IODINE, n.iodine100g),
            scaled(Nutrients.VIT_A, n.vitaminA100g),
            scaled(Nutrients.VIT_C, n.vitaminC100g),
            scaled(Nutrients.VIT_D, n.vitaminD100g),
            scaled(Nutrients.VIT_E, n.vitaminE100g),
            scaled(Nutrients.VIT_K, n.vitaminK100g),
            scaled(Nutrients.VIT_B1, n.vitaminB1100g),
            scaled(Nutrients.VIT_B2, n.vitaminB2100g),
            scaled(Nutrients.VIT_B3, n.vitaminPp100g),
            scaled(Nutrients.VIT_B5, n.pantothenicAcid100g),
            scaled(Nutrients.VIT_B6, n.vitaminB6100g),
            scaled(Nutrients.VIT_B9, n.vitaminB9100g),
            scaled(Nutrients.VIT_B12, n.vitaminB12100g),
        ).toMap()

        val clean = Nutrients.normalize(micros)
        return if (clean.isEmpty()) null else Json.encodeToString(clean)
    }
}
