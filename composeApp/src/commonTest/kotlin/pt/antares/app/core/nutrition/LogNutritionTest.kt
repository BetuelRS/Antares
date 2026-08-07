package pt.antares.app.core.nutrition

import pt.antares.app.core.model.Sex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LogNutritionTest {

    private val reference = EfsaReference.parse(
        """
        key,male,female,unit
        vitC_mg,110,95,mg
        iron_mg,11,16,mg
        fiber_g,25,25,g
        sodium_mg,2000,2000,mg
        """.trimIndent(),
    )

    private fun labelOf(n: LogNutrition, key: String) =
        n.breakdown?.labels?.firstOrNull { it.key == key }

    @Test
    fun `escala os nutrientes para a quantidade registada`() {
        val n = LogNutrition.of(
            microsPer100Json = """{"vitC_mg":50.0,"fiber_g":2.0,"sodium_mg":100.0}""",
            grams = 200.0,
            reference = reference,
            sex = Sex.MALE,
        )
        assertEquals(4.0, labelOf(n, "fiber_g")!!.amount, 1e-9)
        assertEquals(200.0, labelOf(n, "sodium_mg")!!.amount, 1e-9)
        val vitC = n.breakdown!!.vitamins.single()
        assertEquals(100.0, vitC.amount, 1e-9)
        assertEquals(91, vitC.pctDv)
    }

    @Test
    fun `a fibra e o sodio trazem a percentagem da referencia`() {

        val n = LogNutrition.of("""{"fiber_g":25.0,"sodium_mg":2000.0}""", 100.0, reference, Sex.MALE)
        assertEquals(100, labelOf(n, "fiber_g")!!.pctDv)
        assertEquals(100, labelOf(n, "sodium_mg")!!.pctDv)
    }

    @Test
    fun `os campos de rotulo nao entram nos micronutrientes`() {
        val n = LogNutrition.of(
            """{"fiber_g":3.0,"sugars_g":1.0,"satFat_g":2.0,"sodium_mg":50.0}""",
            100.0, reference, Sex.MALE,
        )
        assertTrue(!n.breakdown!!.hasMicronutrients, "rótulo não é painel de micros")
        assertEquals(4, n.breakdown!!.labels.size)
    }

    @Test
    fun `o sodio nao aparece duas vezes`() {
        val n = LogNutrition.of("""{"sodium_mg":500.0,"iron_mg":5.0}""", 100.0, reference, Sex.MALE)
        assertEquals(500.0, labelOf(n, "sodium_mg")!!.amount, 1e-9)
        assertTrue(
            n.breakdown!!.minerals.none { it.key == "sodium_mg" },
            "o sódio tem linha de rótulo — repeti-lo nos minerais mostrava-o duas vezes",
        )
    }

    @Test
    fun `sodio muito acima da referencia vira aviso, nao destaque`() {
        val n = LogNutrition.of("""{"sodium_mg":5000.0,"iron_mg":5.0}""", 100.0, reference, Sex.MALE)
        val b = n.breakdown!!
        assertTrue(b.overLimits.any { it.key == "sodium_mg" }, "250% do VRN tem de avisar")
        assertTrue(
            b.highlights.none { it.key == "sodium_mg" },
            "no sódio, muito é o contrário de bom — destacá-lo dizia o contrário do que deve",
        )
    }

    @Test
    fun `a fibra pode ser destaque - nela muito e bom`() {
        val n = LogNutrition.of("""{"fiber_g":10.0}""", 100.0, reference, Sex.MALE)
        assertTrue(n.breakdown!!.highlights.any { it.key == "fiber_g" }, "10 g = 40% do VRN")
    }

    @Test
    fun `os outros nutrientes tem grupo proprio`() {
        val n = LogNutrition.of(
            """{"cholesterol_mg":80.0,"fatMono_g":5.0,"water_g":60.0,"alcohol_g":4.0}""",
            100.0, reference, Sex.MALE,
        )
        assertEquals(4, n.breakdown!!.others.size)
        assertTrue(!n.breakdown!!.hasMicronutrients, "colesterol e água não são vitaminas nem minerais")
    }

    @Test
    fun `registo sem snapshot nao inventa nada`() {
        assertEquals(LogNutrition.EMPTY, LogNutrition.of(null, 100.0, reference, Sex.MALE))
    }

    @Test
    fun `json partido nao rebenta a leitura do historico`() {
        assertEquals(LogNutrition.EMPTY, LogNutrition.of("{isto não é json", 100.0, reference, Sex.MALE))
    }

    @Test
    fun `sem referencia mostra os valores na mesma, so sem percentagem`() {
        val n = LogNutrition.of("""{"fiber_g":4.0,"vitC_mg":10.0}""", 100.0, null, Sex.MALE)
        assertEquals(4.0, labelOf(n, "fiber_g")!!.amount, 1e-9)
        assertNull(labelOf(n, "fiber_g")!!.pctDv)
    }
}
