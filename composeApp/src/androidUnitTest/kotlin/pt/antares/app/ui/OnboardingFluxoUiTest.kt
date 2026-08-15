package pt.antares.app.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pt.antares.app.core.model.ActivityLevel
import pt.antares.app.core.model.GoalType
import pt.antares.app.core.model.Sex
import pt.antares.app.feature.onboarding.OnboardingScreen
import pt.antares.app.feature.onboarding.OnboardingViewModel
import pt.antares.app.feature.profile.data.ProfileRepository
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.common_continue
import pt.antares.app.generated.resources.common_kg
import pt.antares.app.generated.resources.onb_activity_moderate
import pt.antares.app.generated.resources.onb_body_height
import pt.antares.app.generated.resources.onb_body_weight
import pt.antares.app.generated.resources.onb_finish
import pt.antares.app.generated.resources.onb_goal_lose
import pt.antares.app.generated.resources.onb_sex_male
import pt.antares.app.generated.resources.onb_welcome_cta
import pt.antares.app.testing.FluxoUiHarness
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * O onboarding é o único ecrã por onde toda a gente passa, e passa uma vez só. Se partir a
 * meio, a app fica sem perfil — e sem perfil não há basal, não há meta e não há orçamento
 * do dia. Nenhum outro teste percorre os nove passos de ponta a ponta.
 *
 * O calendário do nascimento é o único passo que não se toca pela interface: uma célula do
 * `DatePicker` não tem texto por onde lhe pegar sem depender do mês em que o teste corre.
 * Vai pelo ViewModel, e está assinalado onde acontece.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class OnboardingFluxoUiTest : FluxoUiHarness() {

    private fun repositorio() = ProfileRepository(
        db.userProfileDao(),
        db.weightLogDao(),
        db.dailyTargetOverrideDao(),
        db.foodLogDao(),
        db.goalHistoryDao(),
        db.workoutSessionDao(),
        io,
    )

    @Test
    fun `do primeiro ecra ao ultimo, o perfil fica guardado`() = runComposeUiTest {
        val vm = OnboardingViewModel(repositorio(), prefs)
        val textos = Textos()
        var terminou = false

        setContent {
            textos.ler(
                Res.string.onb_welcome_cta,
                Res.string.common_continue,
                Res.string.onb_finish,
                Res.string.onb_sex_male,
                Res.string.onb_body_height,
                Res.string.onb_body_weight,
                Res.string.common_kg,
                Res.string.onb_activity_moderate,
                Res.string.onb_goal_lose,
            )
            OnboardingScreen(onFinished = { terminou = true }, viewModel = vm)
        }

        val continuar = textos[Res.string.common_continue]

        onNodeWithText(textos[Res.string.onb_welcome_cta]).performClick()

        onNodeWithText(textos[Res.string.onb_sex_male]).performClick()
        onNodeWithText(continuar).performClick()

        // 1990-06-15. O calendário não é tocável por texto; o resto do fluxo é.
        vm.setBirth(NASCIMENTO_EPOCH_DAY)
        onNodeWithText(continuar).performClick()

        onNodeWithText(textos[Res.string.onb_body_height]).performTextInput("180")
        val rotuloPeso = "${textos[Res.string.onb_body_weight]} (${textos[Res.string.common_kg]})"
        onNodeWithText(rotuloPeso).performTextInput("80")
        onNodeWithText(continuar).performClick()

        onNodeWithText(textos[Res.string.onb_activity_moderate]).performScrollTo().performClick()
        onNodeWithText(continuar).performClick()

        onNodeWithText(textos[Res.string.onb_goal_lose]).performClick()
        onNodeWithText(continuar).performClick()

        // O peso-alvo fica em branco de propósito: o ecrã diz que se pode deixar assim, e
        // é a única forma de provar que a app não o exige para avançar.
        onNodeWithText(continuar).performClick()

        // O ritmo já vem preenchido com o valor por omissão do objetivo.
        onNodeWithText(continuar).performClick()

        onNodeWithText(textos[Res.string.onb_finish]).performClick()

        // Guardar corre fora da linha da composição: espera-se pelo resultado em vez de o
        // assumir, senão o teste passa ou falha conforme a máquina.
        waitUntil(
            "o onboarding não chamou o `onFinished` — a app ficava presa no último passo",
            ESPERA_MS,
        ) { terminou }

        val perfil = assertNotNull(runBlocking { db.userProfileDao().get() }, "não guardou perfil nenhum")
        assertEquals(Sex.MALE, perfil.sex)
        assertEquals(180, perfil.heightCm)
        assertEquals(NASCIMENTO_EPOCH_DAY, perfil.birthEpochDay)
        assertEquals(ActivityLevel.MODERATE, perfil.activityLevel)
        assertEquals(GoalType.LOSE, perfil.goalType)
        assertTrue(
            perfil.goalRateKcal < 0,
            "quem quer perder tem de ficar com défice, e ficou com ${perfil.goalRateKcal}",
        )

        assertEquals(
            80.0,
            runBlocking { db.weightLogDao().latest() }?.weightKg,
            "o peso escrito no onboarding tem de virar a primeira pesagem: é dele que sai " +
                "o basal, e sem ele as contas do primeiro dia não correm",
        )
    }

    private companion object {
        // 1990-06-15, uma idade adulta que não fica perto de nenhum limite do fluxo.
        const val NASCIMENTO_EPOCH_DAY = 7470L

        // Uma escrita na base em memória não demora nada; isto é o teto antes de desistir.
        const val ESPERA_MS = 5_000L
    }
}
