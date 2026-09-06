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
import pt.antares.app.core.crash.NoCrashStore
import pt.antares.app.core.database.AntaresDb
import pt.antares.app.core.database.entities.ExerciseEntity
import pt.antares.app.core.database.entities.RoutineEntity
import pt.antares.app.core.database.entities.RoutineItemEntity
import pt.antares.app.core.database.entities.WorkoutSessionEntity
import pt.antares.app.core.database.entities.WorkoutSetEntity
import pt.antares.app.core.model.SessionStatus
import pt.antares.app.core.util.epochMillisAt
import pt.antares.app.feature.workout.data.ExerciseLibraryRepository
import pt.antares.app.feature.workout.data.ExerciseSeeder
import pt.antares.app.feature.workout.data.WorkoutHistoryRepository
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A biblioteca de exercícios: o que é da pessoa e o que ela já fez.
 *
 * A `estudo/areas/09-treino-biblioteca.md` diz que a biblioteca «trata os 873 por igual,
 * todas as vezes» — e que a app **tem** os dados para não o fazer. Estes testes são sobre os
 * dois lados disso: a marca, que é escolha, e o uso, que é histórico.
 */
@RunWith(RobolectricTestRunner::class)
class BibliotecaDeExerciciosTest {

    private lateinit var db: AntaresDb
    private lateinit var repo: ExerciseLibraryRepository
    private lateinit var historico: WorkoutHistoryRepository

