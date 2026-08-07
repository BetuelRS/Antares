package pt.antares.app.feature.workout

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
import pt.antares.app.core.database.AntaresDb
import pt.antares.app.core.database.entities.ExerciseEntity
import pt.antares.app.core.database.entities.WorkoutSessionEntity
import pt.antares.app.core.database.entities.WorkoutSetEntity
import pt.antares.app.core.model.SessionStatus
import pt.antares.app.feature.workout.data.WorkoutHistoryRepository
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class WorkoutHistoryRepositoryTest {

    private lateinit var db: AntaresDb
    private lateinit var repo: WorkoutHistoryRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AntaresDb::class.java)
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()
        repo = WorkoutHistoryRepository(db.workoutSessionDao(), db.workoutSetDao(), db.exerciseLibraryDao(), Dispatchers.Default)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun seed() {
        db.exerciseLibraryDao().upsert(
            ExerciseEntity(
                id = "bench", nameEn = "Bench Press", namePt = "Supino", searchText = "supino",
                category = "strength", force = null, mechanic = null, equipment = "barbell", level = "beginner",
                primaryMuscles = "|chest|", secondaryMuscles = "", instructionsEnJson = "[]",
                instructionsPtJson = "[]", imagesJson = "[]", updatedAt = 1L,
            ),
        )
        db.workoutSessionDao().upsertSession(
            WorkoutSessionEntity("A", 1_000, 1_000 + 1_800_000, null, null, SessionStatus.DONE, 1L),
        )
        db.workoutSetDao().upsertSet(WorkoutSetEntity("a1", "A", "bench", 0, 100.0, 5, null, false, 1L))
        db.workoutSetDao().upsertSet(WorkoutSetEntity("a2", "A", "bench", 1, 60.0, 10, null, true, 1L))
    }

    @Test
    fun `historico traz volume so dos sets de trabalho`() = runTest {
        seed()
        val history = repo.observeHistory().first()
        assertEquals(1, history.size)
        assertEquals(500.0, history.first().volume)
    }

    @Test
    fun `records calcula melhor 1RM por exercicio`() = runTest {
        seed()
        val records = repo.records()
        assertEquals(1, records.size)
        assertEquals("Supino", records.first().name)

        assertTrue(records.first().oneRm in 116.0..117.0)
    }

    @Test
    fun `volume por musculo agrega na janela`() = runTest {
        seed()
        val stats = repo.observeMuscleVolume(since = 0).first()
        assertEquals(1, stats.size)
        assertEquals("chest", stats.first().muscle)
        assertEquals(500.0, stats.first().volume)
    }
}
