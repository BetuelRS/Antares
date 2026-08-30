package pt.antares.app.feature.exercise

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.core.database.AntaresDb
import pt.antares.app.core.database.entities.ExerciseLogEntity
import pt.antares.app.core.model.ExerciseOrigin
import pt.antares.app.core.util.todayEpochDay
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * O exercício avulso: a hora a que começou, e corrigi-lo depois de gravado.
 *
 * O que estes testes fixam é o que a app **não** faz: não inventa a hora de um dia que já
 * passou, não deixa corrigir uma linha que pertence a um treino ou a um relógio, e não
 * recalcula as calorias com o peso de hoje quando só se mexeu na hora.
 */
@RunWith(RobolectricTestRunner::class)
class ExercicioAvulsoTest {

    private val db: AntaresDb = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        AntaresDb::class.java,
    ).setQueryCoroutineContext(Dispatchers.Default).build()

    private val repo = ExerciseRepository(db.exerciseLogDao(), Dispatchers.Default)

    @AfterTest
    fun tearDown() = db.close()

    private suspend fun umRegisto(id: String, epochDay: Long, minutos: Int, kcal: Int) =
        db.exerciseLogDao().upsert(
            ExerciseLogEntity(
                id = id,
                epochDay = epochDay,
                origin = ExerciseOrigin.MANUAL,
                label = "Corrida moderada (6 min/km)",
                metId = "run_moderate",
                met = 11.0,
                durationMin = minutos,
                kcal = kcal,
                startedAtMin = null,
                refId = null,
                updatedAt = 1L,
            ),
        )

    @Test
    fun `registar hoje herda a hora do relogio`() = runTest {
        repo.logManual(todayEpochDay(), "Padel", "padel", 7.0, 45, 300)

        val gravado = db.exerciseLogDao().observeDay(todayEpochDay()).first().single()
        assertNotNull(gravado.startedAtMin, "um registo de hoje devia trazer a hora do relógio")
    }

    @Test
    fun `registar num dia passado fica sem hora`() = runTest {
        val ontem = todayEpochDay() - 1
        repo.logManual(ontem, "Padel", "padel", 7.0, 45, 300)

        val gravado = db.exerciseLogDao().observeDay(ontem).first().single()
        assertNull(
            gravado.startedAtMin,
            "o relógio de agora não é testemunha de ontem — a hora fica por saber",
        )
    }

    @Test
    fun `corrigir a duracao escala as calorias`() = runTest {
        umRegisto("1", 100, minutos = 30, kcal = 350)

        repo.updateManual("1", durationMin = 60, startedAtMin = null)

        val depois = db.exerciseLogDao().byId("1")!!
        assertEquals(60, depois.durationMin)
        assertEquals(700, depois.kcal, "o dobro do tempo é o dobro das calorias")
    }

    @Test
    fun `corrigir so a hora deixa as calorias quietas`() = runTest {
        umRegisto("1", 100, minutos = 30, kcal = 350)

        repo.updateManual("1", durationMin = 30, startedAtMin = 7 * 60 + 15)

        val depois = db.exerciseLogDao().byId("1")!!
        assertEquals(350, depois.kcal)
        assertEquals(7 * 60 + 15, depois.startedAtMin)
    }

    /**
     * A análise por texto aceita calorias sem duração (`durationMin ?: 0`), e escalar a
     * partir de zero não é uma conta — as calorias que a AI deu ficam.
     */
    @Test
    fun `duracao antiga a zero mantem as calorias`() = runTest {
        umRegisto("1", 100, minutos = 0, kcal = 420)

        repo.updateManual("1", durationMin = 40, startedAtMin = null)

        val depois = db.exerciseLogDao().byId("1")!!
        assertEquals(40, depois.durationMin)
        assertEquals(420, depois.kcal)
    }

    @Test
    fun `uma linha de treino nao se corrige aqui`() = runTest {
        db.exerciseLogDao().upsert(
            ExerciseLogEntity(
                id = "w1",
                epochDay = 100,
                origin = ExerciseOrigin.WORKOUT,
                label = "Pernas",
                metId = null,
                met = 5.0,
                durationMin = 50,
                kcal = 300,
                startedAtMin = 18 * 60,
                refId = "sessao-1",
                updatedAt = 1L,
            ),
        )

        repo.updateManual("w1", durationMin = 5, startedAtMin = null)

        val depois = db.exerciseLogDao().byId("w1")!!
        assertEquals(50, depois.durationMin, "a sessão é que manda nesta linha")
        assertEquals(18 * 60, depois.startedAtMin)
    }

    @Test
    fun `duracao fora do intervalo nao chega a base`() = runTest {
        umRegisto("1", 100, minutos = 30, kcal = 350)

        assertFailsWith<IllegalArgumentException> { repo.updateManual("1", 0, null) }
        assertFailsWith<IllegalArgumentException> {
            repo.updateManual("1", ExerciseRepository.MAX_DURATION_MIN + 1, null)
        }
        assertFailsWith<IllegalArgumentException> { repo.updateManual("1", 30, 24 * 60) }
    }

    /**
     * Quem faz padel três vezes por semana quer o padel **uma vez** no topo, e não três
     * vezes seguidas — e quer o mais recente primeiro.
     */
    @Test
    fun `os recentes nao repetem a atividade e vem do mais recente`() = runTest {
        val dao = db.exerciseLogDao()
        fun linha(id: String, metId: String?, quando: Long) = ExerciseLogEntity(
            id = id, epochDay = 100, origin = ExerciseOrigin.MANUAL, label = id,
            metId = metId, met = 7.0, durationMin = 30, kcal = 200,
            startedAtMin = null, refId = null, updatedAt = quando,
        )
        dao.upsert(linha("a", "padel", 10))
        dao.upsert(linha("b", "swim_slow", 20))
        dao.upsert(linha("c", "padel", 30))
        // Sem `metId`: veio de um treino, e não é uma atividade da tabela de METs.
        dao.upsert(linha("d", null, 40))

        assertEquals(listOf("padel", "swim_slow"), repo.recentMetIds(limite = 5))
    }
}
