package pt.antares.app.core.privacy

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.core.database.AntaresDb
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class GdprTableParityTest {

    private val db: AntaresDb = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        AntaresDb::class.java,
    ).setQueryCoroutineContext(Dispatchers.Default).build()

    @AfterTest
    fun tearDown() = db.close()

    private val naoSaoDaPessoa = mapOf(
        "db_info" to "flags internas do seeder e da app; não é dado de ninguém",
        "foods_fts" to "índice de pesquisa do catálogo, reconstruído a partir do `foods`",
        "food_nutrient" to
            "os mesmos micronutrientes do `microsJson`, virados ao contrário para se " +
            "poder perguntar que alimentos têm um nutriente. O `foods` **vai** na " +
            "exportação, e é dele que esta tabela se reconstrói — exportá-la seria " +
            "mandar duas vezes a mesma informação",
        "track_point" to
            "pontos crus do GPS, podados assim que o resumo da corrida grava — " +
            "o percurso vive na polyline do `run`, que **vai** na exportação",
    )

    private val doSistema = Regex("^(android_metadata|room_master_table|sqlite_.*|foods_fts_.*)$")

    private fun tabelasDaBase(): Set<String> {
        val nomes = mutableSetOf<String>()
        db.openHelper.readableDatabase
            .query("SELECT name FROM sqlite_master WHERE type = 'table'")
            .use { c ->
                while (c.moveToNext()) nomes += c.getString(0)
            }
        return nomes.filterNot { doSistema.matches(it) }.toSet()
    }

    private fun tabelasExportadas(): Set<String> {
        // O `sources` saiu do `CoreModule` quando ele foi partido por área. Lê-se o
        // ficheiro da privacidade, que é onde ele vive agora.
        val fonte = java.io.File("src/commonMain/kotlin/pt/antares/app/core/di/PrivacyModule.kt").readText()
        val lista = fonte.substringAfter("sources = listOf(").substringBefore("appVersion =")

        return Regex("""ExportSource\(\s*"([a-z_]+)"""").findAll(lista)
            .map { it.groupValues[1] }
            .toSet()
    }

    @Test
    fun `toda a tabela de dados da pessoa sai na exportacao`() {
        val naBase = tabelasDaBase()
        val exportadas = tabelasExportadas()

        assertTrue(naBase.size > 20, "só encontrei ${naBase.size} tabelas — a query deixou de funcionar")
        assertTrue(exportadas.size > 20, "só encontrei ${exportadas.size} na lista — o regex deixou de casar")

        val esquecidas = naBase - exportadas - naoSaoDaPessoa.keys
        assertEquals(
            emptySet(),
            esquecidas,
            "estas tabelas existem na base e NÃO saem na exportação. O `apagar tudo` leva-as " +
                "na mesma — ou seja, a pessoa perde dados que nunca lhe foram devolvidos. " +
                "Acrescenta-as ao `sources` do CoreModule, ou ao `naoSaoDaPessoa` deste " +
                "teste com a razão escrita.",
        )
    }

    @Test
    fun `a exportacao nao promete tabelas que nao existem`() {

        val fantasmas = tabelasExportadas() - tabelasDaBase()
        assertEquals(emptySet(), fantasmas, "nomes na lista de exportação que não existem na base")
    }

    @Test
    fun `toda a excecao traz a razao por escrito`() {
        val semRazao = naoSaoDaPessoa.filterValues { it.isBlank() }.keys
        assertTrue(semRazao.isEmpty(), "exceções sem razão: $semRazao")
    }
}
