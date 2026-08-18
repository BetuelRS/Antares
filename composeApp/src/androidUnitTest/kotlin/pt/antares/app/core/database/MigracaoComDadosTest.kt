package pt.antares.app.core.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Cobra a regra D5: nenhuma migração entra sem um teste que a corra sobre uma base cheia.
 *
 * Distingue-se do [Migration2to3Test], que prova uma migração concreta com SQL escrito à
 * mão: **este não sabe nada do esquema.** Constrói a base a partir do JSON exportado da
 * versão mais antiga com dados, enche todas as tabelas, e abre com o Room. Uma versão nova
 * do esquema fica coberta sem ninguém lhe tocar — que é o que interessa para as migrações
 * que ainda não existem.
 */
@RunWith(RobolectricTestRunner::class)
class MigracaoComDadosTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val dbName = "migracao-com-dados-test.db"

    // A v3 é a primeira em que alguém já podia ter o que perder: as duas anteriores só
    // tinham o perfil e os pesos.
    private val versaoDePartida = 3

    private val esquemas = File("schemas/pt.antares.app.core.database.AntaresDb")

    // A tabela de pesquisa é derivada do catálogo e o semeador reconstrói-a. Enchê-la aqui
    // não provava nada, e um virtual table do FTS4 não aceita as mesmas inserções.
    private val derivadas = setOf("foods_fts")

    @Before fun antes() = context.deleteDatabase(dbName).let { }

    @After fun depois() = context.deleteDatabase(dbName).let { }

    @Test
    fun `abrir uma base antiga cheia migra ate hoje sem perder linhas`() = runTest {
        val tabelas = construirBaseAntiga()
        assertTrue(tabelas.size >= 7, "a v$versaoDePartida devia ter as tabelas todas: $tabelas")

        val db = Room.databaseBuilder(context, AntaresDb::class.java, dbName)
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()

        val versaoFinal: Int
        try {
            // A migração só corre quando alguém pede a base, e não no `build()`.
            val aberta = db.openHelper.writableDatabase
            versaoFinal = aberta.version

            for (tabela in tabelas) {
                aberta.query("SELECT COUNT(*) FROM `$tabela`").use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals(
                        1,
                        cursor.getInt(0),
                        "a linha de `$tabela` não sobreviveu à migração para a v$versaoFinal",
                    )
                }
            }
        } finally {
            db.close()
        }

        assertTrue(
            versaoFinal > versaoDePartida,
            "a base não migrou: ficou na v$versaoFinal",
        )
    }

    /** Devolve os nomes das tabelas que ficaram com uma linha lá dentro. */
    private fun construirBaseAntiga(): List<String> {
        val json = Json { ignoreUnknownKeys = true }
        val raiz = json.parseToJsonElement(
            File(esquemas, "$versaoDePartida.json").readText(),
        ).jsonObject["database"]!!.jsonObject

        val ficheiro = context.getDatabasePath(dbName)
        ficheiro.parentFile?.mkdirs()
        val antiga = SQLiteDatabase.openOrCreateDatabase(ficheiro, null)
        val cheias = mutableListOf<String>()

        antiga.beginTransaction()
        try {
            val entidades = raiz["entities"]!!.jsonArray.map { it.jsonObject }

            for (entidade in entidades) {
                val tabela = entidade["tableName"]!!.jsonPrimitive.content
                antiga.execSQL(
                    entidade["createSql"]!!.jsonPrimitive.content
                        .replace("\${TABLE_NAME}", tabela),
                )
                entidade["indices"]?.jsonArray?.forEach { indice ->
                    antiga.execSQL(
                        indice.jsonObject["createSql"]!!.jsonPrimitive.content
                            .replace("\${TABLE_NAME}", tabela),
                    )
                }
            }

            // O Room recusa abrir uma base cuja impressão digital não conheça, e é ela que
            // lhe diz de que versão está a partir.
            raiz["setupQueries"]?.jsonArray?.forEach { antiga.execSQL(it.jsonPrimitive.content) }

            for (entidade in entidades) {
                val tabela = entidade["tableName"]!!.jsonPrimitive.content
                if (tabela in derivadas) continue
                antiga.execSQL(inserirUmaLinha(entidade))
                cheias += tabela
            }

            antiga.execSQL("PRAGMA user_version = $versaoDePartida")
            antiga.setTransactionSuccessful()
        } finally {
            antiga.endTransaction()
            antiga.close()
        }
        return cheias
    }

    /**
     * Monta um `INSERT` a partir dos campos declarados no esquema. Os valores não querem
     * dizer nada — o que se prova é que a linha atravessa as migrações, e não o que tem
     * dentro. Por isso também não passam por conversores: as leituras deste teste são
     * `COUNT(*)`, e um `mealSlot` a dizer «t1» nunca chega a virar enumeração.
     */
    private fun inserirUmaLinha(entidade: kotlinx.serialization.json.JsonObject): String {
        val tabela = entidade["tableName"]!!.jsonPrimitive.content
        val campos = entidade["fields"]!!.jsonArray.map { it.jsonObject }

        val colunas = campos.map { "`" + it["columnName"]!!.jsonPrimitive.content + "`" }
        val valores = campos.mapIndexed { i, campo ->
            when (campo["affinity"]!!.jsonPrimitive.content) {
                "INTEGER" -> "$i"
                "REAL" -> "$i.0"
                else -> "'t$i'"
            }
        }

        return "INSERT INTO `$tabela` (${colunas.joinToString(",")}) " +
            "VALUES (${valores.joinToString(",")})"
    }
}
