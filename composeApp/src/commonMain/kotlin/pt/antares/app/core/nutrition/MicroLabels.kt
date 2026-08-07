package pt.antares.app.core.nutrition

import org.jetbrains.compose.resources.StringResource
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*

fun microLabelRes(key: String): StringResource = when (key) {
    "vitA_ug" -> Res.string.stat_micro_vitA_ug
    "vitB1_mg" -> Res.string.stat_micro_vitB1_mg
    "vitB2_mg" -> Res.string.stat_micro_vitB2_mg
    "vitB3_mg" -> Res.string.stat_micro_vitB3_mg
    "vitB5_mg" -> Res.string.stat_micro_vitB5_mg
    "vitB6_mg" -> Res.string.stat_micro_vitB6_mg
    "vitB9_ug" -> Res.string.stat_micro_vitB9_ug
    "vitB12_ug" -> Res.string.stat_micro_vitB12_ug
    "vitC_mg" -> Res.string.stat_micro_vitC_mg
    "vitD_ug" -> Res.string.stat_micro_vitD_ug
    "vitE_mg" -> Res.string.stat_micro_vitE_mg
    "vitK_ug" -> Res.string.stat_micro_vitK_ug
    "calcium_mg" -> Res.string.stat_micro_calcium_mg
    "iron_mg" -> Res.string.stat_micro_iron_mg
    "magnesium_mg" -> Res.string.stat_micro_magnesium_mg
    "zinc_mg" -> Res.string.stat_micro_zinc_mg
    "potassium_mg" -> Res.string.stat_micro_potassium_mg
    "copper_mg" -> Res.string.stat_micro_copper_mg
    "selenium_ug" -> Res.string.stat_micro_selenium_ug

    "phosphorus_mg" -> Res.string.stat_micro_phosphorus_mg
    "manganese_mg" -> Res.string.stat_micro_manganese_mg
    "iodine_ug" -> Res.string.stat_micro_iodine_ug
    "fiber_g" -> Res.string.stat_micro_fiber_g
    "sodium_mg" -> Res.string.stat_micro_sodium_mg

    "sugars_g" -> Res.string.nutrition_sugar
    "satFat_g" -> Res.string.nutrition_satfat
    "water_g" -> Res.string.stat_micro_water
    "alcohol_g" -> Res.string.stat_micro_alcohol
    "cholesterol_mg" -> Res.string.stat_micro_cholesterol
    "fatMono_g" -> Res.string.stat_micro_fatmono
    "fatTrans_g" -> Res.string.stat_micro_fattrans
    "fatPoly_g" -> Res.string.stat_micro_fatpoly
    else -> Res.string.stat_title
}
