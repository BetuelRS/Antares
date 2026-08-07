package pt.antares.app.core.health

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HealthPublisherTest {

    private fun ms(min: Long) = min * 60_000L

    private open class FakeGateway(
        var available: HealthAvailability = HealthAvailability.AVAILABLE,
        var canWrite: Boolean = true,
    ) : HealthGateway {
        val bodyWrites = mutableListOf<OutboundBodyComposition>()
        override suspend fun writeBodyComposition(
            epochDay: Long,
            bodyFatPct: Double?,
            leanMassKg: Double?,
        ): Boolean {
            bodyWrites += OutboundBodyComposition(epochDay, bodyFatPct, leanMassKg)
            return true
        }

        val nutritionWrites = mutableListOf<Long>()
        val sessionWrites = mutableListOf<OutboundSession>()

        override fun availability() = available
        override val readPermissions: Set<String> = emptySet()
        override suspend fun hasReadPermissions() = false
        override suspend fun steps(startMs: Long, endMs: Long): Long? = null
        override suspend fun weights(sinceMs: Long) = emptyList<HealthWeight>()
        override suspend fun bodyComposition(sinceMs: Long) = emptyList<HealthBodyComposition>()
        override suspend fun sessions(sinceMs: Long) = emptyList<HealthSession>()

        override val writePermissions: Set<String> = setOf("w")
        override suspend fun hasWritePermissions() = canWrite
        override suspend fun writeNutrition(
            epochDay: Long,
            kcal: Int,
            proteinG: Double,
            carbsG: Double,
            fatG: Double,
            micros: Map<String, Double>,
        ) {
            nutritionWrites += epochDay
        }

        open override suspend fun writeSession(session: OutboundSession): Boolean {
            sessionWrites += session
            return true
        }
    }

    private fun publisher(
        gateway: FakeGateway,
        nutrition: List<DayNutrition> = emptyList(),
        sessions: List<OutboundSession> = emptyList(),
        body: List<OutboundBodyComposition> = emptyList(),
    ): HealthPublisher {
        var cursor = 0L
        return HealthPublisher(
            gateway = gateway,
            nutrition = { nutrition },
            sessions = { sessions },
            bodyComposition = { body },
            lastPublishAt = { cursor },
            setLastPublishAt = { cursor = it },
            io = Dispatchers.Unconfined,
            now = { 5_000_000L },
        )
    }

    private fun session(id: String, kind: OutboundKind = OutboundKind.RUN) = OutboundSession(
        clientId = id,
        kind = kind,
        title = "Corrida matinal",
        startMs = ms(0),
        endMs = ms(30),
        kcal = 300,
    )

    @Test
    fun `publica nutricao e sessoes`() = runTest {
        val gw = FakeGateway()
        val result = publisher(
            gw,
            nutrition = listOf(DayNutrition(20_000, 2000, 120.0, 200.0, 60.0)),
            sessions = listOf(session("run-1"), session("wk-1", OutboundKind.WORKOUT)),
        ).publishNow(epochDayToday = 20_000)

        assertEquals(1, result.nutritionDays)
        assertEquals(2, result.sessions)
        assertEquals(listOf(20_000L), gw.nutritionWrites)
        assertEquals(2, gw.sessionWrites.size)
    }

    @Test
    fun `publica a composicao corporal, que ninguem chamava`() = runTest {

        val gw = FakeGateway()
        val result = publisher(
            gw,
            body = listOf(OutboundBodyComposition(20_000, bodyFatPct = 22.0, leanMassKg = 62.4)),
        ).publishNow(epochDayToday = 20_000)

        assertEquals(1, result.bodyMeasurements)
        assertEquals(22.0, gw.bodyWrites.single().bodyFatPct)
        assertEquals(62.4, gw.bodyWrites.single().leanMassKg)
    }

    @Test
    fun `sem Health Connect nao publica nada`() = runTest {
        val gw = FakeGateway(available = HealthAvailability.NOT_SUPPORTED)
        val result = publisher(gw, nutrition = listOf(DayNutrition(20_000, 2000, 1.0, 1.0, 1.0)))
            .publishNow(20_000)
        assertTrue(result.isEmpty)
        assertTrue(gw.nutritionWrites.isEmpty())
    }

    @Test
    fun `sem permissao de escrita nao publica nada`() = runTest {
        val gw = FakeGateway(canWrite = false)
        val result = publisher(gw, sessions = listOf(session("run-1"))).publishNow(20_000)
        assertTrue(result.isEmpty)
        assertTrue(gw.sessionWrites.isEmpty())
    }

    @Test
    fun `uma sessao que o gateway recusa nao conta como publicada`() = runTest {

        val gw = object : FakeGateway() {
            override suspend fun writeSession(session: OutboundSession) = false
        }
        val result = publisher(gw, sessions = listOf(session("run-1"))).publishNow(20_000)
        assertEquals(0, result.sessions)
    }

    @Test
    fun `dia sem nada publica vazio (e nao rebenta)`() = runTest {
        val gw = FakeGateway()
        val result = publisher(gw).publishNow(20_000)
        assertTrue(result.isEmpty)
        assertFalse(gw.available == HealthAvailability.NOT_SUPPORTED)
    }
}
