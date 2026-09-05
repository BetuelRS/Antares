package pt.antares.app.feature.workout

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pt.antares.app.core.database.entities.ExerciseEntity
import pt.antares.app.core.database.entities.WorkoutSessionEntity
import pt.antares.app.core.database.entities.WorkoutSetEntity
import pt.antares.app.core.model.SessionStatus
import pt.antares.app.core.util.dayShortDated
import pt.antares.app.core.util.todayEpochDay
import pt.antares.app.feature.workout.data.WorkoutHistoryRepository
import pt.antares.app.feature.workout.ui.WorkoutStatsScreen
import pt.antares.app.feature.workout.ui.WorkoutStatsViewModel
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.stat_period_day
import pt.antares.app.generated.resources.stat_period_month
import pt.antares.app.generated.resources.workout_stats_frequency_count
import pt.antares.app.generated.resources.workout_stats_no_series
import pt.antares.app.testing.FluxoUiHarness
import kotlin.test.Test

/**
 * As estatísticas do treino, tocadas como se tocam.
 *
 * Existe por causa de dois defeitos que só apareceram no aparelho e que nenhum teste de
 * estado via, porque os dois são sobre **duas frases lidas juntas**:
 *
 * - com «Dia» escolhido, o cartão dizia «1 no período escolhido» por cima de «Sem séries no
 *   período escolhido» — a contagem de treinos vinha da semana ISO e as séries do dia;
 * - a média semanal arredondava a zero e a linha lia-se «0 séries · 600 kg», que é uma
 *   contradição dentro da mesma linha.
 *
 * Um teste de estado vê os números certos nos dois casos. O que estava errado era o ecrã
 * mostrá-los ao mesmo tempo.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, qualifiers = "w411dp-h891dp")
class EstatisticasDoTreinoUiTest : FluxoUiHarness() {

    private fun repositorio() = WorkoutHistoryRepository(
        db.workoutSessionDao(),
        db.workoutSetDao(),
        db.exerciseLibraryDao(),
        db.routineDao(),
        io,
    )

    /**
     * Um treino terminado **agora**, com uma série de trabalho de peito.
     *
     * O instante é o do relógio e não uma constante: o ecrã filtra por períodos contados a
     * partir de agora, e um instante fixo cairia fora de todos eles no dia seguinte.
     */
    private fun treinoDeHoje() = treino(diasAtras = 0)

    /**
     * Três dias, e é o número que faz o defeito aparecer: cai fora do «Dia» e dentro da
     * semana ISO na maior parte dos dias da semana.
     */
    private fun treinoDeHaTresDias() = treino(diasAtras = 3)

    private fun treino(diasAtras: Int) = runBlocking {
        db.exerciseLibraryDao().upsert(
            ExerciseEntity(
                id = "bench", nameEn = "Bench Press", namePt = NOME, searchText = "supino",
                category = "strength", force = null, mechanic = null, equipment = "barbell",
                level = "beginner", primaryMuscles = "|chest|", secondaryMuscles = "",
                instructionsEnJson = "[]", instructionsPtJson = "[]", imagesJson = "[]",
                updatedAt = 1L,
            ),
        )
        val quando = Clock.System.now().toEpochMilliseconds() - diasAtras * MS_POR_DIA
        db.workoutSessionDao().upsertSession(
            WorkoutSessionEntity("s", quando, quando, null, null, SessionStatus.DONE, quando),
        )
        db.workoutSetDao().upsertSet(
            WorkoutSetEntity("s1", "s", "bench", 0, 100.0, reps = 5, rpe = null, updatedAt = quando),
        )
    }

    /**
     * O ViewModel é criado **fora da composição** e espera-se que carregue antes de compor,
     * como no centro de treino: criá-lo lá dentro dava um ViewModel novo a cada
     * recomposição, e o relógio do teste de interface não deixa a base chegar depois.
     */
    private fun carregado(): WorkoutStatsViewModel =
        vivo(WorkoutStatsViewModel(repositorio())).also { vm ->
            runBlocking { vm.state.first { !it.loading } }
        }

    /**
     * A data de hoje, escrita pelo mesmo formatador que o ecrã usa e lida **dentro** da
     * composição. Escrevê-la à mão no teste prendia-o ao formato: bastava alguém corrigir uma
     * abreviatura de mês para o teste passar a acusar quem não partiu nada.
     */
    private var dataDeHoje: String = ""

    @Composable
    private fun ecra(vm: WorkoutStatsViewModel, textos: Textos) {
        WorkoutStatsScreen(onBack = {}, viewModel = vm)
        textos.ler(
            Res.string.stat_period_day,
            Res.string.stat_period_month,
            Res.string.workout_stats_no_series,
        )
        // Formatada com zero de propósito: é a frase que tem de aparecer quando também não
        // há séries, e é o par das duas que este teste defende.
        textos.lerFormatado(Res.string.workout_stats_frequency_count, 0)
        dataDeHoje = dayShortDated(todayEpochDay())
    }

    /**
     * **As duas secções contam o mesmo período.**
     *
     * Este é o defeito, tal como se viu no aparelho: com «Dia» escolhido e o treino a ser de
     * há três dias, o cartão de cima dizia «1 no período escolhido» e o de baixo dizia «Sem
     * séries no período escolhido». Os dois números estavam certos — um contava a semana ISO
     * e o outro o dia —, e era lê-los juntos que não fazia sentido.
     *
     * A afirmação é sobre o **par**: quando não há séries no período, a contagem tem de ser
     * zero. Um teste que só olhasse para um dos dois passava com o defeito posto.
     */
    @Test
    fun `com o dia escolhido as duas seccoes contam o mesmo periodo`() = runComposeUiTest {
        arrancaKoin()
        treinoDeHaTresDias()

        val textos = Textos()
        val vm = carregado()
        setContent { ecra(vm, textos) }
        waitForIdle()

        onNodeWithText(textos[Res.string.stat_period_day]).performClick()
        waitForIdle()

        onNode(hasText(textos[Res.string.workout_stats_no_series])).assertExists()
        onNode(hasText(textos[Res.string.workout_stats_frequency_count])).assertExists()
    }

    /** O simétrico: com o mês, o mesmo treino entra nas duas. */
    @Test
    fun `com o mes escolhido o treino entra nas duas seccoes`() = runComposeUiTest {
        arrancaKoin()
        treinoDeHaTresDias()

        val textos = Textos()
        val vm = carregado()
        setContent { ecra(vm, textos) }
        waitForIdle()

        onNodeWithText(textos[Res.string.stat_period_month]).performClick()
        waitForIdle()

        onNode(hasText(textos[Res.string.workout_stats_no_series])).assertDoesNotExist()
        onNode(hasText(textos[Res.string.workout_stats_frequency_count])).assertDoesNotExist()
    }

    /**
     * O recorde traz a data. É o defeito concreto 4 da `estudo/areas/10`: sem ela, um de 2024
     * aparecia igual a um de ontem, e a lista deixava de dizer onde houve progresso.
     */
    @Test
    fun `o recorde mostra o dia em que aconteceu`() = runComposeUiTest {
        arrancaKoin()
        treinoDeHoje()

        val textos = Textos()
        val vm = carregado()
        setContent { ecra(vm, textos) }
        waitForIdle()

        // O nome e a data são dois nós — o nome numa linha, a data na de baixo —, e por isso
        // afirmam-se em separado. Rola-se até ao nome primeiro: os recordes são a última
        // secção, e num ecrã que rola um `assertIsDisplayed` sem isto falha por o cartão
        // estar abaixo da dobra, que é outra coisa que não existir.
        onNode(hasScrollAction()).performScrollToNode(hasText(NOME))
        onNodeWithText(NOME).assertIsDisplayed()
        onNode(hasText(dataDeHoje, substring = true)).assertIsDisplayed()
    }

    private companion object {
        const val NOME = "Supino"
        const val MS_POR_DIA = 24L * 60 * 60 * 1000
    }
}
