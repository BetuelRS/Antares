package pt.antares.app.feature.onboarding

import pt.antares.app.core.model.GoalType

object OnboardingFlow {

    /**
     * O que a app não consegue inventar. Sem sexo, data de nascimento, altura e peso não há
     * metabolismo basal nenhum para calcular, e todo o resto da app fica a mostrar zeros.
     *
     * Tudo o resto tem um valor por omissão defensável, e por isso pode ser saltado: eram
     * nove passos obrigatórios à frente de quem só queria ver a app.
     */
    val OBRIGATORIOS = setOf(OnboardingStep.SEX, OnboardingStep.BIRTH, OnboardingStep.BODY)

    /** O primeiro ecrã não faz pergunta nenhuma — saltá-lo é continuar. */
    fun canSkip(step: OnboardingStep): Boolean =
        step != OnboardingStep.WELCOME && step !in OBRIGATORIOS

    fun applies(step: OnboardingStep, goalType: GoalType?): Boolean = when (step) {
        OnboardingStep.RATE, OnboardingStep.GOAL_WEIGHT -> goalType != GoalType.MAINTAIN
        else -> true
    }

    fun steps(goalType: GoalType?): List<OnboardingStep> =
        OnboardingStep.entries.filter { applies(it, goalType) }

    fun next(from: OnboardingStep, goalType: GoalType?): OnboardingStep? =
        OnboardingStep.entries
            .drop(OnboardingStep.entries.indexOf(from) + 1)
            .firstOrNull { applies(it, goalType) }

    fun previous(from: OnboardingStep, goalType: GoalType?): OnboardingStep? =
        OnboardingStep.entries
            .take(OnboardingStep.entries.indexOf(from))
            .lastOrNull { applies(it, goalType) }

    fun progress(step: OnboardingStep, goalType: GoalType?): Float {
        val visiveis = steps(goalType)
        val posicao = visiveis.indexOf(step)

        if (posicao < 0 || visiveis.isEmpty()) return 0f
        return (posicao + 1f) / visiveis.size
    }
}
