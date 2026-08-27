package pt.antares.app.core.network.off

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OffProductResponse(

    val status: Int = 0,
    val code: String? = null,
    val product: OffProduct? = null,
)

@Serializable
data class OffSearchResponse(
    val products: List<OffProduct> = emptyList(),
    val count: Int = 0,
)

@Serializable
data class OffProduct(
    val code: String? = null,
    @SerialName("product_name") val productName: String? = null,

    @SerialName("product_name_pt") val productNamePt: String? = null,
    val brands: String? = null,
    // A miniatura do produto, tal como a Open Food Facts a publica. É o tamanho pequeno de
    // propósito: numa lista de vinte, o tamanho grande são vinte megabytes.
    @SerialName("image_small_url") val imageSmallUrl: String? = null,
    @SerialName("serving_quantity") val servingQuantity: String? = null,

    @SerialName("serving_size") val servingSize: String? = null,
    val nutriments: OffNutriments? = null,
)

@Serializable
data class OffNutriments(
    @SerialName("energy-kcal_100g") val energyKcal100g: Double? = null,
    @SerialName("energy_100g") val energy100g: Double? = null,
    @SerialName("proteins_100g") val proteins100g: Double? = null,
    @SerialName("carbohydrates_100g") val carbohydrates100g: Double? = null,
    @SerialName("sugars_100g") val sugars100g: Double? = null,
    @SerialName("fat_100g") val fat100g: Double? = null,
    @SerialName("saturated-fat_100g") val saturatedFat100g: Double? = null,
    @SerialName("fiber_100g") val fiber100g: Double? = null,

    @SerialName("sodium_100g") val sodium100g: Double? = null,
    @SerialName("salt_100g") val salt100g: Double? = null,

    @SerialName("calcium_100g") val calcium100g: Double? = null,
    @SerialName("iron_100g") val iron100g: Double? = null,
    @SerialName("magnesium_100g") val magnesium100g: Double? = null,
    @SerialName("phosphorus_100g") val phosphorus100g: Double? = null,
    @SerialName("potassium_100g") val potassium100g: Double? = null,
    @SerialName("zinc_100g") val zinc100g: Double? = null,
    @SerialName("copper_100g") val copper100g: Double? = null,
    @SerialName("manganese_100g") val manganese100g: Double? = null,
    @SerialName("selenium_100g") val selenium100g: Double? = null,
    @SerialName("iodine_100g") val iodine100g: Double? = null,
    @SerialName("vitamin-a_100g") val vitaminA100g: Double? = null,
    @SerialName("vitamin-c_100g") val vitaminC100g: Double? = null,
    @SerialName("vitamin-d_100g") val vitaminD100g: Double? = null,
    @SerialName("vitamin-e_100g") val vitaminE100g: Double? = null,
    @SerialName("vitamin-k_100g") val vitaminK100g: Double? = null,
    @SerialName("vitamin-b1_100g") val vitaminB1100g: Double? = null,
    @SerialName("vitamin-b2_100g") val vitaminB2100g: Double? = null,
    @SerialName("vitamin-pp_100g") val vitaminPp100g: Double? = null,
    @SerialName("pantothenic-acid_100g") val pantothenicAcid100g: Double? = null,
    @SerialName("vitamin-b6_100g") val vitaminB6100g: Double? = null,
    @SerialName("vitamin-b9_100g") val vitaminB9100g: Double? = null,
    @SerialName("vitamin-b12_100g") val vitaminB12100g: Double? = null,
)
