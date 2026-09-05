package pt.antares.app.feature.workout.ui

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.core.database.entities.ExerciseEntity
import pt.antares.app.core.database.entities.RoutineEntity
import pt.antares.app.core.database.entities.WorkoutSessionEntity
import pt.antares.app.core.database.entities.WorkoutSetEntity
import pt.antares.app.core.model.SessionStatus
import pt.antares.app.feature.workout.data.WorkoutSessionRepository
import pt.antares.app.testing.ViewModelHarness
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * O resumo que aparece quando um treino termina.
 *
 * Mostrava três números e uma lista, e não comparava com nada. O que estes testes defendem é
 * **com o quê** ele compara: treinos da mesma rotina, sem o que acabou de terminar lá dentro,
 * e as duas ausências — treino livre e primeira vez — a serem coisas diferentes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ResumoDoTreinoTest : ViewModelHarness() {

    private val minuto = 60_000L

    private fun viewModel() = vivo(WorkoutSummaryViewModel(
        sessionRepository = WorkoutSessionRepository(
            db.workoutSessionDao(),
            db.workoutSetDao(),
            db.exerciseLogDao(),
            db.weightLogDao(),
            db.routineDao(),
            db.sessionExerciseNoteDao(),
            db.exerciseLoadDao(),
            dispatcher,
        ),
        exerciseDao = db.exerciseLibraryDao(),
    ),
    )

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
            RoutineEntity(id = id, name = nome, note = null, position = 0, updatedAt = 1L, deleted = apagada),
        )

    /** Um treino terminado, com N séries iguais — o volume sai da multiplicação. */
    private suspend fun treino(
        id: String,
        rotinaId: String?,
        inicio: Long,
        minutos: Int,
        series: Int,
        peso: Double = 100.0,
        reps: Int = 10,
    ) {
        db.workoutSessionDao().upsertSession(
            WorkoutSessionEntity(
                id = id, startedAt = inicio, endedAt = inicio + minutos * minuto,
                routineId = rotinaId, note = null, status = SessionStatus.DONE, updatedAt = inicio,
            ),
        )
        repeat(series) { i ->
            db.workoutSetDao().upsertSet(
                WorkoutSetEntity(
                    id = "$id-$i", sessionId = id, exerciseId = "supino", setIndex = i,
                    weightKg = peso, reps = reps, rpe = null, isWarmup = false, updatedAt = 1L,
                ),
            )
        }
    }

    private suspend fun resumoDe(sessionId: String): WorkoutSummaryState {
        val vm = viewModel()
        vm.load(sessionId)
        return vm.state.first { !it.loading }
    }

    @Test
    fun `a comparacao e com a ultima vez da mesma rotina`() = runTest(dispatcher) {
        exercicio("supino")
        rotina("r1", "Empurrar A")
        treino("antigo", "r1", inicio = 1_000L, minutos = 56, series = 18, peso = 90.0)
        treino("hoje", "r1", inicio = 100_000L, minutos = 52, series = 18, peso = 100.0)

        val estado = resumoDe("hoje")
        val ultima = assertNotNull(estado.comparacao.ultimaVez)

        assertEquals("Empurrar A", estado.nomeDaRotina)
        assertEquals(-4, ultima.duracaoMin)
        // 18 × 100 × 10 = 18 000 contra 18 × 90 × 10 = 16 200.
        assertEquals(1_800.0, ultima.volume)
        assertEquals(0, ultima.series)
    }

    /**
     * Um treino não se compara consigo próprio. A consulta exclui-o pelo `id` — e sem isso a
     * comparação dava sempre zeros, que é o modo de falhar mais silencioso que ela tem.
     */
    @Test
    fun `o treino que acabou nao entra na sua propria comparacao`() = runTest(dispatcher) {
        exercicio("supino")
        rotina("r1", "Empurrar A")
        treino("hoje", "r1", inicio = 100_000L, minutos = 52, series = 18)

        val estado = resumoDe("hoje")

        assertNull(estado.comparacao.ultimaVez)
        assertNull(estado.comparacao.media)
        // E não é um treino livre: a rotina existe, o que falta é o passado dela.
        assertEquals("Empurrar A", estado.nomeDaRotina)
        assertTrue(!estado.treinoLivre)
    }

    @Test
    fun `um treino de outra rotina nao serve de comparacao`() = runTest(dispatcher) {
        exercicio("supino")
        rotina("r1", "Empurrar A")
        rotina("r2", "Puxar B")
        treino("outro", "r2", inicio = 1_000L, minutos = 90, series = 30)
        treino("hoje", "r1", inicio = 100_000L, minutos = 52, series = 18)

        assertNull(resumoDe("hoje").comparacao.ultimaVez)
    }

    /** Com dois anteriores há última vez e não há média: a média é das três. */
    @Test
    fun `a media aparece ao terceiro treino anterior`() = runTest(dispatcher) {
        exercicio("supino")
        rotina("r1", "Empurrar A")
        treino("a1", "r1", inicio = 1_000L, minutos = 50, series = 18)
        treino("a2", "r1", inicio = 2_000L, minutos = 50, series = 18)
        treino("hoje", "r1", inicio = 100_000L, minutos = 52, series = 18)

        val comDois = resumoDe("hoje")
        assertNotNull(comDois.comparacao.ultimaVez)
        assertNull(comDois.comparacao.media)

        treino("a3", "r1", inicio = 3_000L, minutos = 50, series = 18)
        val comTres = resumoDe("hoje")
        assertEquals(2, assertNotNull(comTres.comparacao.media).duracaoMin)
    }

    /**
     * Um treino livre não tem rotina, e por isso não tem com que se comparar. A bandeira é o
     * que faz o ecrã dizer **porquê** em vez de ficar calado.
     */
    @Test
    fun `um treino livre nao compara e diz-se`() = runTest(dispatcher) {
        exercicio("supino")
        treino("livre1", null, inicio = 1_000L, minutos = 40, series = 10)
        treino("hoje", null, inicio = 100_000L, minutos = 52, series = 18)

        val estado = resumoDe("hoje")

        assertTrue(estado.treinoLivre)
        assertNull(estado.nomeDaRotina)
        assertNull(estado.comparacao.ultimaVez)
    }

    /**
     * Uma rotina apagada continua a dar nome ao treino feito com ela, e continua a servir de
     * comparação: o resumo fala do passado, e o passado não se reescreve.
     */
    @Test
    fun `uma rotina apagada continua a nomear e a comparar`() = runTest(dispatcher) {
        exercicio("supino")
        rotina("r1", "Empurrar A", apagada = true)
        treino("antigo", "r1", inicio = 1_000L, minutos = 56, series = 18)
        treino("hoje", "r1", inicio = 100_000L, minutos = 52, series = 18)

        val estado = resumoDe("hoje")

        assertEquals("Empurrar A", estado.nomeDaRotina)
        assertEquals(-4, assertNotNull(estado.comparacao.ultimaVez).duracaoMin)
    }

    /** O aquecimento não conta para as séries nem para o volume, aqui como em toda a app. */
    @Test
    fun `o aquecimento fica de fora dos dois lados da comparacao`() = runTest(dispatcher) {
        exercicio("supino")
        rotina("r1", "Empurrar A")
        treino("antigo", "r1", inicio = 1_000L, minutos = 50, series = 3)
        db.workoutSetDao().upsertSet(
            WorkoutSetEntity(
                id = "antigo-aq", sessionId = "antigo", exerciseId = "supino", setIndex = 9,
                weightKg = 20.0, reps = 15, rpe = null, isWarmup = true, updatedAt = 1L,
            ),
        )
        treino("hoje", "r1", inicio = 100_000L, minutos = 50, series = 3)

        val ultima = assertNotNull(resumoDe("hoje").comparacao.ultimaVez)

        assertEquals(3, ultima.referencia.series)
        assertEquals(0, ultima.series)
        assertEquals(0.0, ultima.volume)
    }
}
