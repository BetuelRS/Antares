package pt.antares.app.feature.workout

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.testing.ViewModelHarness
import pt.antares.app.core.database.entities.ExerciseEntity
import pt.antares.app.core.database.entities.RoutineEntity
import pt.antares.app.core.database.entities.RoutineItemEntity
import pt.antares.app.core.database.entities.RoutineScheduleEntity
import pt.antares.app.core.database.entities.WorkoutSessionEntity
import pt.antares.app.core.database.entities.WorkoutSetEntity
import pt.antares.app.core.model.SessionStatus
import pt.antares.app.core.util.epochDayToLocalDate
import pt.antares.app.feature.workout.data.DestaqueDoTreino
import pt.antares.app.feature.workout.data.WorkoutHubRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber

/**
 * O centro de treino, que passou de menu a painel.
 *
 * O que estes testes defendem não é o desenho: é a **ordem de quem ganha o cartão principal**
 * e o facto de nenhum destes números ser novo. Um painel que mostre o treino errado é pior do
 * que os cinco botões cinzentos que ele substitui.
 */
@RunWith(RobolectricTestRunner::class)
class CentroDeTreinoTest : ViewModelHarness() {

    private val zona = TimeZone.UTC

    private fun hub() = WorkoutHubRepository(
        routineDao = db.routineDao(),
        sessionDao = db.workoutSessionDao(),
        setDao = db.workoutSetDao(),
        scheduleDao = db.routineScheduleDao(),
        exerciseDao = db.exerciseLibraryDao(),
    )

    private fun msDoDia(dia: Long): Long =
        epochDayToLocalDate(dia).atStartOfDayIn(zona).toEpochMilliseconds()

    private suspend fun rotina(id: String, nome: String, exercicios: Int = 0) {
        db.routineDao().upsertRoutine(
            RoutineEntity(id = id, name = nome, note = null, position = 0, updatedAt = 1L),
        )
        repeat(exercicios) { i ->
            val exId = "$id-ex$i"
            db.exerciseLibraryDao().upsertAll(
                listOf(
                    ExerciseEntity(
                        id = exId,
                        nameEn = "Exercise $i",
                        namePt = "Exercício $i",
                        searchText = "exercicio $i",
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
                    ),
                ),
            )
            db.routineDao().upsertItem(
                RoutineItemEntity(
                    id = "$id-item$i",
                    routineId = id,
                    exerciseId = exId,
                    targetSets = 3,
                    targetRepsMin = 8,
                    targetRepsMax = 12,
                    targetWeightKg = null,
                    restSec = 90,
                    position = i,
                    supersetGroup = null,
                    updatedAt = 1L,
                ),
            )
        }
    }

    private suspend fun treino(
        id: String,
        rotinaId: String?,
        dia: Long,
        duracaoMin: Int = 40,
        series: Int = 0,
        pesoKg: Double = 60.0,
    ) {
        val inicio = msDoDia(dia)
        db.workoutSessionDao().upsertSession(
            WorkoutSessionEntity(
                id = id,
                startedAt = inicio,
                endedAt = inicio + duracaoMin * 60_000L,
                routineId = rotinaId,
                note = null,
                status = SessionStatus.DONE,
                updatedAt = inicio,
            ),
        )
        repeat(series) { i ->
            db.workoutSetDao().upsertSet(
                WorkoutSetEntity(
                    id = "$id-s$i",
                    sessionId = id,
                    exerciseId = "qualquer",
                    setIndex = i,
                    weightKg = pesoKg,
                    reps = 10,
                    rpe = null,
                    isWarmup = false,
                    updatedAt = inicio,
                ),
            )
        }
    }

    @Test
    fun `o plano da semana ganha ao historico no cartao principal`() = runTest(dispatcher) {
        val hoje = 20_000L
        rotina("plano", "Do plano", exercicios = 3)
        rotina("outra", "A que fiz ontem", exercicios = 5)
        treino("t1", "outra", hoje - 1)

        db.routineScheduleDao().upsert(
            RoutineScheduleEntity(
                dayOfWeek = epochDayToLocalDate(hoje).dayOfWeek.isoDayNumber,
                routineId = "plano",
                updatedAt = 1L,
            ),
        )

        val estado = hub().observe(hoje, zona).first { it.carregado }
        val destaque = estado.destaque

        assertTrue(destaque is DestaqueDoTreino.DeHoje, "o plano da semana tem de ganhar")
        assertEquals("Do plano", destaque.rotina.nome)
        assertEquals(3, destaque.rotina.totalDeExercicios)
    }

    @Test
    fun `sem plano para hoje o cartao mostra a ultima rotina treinada`() = runTest(dispatcher) {
        val hoje = 20_000L
        rotina("a", "Antiga", exercicios = 2)
        rotina("b", "A mais recente", exercicios = 4)
        treino("velho", "a", hoje - 10)
        treino("recente", "b", hoje - 3)

        val estado = hub().observe(hoje, zona).first { it.carregado }
        val destaque = estado.destaque

        assertTrue(destaque is DestaqueDoTreino.Ultima)
        assertEquals("A mais recente", destaque.rotina.nome)
        assertEquals(hoje - 3, destaque.ultimaVezEpochDay)
    }

