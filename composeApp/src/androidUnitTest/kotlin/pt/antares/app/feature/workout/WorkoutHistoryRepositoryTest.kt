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
import pt.antares.app.core.util.epochMillisToLocalDate
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
        db.workoutSetDao().upsertSet(
            WorkoutSetEntity("a1", "A", "bench", 0, 100.0, reps = 5, rpe = null, updatedAt = 1L),
        )
        db.workoutSetDao().upsertSet(
            WorkoutSetEntity(
                "a2", "A", "bench", 1, 60.0, reps = 10, rpe = null, isWarmup = true, updatedAt = 1L,
            ),
        )
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

    /**
     * O recorde traz o dia em que aconteceu, e não o de hoje: sem ele, um recorde de 2024
     * aparecia igual a um de ontem — é o defeito concreto 4 da `estudo/areas/10`.
     */
    @Test
    fun `o recorde diz o dia em que aconteceu`() = runTest {
        seed()
        val records = repo.records()
        assertEquals(
            epochMillisToLocalDate(1_000).toEpochDays().toLong(),
            records.first().epochDay,
        )
    }

    @Test
    fun `as series por musculo contam so as de trabalho`() = runTest {
        seed()
        val stats = estatisticas()
        assertEquals(1, stats.musculos.size)
        assertEquals("chest", stats.musculos.first().musculo)
        // Duas séries gravadas, uma delas aquecimento: conta uma.
        assertEquals(1, stats.musculos.first().series)
        assertEquals(500.0, stats.musculos.first().volume)
    }

    /**
     * A faixa é semanal, e um período mais curto do que uma semana não lhe chega. Multiplicar
     * um dia por sete inventava seis dias que não aconteceram.
     */
    @Test
    fun `um periodo de um dia nao da media semanal`() = runTest {
        seed()
        assertEquals(null, estatisticas(dias = 1).musculos.first().porSemana)
    }

    @Test
    fun `um mes divide as series pelas semanas que ele tem`() = runTest {
        seed()
        // Uma série de trabalho em 30 dias arredonda a zero por semana, e é o que ela é.
        assertEquals(0, estatisticas(dias = 30).musculos.first().porSemana)
    }

    /**
     * A contagem de treinos é a do **período** e não a das semanas ISO que o cobrem.
     *
     * Vistas no aparelho, as duas discordavam: com «Dia» escolhido, o cartão dizia «1 no
     * período escolhido» por cima de «Sem séries no período escolhido» — o treino era de há
     * três dias e a semana ISO apanhava-o.
     */
    @Test
    fun `a contagem de treinos respeita o periodo e nao a semana ISO`() = runTest {
        seed()
        val depoisDoTreino = 1_000L + 3 * 24 * 60 * 60 * 1000
        val stats = repo.observeEstatisticas(
            desdeMs = depoisDoTreino,
            diasDoPeriodo = 1,
            hojeEpochDay = epochMillisToLocalDate(depoisDoTreino).toEpochDays().toLong(),
            semanas = 1,
        ).first()

        assertEquals(0, stats.treinosNoPeriodo, "contou um treino que está fora do período")
        assertTrue(stats.musculos.isEmpty(), "contou séries que estão fora do período")
    }

    private suspend fun estatisticas(dias: Int = 7) = repo.observeEstatisticas(
        desdeMs = 0,
        diasDoPeriodo = dias,
        hojeEpochDay = epochMillisToLocalDate(1_000).toEpochDays().toLong(),
        semanas = 1,
    ).first()
}
