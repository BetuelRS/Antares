package pt.antares.app.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pt.antares.app.core.database.entities.UserProfileEntity
import pt.antares.app.core.model.ActivityLevel
import pt.antares.app.core.model.GoalType
import pt.antares.app.core.model.MacroStrategy
import pt.antares.app.core.model.MealSlot
import pt.antares.app.core.model.Sex
import pt.antares.app.core.util.todayEpochDay
import pt.antares.app.feature.today.TodayScreen
import pt.antares.app.testing.Fabricas
import pt.antares.app.testing.FluxoUiHarness
import kotlin.test.assertNotNull

/**
 * O anel do ecrã Hoje é a primeira coisa que a app mostra e a razão de se registar alguma
 * coisa. O número que ele traz ao centro vem de um `Flow` sobre a base: registar comida
 * noutro ecrã tem de o mudar sem ninguém mandar recarregar nada.
 *
 * O teste não passa pelo ecrã de registo, que tem navegação pelo meio — chama o
 * `DiaryRepository`, que é o que esse ecrã acaba por chamar, e exige que o ecrã Hoje reaja.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class AnelDoDiaUiTest : FluxoUiHarness() {

    private fun perfilCom80Kg() = runBlocking {
        db.userProfileDao().upsert(
            UserProfileEntity(
                sex = Sex.MALE,
                birthEpochDay = NASCIMENTO_EPOCH_DAY,
                heightCm = 180,
                activityLevel = ActivityLevel.MODERATE,
                // Manter o peso deixa a meta igual ao gasto estimado, sem défice a somar
                // ruído ao número que se está a verificar.
                goalType = GoalType.MAINTAIN,
                goalRateKcal = 0,
                macroStrategy = MacroStrategy.BALANCED,
                customProteinG = null,
                customCarbsG = null,
                customFatG = null,
                updatedAt = 1_000,
            ),
        )
        Fabricas.profileRepository(db, io).upsertWeight(todayEpochDay(), 80.0, note = null)
    }

    @Test
    fun `registar comida faz o anel descer sem recarregar o ecra`() = runComposeUiTest {
        arrancaKoin()
        perfilCom80Kg()
        val vm = Fabricas.todayViewModel(db, prefs, io)

        setContent {
            TodayScreen(
                onLogWeight = {},
                onAddMeal = {},
                onOpenWorkout = {},
                onOpenFasting = {},
                onOpenRun = {},
                onOpenCoach = {},
                onOpenProfile = {},
                onQuickLog = { _, _, _, _ -> },
                onOpenGap = {},
                viewModel = vm,
            )
        }

        waitUntil("o perfil nunca chegou ao ecrã", ESPERA_MS) { vm.state.value.targets != null }
        val meta = assertNotNull(vm.state.value.targets).kcal

        // Sem nada registado, o que sobra do dia é a meta inteira.
        onNodeWithText("$meta").assertIsDisplayed()

        runBlocking {
            Fabricas.diaryRepository(db, io).logQuickCalories(
                kcal = REFEICAO_KCAL,
                name = "almoço",
                slot = MealSlot.LUNCH,
                epochDay = todayEpochDay(),
            )
        }

        waitUntil("o anel ficou parado depois de se registar comida", ESPERA_MS) {
            onAllNodesWithText("${meta - REFEICAO_KCAL}").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        // 1990-06-15, uma idade adulta que não fica perto de nenhum limite das contas.
        const val NASCIMENTO_EPOCH_DAY = 7470L
        const val REFEICAO_KCAL = 500

        // Uma escrita na base em memória não demora nada; isto é o teto antes de desistir.
        const val ESPERA_MS = 5_000L
    }
}
