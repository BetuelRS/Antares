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
import pt.antares.app.core.database.entities.RoutineEntity
import pt.antares.app.core.database.entities.WorkoutSessionEntity
import pt.antares.app.core.database.entities.WorkoutSetEntity
import pt.antares.app.core.model.SessionStatus
import pt.antares.app.feature.workout.data.WorkoutHistoryRepository
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A linha do histórico e o cabeçalho do detalhe.
 *
 * A linha tinha dois dados — a data e o volume — e por isso dois treinos completamente
 * diferentes ficavam iguais. Estes testes são sobre os outros quatro, e sobre a estrela, que
 * é calculada e não guardada.
 */
@RunWith(RobolectricTestRunner::class)
class HistoricoDoTreinoTest {

    private lateinit var db: AntaresDb
    private lateinit var repo: WorkoutHistoryRepository

    private val minuto = 60_000L

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

    private suspend fun exercicio(id: String) = db.exerciseLibraryDao().upsert(
        ExerciseEntity(
            id = id, nameEn = id, namePt = id, searchText = id,
            category = "strength", force = null, mechanic = null,
            equipment = "barbell", level = "beginner",
            primaryMuscles = "|chest|", secondaryMuscles = "",
            instructionsEnJson = "[]", instructionsPtJson = "[]", imagesJson = "[]",
            updatedAt = 1L,
        ),
    )

    private suspend fun rotina(id: String, nome: String, apagada: Boolean = false) =
        db.routineDao().upsertRoutine(
            RoutineEntity(
                id = id, name = nome, note = null, position = 0,
                updatedAt = 1L, deleted = apagada,
            ),
        )

    private suspend fun treino(
        id: String,
        rotinaId: String?,
        inicio: Long,
        minutos: Int,
    ) = db.workoutSessionDao().upsertSession(
        WorkoutSessionEntity(
            id = id, startedAt = inicio, endedAt = inicio + minutos * minuto,
            routineId = rotinaId, note = null, status = SessionStatus.DONE, updatedAt = inicio,
        ),
    )

    private suspend fun serie(
        id: String,
        treinoId: String,
        exercicioId: String,
        indice: Int,
        peso: Double,
        reps: Int,
        aquecimento: Boolean = false,
        rpe: Double? = null,
    ) = db.workoutSetDao().upsertSet(
        WorkoutSetEntity(
            id = id, sessionId = treinoId, exerciseId = exercicioId, setIndex = indice,
            weightKg = peso, reps = reps, rpe = rpe, isWarmup = aquecimento, updatedAt = 1L,
        ),
    )

    @Test
    fun `a linha traz rotina duracao e series de trabalho`() = runTest {
        exercicio("supino")
        rotina("r1", "Empurrar A")
        treino("t1", "r1", inicio = 1_000L, minutos = 52)
        serie("s1", "t1", "supino", 0, 60.0, 10)
        serie("s2", "t1", "supino", 1, 60.0, 9)
        // O aquecimento não conta para as séries, como não conta para o volume.
        serie("s3", "t1", "supino", 2, 20.0, 12, aquecimento = true)

        val linha = repo.observeHistory().first().single()

        assertEquals("Empurrar A", linha.nomeDaRotina)
        assertEquals(52, linha.durationMin)
        assertEquals(2, linha.series)
        assertEquals("r1", linha.routineId)
    }

    @Test
    fun `um treino livre nao inventa nome de rotina`() = runTest {
        exercicio("supino")
        treino("t1", rotinaId = null, inicio = 1_000L, minutos = 30)
        serie("s1", "t1", "supino", 0, 60.0, 10)

        val linha = repo.observeHistory().first().single()

        assertNull(linha.nomeDaRotina)
        assertNull(linha.routineId)
    }

    @Test
    fun `uma rotina apagada continua a dar nome aos treinos ja feitos com ela`() = runTest {
        exercicio("supino")
        rotina("r1", "Empurrar A", apagada = true)
        treino("t1", "r1", inicio = 1_000L, minutos = 30)
        serie("s1", "t1", "supino", 0, 60.0, 10)

        // O histórico fala do passado: o treino foi mesmo feito com aquela rotina, e
        // chamar-lhe treino livre por ela já não existir seria reescrever o que aconteceu.
        assertEquals("Empurrar A", repo.observeHistory().first().single().nomeDaRotina)
        assertEquals("Empurrar A", repo.breakdown("t1")!!.nomeDaRotina)
    }

    @Test
    fun `a estrela vai ao treino que bateu o recorde e nao ao mais recente`() = runTest {
        exercicio("supino")
        treino("t1", null, inicio = 1_000L, minutos = 30)
        serie("s1", "t1", "supino", 0, 80.0, 5)
        treino("t2", null, inicio = 2_000_000L, minutos = 30)
        serie("s2", "t2", "supino", 0, 60.0, 5)

        val porId = repo.observeHistory().first().associateBy { it.id }

        assertTrue(porId.getValue("t1").temRecorde)
        assertFalse(porId.getValue("t2").temRecorde)
    }

    @Test
    fun `corrigir a serie corrige a estrela sem passar por lado nenhum`() = runTest {
        exercicio("supino")
        treino("t1", null, inicio = 1_000L, minutos = 30)
        serie("s1", "t1", "supino", 0, 80.0, 5)
        treino("t2", null, inicio = 2_000_000L, minutos = 30)
        serie("s2", "t2", "supino", 0, 60.0, 5)

        // É a razão de a estrela ser calculada e não guardada: um engano de dedo no primeiro
        // treino deixaria o segundo sem estrela para sempre se ela fosse uma coluna.
        serie("s1", "t1", "supino", 0, 40.0, 5)

        val porId = repo.observeHistory().first().associateBy { it.id }

        assertTrue(porId.getValue("t2").temRecorde)
    }

    @Test
    fun `o detalhe traz a rotina as series e o RPE gravado`() = runTest {
        exercicio("supino")
        rotina("r1", "Empurrar A")
        treino("t1", "r1", inicio = 1_000L, minutos = 45)
        serie("s1", "t1", "supino", 0, 60.0, 10, rpe = 8.0)
        serie("s2", "t1", "supino", 1, 20.0, 12, aquecimento = true)

        val d = repo.breakdown("t1")!!

        assertEquals("Empurrar A", d.nomeDaRotina)
        assertEquals(45, d.durationMin)
        // Uma série de trabalho: o aquecimento está na lista e não conta para a contagem.
        assertEquals(1, d.series)
        assertEquals(2, d.exercises.single().sets.size)
        // O RPE é gravado desde sempre e nunca era lido em lado nenhum da app.
        assertEquals(8.0, d.exercises.single().sets.first().rpe)
    }

    @Test
    fun `o filtro por rotina so oferece rotinas que chegaram a ser treinadas`() = runTest {
        exercicio("supino")
        rotina("r1", "Empurrar A")
        rotina("r2", "Nunca treinada")
        treino("t1", "r1", inicio = 1_000L, minutos = 30)
        serie("s1", "t1", "supino", 0, 60.0, 10)

        // Uma opção que devolve sempre lista vazia é pior do que um menu mais curto.
        assertEquals(listOf("Empurrar A"), repo.routineOptions().map { it.name })
    }
}
