package pt.antares.app.feature.onboarding

import pt.antares.app.core.model.GoalType

object OnboardingFlow {

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
