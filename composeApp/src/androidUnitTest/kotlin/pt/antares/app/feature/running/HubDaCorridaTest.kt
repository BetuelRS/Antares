package pt.antares.app.feature.running

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.core.database.entities.RunEntity
import pt.antares.app.core.util.toEpochDay
import pt.antares.app.feature.running.data.RunRepository
import pt.antares.app.feature.running.domain.ActivityType
import pt.antares.app.feature.running.domain.RunStatus
import pt.antares.app.feature.running.domain.Split
import pt.antares.app.feature.running.ui.HubDaCorrida
import pt.antares.app.feature.running.ui.RunHubViewModel
import pt.antares.app.testing.ViewModelHarness
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * O hub da corrida passou a mostrar o que se fez, e cada número dele tem uma definição que
 * já existia noutro sítio e tem de continuar a ser a mesma.
 *
 * - **A semana é a ISO**, de segunda a domingo, a mesma do painel de treino: uma corrida de
 *   domingo não é desta semana se hoje é quarta.
 * - **O ritmo da semana sai dos totais** e não da média dos ritmos.
 * - **Os recordes são de sempre**, e continuam a sair só dos quilómetros inteiros.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class HubDaCorridaTest : ViewModelHarness() {

    private val utc = TimeZone.UTC

    // Quarta, 9 de setembro de 2026. A semana ISO começa na segunda, dia 7.
    private val quarta = LocalDate(2026, 9, 9).toEpochDay()

    private fun instante(dia: Int, hora: Int): Long =
        LocalDateTime(2026, 9, dia, hora, 0).toInstant(utc).toEpochMilliseconds()

    private val json = Json

    private fun quilometros(n: Int, msPorKm: Long) = json.encodeToString(
        ListSerializer(Split.serializer()),
        (1..n).map {
            Split(
                index = it,
                distanceM = 1000.0,
                movingMs = msPorKm,
                paceSecPerKm = (msPorKm / 1000).toInt(),
                kcal = 60,
            )
        },
    )

    private suspend fun corrida(
        id: String,
        comeca: Long,
        metros: Double,
        segundos: Long,
        status: RunStatus = RunStatus.DONE,
        splitsJson: String = "[]",
    ) = db.runDao().upsert(
        RunEntity(
            id = id, type = ActivityType.RUN, startedAt = comeca, endedAt = comeca + segundos * 1000,
            distanceM = metros, movingS = segundos, elapsedS = segundos,
            avgPaceSecPerKm = (segundos / (metros / 1000)).toInt(), kcal = 300, elevGainM = 0.0,
            polyline = "", splitsJson = splitsJson, name = id, note = "", status = status, updatedAt = 0L,
        ),
    )

    private fun hub() = vivo(
        RunHubViewModel(RunRepository(db.runDao(), db.exerciseLogDao(), dispatcher), hoje = quarta, zona = utc),
    )

    private suspend fun kotlinx.coroutines.test.TestScope.estado(vm: RunHubViewModel): HubDaCorrida {
        backgroundScope.launch { vm.state.collect {} }
        advanceUntilIdle()
        return vm.state.value
    }

    @Test
    fun `a semana e a ISO, e soma so as corridas feitas dentro dela`() = runTest(dispatcher) {
        corrida("domingo", instante(6, 9), 10_000.0, 3000)
        corrida("segunda", instante(7, 8), 5_000.0, 1500)
        corrida("quarta", instante(9, 7), 1_000.0, 360)
        corrida("descartada", instante(8, 8), 7_000.0, 2100, status = RunStatus.DISCARDED)

        val s = estado(hub())

        assertTrue(s.carregado)
        assertEquals(2, s.corridasNaSemana)
        assertEquals(6_000.0, s.metrosNaSemana)
        assertEquals(1860L, s.movimentoNaSemanaS)
        // 6 km em 1 860 s são 310 s por quilómetro. A média dos dois ritmos (300 e 360) daria
        // 330, com o mesmo peso para uma corrida cinco vezes mais curta.
        assertEquals(310, s.ritmoNaSemanaSegPorKm)
    }

    @Test
    fun `as ultimas vem da mais recente, param em tres, e atravessam a semana`() = runTest(dispatcher) {
        corrida("a", instante(1, 8), 5_000.0, 1500)
        corrida("b", instante(3, 8), 5_000.0, 1500)
        corrida("c", instante(6, 8), 5_000.0, 1500)
        corrida("d", instante(8, 8), 5_000.0, 1500)

        val s = estado(hub())

        assertEquals(listOf("d", "c", "b"), s.ultimas.map { it.id })
    }

    @Test
    fun `os recordes sao de sempre, e nao da semana`() = runTest(dispatcher) {
        // A melhor de sempre é de agosto; a desta semana é mais lenta.
        corrida("agosto", instante(1, 8) - 30L * 86_400_000L, 5_000.0, 1400, splitsJson = quilometros(5, 280_000))
        corrida("segunda", instante(7, 8), 5_000.0, 1600, splitsJson = quilometros(5, 320_000))

        val s = estado(hub())

        assertEquals(280_000L, s.pr1kMs)
        assertEquals(1_400_000L, s.pr5kMs)
        assertNull(s.pr10kMs)
        assertTrue(s.temRecordes)
    }

    @Test
    fun `sem corridas nenhumas o hub carrega vazio, e nao fica a carregar`() = runTest(dispatcher) {
        val s = estado(hub())

        assertTrue(s.carregado, "sem isto o ecrã não distingue «a ler» de «não há nada»")
        assertEquals(0, s.corridasNaSemana)
        assertTrue(s.ultimas.isEmpty())
        assertFalse(s.temRecordes)
        assertEquals(0, s.ritmoNaSemanaSegPorKm)
    }
}