    /** Uma segunda-feira, para as semanas ISO da janela caírem onde se espera. */
    private val hoje = 20_000L

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AntaresDb::class.java)
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()
        repo = ExerciseLibraryRepository(
            db.exerciseLibraryDao(),
            db.exerciseMarkDao(),
            db.workoutSetDao(),
            ExerciseSeeder(db, Dispatchers.Default, NoCrashStore),
            Dispatchers.Default,
        )
        historico = WorkoutHistoryRepository(
            db.workoutSessionDao(),
            db.workoutSetDao(),
            db.exerciseLibraryDao(),
            db.routineDao(),
            Dispatchers.Default,
        )
    }

    @After
    fun tearDown() = db.close()

    private suspend fun exercicio(id: String, meu: Boolean = false) =
        db.exerciseLibraryDao().upsert(
            ExerciseEntity(
                id = id, nameEn = id, namePt = id, searchText = id,
                category = "strength", force = null, mechanic = null,
                equipment = "barbell", level = "beginner",
                primaryMuscles = "|chest|", secondaryMuscles = "",
                instructionsEnJson = "[]", instructionsPtJson = "[]", imagesJson = "[]",
                isCustom = meu, updatedAt = 1L,
            ),
        )

    private suspend fun treino(id: String, diaEpoch: Long) =
        db.workoutSessionDao().upsertSession(
            WorkoutSessionEntity(
                id = id,
                startedAt = epochMillisAt(diaEpoch, minuteOfDay = 600),
                endedAt = epochMillisAt(diaEpoch, minuteOfDay = 660),
                routineId = null, note = null,
                status = SessionStatus.DONE, updatedAt = 1L,
            ),
        )

    private suspend fun serie(id: String, treinoId: String, exercicioId: String, peso: Double = 60.0) =
        db.workoutSetDao().upsertSet(
            WorkoutSetEntity(
                id = id, sessionId = treinoId, exerciseId = exercicioId, setIndex = 0,
                weightKg = peso, reps = 10, rpe = null, isWarmup = false, updatedAt = 1L,
            ),
        )

    @Test
    fun `marcar grava e desmarcar apaga a linha`() = runTest {
        exercicio("supino")

        repo.marcarFavorito("supino", true)
        assertEquals(setOf("supino"), repo.observeFavoritos().first())
        assertTrue(repo.eFavorito("supino"))

        repo.marcarFavorito("supino", false)
        assertEquals(emptySet(), repo.observeFavoritos().first())
        assertFalse(repo.eFavorito("supino"))

        // Desmarcar apaga mesmo. A linha é o facto, e por isso não há coluna nenhuma a
        // repeti-lo: gravar um `false` era a segunda maneira de dizer o que a ausência já diz.
        assertTrue(db.exerciseMarkDao().exportRows().isEmpty())
    }

    @Test
    fun `os mais feitos contam treinos e nao series`() = runTest {
        exercicio("supino")
        exercicio("agachamento")
        treino("t1", hoje - 1)
        serie("s1", "t1", "supino")
        serie("s2", "t1", "supino")
        serie("s3", "t1", "supino")
        treino("t2", hoje - 2)
        serie("s4", "t2", "agachamento")
        treino("t3", hoje - 3)
        serie("s5", "t3", "agachamento")

        val uso = repo.observeMaisFeitos(hoje).first()

        // Três séries de supino num treino são **uma** vez que se fez supino. O agachamento
        // levou uma série em cada um de dois treinos, e por isso vem primeiro.
        assertEquals("agachamento", uso.first().exerciseId)
        assertEquals(2, uso.first().vezes)
        assertEquals(1, uso.first { it.exerciseId == "supino" }.vezes)
    }

    @Test
    fun `um treino de ha mais de doze semanas fica fora da janela`() = runTest {
        exercicio("supino")
        // Treze semanas atrás: fora da janela que a `estudo/areas/10` usa para «as últimas
        // semanas», e é essa que esta lista segue para a app não ter duas.
        treino("velho", hoje - 13 * 7)
        serie("s1", "velho", "supino")

        assertTrue(repo.observeMaisFeitos(hoje).first().isEmpty())

        treino("recente", hoje - 3)
        serie("s2", "recente", "supino")
        assertEquals(1, repo.observeMaisFeitos(hoje).first().size)
    }

    @Test
    fun `um treino por terminar nao conta para os mais feitos`() = runTest {
        exercicio("supino")
        db.workoutSessionDao().upsertSession(
            WorkoutSessionEntity(
                id = "aberto",
                startedAt = epochMillisAt(hoje, minuteOfDay = 600),
                endedAt = null, routineId = null, note = null,
                status = SessionStatus.ACTIVE, updatedAt = 1L,
            ),
        )
        serie("s1", "aberto", "supino")

        // O treino que está a decorrer ainda não é uma vez que se fez o exercício: ele pode
        // ser descartado, e a lista passaria a contar um treino que nunca existiu.
        assertTrue(repo.observeMaisFeitos(hoje).first().isEmpty())
    }

    @Test
    fun `o desempenho sai das series feitas`() = runTest {
        exercicio("supino")
        treino("t1", hoje - 5)
        serie("s1", "t1", "supino", peso = 60.0)
        treino("t2", hoje - 2)
        serie("s2", "t2", "supino", peso = 80.0)

        val d = historico.desempenhoDoExercicio("supino")!!
        assertEquals(80.0, d.melhorPesoKg)
        assertEquals(2, d.vezes)
        assertEquals(epochMillisAt(hoje - 2, minuteOfDay = 600), d.ultimaEm)
    }

    @Test
    fun `apagar conta rotinas e nao linhas, e ignora as apagadas`() = runTest {
        exercicio("meu", meu = true)
        rotina("viva", apagada = false)
        rotina("morta", apagada = true)

        // A mesma rotina com o mesmo exercício duas vezes continua a ser **uma** rotina: quem
        // vai apagar quer saber a quantas mexe, e não quantas linhas há.
        item("i1", "viva", "meu", 0)
        item("i2", "viva", "meu", 1)
        item("i3", "morta", "meu", 0)

        assertEquals(1, historico.rotinasCom("meu"))
    }

    private suspend fun rotina(id: String, apagada: Boolean) =
        db.routineDao().upsertRoutine(
            RoutineEntity(id = id, name = id, note = null, position = 0, updatedAt = 1L, deleted = apagada),
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
}
