package pt.antares.app.feature.workout

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pt.antares.app.core.database.entities.RoutineEntity
import pt.antares.app.core.database.entities.WorkoutSessionEntity
import pt.antares.app.core.model.SessionStatus
import pt.antares.app.feature.workout.data.RoutineRepository
import pt.antares.app.feature.workout.data.WorkoutHubRepository
import pt.antares.app.feature.workout.ui.WorkoutHubViewModel
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.workout_hub_no_plan_title
import pt.antares.app.generated.resources.workout_hub_run_none_ever
import pt.antares.app.generated.resources.workout_hub_sem_rotina_title
import pt.antares.app.generated.resources.workout_hub_start
import pt.antares.app.generated.resources.workout_hub_start_empty
import pt.antares.app.generated.resources.workout_hub_start_named
import pt.antares.app.testing.FluxoUiHarness
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * O centro de treino, tocado como se toca.
 *
 * Existe por causa de um defeito que nenhum teste de estado via: o `startOrResume` devolve a
 * sessão que já está aberta e **ignora a rotina que se lhe pede**. Com um treino a decorrer,
 * um ▶ ao lado de uma rotina levaria ao treino errado — sem erro, sem aviso, e com o nome da
 * rotina certa escrito ao lado.
 *
 * É a mesma família de defeito da 2.17.0, onde um componente aceitava um `onClick` e o
 * deitava fora: não muda estado nenhum, e por isso só se vê a compor o ecrã.
 *
 * **A afirmação é sobre a descrição do ▶, e não sobre a palavra «Começar»:** essa é substring
 * de «Começar um treino vazio», que está sempre no ecrã — um teste escrito assim passava com
 * o código partido, e passou, enquanto se escrevia este.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, qualifiers = "w411dp-h891dp")
class CentroDeTreinoUiTest : FluxoUiHarness() {

    private fun viewModel() = vivo(
        WorkoutHubViewModel(
            routineRepository = RoutineRepository(
                db.routineDao(),
                db.exerciseLibraryDao(),
                db.routineScheduleDao(),
                db.workoutSetDao(),
                io,
            ),
            hubRepository = WorkoutHubRepository(
                routineDao = db.routineDao(),
                sessionDao = db.workoutSessionDao(),
                setDao = db.workoutSetDao(),
                scheduleDao = db.routineScheduleDao(),
                exerciseDao = db.exerciseLibraryDao(),
                runDao = db.runDao(),
            ),
        ),
    )

    private fun rotina(id: String, nome: String) = runBlocking {
        db.routineDao().upsertRoutine(
            RoutineEntity(id = id, name = nome, note = null, position = 0, updatedAt = 1L),
        )
    }

    private fun sessao(
        id: String,
        status: SessionStatus,
        fim: Long?,
        rotinaId: String? = "r",
    ) = runBlocking {
        db.workoutSessionDao().upsertSession(
            WorkoutSessionEntity(
                id = id,
                startedAt = 1_000L,
                endedAt = fim,
                routineId = rotinaId,
                note = null,
                status = status,
                updatedAt = 1_000L,
            ),
        )
    }

    /**
     * O ViewModel é criado **fora da composição** e espera-se que carregue **antes** de
     * compor: criá-lo lá dentro dava um ViewModel novo a cada recomposição, e o relógio do
     * teste de interface não deixa o trabalho da base chegar depois.
     */
    private fun carregado(): WorkoutHubViewModel = viewModel().also { vm ->
        runBlocking { vm.state.first { it.carregado } }
    }

    @Composable
    private fun ecra(vm: WorkoutHubViewModel, textos: Textos, onRun: () -> Unit = {}) {
        WorkoutScreen(
            menu = MenuDoTreino(biblioteca = {}, historico = {}, estatisticas = {}, plano = {}),
            onRoutine = {},
            onStartRoutine = {},
            onStartEmpty = {},
            onResume = {},
            onWorkout = {},
            onRun = onRun,
            viewModel = vm,
        )
        textos.ler(Res.string.workout_hub_start)
        textos.ler(Res.string.workout_hub_run_none_ever)
        textos.ler(Res.string.workout_hub_start_empty)
        textos.lerFormatado(Res.string.workout_hub_start_named, NOME)
    }