    /**
     * O cartão levava o número de dias passados e escrevia «há 1 dias» no dia seguinte a um
     * treino — apanhado a correr, e não por teste nenhum. Passa a levar o dia, que é o que o
     * formatador de datas da app sabe dizer sem plural.
     */
    @Test
    fun `o cartao da ultima rotina leva o dia e nao uma contagem de dias`() = runTest(dispatcher) {
        val hoje = 20_000L
        rotina("r", "Ontem", exercicios = 3)
        treino("t", "r", hoje - 1)

        val destaque = hub().observe(hoje, zona).first { it.carregado }.destaque

        assertTrue(destaque is DestaqueDoTreino.Ultima)
        assertEquals(hoje - 1, destaque.ultimaVezEpochDay)
    }

    /**
     * O terceiro estado, e é o do primeiro arranque: a app semeia sete rotinas e o plano da
     * semana nasce vazio. Escolher uma delas seria fingir que sabe qual é a desta pessoa.
     */
    @Test
    fun `sem plano e sem historico o cartao convida em vez de escolher uma rotina`() = runTest(dispatcher) {
        rotina("a", "Full Body A", exercicios = 5)
        rotina("b", "Full Body B", exercicios = 5)

        val estado = hub().observe(20_000L, zona).first { it.carregado }

        assertEquals(DestaqueDoTreino.Convite, estado.destaque)
    }

    /**
     * Um dia do plano pode apontar para uma rotina apagada: o calendário guarda o
     * identificador e não há chave estrangeira. Vale o mesmo que não haver plano.
     */
    @Test
    fun `um plano que aponta para uma rotina apagada nao ganha o cartao`() = runTest(dispatcher) {
        val hoje = 20_000L
        rotina("viva", "Viva", exercicios = 2)
        treino("t", "viva", hoje - 2)

        db.routineScheduleDao().upsert(
            RoutineScheduleEntity(
                dayOfWeek = epochDayToLocalDate(hoje).dayOfWeek.isoDayNumber,
                routineId = "fantasma",
                updatedAt = 1L,
            ),
        )

        val destaque = hub().observe(hoje, zona).first { it.carregado }.destaque

        assertTrue(destaque is DestaqueDoTreino.Ultima, "caiu numa rotina que já não existe")
        assertEquals("Viva", destaque.rotina.nome)
    }

    @Test
    fun `a semana conta so os dias desta semana e soma volume e series`() = runTest(dispatcher) {
        // Uma quinta-feira, para a semana ISO ter dias para trás e para a frente.
        val hoje = 20_000L
        val inicio = pt.antares.app.core.util.weekStartEpochDay(hoje)
        rotina("r", "R")
        treino("dentro1", "r", inicio, series = 4, pesoKg = 50.0)
        treino("dentro2", "r", inicio + 2, series = 6, pesoKg = 50.0)
        treino("fora", "r", inicio - 3, series = 10, pesoKg = 50.0)

        val semana = hub().observe(hoje, zona).first { it.carregado }.semana

        assertEquals(listOf(inicio, inicio + 2), semana.diasComTreino.sorted())
        assertEquals(10, semana.series, "só as séries dos treinos desta semana")
        assertEquals(10 * 50.0 * 10, semana.volume, "quatro mais seis séries de 50 kg × 10")
    }

    @Test
    fun `a lista de rotinas diz quantos exercicios tem e quando foi feita`() = runTest(dispatcher) {
        val hoje = 20_000L
        rotina("feita", "Feita", exercicios = 6)
        rotina("nunca", "Nunca feita", exercicios = 2)
        treino("t", "feita", hoje - 5)

        val rotinas = hub().observe(hoje, zona).first { it.carregado }.rotinas.associateBy { it.id }

        assertEquals(6, rotinas.getValue("feita").totalDeExercicios)
        assertEquals(hoje - 5, rotinas.getValue("feita").ultimaVezEpochDay)
        assertEquals(2, rotinas.getValue("nunca").totalDeExercicios)
        assertEquals(null, rotinas.getValue("nunca").ultimaVezEpochDay, "nunca foi treinada")
    }

    /**
     * A linha do histórico tinha dois dados — data e volume — e dois treinos diferentes
     * ficavam iguais. Passa a ter quatro, e todos já estavam gravados.
     */
    @Test
    fun `os ultimos treinos trazem rotina duracao e series`() = runTest(dispatcher) {
        val hoje = 20_000L
        rotina("r", "Empurrar A")
        treino("t", "r", hoje - 1, duracaoMin = 52, series = 18, pesoKg = 40.0)

        val treinos = hub().observe(hoje, zona).first { it.carregado }.ultimos

        assertEquals(1, treinos.size)
        assertEquals("Empurrar A", treinos[0].nomeDaRotina)
        assertEquals(52, treinos[0].duracaoMin)
        assertEquals(18, treinos[0].series)
        assertEquals(18 * 40.0 * 10, treinos[0].volume)
    }

    @Test
    fun `uma sessao a decorrer chega ao estado`() = runTest(dispatcher) {
        rotina("r", "R")
        db.workoutSessionDao().upsertSession(
            WorkoutSessionEntity(
                id = "activa",
                startedAt = 1_000L,
                endedAt = null,
                routineId = "r",
                note = null,
                status = SessionStatus.ACTIVE,
                updatedAt = 1_000L,
            ),
        )

        val estado = hub().observe(20_000L, zona).first { it.carregado }

        assertEquals(1_000L, estado.sessaoActivaDesde)
    }

    @Test
    fun `um treino livre nao inventa nome de rotina`() = runTest(dispatcher) {
        val hoje = 20_000L
        treino("livre", null, hoje - 1, series = 3)

        val treinos = hub().observe(hoje, zona).first { it.carregado }.ultimos

        assertEquals(null, treinos.single().nomeDaRotina)
    }
}
