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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A migração que tira o que é da pessoa de dentro da linha do alimento, corrida sobre dados.
 *
 * É a verificação que interessa neste bloco, e a razão é o modo de falhar: **uma migração
 * que perde linhas não rebenta.** A app abre, a pesquisa funciona, e a pessoa é que, dias
 * depois, abre os favoritos e estão vazios — sem erro, sem aviso, e sem nada que ligue o que
 * ela perdeu à actualização que o fez.
 *
 * Por isso o teste conta os dois lados: quantas linhas tinham marca antes, quantas marcas
 * existem depois, e se cada uma trouxe o que era dela. Um teste que só verificasse que a
 * migração corre sem exceção passaria com a tabela vazia.
 *
 * A base de partida é construída a partir do esquema exportado da v26, e não com SQL escrito
 * aqui: o Room valida **todas** as tabelas ao abrir, e uma base com duas seria recusada
 * antes de a migração chegar a correr.
 */
@RunWith(RobolectricTestRunner::class)
class MigracaoDeMarcasTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val nome = "migracao-de-marcas-test.db"

    @Before fun antes() { context.deleteDatabase(nome) }

    @After fun depois() { context.deleteDatabase(nome) }

    private fun baseNaV26(): File {
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

        // O Room recusa abrir uma base cuja impressão digital não conheça, e é ela que lhe
        // diz de que versão está a partir.
        raiz["setupQueries"]?.jsonArray?.forEach { db.execSQL(it.jsonPrimitive.content) }

        db.version = VERSAO_DE_PARTIDA
        db.close()
        return ficheiro
    }

    private fun inserirAlimento(
        db: SQLiteDatabase,
        id: String,
        favorito: Int = 0,
        usadoEm: Long = 0L,
        porcao: Double? = null,
    ) {
        db.execSQL(
            "INSERT INTO foods (id, source, sourceRef, namePt, nameEn, brand, kcal, proteinG, " +
                "carbsG, sugarsG, fatG, satFatG, fiberG, sodiumMg, microsJson, servingName, " +
                "servingGrams, isLiquid, isFavorite, lastUsedAt, lastAmountG, verified, " +
                "updatedAt, deleted) VALUES " +
                "(?, 'SEED', NULL, ?, ?, NULL, 100, 1.0, 1.0, NULL, 1.0, NULL, NULL, NULL, " +
                "NULL, NULL, NULL, 0, ?, ?, ?, 1, 1, 0)",
            arrayOf(id, id, id, favorito, usadoEm, porcao),
        )
    }

    private fun comABaseAberta(bloco: (SQLiteDatabase) -> Unit) {
        val ficheiro = baseNaV26()
        SQLiteDatabase.openDatabase(ficheiro.path, null, SQLiteDatabase.OPEN_READWRITE).use(bloco)
    }

    @Test
    fun `as marcas mudam de casa sem se perder nenhuma`() = runTest {
        comABaseAberta { db ->
            inserirAlimento(db, "ciqual-favorito", favorito = 1)
            inserirAlimento(db, "ciqual-usado", usadoEm = 1_700_000_000_000L)
            inserirAlimento(db, "ciqual-porcao", porcao = 80.0)
            inserirAlimento(db, "ciqual-tudo", favorito = 1, usadoEm = 42L, porcao = 55.5)
            inserirAlimento(db, "ciqual-intocado")
            inserirAlimento(db, "ciqual-outro-intocado")
        }

        abrirComORoom { db ->
            assertEquals(
                MARCADOS,
                db.foodMarkDao().count(),
                "perderam-se marcas na mudança de casa",
            )

            val tudo = db.foodMarkDao().byFoodId("ciqual-tudo")
            assertNotNull(tudo, "a marca com as três coisas não chegou ao outro lado")
            assertTrue(tudo.isFavorite)
            assertEquals(42L, tudo.lastUsedAt)
            assertEquals(55.5, tudo.lastAmountG)

            // Sem marca, e não com uma marca vazia: é isso que faz a tabela ter dezenas de
            // linhas num catálogo de oito mil alimentos.
            assertNull(
                db.foodMarkDao().byFoodId("ciqual-intocado"),
                "criou marca para quem não tinha nada",
            )
        }
    }

    @Test
    fun `os alimentos sobrevivem todos a reconstrucao da tabela`() = runTest {
        comABaseAberta { db ->
            repeat(QUANTOS_ALIMENTOS) { i -> inserirAlimento(db, "ciqual-$i", favorito = i % 2) }
        }

        // A tabela é reconstruída de raiz para lhe tirar três colunas. Perder linhas aqui
        // seria perder o catálogo e os alimentos criados pela pessoa ao mesmo tempo.
        abrirComORoom { db -> assertEquals(QUANTOS_ALIMENTOS, db.foodDao().count()) }
    }

    @Test
    fun `a migracao deixa escrito quantas marcas passaram`() = runTest {
        comABaseAberta { db ->
            inserirAlimento(db, "ciqual-1", favorito = 1)
            inserirAlimento(db, "ciqual-2", usadoEm = 9L)
            inserirAlimento(db, "ciqual-3")
        }

        // Não é usado por nada. Serve para alguém, daqui a um ano, saber se a base que tem
        // na mão passou por aqui e com o quê — que é uma pergunta que já foi feita antes.
        abrirComORoom { db ->
            assertEquals("2", db.dbInfoDao().get("marcas_migradas_v27")?.value)
        }
    }

    private suspend fun abrirComORoom(bloco: suspend (AntaresDb) -> Unit) {
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

    private companion object {
        const val VERSAO_DE_PARTIDA = 26
        const val QUANTOS_ALIMENTOS = 40

        /** Quantos dos seis alimentos do primeiro teste têm alguma coisa da pessoa. */
        const val MARCADOS = 4

        val ESQUEMAS = File("schemas/pt.antares.app.core.database.AntaresDb")

        /** O que o Room deixa no `createSql` exportado, à espera do nome da tabela. */
        const val MARCA = "\${TABLE_NAME}"
    }
}