    /**
     * Rola a lista até à linha da corrida, que é o penúltimo item do ecrã. Numa
     * `LazyColumn` só está composto o que está à vista, e uma afirmação sobre o último
     * item sem isto fala do ecrã e não do que lá devia estar.
     */
    private fun ComposeUiTest.aoFundo(textos: Textos) {
        onNode(hasScrollAction())
            .performScrollToNode(hasText(textos[Res.string.workout_hub_run_none_ever]))
    }
    @Test
    fun `com um treino a decorrer o ecra oferece retomar e nao comecar uma rotina`() = runComposeUiTest {
        arrancaKoin()
        rotina("r", NOME)
        sessao("activa", SessionStatus.ACTIVE, fim = null)

        val textos = Textos()
        val vm = carregado()
        setContent { ecra(vm, textos) }
        waitForIdle()

        // Primeiro a presença: sem ela, um ecrã que não desenhasse nada passava neste teste.
        onNodeWithText(NOME).assertExists()

        // O cartão de destaque não está lá: é o botão «Começar» dele que levaria ao treino
        // errado. E «Começar» sozinho não serve de afirmação — é substring de «Começar um
        // treino vazio», que está sempre no ecrã.
        onNodeWithContentDescription(textos[Res.string.workout_hub_start_named])
            .assertDoesNotExist()
        onNodeWithText(textos[Res.string.workout_hub_start]).assertDoesNotExist()

        // **E o treino vazio também não.** Ficou de fora quando os outros dois foram
        // escondidos, e fazia exactamente o mesmo: o `startOrResume` devolve a sessão
        // aberta, portanto «Iniciar treino vazio» abria o treino que estava a meio.
        // Verificado no aparelho antes de se escrever este teste.
        //
        // Rola-se até à linha da corrida primeiro, que é o item logo acima dele: numa
        // `LazyColumn` só existe o que está à vista, e sem isto um `assertDoesNotExist`
        // passava por o botão estar fora do ecrã em vez de não existir.
        aoFundo(textos)
        onNodeWithText(textos[Res.string.workout_hub_start_empty]).assertDoesNotExist()
    }

    @Test
    fun `sem treino a decorrer o treino vazio esta la`() = runComposeUiTest {
        arrancaKoin()
        rotina("r", NOME)
        sessao("feito", SessionStatus.DONE, fim = 2_000L)

        val textos = Textos()
        val vm = carregado()
        setContent { ecra(vm, textos) }
        waitForIdle()

        // O simétrico do de cima: sem ele, esconder o botão para sempre passava nos dois.
        aoFundo(textos)
        onNodeWithText(textos[Res.string.workout_hub_start_empty]).assertExists()
    }
    @Test
    fun `sem treino a decorrer a rotina pode comecar-se da lista`() = runComposeUiTest {
        arrancaKoin()
        rotina("r", NOME)
        sessao("feito", SessionStatus.DONE, fim = 2_000L)

        val textos = Textos()
        val vm = carregado()
        setContent { ecra(vm, textos) }
        waitForIdle()

        onNodeWithContentDescription(textos[Res.string.workout_hub_start_named]).assertExists()
        onNodeWithText(textos[Res.string.workout_hub_start]).assertExists()
    }

    /**
     * A corrida perdeu o separador na 2.20.1, e a promessa da versão foi que nada
     * desapareceu — mudou de sítio. Este teste é essa promessa: o painel de treino tem a
     * linha da corrida e ela leva ao hub dela. Sem isto, apagar seis linhas do ecrã deixava
     * a corrida sem porta nenhuma e nada acusava.
     */
    @Test
    fun `a corrida continua a ter porta depois de sair da barra`() = runComposeUiTest {
        arrancaKoin()
        rotina("r", NOME)

        var foiParaACorrida = false
        val textos = Textos()
        val vm = carregado()
        setContent { ecra(vm, textos, onRun = { foiParaACorrida = true }) }
        waitForIdle()

        onNodeWithText(textos[Res.string.workout_hub_run_none_ever]).performClick()
        waitForIdle()

        assertTrue(foiParaACorrida, "a linha da corrida deixou de levar ao hub dela")
    }

    /**
     * O cartão de destaque dizia **«Ainda não treinaste»** por cima de um cartão da semana com
     * treinos contados nele. Quem só faz treinos livres nunca treinou uma **rotina**, e
     * treinou: o destaque só olha para as sessões que nasceram de uma rotina, e a semana
     * conta-as todas.
     *
     * Os dois números estavam certos, e por isso nenhum teste de estado via nada — o que
     * estava errado era a frase que os acompanhava.
     */
    @Test
    fun `um treino livre nao faz o cartao dizer que nunca treinaste`() = runComposeUiTest {
        arrancaKoin()
        rotina("r", NOME)
        sessao("livre", SessionStatus.DONE, fim = 2_000L, rotinaId = null)

        val textos = Textos()
        val vm = carregado()
        setContent {
            ecra(vm, textos)
            textos.ler(Res.string.workout_hub_no_plan_title)
            textos.ler(Res.string.workout_hub_sem_rotina_title)
        }
        waitForIdle()

        onNodeWithText(textos[Res.string.workout_hub_no_plan_title]).assertDoesNotExist()
        onNodeWithText(textos[Res.string.workout_hub_sem_rotina_title]).assertExists()
    }

    /** O simétrico: sem treino nenhum, é mesmo a primeira vez, e a frase de sempre serve. */
    @Test
    fun `sem treinos o cartao continua a dizer que ainda nao treinaste`() = runComposeUiTest {
        arrancaKoin()
        rotina("r", NOME)

        val textos = Textos()
        val vm = carregado()
        setContent {
            ecra(vm, textos)
            textos.ler(Res.string.workout_hub_no_plan_title)
            textos.ler(Res.string.workout_hub_sem_rotina_title)
        }
        waitForIdle()

        onNodeWithText(textos[Res.string.workout_hub_no_plan_title]).assertExists()
        onNodeWithText(textos[Res.string.workout_hub_sem_rotina_title]).assertDoesNotExist()
    }

    private companion object {
        const val NOME = "Full Body A"
    }
}
