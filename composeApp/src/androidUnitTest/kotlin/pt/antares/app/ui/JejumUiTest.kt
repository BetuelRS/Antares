package pt.antares.app.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pt.antares.app.core.database.entities.FastingProtocolEntity
import pt.antares.app.core.database.entities.FastingSessionEntity
import pt.antares.app.core.model.FastingStatus
import pt.antares.app.feature.fasting.ui.FastingScreen
import pt.antares.app.feature.fasting.ui.FastingViewModel
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.fasting_break
import pt.antares.app.generated.resources.fasting_finish
import pt.antares.app.testing.Fabricas
import pt.antares.app.testing.FluxoUiHarness
import kotlin.test.assertEquals

/**
 * O ecrã do jejum tinha «terminar» e «terminar cedo» lado a lado desde o primeiro minuto,
 * os dois disponíveis e nada a dizer qual era o certo. Carregar no errado não dá erro
 * nenhum: dá um jejum de dezasseis horas registado como interrompido, ou um interrompido
 * ao fim de dez minutos registado como concluído. O histórico e a taxa de conclusão vivem
 * dessa distinção.
 *
 * Um botão só, e o que ele diz sai do relógio.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, qualifiers = "w411dp-h891dp")
class JejumUiTest : FluxoUiHarness() {

    private fun jejumAtivo(faltamMs: Long) = runBlocking {
        val agora = Clock.System.now().toEpochMilliseconds()
        db.fastingProtocolDao().upsert(
            FastingProtocolEntity(id = PROTOCOLO_ID, name = "16:8", fastingHours = 16, updatedAt = agora),
        )
        db.fastingSessionDao().upsert(
            FastingSessionEntity(
                id = "sessao-1",
                protocolId = PROTOCOLO_ID,
                startedAt = agora - HORAS_16_MS + faltamMs,
                targetEndAt = agora + faltamMs,
                endedAt = null,
                status = FastingStatus.ACTIVE,
                updatedAt = agora,
            ),
        )
    }

    private fun quantosBotoes(faltamMs: Long): Pair<Int, Int> {
        var terminar = 0
        var terminarCedo = 0
        runComposeUiTest {
            arrancaKoin()
            jejumAtivo(faltamMs)

            val vm = FastingViewModel(Fabricas.fastingRepository(db, io))
            val textos = Textos()
            setContent {
                textos.ler(Res.string.fasting_finish, Res.string.fasting_break)
                FastingScreen(onBack = {}, onOpenHistory = {}, onOpenDiary = {}, viewModel = vm)
            }

            waitUntil("o jejum a decorrer nunca chegou ao ecrã", ESPERA_MS) {
                onAllNodesWithText(textos[Res.string.fasting_finish]).fetchSemanticsNodes().isNotEmpty() ||
                    onAllNodesWithText(textos[Res.string.fasting_break]).fetchSemanticsNodes().isNotEmpty()
            }

            terminar = onAllNodesWithText(textos[Res.string.fasting_finish]).fetchSemanticsNodes().size
            terminarCedo = onAllNodesWithText(textos[Res.string.fasting_break]).fetchSemanticsNodes().size
        }
        return terminar to terminarCedo
    }

    @Test
    fun `antes da meta so ha o botao de interromper`() {
        val (terminar, terminarCedo) = quantosBotoes(faltamMs = 2 * HORA_MS)

        assertEquals(0, terminar, "«terminar» aparecia com o jejum a meio, e dava-o por concluído")
        assertEquals(1, terminarCedo)
    }

    @Test
    fun `passada a meta so ha o botao de terminar`() {
        val (terminar, terminarCedo) = quantosBotoes(faltamMs = -HORA_MS)

        assertEquals(1, terminar)
        assertEquals(
            0,
            terminarCedo,
            "«terminar cedo» sobrevivia à meta, e marcava como interrompido um jejum inteiro",
        )
    }

    private companion object {
        const val PROTOCOLO_ID = "proto-16-8"
        const val HORA_MS = 3_600_000L
        const val HORAS_16_MS = 16 * HORA_MS
        const val ESPERA_MS = 5_000L
    }
}
