package pt.antares.app.core.privacy

import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class DataExporter(

    val sources: List<ExportSource<*>>,
    private val appVersion: String,
) {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    fun filename(): String = "antares-dados-${today()}.json"

    fun zipName(): String = "antares-dados-${today()}.zip"

    private fun today(): String = Clock.System.now().toString().substringBefore('T')

    suspend fun exportJson(): String {
        val root = buildJsonObject {
            put("exportadoEm", Clock.System.now().toString())
            put("versaoApp", appVersion)
            put("nota", "Dados pessoais exportados do Antares. O catálogo de alimentos e a biblioteca de exercícios que vêm com a app não estão incluídos.")
        }

        val body = mutableMapOf<String, JsonArray>()
        for (source in sources) body[source.name] = rowsOf(source)

        return json.encodeToString(
            JsonObject.serializer(),
            JsonObject(root + body.mapValues { it.value }),
        )
    }

    private suspend fun <T : Any> rowsOf(source: ExportSource<T>): JsonArray =
        JsonArray(source.rows().map { json.encodeToJsonElement(source.serializer, it) })

    suspend fun exportCsvFiles(): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        for (source in sources) {
            val csv = csvOf(source)
            if (csv != null) out["${source.name}.csv"] = csv
        }
        return out
    }

    private suspend fun <T : Any> csvOf(source: ExportSource<T>): String? =
        csvFrom(rowsOf(source))

    private fun csvFrom(rows: JsonArray): String? {
        if (rows.isEmpty()) return null
        return DataCsv.fromJsonRows(rows.filterIsInstance<JsonObject>())
    }
}

object DataCsv {

    fun fromJsonRows(rows: List<JsonObject>): String {
        if (rows.isEmpty()) return ""
        val columns = LinkedHashSet<String>()
        for (row in rows) columns.addAll(row.keys)

        val sb = StringBuilder()
        sb.append(columns.joinToString(",") { escape(it) }).append('\n')
        for (row in rows) {
            sb.append(
                columns.joinToString(",") { col -> escape(cell(row[col])) },
            ).append('\n')
        }
        return sb.toString()
    }

    private fun cell(value: kotlinx.serialization.json.JsonElement?): String {
        if (value == null) return ""
        val prim = value as? kotlinx.serialization.json.JsonPrimitive ?: return value.toString()
        if (prim is kotlinx.serialization.json.JsonNull) return ""
        return prim.content
    }

    private fun escape(field: String): String {
        val needsQuoting = field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuoting) return field
        return "\"" + field.replace("\"", "\"\"") + "\""
    }
}
