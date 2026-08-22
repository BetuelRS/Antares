package pt.antares.app.core.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.core.nutrition.microsDeJson
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * O sódio e a fibra mudam da coluna para o mapa sem se perderem pelo caminho.
 *
 * A migração escreve dentro de uma cadeia de JSON com operações de texto, e não com as funções
 * de JSON do SQLite — que são uma extensão que pode não estar compilada no motor de um
 * telemóvel qualquer, e uma migração que rebenta lá é uma app que não abre. O preço dessa
 * escolha é este teste: **o campo tem três formas**, e a operação de texto tem de servir as
 * três.
 *
 * O modo de falhar é o do costume neste bloco: um JSON mal fechado não dá erro na migração.
 * Dá uma app que abre, com um alimento cujo mapa de micronutrientes deixou de se conseguir
 * ler — e os ecrãs de micronutrientes ficam vazios para esse alimento, sem nada a dizer
 * porquê.
 */
@RunWith(RobolectricTestRunner::class)
class MigracaoDeSodioEFibraTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val nome = "migracao-sodio-fibra-test.db"

    @Before fun antes() { context.deleteDatabase(nome) }

    @After fun depois() { context.deleteDatabase(nome) }

    private fun baseNaV27(): File {
        val json = Json { ignoreUnknownKeys = true }
        val raiz = json
            .parseToJsonElement(File(ESQUEMAS, "$VERSAO_DE_PARTIDA.json").readText())
            .jsonObject["database"]!!.jsonObject

        val ficheiro = context.getDatabasePath(nome)
        ficheiro.parentFile?.mkdirs()
        val db = SQLiteDatabase.openOrCreateDatabase(ficheiro, null)

        for (elemento in raiz["entities"]!!.jsonArray) {
            val entidade = elemento.jsonObject
            val tabela = entidade["tableName"]!!.jsonPrimitive.content
            db.execSQL(entidade["createSql"]!!.jsonPrimitive.content.replace(MARCA, tabela))
            entidade["indices"]?.jsonArray?.forEach { indice ->
                db.execSQL(
                    indice.jsonObject["createSql"]!!.jsonPrimitive.content.replace(MARCA, tabela),
                )
            }
        }
        raiz["setupQueries"]?.jsonArray?.forEach { db.execSQL(it.jsonPrimitive.content) }
        db.version = VERSAO_DE_PARTIDA
        db.close()
        return ficheiro
    }

    private fun inserir(
        db: SQLiteDatabase,
        id: String,
        microsJson: String?,
        fiberG: Double?,
        sodiumMg: Int?,
    ) {
        db.execSQL(
            "INSERT INTO foods (id, source, sourceRef, namePt, nameEn, brand, kcal, proteinG, " +
                "carbsG, sugarsG, fatG, satFatG, fiberG, sodiumMg, microsJson, servingName, " +
                "servingGrams, isLiquid, verified, updatedAt, deleted) VALUES " +
                "(?, 'SEED', NULL, ?, ?, NULL, 100, 1.0, 1.0, NULL, 1.0, NULL, ?, ?, ?, " +
                "NULL, NULL, 0, 1, 1, 0)",
            arrayOf(id, id, id, fiberG, sodiumMg, microsJson),
        )
    }

    private fun comABaseAberta(bloco: (SQLiteDatabase) -> Unit) {
        val ficheiro = baseNaV27()
        SQLiteDatabase.openDatabase(ficheiro.path, null, SQLiteDatabase.OPEN_READWRITE).use(bloco)
    }

    private fun abrirComORoom(bloco: suspend (AntaresDb) -> Unit) = runBlocking {
        val db = Room.databaseBuilder(context, AntaresDb::class.java, nome)
            .addMigrations(MIGRACAO_26_PARA_27, MIGRACAO_27_PARA_28)
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()
        try {
            bloco(db)
        } finally {
            db.close()
        }
    }

    @Test
    fun `as tres formas do campo aguentam a mudanca`() {
        comABaseAberta { db ->
            inserir(db, "sem-mapa", microsJson = null, fiberG = 2.5, sodiumMg = 400)
            inserir(db, "mapa-vazio", microsJson = "{}", fiberG = 3.0, sodiumMg = 12)
            inserir(
                db, "mapa-cheio",
                microsJson = """{"calcium_mg":120.0,"iron_mg":2.0}""",
                fiberG = 1.5, sodiumMg = 90,
            )
        }

        abrirComORoom { db ->
            for ((id, outras) in listOf("sem-mapa" to 0, "mapa-vazio" to 0, "mapa-cheio" to 2)) {
                val micros = microsDeJson(db.foodDao().byId(id)?.microsJson)
                assertEquals(
                    outras + 2,
                    micros.size,
                    "o mapa de `$id` não ficou legível, ou perdeu chaves: $micros",
                )
                assertTrue(micros["fiber_g"] != null, "`$id` perdeu a fibra")
                assertTrue(micros["sodium_mg"] != null, "`$id` perdeu o sódio")
            }

            // As que já lá estavam continuam lá, e com o mesmo valor.
            val cheio = microsDeJson(db.foodDao().byId("mapa-cheio")?.microsJson)
            assertEquals(120.0, cheio["calcium_mg"])
            assertEquals(2.0, cheio["iron_mg"])
        }
    }

    @Test
    fun `o que ja estava no mapa ganha a coluna`() {

        // É esta a razão de haver duas casas para o mesmo número: a coluna do sódio era um
        // inteiro arredondado e o mapa guardava as casas decimais. Ao juntá-las, o número
        // com casas decimais é o que a fonte publicou.
        comABaseAberta { db ->
            inserir(
                db, "discordantes",
                microsJson = """{"sodium_mg":12.4}""",
                fiberG = null, sodiumMg = 12,
            )
        }

        abrirComORoom { db ->
            val micros = microsDeJson(db.foodDao().byId("discordantes")?.microsJson)
            assertEquals(12.4, micros["sodium_mg"], "a coluna arredondada ganhou ao valor medido")
        }
    }

    @Test
    fun `quem nao tinha nenhum dos dois fica como estava`() {
        comABaseAberta { db ->
            inserir(db, "sem-nada", microsJson = null, fiberG = null, sodiumMg = null)
            inserir(db, "so-outros", microsJson = """{"zinc_mg":1.0}""", fiberG = null, sodiumMg = null)
        }

        abrirComORoom { db ->
            assertNull(db.foodDao().byId("sem-nada")?.microsJson, "inventou um mapa a quem não tinha nada")
            assertEquals(
                mapOf("zinc_mg" to 1.0),
                microsDeJson(db.foodDao().byId("so-outros")?.microsJson),
            )
        }
    }

    private companion object {
        const val VERSAO_DE_PARTIDA = 27
        val ESQUEMAS = File("schemas/pt.antares.app.core.database.AntaresDb")
        const val MARCA = "\${TABLE_NAME}"
    }
}
