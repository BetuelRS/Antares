package pt.antares.app.core.nutrition

import pt.antares.app.core.model.Sex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NutritionBreakdownTest {

    private val reference = EfsaReference.parse(
        """
        key,male,female,unit
        vitC_mg,110,95,mg
        iron_mg,11,16,mg
        calcium_mg,950,950,mg
        """.trimIndent(),
    )

    @Test
    fun scalesToPortionAndComputesPctDv() {

        val b = NutritionFacts.build(mapOf("vitC_mg" to 100.0), amountG = 200.0, reference, Sex.MALE)
        val vitC = b.vitamins.single()
        assertEquals("vitC_mg", vitC.key)
        assertEquals(200.0, vitC.amount)
        assertEquals("mg", vitC.unit)

        assertEquals(182, vitC.pctDv)
    }

    @Test
    fun pctDvDependsOnSex() {

        val micros = mapOf("iron_mg" to 11.0)
        val male = NutritionFacts.build(micros, 100.0, reference, Sex.MALE).minerals.single()
        val female = NutritionFacts.build(micros, 100.0, reference, Sex.FEMALE).minerals.single()
        assertEquals(100, male.pctDv)
        assertEquals(69, female.pctDv)
    }

    @Test
    fun nutrientWithoutDrvHasNullPct() {

        val b = NutritionFacts.build(mapOf("vitB1_mg" to 2.0), 100.0, reference, Sex.MALE)
        val v = b.vitamins.single()
        assertEquals(2.0, v.amount)
        assertNull(v.pctDv)
    }

    @Test
    fun onlyReportedNutrientsAppear() {

        val b = NutritionFacts.build(mapOf("calcium_mg" to 120.0), 100.0, reference, Sex.MALE)
        assertTrue(b.vitamins.isEmpty())
        assertEquals(listOf("calcium_mg"), b.minerals.map { it.key })
    }

    @Test
    fun emptyMicrosGiveEmptyBreakdown() {
        val b = NutritionFacts.build(emptyMap(), 100.0, reference, Sex.MALE)
        assertTrue(b.isEmpty)
    }

    @Test
    fun zeroValuesAreHidden() {

        val b = NutritionFacts.build(
            mapOf("vitA_ug" to 0.0, "vitC_mg" to 50.0),
            amountG = 100.0, reference, Sex.MALE,
        )
        assertEquals(listOf("vitC_mg"), b.vitamins.map { it.key })
    }

    @Test
    fun highlightsFollowTheEuClaimThreshold() {

        val b = NutritionFacts.build(
            mapOf("vitC_mg" to 200.0, "calcium_mg" to 50.0),
            amountG = 100.0, reference, Sex.MALE,
        )
        val keys = b.highlights.map { it.key }
        assertTrue("vitC_mg" in keys, "vitamina C bem acima do limiar devia destacar-se")
        val calcium = b.minerals.single { it.key == "calcium_mg" }
        if ((calcium.pctDv ?: 0) < NutrientClaim.SOURCE_OF) {
            assertTrue("calcium_mg" !in keys, "abaixo de 15% do VRN não é destaque")
        }
    }

    @Test
    fun highlightsComeSortedByPercentage() {
        val b = NutritionFacts.build(
            mapOf("vitC_mg" to 500.0, "vitB1_mg" to 1.0, "iron_mg" to 8.0),
            amountG = 100.0, reference, Sex.MALE,
        )
        val pcts = b.highlights.mapNotNull { it.pctDv }
        assertEquals(pcts.sortedDescending(), pcts, "o destaque mais forte tem de vir primeiro")
    }

    @Test
    fun noHighlightsWhenNothingIsSignificant() {
        val b = NutritionFacts.build(mapOf("iron_mg" to 0.01), 100.0, reference, Sex.MALE)
        assertTrue(b.highlights.isEmpty())
    }
}
