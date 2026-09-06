package pt.antares.app.feature.workout

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pt.antares.app.core.calc.SerieDaUltimaVez
import pt.antares.app.core.database.AntaresDb
import pt.antares.app.core.database.entities.ExerciseEntity
import pt.antares.app.core.database.entities.RoutineEntity
import pt.antares.app.core.database.entities.RoutineItemEntity
import pt.antares.app.core.database.entities.WorkoutSessionEntity
import pt.antares.app.core.database.entities.WorkoutSetEntity
import pt.antares.app.core.model.RegraDeProgressao
import pt.antares.app.core.model.SessionStatus
import pt.antares.app.testing.Fabricas

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ProgressaoDaRotinaTest {

    private lateinit var db: AntaresDb
    private lateinit var repo: pt.antares.app.feature.workout.data.RoutineRepository

    @BeforeTest
    fun antes() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AntaresDb::class.java,
        ).allowMainThreadQueries().build()
        repo = Fabricas.routineRepository(db, Dispatchers.Unconfined)
    }

    @AfterTest
    fun depois() = db.close()

    @Test
    fun `a regra nasce nenhuma e o incremento nulo`() = runTest {
        rotina("r1")
        val r = db.routineDao().routineById("r1")!!
        assertEquals(RegraDeProgressao.NENHUMA, r.progressao)
        assertNull(r.incrementoKg)
    }

    @Test
    fun `gravar a regra guarda-a pelo nome, e o incremento em quilos`() = runTest {
        rotina("r1")
        repo.setProgressao("r1", RegraDeProgressao.DUPLA, 2.0)

        val r = db.routineDao().routineById("r1")!!
        assertEquals(RegraDeProgressao.DUPLA, r.progressao)
        assertEquals(2.0, r.incrementoKg)
    }

    @Test
    fun `o incremento volta a nulo, que e o degrau da unidade`() = runTest {
        rotina("r1")
        repo.setProgressao("r1", RegraDeProgressao.LINEAR, 5.0)
        repo.setProgressao("r1", RegraDeProgressao.LINEAR, null)

        assertNull(db.routineDao().routineById("r1")!!.incrementoKg)
    }

    @Test
    fun `a ultima vez de cada exercicio e a da sessao dele, e nao a mais recente de todas`() = runTest {
        rotina("r1")
        exercicio("supino")
        exercicio("agachamento")
        item("i1", "r1", "supino", 0)
        item("i2", "r1", "agachamento", 1)

        // O agachamento foi feito **depois**, noutra sessão. A subconsulta é por exercício,
        // e por isso o supino tem de continuar a devolver as séries da sessão dele.
        sessao("s1", 1_000L)
        serie("x1", "s1", "supino", 60.0, 10)
        sessao("s2", 2_000L)
        serie("x2", "s2", "agachamento", 100.0, 5)

        val ultimas = repo.ultimasSeriesDaRotina("r1")
        assertEquals(listOf(SerieDaUltimaVez(60.0, 10)), ultimas["supino"])
        assertEquals(listOf(SerieDaUltimaVez(100.0, 5)), ultimas["agachamento"])
    }

    @Test
    fun `o aquecimento nao entra na ultima vez`() = runTest {
        rotina("r1")
        exercicio("supino")
        item("i1", "r1", "supino", 0)
        sessao("s1", 1_000L)
        serie("aq", "s1", "supino", 20.0, 15, aquecimento = true)
        serie("x1", "s1", "supino", 60.0, 10)

        assertEquals(listOf(SerieDaUltimaVez(60.0, 10)), repo.ultimasSeriesDaRotina("r1")["supino"])
    }

    @Test
    fun `um treino abandonado nao conta como a ultima vez`() = runTest {
        rotina("r1")
        exercicio("supino")
        item("i1", "r1", "supino", 0)
        sessao("s1", 1_000L)
        serie("x1", "s1", "supino", 60.0, 10)
        sessao("s2", 2_000L, status = SessionStatus.DISCARDED)
        serie("x2", "s2", "supino", 20.0, 3)

        // Um treino que se abandonou a meio não é o que se fez da última vez, e a regra não
        // pode partir de um peso que ninguém quis levantar.
        assertEquals(listOf(SerieDaUltimaVez(60.0, 10)), repo.ultimasSeriesDaRotina("r1")["supino"])
    }

    @Test
    fun `sem exercicios nao ha consulta nenhuma a fazer`() = runTest {
        rotina("r1")
        assertTrue(repo.ultimasSeriesDaRotina("r1").isEmpty())
    }

    private suspend fun rotina(id: String) =
        db.routineDao().upsertRoutine(
            RoutineEntity(id = id, name = id, note = null, position = 0, updatedAt = 1L),
        )

    private suspend fun exercicio(id: String) =
        db.exerciseLibraryDao().upsertAll(
            listOf(
                ExerciseEntity(
                    id = id, nameEn = id, namePt = id, searchText = id, category = "strength",
                    force = null, mechanic = null, equipment = null, level = "beginner",
                    primaryMuscles = "[]", secondaryMuscles = "[]",
                    instructionsEnJson = "[]", instructionsPtJson = "[]", imagesJson = "[]",
                    updatedAt = 1L,
                ),
            ),
        )

    private suspend fun item(id: String, rotinaId: String, exercicioId: String, posicao: Int) =
        db.routineDao().upsertItem(
            RoutineItemEntity(
                id = id, routineId = rotinaId, exerciseId = exercicioId,
                targetSets = 3, targetRepsMin = 8, targetRepsMax = 12,
                targetWeightKg = null, restSec = 90, position = posicao,
                supersetGroup = null, updatedAt = 1L,
            ),
        )

    private suspend fun sessao(id: String, instante: Long, status: SessionStatus = SessionStatus.DONE) =
        db.workoutSessionDao().upsertSession(
            WorkoutSessionEntity(
                id = id, startedAt = instante, endedAt = instante, routineId = "r1",
                note = null, status = status, updatedAt = instante,
            ),
        )

    private suspend fun serie(
        id: String,
        sessionId: String,
        exercicioId: String,
        peso: Double,
        reps: Int,
        aquecimento: Boolean = false,
    ) = db.workoutSetDao().upsertSet(
        WorkoutSetEntity(
            id = id, sessionId = sessionId, exerciseId = exercicioId, setIndex = 0,
            weightKg = peso, reps = reps, rpe = null, isWarmup = aquecimento, updatedAt = 1L,
        ),
    )
}
