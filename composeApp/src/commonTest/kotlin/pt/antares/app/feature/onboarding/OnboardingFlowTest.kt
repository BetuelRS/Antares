package pt.antares.app.feature.onboarding

import pt.antares.app.core.model.GoalType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OnboardingFlowTest {

    @Test
    fun `quem quer manter nao passa pelo ritmo`() {
        assertEquals(
            OnboardingStep.PLAN_PREVIEW,
            OnboardingFlow.next(OnboardingStep.GOAL, GoalType.MAINTAIN),
        )
    }

    @Test
    fun `quem quer perder ou ganhar passa pelo ritmo`() {
        for (goal in listOf(GoalType.LOSE, GoalType.GAIN)) {
            assertTrue(
                OnboardingFlow.applies(OnboardingStep.RATE, goal),
                "o ritmo tem de ser perguntado a quem escolhe $goal",
            )

            assertEquals(
                OnboardingStep.RATE,
                OnboardingFlow.next(OnboardingStep.GOAL_WEIGHT, goal),
            )
        }
    }

    @Test
    fun `voltar atras do preview salta o ritmo em manutencao`() {

        assertEquals(
            OnboardingStep.GOAL,
            OnboardingFlow.previous(OnboardingStep.PLAN_PREVIEW, GoalType.MAINTAIN),
        )
        assertEquals(
            OnboardingStep.RATE,
            OnboardingFlow.previous(OnboardingStep.PLAN_PREVIEW, GoalType.LOSE),
        )
    }

    @Test
    fun `o primeiro passo nao tem anterior`() {
        assertNull(OnboardingFlow.previous(OnboardingStep.WELCOME, null))
    }

    @Test
    fun `o ultimo passo nao tem seguinte`() {
        assertNull(OnboardingFlow.next(OnboardingStep.PLAN_PREVIEW, GoalType.LOSE))
    }

    @Test
    fun `sem objetivo escolhido ainda nenhum passo e saltado`() {

        assertTrue(OnboardingStep.entries.all { OnboardingFlow.applies(it, null) })
    }

    @Test
    fun `quem quer perder ou ganhar e perguntado pelo peso-alvo`() {
        for (goal in listOf(GoalType.LOSE, GoalType.GAIN)) {
            assertEquals(
                OnboardingStep.GOAL_WEIGHT,
                OnboardingFlow.next(OnboardingStep.GOAL, goal),
                "o peso-alvo tem de ser perguntado a quem escolhe $goal",
            )
        }
    }

    @Test
    fun `quem quer manter nao e perguntado pelo peso-alvo`() {

        assertTrue(!OnboardingFlow.applies(OnboardingStep.GOAL_WEIGHT, GoalType.MAINTAIN))
        assertEquals(
            OnboardingStep.PLAN_PREVIEW,
            OnboardingFlow.next(OnboardingStep.GOAL, GoalType.MAINTAIN),
        )
    }

    @Test
    fun `o corpo continua a vir antes do peso-alvo`() {

        val passos = OnboardingStep.entries
        assertTrue(
            passos.indexOf(OnboardingStep.BODY) < passos.indexOf(OnboardingStep.GOAL_WEIGHT),
            "o corpo tem de vir antes do peso-alvo",
        )
    }

    @Test
    fun `o onboarding tem nove passos, e nao onze`() {

        assertEquals(9, OnboardingStep.entries.size)
    }

    @Test
    fun `dos nove passos so quatro sao obrigatorios`() {

        // Sexo, nascimento e corpo — que traz altura e peso — mais o primeiro ecrã, que não
        // pergunta nada. É o que a app precisa para calcular o basal; o resto tem omissão.
        val obrigatorios = OnboardingStep.entries.filterNot { OnboardingFlow.canSkip(it) }
        assertEquals(
            listOf(
                OnboardingStep.WELCOME,
                OnboardingStep.SEX,
                OnboardingStep.BIRTH,
                OnboardingStep.BODY,
            ),
            obrigatorios,
            "eram nove passos obrigatórios à frente de quem só queria ver a app",
        )
    }

    @Test
    fun `os cinco que sobram podem ser saltados`() {
        val saltaveis = OnboardingStep.entries.filter { OnboardingFlow.canSkip(it) }
        assertEquals(5, saltaveis.size)
        assertTrue(OnboardingStep.ACTIVITY in saltaveis)
        assertTrue(OnboardingStep.PLAN_PREVIEW in saltaveis)
    }

    @Test
    fun `a barra chega ao fim para todos os objetivos`() {

        for (goal in listOf(GoalType.LOSE, GoalType.MAINTAIN, GoalType.GAIN)) {
            assertEquals(
                1f,
                OnboardingFlow.progress(OnboardingStep.PLAN_PREVIEW, goal),
                "a barra não fecha para $goal",
            )
        }
    }

    @Test
    fun `a barra nunca recua ao avancar`() {
        for (goal in listOf(GoalType.LOSE, GoalType.MAINTAIN, GoalType.GAIN)) {
            var step: OnboardingStep? = OnboardingStep.WELCOME
            var anterior = 0f
            while (step != null) {
                val p = OnboardingFlow.progress(step, goal)
                assertTrue(p > anterior, "a barra recuou em $step ($goal): $p depois de $anterior")
                assertTrue(p <= 1f, "a barra passou de 100% em $step ($goal)")
                anterior = p
                step = OnboardingFlow.next(step, goal)
            }
        }
    }

    @Test
    fun `um passo que ja nao se aplica nao rebenta a barra`() {

        val p = OnboardingFlow.progress(OnboardingStep.RATE, GoalType.MAINTAIN)
        assertTrue(p in 0f..1f, "progresso fora de escala: $p")
    }

    @Test
    fun `andar para a frente e para tras devolve ao mesmo sitio`() {
        for (goal in listOf(GoalType.LOSE, GoalType.MAINTAIN, GoalType.GAIN)) {
            var step = OnboardingStep.WELCOME
            val visitados = mutableListOf(step)
            while (true) {
                step = OnboardingFlow.next(step, goal) ?: break
                visitados += step
            }

            val volta = mutableListOf(step)
            while (true) {
                step = OnboardingFlow.previous(step, goal) ?: break
                volta += step
            }
            assertEquals(visitados, volta.reversed(), "o caminho de volta difere ($goal)")
            assertTrue(
                OnboardingStep.PLAN_PREVIEW in visitados,
                "nenhum objetivo pode saltar o preview do plano ($goal)",
            )
        }
    }
}
