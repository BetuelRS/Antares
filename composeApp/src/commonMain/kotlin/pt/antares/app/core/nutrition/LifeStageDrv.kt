package pt.antares.app.core.nutrition

import pt.antares.app.core.model.LifeStage

/**
 * As poucas referências que mudam com a fase da vida. Só entram aqui as que têm um parecer
 * da EFSA a citar — a app não interpola valores nem estende ajustes por analogia, e é por
 * isso que a lista é curta.
 */
object LifeStageDrv {

    // A citação viaja com o valor para o ecrã poder mostrar de onde vem o número ajustado.
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

    // `NONE` e ausente valem o mesmo: quem não declarou fase da vida usa os valores de
    // adulto, sem a app inferir nada a partir do sexo ou da idade.
    fun adjustments(stage: LifeStage?): List<Adjusted> =
        if (stage == null || stage == LifeStage.NONE) emptyList() else ajustes[stage].orEmpty()

    fun adjustedKeys(stage: LifeStage?): Set<String> =
        adjustments(stage).map { it.key }.toSet()

    // O valor de adulto é o padrão: um nutriente sem ajuste para esta fase passa intacto.
    fun valueFor(key: String, stage: LifeStage?, adultValue: Double): Double =
        adjustments(stage).firstOrNull { it.key == key }?.value ?: adultValue

    fun sourceFor(key: String, stage: LifeStage?): String? =
        adjustments(stage).firstOrNull { it.key == key }?.source
}
