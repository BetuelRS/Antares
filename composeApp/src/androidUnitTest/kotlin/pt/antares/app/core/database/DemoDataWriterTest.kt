package pt.antares.app.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.core.database.entities.ExerciseEntity
import pt.antares.app.core.database.entities.FastingProtocolEntity
import pt.antares.app.core.database.entities.FoodEntity
import pt.antares.app.core.database.entities.WeightLogEntity
import pt.antares.app.core.demo.DEMO_ID_PREFIX
import pt.antares.app.core.demo.DemoDataWriter
import pt.antares.app.core.demo.DemoResult
import pt.antares.app.core.model.FoodSource
import pt.antares.app.core.model.WeightSource
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class DemoDataWriterTest {

    private lateinit var db: AntaresDb
    private lateinit var writer: DemoDataWriter

    private val hoje = 20_668L

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AntaresDb::class.java)
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()
        writer = DemoDataWriter(db.demoDao(), Dispatchers.Default)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun semear() {
        (1..10).forEach { n ->
            db.foodDao().upsert(
                FoodEntity(
                    id = "seed-food-$n",
                    source = FoodSource.SEED,
                    sourceRef = null,
                    namePt = "Alimento $n",
                    nameEn = "Food $n",
                    brand = null,
                    kcal = 80 + n * 30,
                    proteinG = 5.0 + n,
                    carbsG = 10.0 + n,
                    sugarsG = null,
                    fatG = 2.0 + n * 0.5,
                    satFatG = null,
                    fiberG = null,
                    sodiumMg = null,
                    microsJson = null,
                    servingName = null,
                    servingGrams = null,
                    updatedAt = 1L,
                ),
            )
        }
        db.exerciseLibraryDao().upsertAll(
            (1..8).map { n ->
                ExerciseEntity(
                    id = "seed-ex-$n",
                    nameEn = "Exercise $n",
                    namePt = "Exercício $n",
                    searchText = "exercicio $n",
                    category = "strength",
                    force = null,
                    mechanic = null,
                    equipment = null,
                    level = "beginner",
                    primaryMuscles = "[]",
                    secondaryMuscles = "[]",
                    instructionsEnJson = "[]",
                    instructionsPtJson = "[]",
                    imagesJson = "[]",
                    updatedAt = 1L,
                )
            },
        )
        db.fastingProtocolDao().upsertAll(
            listOf(FastingProtocolEntity(id = "seed-16-8", name = "16:8", fastingHours = 16, updatedAt = 1L)),
        )
    }

    private fun pesagemReal(dia: Long) = WeightLogEntity(
        id = "real-$dia",
        epochDay = dia,
        weightKg = 80.0,
        note = null,
        source = WeightSource.MANUAL,
        sourceRef = null,
        updatedAt = 1L,
    )

    @Test
    fun `ligar enche a base e desligar devolve-a ao que era`() = runTest {
        semear()
        val dao = db.demoDao()
        assertEquals(0, dao.demoCount())
        assertFalse(writer.estaLigado())

        val ligou = writer.ligar(hoje)
        assertIs<DemoResult.Ligado>(ligou, "esperava Ligado, veio $ligou")
        assertTrue(ligou.linhas > 5000, "dois anos de uma pessoa dão mais do que ${ligou.linhas} linhas")
        assertTrue(writer.estaLigado())
        assertEquals(ligou.linhas, dao.demoCount())

        val desligou = writer.desligar()
        assertIs<DemoResult.Desligado>(desligou, "esperava Desligado, veio $desligou")
        assertEquals(ligou.linhas, desligou.linhas)

        assertEquals(0, dao.demoCount())
        assertEquals(0, dao.realCount())
        assertFalse(writer.estaLigado())
    }

    @Test
    fun `desligar nao leva as pesagens de quem estava a usar a app`() = runTest {

        semear()
        writer.ligar(hoje)

        db.weightLogDao().upsert(pesagemReal(hoje + 5))

        writer.desligar()

        val sobreviveu = assertNotNull(
            db.weightLogDao().byDay(hoje + 5),
            "o desligar levou uma pesagem que não era dele",
        )
        assertEquals(80.0, sobreviveu.weightKg)
    }

    @Test
    fun `recusa quando ja ha dados reais, e nao escreve nada`() = runTest {
        semear()
        db.weightLogDao().upsert(pesagemReal(hoje - 3))

        val r = writer.ligar(hoje)
        assertIs<DemoResult.RecusadoPorDadosReais>(r, "esperava recusa, veio $r")
        assertEquals(1, r.linhas)

        assertEquals(0, db.demoDao().demoCount())
        assertFalse(writer.estaLigado())
    }

    @Test
    fun `nao recusa por causa de lapides de sync`() = runTest {

        semear()
        db.weightLogDao().upsert(pesagemReal(hoje - 3).copy(deleted = true))

        val r = writer.ligar(hoje)
        assertTrue(r is DemoResult.Ligado, "uma lápide bloqueou o modo demonstração: $r")
    }

    @Test
    fun `ligar duas vezes nao duplica`() = runTest {

        semear()
        val primeira = writer.ligar(hoje)
        val segunda = writer.ligar(hoje)

        assertIs<DemoResult.Ligado>(primeira)
        assertIs<DemoResult.Ligado>(segunda, "a segunda vez falhou: $segunda")
        assertEquals(primeira.linhas, segunda.linhas)
        assertEquals(segunda.linhas, db.demoDao().demoCount())
    }

    @Test
    fun `sem catalogo semeado continua a ligar`() = runTest {

        val r = writer.ligar(hoje)
        assertIs<DemoResult.Ligado>(r, "esperava Ligado, veio $r")
        assertTrue(r.linhas > 0)
        assertTrue(writer.estaLigado())
    }

    @Test
    fun `todos os ids gravados tem o prefixo demo`() = runTest {
        semear()
        writer.ligar(hoje)

        assertEquals(0, db.demoDao().realCount(), "escapou uma linha ao prefixo `$DEMO_ID_PREFIX`")
    }
}
