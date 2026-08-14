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
import pt.antares.app.core.database.entities.WeightLogEntity
import pt.antares.app.core.demo.DemoDataWriter
import pt.antares.app.core.demo.DemoResult
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class DemoTombstoneTest {

    private lateinit var db: AntaresDb
    private lateinit var writer: DemoDataWriter

    private val hoje = 20679L

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

    private suspend fun pesagemApagadaEm(dia: Long) {
        db.weightLogDao().upsert(
            WeightLogEntity(
                id = "do-onboarding",
                epochDay = dia,
                weightKg = 84.5,
                note = null,
                updatedAt = 1L,
            ),
        )
        db.weightLogDao().softDelete("do-onboarding", now = 2L)
    }

    @Test
    fun `a lapide de uma pesagem apagada nao pode impedir a demonstracao`() = runTest {
        pesagemApagadaEm(hoje)

        val r = writer.ligar(hoje)

        assertTrue(
            r is DemoResult.Ligado,
            "a demonstração não ligou por causa de uma pesagem apagada: $r",
        )
        assertTrue((r as DemoResult.Ligado).linhas > 0, "ligou sem escrever nada")
    }

    @Test
    fun `sem lapide nenhuma continua a ligar`() = runTest {
        val r = writer.ligar(hoje)
        assertTrue(r is DemoResult.Ligado, "a demonstração deixou de ligar no caso simples: $r")
    }

    @Test
    fun `uma lapide a meio da janela tambem nao impede`() = runTest {

        pesagemApagadaEm(hoje - 300)

        val r = writer.ligar(hoje)
        assertTrue(r is DemoResult.Ligado, "colidiu com uma lápide antiga: $r")
    }

    @Test
    fun `desligar continua a nao levar o que nao e de demonstracao`() = runTest {

        db.weightLogDao().upsert(
            WeightLogEntity(
                id = "de-verdade",
                epochDay = hoje - 900,
                weightKg = 90.0,
                note = null,
                updatedAt = 1L,
            ),
        )
        writer.desligar()

        assertTrue(
            db.weightLogDao().byDay(hoje - 900) != null,
            "desligar a demonstração levou uma pesagem de verdade",
        )
    }
}
