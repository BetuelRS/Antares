package pt.antares.app.core.nutrition

import pt.antares.app.core.model.LifeStage

object LifeStageDrv {

    data class Adjusted(val key: String, val value: Double, val source: String)

    private const val FOLATE_OPINION =
        "EFSA NDA Panel (2014), DRVs for folate, EFSA Journal 12(11):3893"
    private const val IRON_OPINION =
        "EFSA NDA Panel (2015), DRVs for iron, EFSA Journal 13(10):4254"
    private const val IODINE_OPINION =
        "EFSA NDA Panel (2014), DRVs for iodine, EFSA Journal 12(5):3660"

    private val ajustes: Map<LifeStage, List<Adjusted>> = mapOf(
        LifeStage.PREGNANCY to listOf(

            Adjusted("vitB9_ug", 600.0, FOLATE_OPINION),

            Adjusted("iodine_ug", 200.0, IODINE_OPINION),
        ),
        LifeStage.LACTATION to listOf(

            Adjusted("vitB9_ug", 500.0, FOLATE_OPINION),

            Adjusted("iodine_ug", 200.0, IODINE_OPINION),
        ),
        LifeStage.POSTMENOPAUSAL to listOf(

            Adjusted("iron_mg", 11.0, IRON_OPINION),
        ),
    )

    fun adjustments(stage: LifeStage?): List<Adjusted> =
        if (stage == null || stage == LifeStage.NONE) emptyList() else ajustes[stage].orEmpty()

    fun adjustedKeys(stage: LifeStage?): Set<String> =
        adjustments(stage).map { it.key }.toSet()

    fun valueFor(key: String, stage: LifeStage?, adultValue: Double): Double =
        adjustments(stage).firstOrNull { it.key == key }?.value ?: adultValue

    fun sourceFor(key: String, stage: LifeStage?): String? =
        adjustments(stage).firstOrNull { it.key == key }?.source
}
