package pt.antares.app.core.privacy

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DataCsvTest {

    private fun row(json: String): JsonObject =
        Json.decodeFromString(JsonObject.serializer(), json)

    @Test
    fun `cabecalho e linhas na ordem das chaves`() {
        val rows = listOf(
            row("""{"id":"a","kcal":100,"nota":"pequeno-almoco"}"""),
            row("""{"id":"b","kcal":250,"nota":"almoco"}"""),
        )
        val csv = DataCsv.fromJsonRows(rows).trim().lines()
        assertEquals("id,kcal,nota", csv[0])
        assertEquals("a,100,pequeno-almoco", csv[1])
        assertEquals("b,250,almoco", csv[2])
    }

    @Test
    fun `virgula na celula e envolvida em aspas (nao parte colunas)`() {
        val rows = listOf(row("""{"nota":"comi arroz, feijao e ovo"}"""))
        val csv = DataCsv.fromJsonRows(rows).trim().lines()
        assertEquals("nota", csv[0])
        assertEquals("\"comi arroz, feijao e ovo\"", csv[1])

        assertEquals(2, csv.size)
    }

    @Test
    fun `aspas na celula sao duplicadas`() {
        val rows = listOf(row("""{"nota":"o chamado \"bulk\""}"""))
        val csv = DataCsv.fromJsonRows(rows).trim().lines()
        assertEquals("\"o chamado \"\"bulk\"\"\"", csv[1])
    }

    @Test
    fun `round-trip - parse do CSV devolve os mesmos valores`() {
        val rows = listOf(
            row("""{"id":"x","nota":"arroz, feijao","kcal":300}"""),
            row("""{"id":"y","nota":"o \"treino\"","kcal":0}"""),
        )
        val csv = DataCsv.fromJsonRows(rows)
        val parsed = parseCsv(csv)

        assertEquals(listOf("id", "nota", "kcal"), parsed.first)
        assertEquals(listOf("x", "arroz, feijao", "300"), parsed.second[0])
        assertEquals(listOf("y", "o \"treino\"", "0"), parsed.second[1])
    }

    @Test
    fun `celula ausente ou nula fica vazia`() {
        val rows = listOf(
            row("""{"id":"a","nota":"tem"}"""),
            row("""{"id":"b","nota":null}"""),
            row("""{"id":"c"}"""),
        )
        val csv = DataCsv.fromJsonRows(rows).trim().lines()
        assertEquals("b,", csv[2])
        assertEquals("c,", csv[3])
    }

    @Test
    fun `lista vazia da CSV vazio`() {
        assertTrue(DataCsv.fromJsonRows(emptyList()).isEmpty())
    }

    private fun parseCsv(csv: String): Pair<List<String>, List<List<String>>> {
        val rows = mutableListOf<List<String>>()
        val field = StringBuilder()
        val current = mutableListOf<String>()
        var inQuotes = false
        var i = 0
        while (i < csv.length) {
            val c = csv[i]
            when {
                inQuotes -> when {
                    c == '"' && i + 1 < csv.length && csv[i + 1] == '"' -> { field.append('"'); i++ }
                    c == '"' -> inQuotes = false
                    else -> field.append(c)
                }
                c == '"' -> inQuotes = true
                c == ',' -> { current.add(field.toString()); field.clear() }
                c == '\n' -> {
                    current.add(field.toString()); field.clear()
                    rows.add(current.toList()); current.clear()
                }
                c == '\r' -> {}
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || current.isNotEmpty()) {
            current.add(field.toString())
            rows.add(current.toList())
        }
        return rows.first() to rows.drop(1)
    }
}
