package pt.antares.app.core.catalogo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.core.crash.CrashStore
import pt.antares.app.core.database.AntaresDb
import pt.antares.app.core.database.DbInfo
import pt.antares.app.feature.fooddata.FoodSeeder
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * O outro lado da actualização: **o catálogo que desceu tem de ser o que a app semeia**.
 *
 * Uma descarga que fica no disco sem nunca entrar na base é a peça inteira a não servir para
 * nada, e não dá erro nenhum — a app continua a mostrar o catálogo do APK como se nada
 * tivesse acontecido.
 *
 * O segundo teste é o do ciclo. Um ficheiro que não abre não pode fazer a app tentar outra
 * vez em cada abertura, para sempre: esquece-se que existe, e o que já estava semeado fica
 * como estava.
 */
@RunWith(RobolectricTestRunner::class)
class CatalogoDoDiscoTest {

    private val pasta: File = Files.createTempDirectory("catalogo-semeador").toFile()

    private val db: AntaresDb = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        AntaresDb::class.java,
    ).setQueryCoroutineContext(Dispatchers.Default).build()

    private val armazem = ArmazemDoCatalogo(pasta, Dispatchers.Default)

    @AfterTest
    fun limpar() {
        db.close()
        pasta.deleteRecursively()
    }

    @Test
    fun `o que desceu e o que fica semeado, e o anterior deixa de fazer falta`() = runTest {
        marcar(semeada = FoodSeeder.VERSAO_DO_CATALOGO, descarregada = VERSAO_NOVA)
        descarregado(catalogo(VERSAO_NOVA))
        val anterior = File(pasta, "catalogo.json.anterior").apply { writeText("o velho") }

        semeador().seedIfNeeded()

        assertNotNull(db.foodDao().byId("descarregado"), "semeou o do APK e não o que desceu")
        assertEquals(VERSAO_NOVA.toString(), db.dbInfoDao().get("catalogo_versao")?.value)
        assertFalse(
            anterior.exists(),
            "o anterior ficou no disco depois de o novo ter aberto e entrado",
        )
    }

    @Test
    fun `um ficheiro que nao abre nao prende a app num ciclo`() = runTest {
        marcar(semeada = FoodSeeder.VERSAO_DO_CATALOGO, descarregada = VERSAO_NOVA)
        descarregado("nem sequer e JSON")

        semeador().seedIfNeeded()

        // A marca volta a zero: sem isto, cada abertura ia ao disco, falhava, e a app ficava
        // presa a uma versão que nunca chegava a entrar.
        assertEquals("0", db.dbInfoDao().get(FoodSeeder.KEY_DESCARREGADO)?.value)

        // E o que já lá estava não se mexeu — que é a regra desta peça inteira.
        assertEquals(
            FoodSeeder.VERSAO_DO_CATALOGO.toString(),
            db.dbInfoDao().get("catalogo_versao")?.value,
        )
        assertNull(db.foodDao().byId("descarregado"))
    }

    private suspend fun marcar(semeada: Int, descarregada: Int) {
        db.dbInfoDao().upsert(DbInfo("catalogo_versao", semeada.toString()))
        db.dbInfoDao().upsert(DbInfo(FoodSeeder.KEY_DESCARREGADO, descarregada.toString()))
    }

    private suspend fun descarregado(corpo: String) {
        armazem.guardarProvisorio(corpo.encodeToByteArray())
        armazem.trocar()
    }

    // O `CrashStore` é de escrever e esquecer: o que este ficheiro mede é o estado da base e
    // do disco, e o rasto de um seed falhado já tem o teste dele.
    private fun semeador() = FoodSeeder(db, Dispatchers.Default, SemRasto(), armazem)

    private class SemRasto : CrashStore {
        override fun write(report: String) = Unit
        override fun read(): String? = null
        override fun clear() = Unit
    }

    private companion object {
        val VERSAO_NOVA = FoodSeeder.VERSAO_DO_CATALOGO + 1

        fun catalogo(versao: Int) = """
            {"versao":$versao,"alimentos":[{"id":"descarregado","source":"SEED",
            "nameEn":"a","namePt":"a","kcal":1,"proteinG":1.0,"carbsG":1.0,"fatG":1.0}]}
        """.trimIndent()
    }
}
