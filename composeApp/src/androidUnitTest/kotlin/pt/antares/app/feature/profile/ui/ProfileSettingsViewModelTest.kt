package pt.antares.app.feature.profile.ui

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.core.calc.NutritionCalc
import pt.antares.app.core.database.entities.UserProfileEntity
import pt.antares.app.core.model.ActivityLevel
import pt.antares.app.core.model.GoalRates
import pt.antares.app.core.model.GoalType
import pt.antares.app.core.model.MacroStrategy
import pt.antares.app.core.model.Sex
import pt.antares.app.testing.ViewModelHarness
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * O ecrã de definições é a única porta por onde o objetivo e os macros mudam, e o perfil que
 * ele grava alimenta todas as metas da app. O que se testa aqui não é a aritmética — essa vive
 * no `NutritionCalc` e tem testes próprios — mas o acoplamento entre campos: mudar um obriga a
 * repor outro, e é aí que uma meta pode ficar errada sem ninguém dar por isso.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ProfileSettingsViewModelTest : ViewModelHarness() {

    private fun perfil(
        goalType: GoalType = GoalType.LOSE,
        goalRateKcal: Int = -550,
        macroStrategy: MacroStrategy = MacroStrategy.BALANCED,
        customProteinG: Int? = null,
        customCarbsG: Int? = null,
        customFatG: Int? = null,
    ) = UserProfileEntity(
        sex = Sex.MALE,
        birthEpochDay = 10_000L,
        heightCm = 178,
        activityLevel = ActivityLevel.MODERATE,
        goalType = goalType,
        goalRateKcal = goalRateKcal,
        macroStrategy = macroStrategy,
        customProteinG = customProteinG,
        customCarbsG = customCarbsG,
        customFatG = customFatG,
        updatedAt = 0L,
    )

    /** Devolve o ViewModel já com o perfil carregado: os setters não fazem nada antes disso. */
    private suspend fun viewModelCom(profile: UserProfileEntity): ProfileSettingsViewModel {
        profileRepository().saveProfile(profile)
        val vm = ProfileSettingsViewModel(profileRepository(), prefs)
        vm.state.first { !it.loading }
        return vm
    }

    private suspend fun gravado() = db.userProfileDao().get()

    @Test
    fun `mudar de objetivo repoe o ritmo por omissao desse objetivo`() = runTest(dispatcher) {
        val vm = viewModelCom(perfil(goalType = GoalType.LOSE, goalRateKcal = -1000))

        vm.setGoal(GoalType.GAIN)
        advanceUntilIdle()

        val esperado = NutritionCalc.kcalPerDayFromWeeklyKg(GoalRates.DEFAULT_GAIN_KG_WEEK)
        assertEquals(GoalType.GAIN, gravado()?.goalType)
        assertEquals(esperado, gravado()?.goalRateKcal, "o défice anterior sobreviveu à mudança")
    }

    @Test
    fun `passar a manutencao poe o ritmo a zero`() = runTest(dispatcher) {
        val vm = viewModelCom(perfil(goalType = GoalType.LOSE, goalRateKcal = -550))

        vm.setGoal(GoalType.MAINTAIN)
        advanceUntilIdle()

        assertEquals(GoalRates.MAINTAIN, gravado()?.goalRateKcal)
    }

    @Test
    fun `recomposicao e defice, e nao excedente`() = runTest(dispatcher) {
        val vm = viewModelCom(perfil(goalType = GoalType.MAINTAIN, goalRateKcal = 0))

        vm.setGoal(GoalType.RECOMP)
        advanceUntilIdle()

        val rate = assertNotNull(gravado()?.goalRateKcal)
        assertTrue(rate < 0, "a recomposição gravou um excedente de $rate kcal")
        assertEquals(NutritionCalc.kcalPerDayFromWeeklyKg(-GoalRates.RECOMP_KG_WEEK), rate)
    }

    @Test
    fun `o ritmo escrito no ecra segue o sinal do objetivo, e nao o do numero`() =
        runTest(dispatcher) {
            val vm = viewModelCom(perfil(goalType = GoalType.LOSE))

            // O ecrã oferece um valor sempre positivo; quem decide a direção é o objetivo.
            vm.setWeeklyRate(0.75)
            advanceUntilIdle()

            val rate = assertNotNull(gravado()?.goalRateKcal)
            assertTrue(rate < 0, "quem quer perder ficou com um excedente de $rate kcal")
            assertEquals(NutritionCalc.kcalPerDayFromWeeklyKg(-0.75), rate)
        }

    @Test
    fun `um ritmo negativo dado a quem quer ganhar continua a ser ganho`() = runTest(dispatcher) {
        val vm = viewModelCom(perfil(goalType = GoalType.GAIN, goalRateKcal = 275))

        vm.setWeeklyRate(-0.25)
        advanceUntilIdle()

        val rate = assertNotNull(gravado()?.goalRateKcal)
        assertTrue(rate > 0, "quem quer ganhar ficou com um défice de $rate kcal")
    }

    @Test
    fun `a recomposicao aceita o ritmo como perda`() = runTest(dispatcher) {
        val vm = viewModelCom(perfil(goalType = GoalType.RECOMP))

        vm.setWeeklyRate(GoalRates.RECOMP_KG_WEEK)
        advanceUntilIdle()

        assertTrue(assertNotNull(gravado()?.goalRateKcal) < 0)
    }

    @Test
    fun `uma altura impossivel nao chega a ser gravada`() = runTest(dispatcher) {
        val vm = viewModelCom(perfil())

        vm.setHeight(40)
        advanceUntilIdle()
        assertEquals(178, gravado()?.heightCm, "aceitou 40 cm")

        vm.setHeight(300)
        advanceUntilIdle()
        assertEquals(178, gravado()?.heightCm, "aceitou 3 metros")

        vm.setHeight(165)
        advanceUntilIdle()
        assertEquals(165, gravado()?.heightCm, "recusou uma altura normal")
    }

    @Test
    fun `um peso-alvo fora do plausivel fica por escolher, e nao guardado`() = runTest(dispatcher) {
        val vm = viewModelCom(perfil())

        vm.setGoalWeight(5.0)
        advanceUntilIdle()
        assertNull(gravado()?.goalWeightKg, "aceitou 5 kg como objetivo")

        vm.setGoalWeight(72.0)
        advanceUntilIdle()
        assertEquals(72.0, gravado()?.goalWeightKg)
    }

    @Test
    fun `passar a macros manuais arranca nos numeros que estavam a valer`() = runTest(dispatcher) {
        val vm = viewModelCom(perfil(macroStrategy = MacroStrategy.BALANCED))

        // Sem esta semente, quem passa a manual encontra três campos vazios e perde a meta
        // que tinha — e o ecrã passa a mostrar zeros até os preencher aos três.
        val alvos = assertNotNull(vm.state.value.targets, "os alvos não chegaram a ser calculados")
        vm.setStrategy(MacroStrategy.CUSTOM)
        advanceUntilIdle()

        val p = assertNotNull(gravado())
        assertEquals(MacroStrategy.CUSTOM, p.macroStrategy)
        assertEquals(alvos.proteinG, p.customProteinG)
        assertEquals(alvos.carbsG, p.customCarbsG)
        assertEquals(alvos.fatG, p.customFatG)
    }

    @Test
    fun `voltar a uma estrategia automatica nao apaga os macros manuais`() = runTest(dispatcher) {
        val vm = viewModelCom(
            perfil(
                macroStrategy = MacroStrategy.CUSTOM,
                customProteinG = 180,
                customCarbsG = 200,
                customFatG = 70,
            ),
        )

        vm.setStrategy(MacroStrategy.KETO)
        advanceUntilIdle()

        val p = assertNotNull(gravado())
        assertEquals(MacroStrategy.KETO, p.macroStrategy)
        assertEquals(180, p.customProteinG, "os macros manuais foram apagados ao trocar")
        assertEquals(200, p.customCarbsG)
        assertEquals(70, p.customFatG)
    }

    @Test
    fun `escrever macros a mao passa a estrategia para manual`() = runTest(dispatcher) {
        val vm = viewModelCom(perfil(macroStrategy = MacroStrategy.BALANCED))

        vm.setCustomMacros(proteinG = 150, carbsG = 250, fatG = 60)
        advanceUntilIdle()

        val p = assertNotNull(gravado())
        assertEquals(MacroStrategy.CUSTOM, p.macroStrategy, "os números ficaram sem efeito")
        assertEquals(150, p.customProteinG)
    }
}
