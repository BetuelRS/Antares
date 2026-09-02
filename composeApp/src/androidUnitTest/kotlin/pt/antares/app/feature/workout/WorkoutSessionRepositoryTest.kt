package pt.antares.app.feature.workout

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
import pt.antares.app.core.model.SessionStatus
import pt.antares.app.core.util.epochMillisToLocalDate
import pt.antares.app.core.util.toEpochDay
import pt.antares.app.feature.workout.data.WorkoutSessionRepository
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class WorkoutSessionRepositoryTest {

    private lateinit var db: AntaresDb
    private lateinit var repo: WorkoutSessionRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AntaresDb::class.java)
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()
        repo = WorkoutSessionRepository(
            db.workoutSessionDao(), db.workoutSetDao(), db.exerciseLogDao(),
            db.weightLogDao(), db.routineDao(), db.sessionExerciseNoteDao(), db.exerciseLoadDao(),
            Dispatchers.Default,
        )
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `iniciar duas vezes retoma a mesma sessao ativa`() = runTest {
        val id1 = repo.startOrResume(routineId = "r1")
        val id2 = repo.startOrResume(routineId = "r1")
        assertEquals(id1, id2)
    }

    @Test
    fun `sets gravam e sobrevivem, terminar marca DONE`() = runTest {
        val id = repo.startOrResume(null)
        repo.putSet("s1", id, "bench", 0, 80.0, 8, null, false)
        repo.putSet("s2", id, "bench", 1, 80.0, 6, null, false)
        assertEquals(2, repo.setsForSession(id).size)

        repo.finish(id)
        assertEquals(SessionStatus.DONE, repo.sessionById(id)!!.status)

        assertNull(repo.activeSession())
    }

    @Test
    fun `descartar tira dos ghosts e nao deixa sessao ativa`() = runTest {
        val id = repo.startOrResume(null)
        repo.putSet("s1", id, "bench", 0, 100.0, 5, null, false)
        repo.discard(id)

        assertEquals(SessionStatus.DISCARDED, repo.sessionById(id)!!.status)
        assertNull(repo.activeSession())

        val id2 = repo.startOrResume(null)
        assertEquals(emptyList(), repo.ghostSets("bench", id2))
    }

    @Test
    fun `terminar treino real gera ExerciseLog WORKOUT no diario`() = runTest {

        val started = System.currentTimeMillis() - 120_000
        db.workoutSessionDao().upsertSession(
            pt.antares.app.core.database.entities.WorkoutSessionEntity(
                id = "S", startedAt = started, endedAt = null, routineId = null, note = null,
                status = pt.antares.app.core.model.SessionStatus.ACTIVE, updatedAt = started,
            ),
        )
        db.workoutSetDao().upsertSet(
            pt.antares.app.core.database.entities.WorkoutSetEntity(
                id = "s1", sessionId = "S", exerciseId = "bench", setIndex = 0,
                weightKg = 80.0, reps = 8, rpe = null, isWarmup = false, updatedAt = started,
            ),
        )

        repo.finish("S")

        val log = db.exerciseLogDao().byRef("S")
        assertNotNull(log)
        assertEquals(pt.antares.app.core.model.ExerciseOrigin.WORKOUT, log.origin)

        assertEquals(true, log.kcal > 0)
        val epochDay = pt.antares.app.core.util.epochMillisToLocalDate(started).toEpochDay()
        assertEquals(epochDay, log.epochDay)
    }

    @Test
    fun `editar um set pelo id estavel atualiza em vez de duplicar`() = runTest {
        val id = repo.startOrResume(null)
        repo.putSet("s1", id, "bench", 0, 80.0, 8, null, false)
        repo.putSet("s1", id, "bench", 0, 82.5, 8, null, false)
        val sets = repo.setsForSession(id)
        assertEquals(1, sets.size)
        assertEquals(82.5, sets.first().weightKg)
    }
}
