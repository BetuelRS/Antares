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
import pt.antares.app.core.util.epochMillisAt
import pt.antares.app.core.util.epochMillisToLocalDate
import pt.antares.app.core.util.weekStartEpochDay
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
     * O volume da linha usa a **mesma janela** das séries ao lado dele. Visto no aparelho:
     * «15 séries · 39 000 kg» eram a média da semana e o total do mês na mesma linha.
     */
    @Test
    fun `o volume da linha esta na mesma janela das series`() = runTest {
        seed()
        val mes = estatisticas(dias = 30).musculos.first()
        assertEquals(500.0, mes.volume, "o total do período mudou")
        // 30 dias são 4,28 semanas: 500 kg dão 116,6 por semana.
        assertTrue(
            mes.volumeDaJanela!! in 116.0..117.0,
            "o volume da janela é ${mes.volumeDaJanela} e devia ser o do período a dividir " +
                "pelas semanas que ele tem",
        )

        // Com um dia não há janela semanal, e o volume da linha volta a ser o do período.
        assertEquals(null, estatisticas(dias = 1).musculos.first().volumeDaJanela)
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

    /**
     * **Os dois gráficos cobrem a mesma primeira semana.**
     *
     * Eles desenham-se um por baixo do outro, e o de cima conta treinos por semana ISO
     * enquanto o de baixo somava o volume só do que caía dentro da janela do período. Quando
     * o período começa a meio de uma semana — que é quase sempre —, a primeira coluna de um
     * cobria sete dias e a do outro três.
     *
     * O treino deste teste fica **antes do início do período e dentro da primeira semana**,
     * que é onde as duas discordavam.
     */
    @Test
    fun `os dois graficos cobrem a mesma primeira semana`() = runTest {
        val hoje = epochMillisToLocalDate(1_000).toEpochDays().toLong()
        val inicioDaSemana = weekStartEpochDay(hoje)
        seedEm(epochMillisAt(inicioDaSemana, minuteOfDay = 0))

        val stats = repo.observeEstatisticas(
            // O período começa hoje; a semana ISO começou antes, e é lá que está o treino.
            desdeMs = epochMillisAt(hoje, minuteOfDay = 0),
            diasDoPeriodo = 1,
            hojeEpochDay = hoje,
            semanas = 1,
        ).first()

        assertEquals(listOf(1), stats.treinosPorSemana, "a frequência não viu o treino")
        assertTrue(
            stats.volumePorSemana.first() > 0.0,
            "o volume por semana não viu o treino que a frequência viu",
        )
    }

    private suspend fun seedEm(quando: Long) {
        db.exerciseLibraryDao().upsert(
            ExerciseEntity(
                id = "bench", nameEn = "Bench Press", namePt = "Supino", searchText = "supino",
                category = "strength", force = null, mechanic = null, equipment = "barbell",
                level = "beginner", primaryMuscles = "|chest|", secondaryMuscles = "",
                instructionsEnJson = "[]", instructionsPtJson = "[]", imagesJson = "[]",
                updatedAt = 1L,
            ),
        )
        db.workoutSessionDao().upsertSession(
            WorkoutSessionEntity("A", quando, quando, null, null, SessionStatus.DONE, quando),
        )
        db.workoutSetDao().upsertSet(
            WorkoutSetEntity("a1", "A", "bench", 0, 100.0, reps = 5, rpe = null, updatedAt = quando),
        )
    }

    private suspend fun estatisticas(dias: Int = 7) = repo.observeEstatisticas(
        desdeMs = 0,
        diasDoPeriodo = dias,
        hojeEpochDay = epochMillisToLocalDate(1_000).toEpochDays().toLong(),
        semanas = 1,
    ).first()
}
