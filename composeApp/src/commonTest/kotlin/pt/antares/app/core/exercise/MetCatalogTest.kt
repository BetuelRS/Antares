package pt.antares.app.core.exercise

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MetCatalogTest {

    private val csv = """
        id,name_pt,name_en,met,category
        walk_casual,Caminhada casual,Walking casual,3.5,walking
        run_moderate,Corrida moderada,Running moderate,11.0,running
        linha_ma
        cycle_light,Bicicleta ligeira,Cycling light,5.8,cycling
        walk_brisk,Caminhada rápida,Walking brisk,4.3,walking
    """.trimIndent()

    @Test
    fun `parse le atividades e ignora linhas mas`() {
        val catalog = MetCatalog.parse(csv)
        assertEquals(4, catalog.activities.size)
    }

    @Test
    fun `byId devolve a atividade certa`() {
        val catalog = MetCatalog.parse(csv)
        assertEquals(11.0, catalog.byId("run_moderate")?.met)
        assertEquals("Caminhada casual", catalog.byId("walk_casual")?.namePt)
        assertNull(catalog.byId("inexistente"))
    }

    @Test
    fun `categorias preservam ordem e filtram`() {
        val catalog = MetCatalog.parse(csv)
        assertEquals(listOf("walking", "running", "cycling"), catalog.categories())
        assertEquals(2, catalog.inCategory("walking").size)
    }
}
