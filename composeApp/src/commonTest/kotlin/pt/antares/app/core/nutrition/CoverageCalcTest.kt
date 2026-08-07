package pt.antares.app.core.nutrition

import pt.antares.app.core.model.Sex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CoverageCalcTest {

    private val drvs = listOf(
        Drv("vitC_mg", male = 110.0, female = 95.0, unit = "mg"),
        Drv("iron_mg", male = 11.0, female = 16.0, unit = "mg"),
        Drv("vitD_ug", male = 15.0, female = 15.0, unit = "µg"),
    )

    private fun fullyMeasured(byKey: Map<String, Double>, kcal: Double = 2000.0) =
        MicroTotals(byKey, byKey.mapValues { kcal }, kcal)

    @Test
    fun `dados parciais cobrem so o que existe`() {

        val totals = fullyMeasured(mapOf("vitC_mg" to 55.0, "iron_mg" to 22.0))
        val result = CoverageCalc.compute(totals, Sex.MALE, drvs).associateBy { it.key }

        assertEquals(50, result["vitC_mg"]!!.coveragePct)
        assertTrue(result["vitC_mg"]!!.hasData)
        assertEquals(200, result["iron_mg"]!!.coveragePct)
        assertFalse(result["vitD_ug"]!!.hasData)
        assertEquals(0, result["vitD_ug"]!!.coveragePct)
    }

    @Test
    fun `referencia depende do sexo`() {
        val totals = fullyMeasured(mapOf("iron_mg" to 16.0))
        val male = CoverageCalc.compute(totals, Sex.MALE, drvs).first { it.key == "iron_mg" }
        val female = CoverageCalc.compute(totals, Sex.FEMALE, drvs).first { it.key == "iron_mg" }
        assertEquals(145, male.coveragePct)
        assertEquals(100, female.coveragePct)
    }

    @Test
    fun `soma medida em pouco do dia vem marcada como parcial`() {

        val totals = MicroTotals(
            byKey = mapOf("iron_mg" to 5.0),
            measuredKcalByKey = mapOf("iron_mg" to 500.0),
            totalKcal = 2000.0,
        )
        val iron = CoverageCalc.compute(totals, Sex.MALE, drvs).first { it.key == "iron_mg" }
        assertEquals(25, iron.measuredPct)
        assertTrue(iron.isPartial, "25% da ingestão medida tem de ser assinalado")
    }

    @Test
    fun `soma que cobre quase tudo nao e parcial`() {
        val totals = MicroTotals(
            byKey = mapOf("iron_mg" to 10.0),
            measuredKcalByKey = mapOf("iron_mg" to 1800.0),
            totalKcal = 2000.0,
        )
        val iron = CoverageCalc.compute(totals, Sex.MALE, drvs).first { it.key == "iron_mg" }
        assertEquals(90, iron.measuredPct)
        assertFalse(iron.isPartial)
    }

    @Test
    fun `sodio sozinho nao conta como micronutriente medido`() {

        assertFalse(
            MicroTotals.hasRealMicros(setOf("fiber_g", "sugars_g", "satFat_g", "sodium_mg")),
            "só campos de rótulo não são micronutrientes medidos",
        )
        assertTrue(MicroTotals.hasRealMicros(setOf("sodium_mg", "magnesium_mg")))
    }

    @Test
    fun `sem dados nunca e parcial - ja diz sem dados`() {
        val iron = CoverageCalc.compute(MicroTotals.EMPTY, Sex.MALE, drvs).first { it.key == "iron_mg" }
        assertFalse(iron.hasData)
        assertFalse(iron.isPartial, "sem dados já se explica sozinho; marcar como parcial só confundia")
    }

    @Test
    fun `parser EFSA ignora linhas mas le as boas`() {
        val csv = """
            key,male,female,unit
            vitC_mg,110,95,mg
            linha_ma_sem_colunas
            iron_mg,11,16,mg
        """.trimIndent()
        val ref = EfsaReference.parse(csv)
        assertEquals(2, ref.all().size)
        assertEquals(110.0, ref.forKey("vitC_mg")?.male)
        assertEquals(16.0, ref.forKey("iron_mg")?.forSex(Sex.FEMALE))
    }
}
