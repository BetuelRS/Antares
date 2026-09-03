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
import pt.antares.app.core.database.entities.WorkoutSessionEntity
import pt.antares.app.core.database.entities.WorkoutSetEntity
import pt.antares.app.core.model.SessionStatus
import pt.antares.app.feature.workout.data.WorkoutHistoryRepository
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class WorkoutBreakdownKeyTest {

    private lateinit var db: AntaresDb
    private lateinit var repo: WorkoutHistoryRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AntaresDb::class.java)
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()
        repo = WorkoutHistoryRepository(
            db.workoutSessionDao(),
            db.workoutSetDao(),
            db.exerciseLibraryDao(),
            db.routineDao(),
            Dispatchers.Default,
        )
    }

    @After
    fun tearDown() = db.close()

    private fun exercicio(id: String, nome: String) = ExerciseEntity(
        id = id,
        nameEn = nome,
        namePt = nome,
        searchText = nome.lowercase(),
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

    private fun set(id: String, sessao: String, exId: String, index: Int) = WorkoutSetEntity(
        id = id,
        sessionId = sessao,
        exerciseId = exId,
        setIndex = index,
        weightKg = 60.0,
        reps = 10,
        rpe = null,
        updatedAt = 1L,
    )

    @Test
    fun `dois exercicios com o mesmo nome dao chaves diferentes`() = runTest {

        db.exerciseLibraryDao().upsertAll(
            listOf(exercicio("seed-supino", "Supino"), exercicio("meu-supino", "Supino")),
        )
        db.workoutSessionDao().upsertSession(
            WorkoutSessionEntity(
                id = "s1",
                startedAt = 1_000L,
                endedAt = 2_000L,
                routineId = null,
                note = null,
                status = SessionStatus.DONE,
                updatedAt = 1L,
            ),
        )
        db.workoutSetDao().upsertSet(set("set-1", "s1", "seed-supino", 0))
        db.workoutSetDao().upsertSet(set("set-2", "s1", "meu-supino", 0))

        val b = assertNotNull(repo.breakdown("s1"), "não encontrei a sessão")
        assertEquals(2, b.exercises.size, "os dois exercícios deviam aparecer separados")

        assertEquals(1, b.exercises.map { it.name }.toSet().size, "o teste deixou de testar o caso")
        assertEquals(
            b.exercises.size,
            b.exercises.map { it.id }.toSet().size,
            "chaves duplicadas: o LazyColumn atira IllegalArgumentException",
        )
    }

    @Test
    fun `nenhuma lista usa um nome como chave`() {
        val dir = File("src/commonMain/kotlin/pt/antares/app/feature")

        val semComentarios = Regex("""/\*[\s\S]*?\*/|//[^\n]*""")
        val ofensores = dir.walkTopDown()
            .filter { it.extension == "kt" }
            .flatMap { f ->
                Regex("""key\s*=\s*\{[^}]*\bit\.name\b[^}]*\}""")
                    .findAll(semComentarios.replace(f.readText(), ""))
                    .map { "${f.name}: ${it.value.trim()}" }
            }
            .toList()

        assertTrue(
            ofensores.isEmpty(),
            "chave de lista por nome — dois registos com o mesmo nome rebentam o ecrã:\n" +
                ofensores.joinToString("\n"),
        )
    }
}
