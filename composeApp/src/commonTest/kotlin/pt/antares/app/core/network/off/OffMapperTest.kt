package pt.antares.app.core.network.off

import kotlinx.serialization.json.Json
import pt.antares.app.core.model.FoodSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class OffMapperTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private fun map(fixture: String, barcode: String) =
        OffMapper.toFood(json.decodeFromString<OffProductResponse>(fixture).product!!, barcode, now = 42L)

    @Test
    fun `produto completo mapeia todos os campos`() {
        val food = map(FIXTURE_COMPLETE, "3017620422003")
        assertEquals("off_3017620422003", food.id)
        assertEquals(FoodSource.OFF, food.source)
        assertEquals("3017620422003", food.sourceRef)
        assertEquals("Creme de avelãs", food.namePt)
        assertEquals("Nutella", food.nameEn)
        assertEquals("Ferrero", food.brand)
        assertEquals(539, food.kcal)
        assertEquals(6.3, food.proteinG, 1e-9)
        assertEquals(57.5, food.carbsG, 1e-9)
        assertEquals(56.3, food.sugarsG!!, 1e-9)
        assertEquals(30.9, food.fatG, 1e-9)
        assertEquals(10.6, food.satFatG!!, 1e-9)
        assertEquals(43, food.sodiumMg)
        assertEquals(15.0, food.servingGrams!!, 1e-9)
        assertNotNull(food.microsJson)
        assertEquals(true, food.verified)
    }

    @Test
    fun `micros saem em chaves canonicas e na unidade da chave`() {
        val json = """
            {"status":1,"code":"999","product":{"product_name":"Teste","nutriments":{
              "energy-kcal_100g":100,"proteins_100g":1,"carbohydrates_100g":1,"fat_100g":1,
              "calcium_100g":0.12,"vitamin-c_100g":0.03,"vitamin-b12_100g":0.0000004,
              "iodine_100g":0.00002}}}
        """.trimIndent()
        val micros = map(json, "999").microsJson!!
        val parsed = kotlinx.serialization.json.Json
            .decodeFromString<Map<String, Double>>(micros)

        assertEquals(120.0, parsed["calcium_mg"]!!, 1e-6)
        assertEquals(30.0, parsed["vitC_mg"]!!, 1e-6)
        assertEquals(0.4, parsed["vitB12_ug"]!!, 1e-6)
        assertEquals(20.0, parsed["iodine_ug"]!!, 1e-6)

        assertEquals(null, parsed["calcium"])
        assertEquals(null, parsed["vitaminC"])
    }

    @Test
    fun `energia so em kJ converte para kcal`() {
        val food = map(FIXTURE_KJ_ONLY, "111")
        assertEquals(239, food.kcal)
        assertEquals(0.5, food.proteinG, 1e-9)
        assertEquals(11.0, food.carbsG, 1e-9)
        assertNull(food.sugarsG)
        assertNull(food.sodiumMg)
    }

    @Test
    fun `sodio derivado do sal quando sodium ausente`() {
        val food = map(FIXTURE_SALT_ONLY, "222")
        assertEquals(1000, food.sodiumMg)
        assertEquals(100, food.kcal)
    }

    @Test
    fun `campos em falta nao rebentam e usam defaults honestos`() {
        val food = map(FIXTURE_EMPTY, "333")
        assertEquals(0, food.kcal)
        assertEquals(0.0, food.proteinG, 1e-9)
        assertEquals(0.0, food.carbsG, 1e-9)
        assertEquals(0.0, food.fatG, 1e-9)
        assertNull(food.sugarsG)
        assertNull(food.satFatG)
        assertNull(food.fiberG)
        assertNull(food.sodiumMg)
        assertNull(food.microsJson)
        assertNull(food.servingGrams)
        assertEquals("Vazio", food.namePt)
    }

    @Test
    fun `sem product_name usa o barcode como nome`() {
        val food = map("""{"status":1,"product":{"nutriments":{}}}""", "999")
        assertEquals("999", food.namePt)
    }

    private companion object {
        val FIXTURE_COMPLETE = """
            {"status":1,"code":"3017620422003","product":{
              "code":"3017620422003","product_name":"Nutella","product_name_pt":"Creme de avelãs",
              "brands":"Ferrero, Nutella","serving_quantity":"15",
              "nutriments":{"energy-kcal_100g":539,"energy_100g":2255,"proteins_100g":6.3,
                "carbohydrates_100g":57.5,"sugars_100g":56.3,"fat_100g":30.9,"saturated-fat_100g":10.6,
                "fiber_100g":0,"salt_100g":0.107,"sodium_100g":0.0428,"calcium_100g":0.05}}}
        """.trimIndent()

        val FIXTURE_KJ_ONLY = """
            {"status":1,"product":{"product_name":"Sumo de laranja",
              "nutriments":{"energy_100g":1000,"proteins_100g":0.5,"carbohydrates_100g":11,"fat_100g":0}}}
        """.trimIndent()

        val FIXTURE_SALT_ONLY = """
            {"status":1,"product":{"product_name":"Bolacha salgada",
              "nutriments":{"energy-kcal_100g":100,"proteins_100g":1,"carbohydrates_100g":1,"fat_100g":1,"salt_100g":2.5}}}
        """.trimIndent()

        val FIXTURE_EMPTY = """
            {"status":1,"product":{"product_name":"Vazio","nutriments":{}}}
        """.trimIndent()
    }
}
