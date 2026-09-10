package pt.antares.app.feature.running

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.core.database.entities.RunEntity
import pt.antares.app.core.health.HealthAvailability
import pt.antares.app.core.health.HealthGateway
import pt.antares.app.core.health.NoHealthGateway
import pt.antares.app.feature.running.data.RunRepository
import pt.antares.app.feature.running.domain.ActivityType
import pt.antares.app.feature.running.domain.RunStatus
import pt.antares.app.feature.running.ui.RunDetailViewModel
import pt.antares.app.testing.ViewModelHarness
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A frequência cardíaca entrou na 2.31.0 «só a ler o que houver», e o que este teste guarda
 * são os três estados que isso tem: com relógio e permissão aparece o número; sem permissão
 * aparece o pedido; e sem Health Connect não aparece nada — nem número, nem pedido para uma
 * coisa que o telemóvel não tem.
 *
 * E guarda a janela: a média pedida é a da corrida, do início ao fim, e não a do dia.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class FrequenciaCardiacaDaCorridaTest : ViewModelHarness() {

    private class Relogio(
        private val disponivel: Boolean,
        private val concedida: Boolean,
        private val bpm: Int?,
    ) : HealthGateway by NoHealthGateway {
        var janela: Pair<Long, Long>? = null
        override fun availability() =
            if (disponivel) HealthAvailability.AVAILABLE else HealthAvailability.NOT_SUPPORTED
        override val heartRatePermissions = setOf("fc")
        override suspend fun hasHeartRatePermission() = concedida
        override suspend fun heartRateAvg(startMs: Long, endMs: Long): Int? {
            janela = startMs to endMs
            return bpm
        }
    }

    private suspend fun corrida() = RunEntity(
        id = "r", type = ActivityType.RUN, startedAt = 1_000L, endedAt = 1_801_000L,
        distanceM = 5000.0, movingS = 1800, elapsedS = 1800, avgPaceSecPerKm = 360,
        kcal = 350, elevGainM = 0.0, polyline = "", splitsJson = "[]",
        name = "", note = "", status = RunStatus.DONE, updatedAt = 0L,
    ).also { db.runDao().upsert(it) }

    private fun detalhe(relogio: HealthGateway) =
        vivo(RunDetailViewModel(RunRepository(db.runDao(), db.exerciseLogDao(), dispatcher), relogio))

    @Test
    fun `com relogio e permissao mostra a media da corrida`() = runTest(dispatcher) {
        corrida()
        val relogio = Relogio(disponivel = true, concedida = true, bpm = 148)
        val vm = detalhe(relogio)

        vm.load("r")
        advanceUntilIdle()

        assertEquals(148, vm.state.value.fcMedia)
        assertFalse(vm.state.value.podePedirFc)
        assertEquals(1_000L to 1_801_000L, relogio.janela, "a média tem de ser a da janela da corrida")
    }

    @Test
    fun `sem permissao pede, e nao le`() = runTest(dispatcher) {
        corrida()
        val relogio = Relogio(disponivel = true, concedida = false, bpm = 148)
        val vm = detalhe(relogio)

        vm.load("r")
        advanceUntilIdle()

        assertNull(vm.state.value.fcMedia)
        assertTrue(vm.state.value.podePedirFc)
        assertNull(relogio.janela, "sem permissão não se lê nada")
    }

    @Test
    fun `sem Health Connect nao ha numero nem pedido`() = runTest(dispatcher) {
        corrida()
        val vm = detalhe(Relogio(disponivel = false, concedida = false, bpm = null))

        vm.load("r")
        advanceUntilIdle()

        assertNull(vm.state.value.fcMedia)
        assertFalse(vm.state.value.podePedirFc, "um pedido para uma coisa que o telemóvel não tem")
    }

    @Test
    fun `com permissao e sem relogio, a linha nao aparece`() = runTest(dispatcher) {
        corrida()
        val vm = detalhe(Relogio(disponivel = true, concedida = true, bpm = null))

        vm.load("r")
        advanceUntilIdle()

        assertNull(vm.state.value.fcMedia)
        assertFalse(vm.state.value.podePedirFc)
    }
}
