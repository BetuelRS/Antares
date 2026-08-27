package pt.antares.app.core.util

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.core.database.AntaresDb
import pt.antares.app.core.database.entities.FoodLogEntity
import pt.antares.app.core.model.MealSlot
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A varredura que decide quanto tempo a fotografia de um prato vive.
 *
 * O que se protege aqui é o inverso do que parece: **não é apagar, é não apagar a mais**.
 * Uma imagem apagada não volta, o apagar de um registo é desfazível, e a app não tem as
 * fotos dos pratos na cópia de segurança — logo uma varredura demasiado zelosa perde
 * dados de vez.
 */
@RunWith(RobolectricTestRunner::class)
class FotosDeRefeicaoTest {

    private lateinit var db: AntaresDb
    private lateinit var fotos: LocalPhotoStore
    private lateinit var varredura: FotosDeRefeicao

    private val hoje = 20_000L

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AntaresDb::class.java)
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()
        fotos = LocalPhotoStore(context, Dispatchers.Default, "meal_photos_test")
        varredura = FotosDeRefeicao(db.foodLogDao(), fotos) { hoje }
    }

    @After
    fun tearDown() = runTest {
        fotos.deleteAll()
        db.close()
    }

    private suspend fun foto(id: String): String =
        requireNonNull(fotos.writeBytes(id, byteArrayOf(1, 2, 3)))

    private fun <T : Any> requireNonNull(v: T?): T = requireNotNull(v) { "não gravou a foto" }

    private suspend fun registo(
        id: String,
        epochDay: Long,
        caminho: String?,
        apagado: Boolean = false,
    ) = db.foodLogDao().upsert(
        FoodLogEntity(
            id = id,
            epochDay = epochDay,
            mealSlot = MealSlot.LUNCH,
            foodId = null,
            nameSnapshot = id,
            quantityGrams = 100.0,
            kcalSnapshot = 100,
            proteinSnapshot = 1.0,
            carbsSnapshot = 1.0,
            fatSnapshot = 1.0,
            microsPer100Json = null,
            photoPath = caminho,
            updatedAt = 1L,
            deleted = apagado,
        ),
    )

    @Test
    fun `uma foto de hoje fica`() = runTest {
        val caminho = foto("a")
        registo("r1", hoje, caminho)

        assertEquals(0, varredura.varrer())
        assertTrue(fotos.exists(caminho))
        assertEquals(caminho, db.foodLogDao().byId("r1")?.photoPath)
    }

    /**
     * Passados os dois meses, a coluna volta a nulo e o ficheiro sai.
     *
     * Nulo aqui não é um valor em falta: é o estado final de um registo cuja foto já
     * cumpriu o que tinha a cumprir. Os números do registo ficam intactos.
     */
    @Test
    fun `uma foto velha some, e o registo fica inteiro`() = runTest {
        val caminho = foto("b")
        registo("r2", hoje - FotosDeRefeicao.DIAS_DE_VIDA - 1, caminho)

        assertEquals(1, varredura.varrer())
        assertTrue(!fotos.exists(caminho))

        val linha = db.foodLogDao().byId("r2")
        assertNull(linha?.photoPath)
        assertEquals(100, linha?.kcalSnapshot, "os números do registo não são a foto")
    }

    /**
     * Um registo apagado ainda segura a imagem, e é a razão de a consulta da varredura ser
     * a única do ficheiro que **não** filtra `deleted = 0`.
     *
     * Apagar um registo no diário é desfazível — há um «anular» na barra. Se a varredura
     * levasse a foto atrás, desfazer devolvia a linha sem retrato, e ninguém percebia
     * porquê.
     */
    @Test
    fun `um registo apagado ainda segura a foto`() = runTest {
        val caminho = foto("c")
        registo("r3", hoje, caminho, apagado = true)

        assertEquals(0, varredura.varrer())
        assertTrue(fotos.exists(caminho))
    }

    /**
     * Uma análise dá vários registos e **uma** imagem. Enquanto sobrar um que a refira, a
     * imagem fica: apagar um item do prato não apaga o retrato do prato.
     */
    @Test
    fun `a foto partilhada so sai quando o ultimo registo a larga`() = runTest {
        val caminho = foto("d")
        registo("r4", hoje, caminho)
        registo("r5", hoje - FotosDeRefeicao.DIAS_DE_VIDA - 1, caminho)

        assertEquals(0, varredura.varrer(), "o registo de hoje ainda a refere")
        assertTrue(fotos.exists(caminho))
    }

    /** Um ficheiro que a base não refere é lixo — de um registo reposto, ou de uma queda. */
    @Test
    fun `um ficheiro orfao e apagado`() = runTest {
        val caminho = foto("e")

        assertEquals(1, varredura.varrer())
        assertTrue(!fotos.exists(caminho))
    }

    /** As fotos dos pratos vivem noutra pasta que não a das fotos de progresso. */
    @Test
    fun `a pasta dos pratos nao e a das fotos de progresso`() {
        assertTrue(LocalPhotoStore.DIR_REFEICOES != LocalPhotoStore.DIR_NAME)
    }

    /**
     * A varredura não mexe no `updatedAt`.
     *
     * O diário ordena as refeições por ele. Uma varredura que o tocasse baralhava a ordem
     * do dia num arranque qualquer, sem ninguém ter tocado em nada.
     */
    @Test
    fun `varrer nao reordena o dia`() = runTest {
        val caminho = foto("f")
        registo("r6", hoje - FotosDeRefeicao.DIAS_DE_VIDA - 1, caminho)

        varredura.varrer()

        assertEquals(1L, db.foodLogDao().byId("r6")?.updatedAt)
    }

    /** A pasta de teste é mesmo separada — senão os testes apagavam fotos a sério. */
    @Test
    fun `o armazem de teste tem pasta propria`() = runTest {
        val caminho = foto("g")
        assertTrue(File(caminho).parentFile?.name == "meal_photos_test", caminho)
    }
}
