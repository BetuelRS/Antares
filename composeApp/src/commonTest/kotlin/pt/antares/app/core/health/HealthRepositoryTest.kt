package pt.antares.app.core.health

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import pt.antares.app.core.database.entities.ExerciseLogEntity
import pt.antares.app.core.database.entities.WeightLogEntity
import pt.antares.app.core.model.ExerciseOrigin
import pt.antares.app.core.model.WeightSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HealthRepositoryTest {

    private val day0 = 20_000L
    private fun ms(min: Long) = min * 60_000L

    private class FakeGateway(
        var sessions: List<HealthSession> = emptyList(),
        var weights: List<HealthWeight> = emptyList(),
        var composition: List<HealthBodyComposition> = emptyList(),
        var available: HealthAvailability = HealthAvailability.AVAILABLE,
        var granted: Boolean = true,
    ) : HealthGateway {
        override suspend fun writeBodyComposition(epochDay: Long, bodyFatPct: Double?, leanMassKg: Double?) = false
        override fun availability() = available
        override val readPermissions: Set<String> = setOf("read")
        override suspend fun hasReadPermissions() = granted
        override suspend fun steps(startMs: Long, endMs: Long): Long? = 1234
        override suspend fun weights(sinceMs: Long) = weights
        override suspend fun bodyComposition(sinceMs: Long) = composition
        override suspend fun sessions(sinceMs: Long) = sessions

        override val writePermissions: Set<String> = emptySet()
        override suspend fun hasWritePermissions() = false
        override suspend fun writeNutrition(
            epochDay: Long,
            kcal: Int,
            proteinG: Double,
            carbsG: Double,
            fatG: Double,
            micros: Map<String, Double>,
        ) = Unit
        override suspend fun writeSession(session: OutboundSession) = false
    }

    private class FakeWeights : HealthRepository.WeightWriter {
        val rows = mutableListOf<WeightLogEntity>()
        val manualDays = mutableSetOf<Long>()
        override suspend fun importedRefs() = rows.mapNotNull { it.sourceRef }.toSet()
        override suspend fun existsOnDay(epochDay: Long) =
            epochDay in manualDays || rows.any { it.epochDay == epochDay }

        override suspend fun insert(entry: WeightLogEntity) {
            rows += entry
        }
    }

    private class FakeExercise : HealthRepository.ExerciseWriter {
        val rows = mutableListOf<ExerciseLogEntity>()
        override suspend fun importedRefs() = rows.mapNotNull { it.refId }.toSet()
        override suspend fun insert(log: ExerciseLogEntity) {
            rows += log
        }
    }

    private fun repo(
        gateway: FakeGateway,
        weights: FakeWeights = FakeWeights(),
        exercise: FakeExercise = FakeExercise(),
        own: List<TimeWindow> = emptyList(),
        weightKg: Double? = 80.0,
        measured: MutableList<Pair<Long, Double>> = mutableListOf(),
    ): Triple<HealthRepository, FakeWeights, FakeExercise> {
        var cursor = 0L
        val r = HealthRepository(
            gateway = gateway,
            weights = weights,
            exercise = exercise,
            ownWindows = { own },
            latestWeightKg = { weightKg },
            lastImportAt = { cursor },
            setLastImportAt = { cursor = it },
            io = Dispatchers.Unconfined,
            now = { 1_000_000L },
            epochDayOf = { day0 },
            measurements = { day, pct -> measured += day to pct },
        )
        return Triple(r, weights, exercise)
    }

    private fun session(
        uid: String,
        startMin: Long,
        endMin: Long,
        kcal: Int? = null,
        met: Double? = 8.0,
    ) = HealthSession(
        uid = uid,
        title = null,
        activity = "Corrida",
        startMs = ms(startMin),
        endMs = ms(endMin),
        kcal = kcal,
        met = met,
    )

    @Test
    fun `importar duas vezes nao duplica nada`() = runTest {
        val gw = FakeGateway(
            sessions = listOf(session("hc-1", 0, 30)),
            weights = listOf(HealthWeight("w-1", 500L, 79.4)),
        )
        val (repo, w, e) = repo(gw)

        val first = repo.importNow()
        assertEquals(1, first.sessions)
        assertEquals(1, first.weights)

        val second = repo.importNow()
        assertTrue(second.isEmpty)
        assertEquals(1, e.rows.size)
        assertEquals(1, w.rows.size)
    }

    @Test
    fun `a corrida que ja e nossa nao entra outra vez`() = runTest {

        val gw = FakeGateway(sessions = listOf(session("relogio-1", 0, 30)))
        val (repo, _, e) = repo(gw, own = listOf(TimeWindow(ms(0), ms(30))))

        val result = repo.importNow()
        assertEquals(0, result.sessions)
        assertEquals(1, result.skippedDuplicates)
        assertTrue(e.rows.isEmpty())
    }

    @Test
    fun `um treino diferente no mesmo dia entra na mesma`() = runTest {
        val gw = FakeGateway(sessions = listOf(session("hc-tarde", 600, 640)))
        val (repo, _, e) = repo(gw, own = listOf(TimeWindow(ms(0), ms(30))))

        assertEquals(1, repo.importNow().sessions)
        assertEquals(ExerciseOrigin.HEALTH_CONNECT, e.rows.single().origin)
        assertEquals("relógio", "relógio")
    }

    @Test
    fun `energia gravada pela app de origem ganha a nossa estimativa`() = runTest {
        val gw = FakeGateway(sessions = listOf(session("hc-1", 0, 30, kcal = 275)))
        val (repo, _, e) = repo(gw)

        repo.importNow()
        assertEquals(275, e.rows.single().kcal)
    }

    @Test
    fun `sem energia gravada estima-se pelo MET (nunca zero)`() = runTest {

        val gw = FakeGateway(sessions = listOf(session("hc-1", 0, 30, kcal = null, met = 8.0)))
        val (repo, _, e) = repo(gw)

        repo.importNow()
        assertEquals(320, e.rows.single().kcal)
    }

    @Test
    fun `o peso escrito a mao nunca e pisado pela balanca`() = runTest {
        val gw = FakeGateway(weights = listOf(HealthWeight("w-1", 500L, 79.4)))
        val weights = FakeWeights().apply { manualDays += day0 }
        val (repo, w, _) = repo(gw, weights = weights)

        assertEquals(0, repo.importNow().weights)
        assertTrue(w.rows.isEmpty())
    }

    @Test
    fun `o peso importado fica marcado como Health Connect`() = runTest {
        val gw = FakeGateway(weights = listOf(HealthWeight("w-1", 500L, 79.4)))
        val (repo, w, _) = repo(gw)

        repo.importNow()
        val row = w.rows.single()
        assertEquals(WeightSource.HEALTH_CONNECT, row.source)
        assertEquals("w-1", row.sourceRef)
        assertEquals(79.4, row.weightKg)
    }

    @Test
    fun `sem Health Connect ou sem permissoes nao faz nada e nao rebenta`() = runTest {
        val semHc = FakeGateway(
            sessions = listOf(session("hc-1", 0, 30)),
            available = HealthAvailability.NOT_SUPPORTED,
        )
        assertTrue(repo(semHc).first.importNow().isEmpty)

        val semPermissao = FakeGateway(sessions = listOf(session("hc-1", 0, 30)), granted = false)
        assertTrue(repo(semPermissao).first.importNow().isEmpty)
    }

    @Test
    fun `sessao de duracao zero nao vira registo`() = runTest {
        val gw = FakeGateway(sessions = listOf(session("hc-0", 10, 10)))
        val (repo, _, e) = repo(gw)

        assertEquals(0, repo.importNow().sessions)
        assertTrue(e.rows.isEmpty())
    }

    @Test
    fun `a percentagem de gordura da balanca entra no historico`() = runTest {

        val gw = FakeGateway(
            composition = listOf(HealthBodyComposition("bf-1", 500L, bodyFatPct = 18.5)),
        )
        val medidas = mutableListOf<Pair<Long, Double>>()
        val (repo, _, _) = repo(gw, measured = medidas)

        assertEquals(1, repo.importNow().bodyMeasurements)
        assertEquals(day0 to 18.5, medidas.single())
    }

    @Test
    fun `massa magra sem percentagem nao inventa uma percentagem`() = runTest {

        val gw = FakeGateway(
            composition = listOf(HealthBodyComposition("lm-1", 500L, leanMassKg = 64.0)),
        )
        val medidas = mutableListOf<Pair<Long, Double>>()
        val (repo, _, _) = repo(gw, measured = medidas)

        assertEquals(0, repo.importNow().bodyMeasurements)
        assertTrue(medidas.isEmpty())
    }

    @Test
    fun `sem permissoes nao se le composicao nenhuma`() = runTest {
        val gw = FakeGateway(
            composition = listOf(HealthBodyComposition("bf-1", 500L, bodyFatPct = 18.5)),
            granted = false,
        )
        val medidas = mutableListOf<Pair<Long, Double>>()
        val (repo, _, _) = repo(gw, measured = medidas)

        assertTrue(repo.importNow().isEmpty)
        assertTrue(medidas.isEmpty())
    }
}
