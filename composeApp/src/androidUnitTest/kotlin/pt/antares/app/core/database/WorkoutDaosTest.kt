package pt.antares.app.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.core.database.entities.ExerciseEntity
import pt.antares.app.core.database.entities.WorkoutSessionEntity
import pt.antares.app.core.database.entities.WorkoutSetEntity
import pt.antares.app.core.model.SessionStatus
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class WorkoutDaosTest {

    private lateinit var db: AntaresDb

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AntaresDb::class.java)
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()
    }

    @After
    fun tearDown() = db.close()

    private fun session(id: String, started: Long, status: SessionStatus) = WorkoutSessionEntity(
        id = id, startedAt = started, endedAt = null, routineId = null, note = null,
        status = status, updatedAt = started,
    )

    private fun set(id: String, sessionId: String, ex: String, idx: Int, w: Double, reps: Int, warmup: Boolean = false) =
        WorkoutSetEntity(
            id = id, sessionId = sessionId, exerciseId = ex, setIndex = idx,
            weightKg = w, reps = reps, rpe = null, isWarmup = warmup, updatedAt = 1L,
        )

    private fun exercise(id: String, primary: String) = ExerciseEntity(
        id = id, nameEn = id, namePt = id, searchText = id, category = "strength",
        force = null, mechanic = null, equipment = "barbell", level = "beginner",
        primaryMuscles = primary, secondaryMuscles = "", instructionsEnJson = "[]",
        instructionsPtJson = "[]", imagesJson = "[]", updatedAt = 1L,
    )

    @Test
    fun `ghost devolve a ultima sessao DONE do exercicio`() = runTest {
        val sess = db.workoutSessionDao()
        val sets = db.workoutSetDao()

        sess.upsertSession(session("A", started = 100, SessionStatus.DONE))
        sess.upsertSession(session("B", started = 200, SessionStatus.DONE))
        sess.upsertSession(session("C", started = 300, SessionStatus.ACTIVE))

        sets.upsertSet(set("a1", "A", "bench", 0, 80.0, 8))
        sets.upsertSet(set("b1", "B", "bench", 0, 85.0, 8))
        sets.upsertSet(set("b2", "B", "bench", 1, 85.0, 6))

        val ghosts = sets.ghostSets("bench", currentSessionId = "C")
        assertEquals(listOf(85.0, 85.0), ghosts.map { it.weightKg })
        assertEquals(listOf(8, 6), ghosts.map { it.reps })
    }

    @Test
    fun `retoma a sessao ACTIVE unica`() = runTest {
        val sess = db.workoutSessionDao()
        sess.upsertSession(session("A", 100, SessionStatus.DONE))
        sess.upsertSession(session("B", 200, SessionStatus.ACTIVE))

        val active = sess.activeSession()
        assertNotNull(active)
        assertEquals("B", active.id)
        assertEquals("B", sess.observeActive().first()?.id)
    }

    @Test
    fun `doneSets exclui a sessao atual e progresso agrega por sessao`() = runTest {
        val sess = db.workoutSessionDao()
        val sets = db.workoutSetDao()
        sess.upsertSession(session("A", 100, SessionStatus.DONE))
        sess.upsertSession(session("C", 300, SessionStatus.ACTIVE))
        sets.upsertSet(set("a1", "A", "bench", 0, 80.0, 10))
        sets.upsertSet(set("c1", "C", "bench", 0, 90.0, 5))

        val done = sets.doneSetsForExercise("bench", excludeSessionId = "C")
        assertEquals(listOf("a1"), done.map { it.id })

        val progress = sets.exerciseProgress("bench")
        assertEquals(1, progress.size)
        assertEquals(800.0, progress.first().volume)
        assertEquals(80.0, progress.first().topWeight)
    }

    @Test
    fun `volume por musculo junta com o exercicio e respeita a janela`() = runTest {
        db.exerciseLibraryDao().upsert(exercise("bench", "|chest|triceps|"))
        val sess = db.workoutSessionDao()
        val sets = db.workoutSetDao()
        sess.upsertSession(session("A", started = 1_000, SessionStatus.DONE))
        sess.upsertSession(session("OLD", started = 10, SessionStatus.DONE))
        sets.upsertSet(set("a1", "A", "bench", 0, 80.0, 10, warmup = false))
        sets.upsertSet(set("a2", "A", "bench", 1, 60.0, 10, warmup = true))
        sets.upsertSet(set("o1", "OLD", "bench", 0, 100.0, 5))

        val rows = db.workoutSetDao().observeMuscleVolumeSince(since = 500).first()
        assertEquals(1, rows.size)
        assertEquals(80.0, rows.first().weightKg)
        assertEquals("|chest|triceps|", rows.first().primaryMuscles)
    }
}
