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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Fundir dois alimentos não pode tirar nada a ninguém.
 *
 * O diário está seguro sozinho: guarda cópia da nutrição no momento do registo, e um dia
 * passado continua a dizer o que dizia. **O que não está são as coisas que guardam só o
 * identificador** — o favorito, a última porção, o ingrediente de uma receita. Sem a lápide,
 * fundir dois alimentos tirava um favorito, ou tirava comida a uma receita, sem aviso nenhum
 * e sem forma de recuperar.
 *
 * O segundo modo de falhar é mais discreto: a pessoa ter marca nos **dois** alimentos. Aí a
 * mudança de identificador colide com uma linha que já existe, e uma escrita que rebente a
 * meio deixa metade das marcas mudadas e a outra metade não.
 */
@RunWith(RobolectricTestRunner::class)
class LapidesTest {

    private val pasta: File = Files.createTempDirectory("lapides").toFile()

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
    fun `um favorito segue o alimento que foi fundido noutro`() = runTest {
        semear(VERSAO_NOVA, comLapide = false)
        db.foodMarkDao().marcarFavorito("perdedor", favorito = true, agora = 1_000)

        semear(VERSAO_NOVA + 1, comLapide = true)

        assertNull(db.foodDao().byId("perdedor"), "o perdedor ficou no catálogo")
        assertNull(db.foodMarkDao().byFoodId("perdedor"), "a marca ficou a apontar ao que saiu")

        val seguiu = assertNotNull(
            db.foodMarkDao().byFoodId("vencedor"),
            "o favorito desapareceu — e ninguém o pôs lá outra vez",
        )
        assertTrue(seguiu.isFavorite)
    }

    @Test
    fun `ter marca nos dois nao rebenta a fusao`() = runTest {
        semear(VERSAO_NOVA, comLapide = false)
        db.foodMarkDao().marcarFavorito("perdedor", favorito = true, agora = 1_000)
        db.foodMarkDao().marcarUso("vencedor", agora = 2_000, gramas = 150.0)

        semear(VERSAO_NOVA + 1, comLapide = true)

        // Fica uma marca só, e é a do que continua a existir. A do perdedor vai-se embora
        // com ele — o que não pode é a escrita rebentar e deixar metade feita.
        assertEquals(1, db.foodMarkDao().count())
        assertNotNull(db.foodMarkDao().byFoodId("vencedor"))
    }

    @Test
    fun `sem lapides nao se mexe em marca nenhuma`() = runTest {
        semear(VERSAO_NOVA, comLapide = false)
        db.foodMarkDao().marcarFavorito("perdedor", favorito = true, agora = 1_000)

        semear(VERSAO_NOVA + 1, comLapide = false)

        assertNotNull(db.foodMarkDao().byFoodId("perdedor"), "uma marca mudou sem ninguém pedir")
        assertNotNull(db.foodDao().byId("perdedor"))
    }

    /**
     * Escreve um catálogo no armazém e semeia-o. Dois alimentos, e na segunda versão o
     * primeiro deixa de vir e traz lápide.
     */
    private suspend fun semear(versao: Int, comLapide: Boolean) {
        val alimentos = buildList {
            if (!comLapide) add(alimento("perdedor"))
            add(alimento("vencedor"))
        }
        val lapides = if (comLapide) ""","lapides":[{"id":"perdedor","sucessor":"vencedor"}]""" else ""
        val corpo = """{"versao":$versao,"alimentos":[${alimentos.joinToString(",")}]$lapides}"""

        armazem.guardarProvisorio(corpo.encodeToByteArray())
        armazem.trocar()
        db.dbInfoDao().upsert(DbInfo(FoodSeeder.KEY_DESCARREGADO, versao.toString()))
        FoodSeeder(db, Dispatchers.Default, SemRasto(), armazem).seedIfNeeded()
    }

    private fun alimento(id: String) =
        """{"id":"$id","source":"SEED","nameEn":"$id","namePt":"$id","kcal":1,""" +
            """"proteinG":1.0,"carbsG":1.0,"fatG":1.0}"""

    private class SemRasto : CrashStore {
        override fun write(report: String) = Unit
        override fun read(): String? = null
        override fun clear() = Unit
    }

    private companion object {
        val VERSAO_NOVA = FoodSeeder.VERSAO_DO_CATALOGO + 1
    }
}
