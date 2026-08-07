package pt.antares.app.core.privacy

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import pt.antares.app.core.database.entities.CycleEntity
import pt.antares.app.core.database.entities.WeightLogEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DataExporterTest {

    private fun tableOf(rows: List<WeightLogEntity>) =

        ExportSource("weight_log", WeightLogEntity.serializer()) { rows.filter { !it.deleted } }

    private fun weight(id: String, kg: Double, deleted: Boolean = false) = WeightLogEntity(
        id = id, epochDay = 20_000, weightKg = kg, note = null,
        updatedAt = 1_000, deleted = deleted, dirty = false,
    )

    @Test
    fun `exporta as linhas vivas com os valores certos`() = runTest {
        val exporter = DataExporter(listOf(tableOf(listOf(weight("a", 80.0)))), appVersion = "9.9.9")

        val root = Json.parseToJsonElement(exporter.exportJson()).jsonObject
        val rows = root["weight_log"]!!.jsonArray

        assertEquals(1, rows.size)
        assertEquals("a", rows[0].jsonObject["id"]!!.jsonPrimitive.content)
        assertEquals(80.0, rows[0].jsonObject["weightKg"]!!.jsonPrimitive.content.toDouble())
        assertEquals("9.9.9", root["versaoApp"]!!.jsonPrimitive.content)
    }

    @Test
    fun `tombstones NAO vao no ficheiro exportado`() = runTest {
        val rows = listOf(weight("viva", 80.0), weight("apagada", 70.0, deleted = true))
        val exporter = DataExporter(listOf(tableOf(rows)), appVersion = "1.0")

        val exported = Json.parseToJsonElement(exporter.exportJson())
            .jsonObject["weight_log"]!!.jsonArray

        assertEquals(1, exported.size)
        assertEquals("viva", exported[0].jsonObject["id"]!!.jsonPrimitive.content)
    }

    @Test
    fun `sem dados o ficheiro sai valido e vazio, nao rebenta`() = runTest {
        val exporter = DataExporter(listOf(tableOf(emptyList())), appVersion = "1.0")

        val root = Json.parseToJsonElement(exporter.exportJson()).jsonObject

        assertTrue(root["weight_log"]!!.jsonArray.isEmpty())
        assertTrue(root.containsKey("exportadoEm"))
    }

    @Test
    fun `o nome do ficheiro tem a data e a extensao certa`() {
        val name = DataExporter(emptyList(), appVersion = "1.0").filename()
        assertTrue(name.startsWith("antares-dados-"), name)
        assertTrue(name.endsWith(".json"), name)
    }

    @Test
    fun `as tabelas locais tambem saem no ficheiro`() = runTest {
        val ciclo = ExportSource("cycle_log", CycleEntity.serializer()) {
            listOf(CycleEntity(id = "c1", startEpochDay = 20_000, endEpochDay = 20_004, createdAt = 1L))
        }
        val exporter = DataExporter(listOf(ciclo), appVersion = "1.0")

        val root = Json.parseToJsonElement(exporter.exportJson()).jsonObject
        val linhas = root["cycle_log"]?.jsonArray

        assertTrue(linhas != null, "o cycle_log não saiu na exportação")
        assertEquals(1, linhas.size)
        assertEquals(
            20_000,
            linhas[0].jsonObject["startEpochDay"]!!.jsonPrimitive.content.toInt(),
        )
    }

    @Test
    fun `as tabelas locais tambem saem em CSV`() = runTest {
        val ciclo = ExportSource("cycle_log", CycleEntity.serializer()) {
            listOf(CycleEntity(id = "c1", startEpochDay = 20_000, endEpochDay = null, createdAt = 1L))
        }
        val ficheiros = DataExporter(listOf(ciclo), appVersion = "1.0")
            .exportCsvFiles()

        assertTrue("cycle_log.csv" in ficheiros, "ficheiros: ${ficheiros.keys}")
        assertTrue(ficheiros.getValue("cycle_log.csv").contains("20000"))
    }
}
