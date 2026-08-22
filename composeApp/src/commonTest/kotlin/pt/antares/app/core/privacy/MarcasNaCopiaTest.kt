package pt.antares.app.core.privacy

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import pt.antares.app.core.database.entities.FoodMarkEntity
import pt.antares.app.feature.fooddata.FoodSeeder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Os favoritos e os recentes viajam na cópia de segurança.
 *
 * Não viajavam, e a maneira como isso acontecia é o que este teste guarda. Da tabela de
 * alimentos só se exportam os que a pessoa criou — o catálogo não se exporta, por ser grande
 * e reconstruível — e o favorito vivia **dentro da linha do alimento do catálogo**. O
 * resultado era uma cópia que parecia completa, com todas as tabelas lá, e que ao ser
 * restaurada deixava a pessoa sem nada do que tinha marcado. Sem erro, e sem nada no ficheiro
 * que dissesse que faltava.
 *
 * A ligação entre as duas coisas — «o catálogo não se exporta» e «o favorito vive no
 * catálogo» — não está escrita em sítio nenhum do código. Está aqui.
 */
class MarcasNaCopiaTest {

    private val marcas = listOf(
        FoodMarkEntity(
            foodId = "ciqual-2381", isFavorite = true, lastUsedAt = 0L,
            lastAmountG = 250.0, updatedAt = 1_000L,
        ),
        FoodMarkEntity(
            foodId = "tca-1099", isFavorite = false, lastUsedAt = 1_700_000_000_000L,
            lastAmountG = null, updatedAt = 1_000L,
        ),
        FoodMarkEntity(
            foodId = "ptx_cafe", isFavorite = true, lastUsedAt = 0L,
            lastAmountG = null, updatedAt = 1_000L, deleted = true,
        ),
    )

    private fun exportador() = DataExporter(
        listOf(
            ExportSource("food_marca", FoodMarkEntity.serializer()) { marcas.filter { !it.deleted } },
        ),
        appVersion = "9.9.9",
    )

    @Test
    fun `as marcas do catalogo vao no ficheiro`() = runTest {
        val raiz = Json.parseToJsonElement(exportador().exportJson()).jsonObject
        val linhas = raiz["food_marca"]!!.jsonArray

        assertEquals(2, linhas.size, "as marcas não chegaram ao ficheiro")

        val favorito = linhas.map { it.jsonObject }.first { it["foodId"]!!.jsonPrimitive.content == "ciqual-2381" }
        assertTrue(favorito["isFavorite"]!!.jsonPrimitive.content.toBoolean())
        assertEquals(250.0, favorito["lastAmountG"]!!.jsonPrimitive.content.toDouble())

        val recente = linhas.map { it.jsonObject }.first { it["foodId"]!!.jsonPrimitive.content == "tca-1099" }
        assertEquals(1_700_000_000_000L, recente["lastUsedAt"]!!.jsonPrimitive.content.toLong())
    }

    @Test
    fun `a copia diz com que versao do catalogo foi feita`() = runTest {

        // Guardada, e não usada para recusar nada: o diário copia a nutrição toda no momento
        // do registo, por isso um histórico restaurado não depende do catálogo. Serve para
        // se perceber, mais tarde, com que números aquela cópia foi feita.
        val raiz = Json.parseToJsonElement(exportador().exportJson()).jsonObject
        val versao = raiz["versaoCatalogo"]!!.jsonPrimitive.content.toInt()

        assertEquals(FoodSeeder.VERSAO_DO_CATALOGO, versao)
    }

    @Test
    fun `o resumo le a versao do catalogo, e aguenta uma copia antiga que nao a traga`() {
        val nova = LeitorDeResumo.ler(
            """{"exportadoEm":"2026-08-22T10:00:00Z","versaoApp":"2.5.0","versaoCatalogo":1,"food_marca":[]}""",
        )
        assertEquals(1, nova?.versaoCatalogo)

        // As cópias feitas antes da 2.5.0 não a trazem, e continuam a poder ser importadas:
        // o resumo não é uma condição de entrada, é o que se lê antes de decidir.
        val antiga = LeitorDeResumo.ler(
            """{"exportadoEm":"2026-01-01T10:00:00Z","versaoApp":"2.0.0","weight_log":[]}""",
        )
        assertEquals(null, antiga?.versaoCatalogo)
        assertEquals("2.0.0", antiga?.versaoApp)
    }
}
