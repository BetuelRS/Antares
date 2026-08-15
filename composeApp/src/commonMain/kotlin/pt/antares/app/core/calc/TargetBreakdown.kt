package pt.antares.app.core.calc

import pt.antares.app.core.database.entities.UserProfileEntity
import kotlin.math.roundToInt

/**
 * A conta da meta calórica passo a passo, para o ecrã a poder mostrar em vez de a
 * afirmar. Não recalcula nada: lê os [Targets] já produzidos e desmonta-os.
 */
data class TargetBreakdown(
    val steps: List<Step>,
    val finalKcal: Int,
) {

    // `values` são os números que entram na linha e `exact` o que sai; o texto vive nos
    // recursos de tradução, por isso aqui não há frases nem símbolos.
    data class Step(
        val kind: Kind,
        val values: List<Double>,
        val exact: Double,
        val result: Int = exact.roundToInt(),
    )

    enum class Kind {

        BMR_FROM_LEAN,

        BMR_MIFFLIN,

        ACTIVITY,

        RATE,

        FLOOR,

        PROTEIN_TRAINED,
    }
}

object TargetBreakdownCalc {

    fun of(
        profile: UserProfileEntity,
        targets: Targets,
        weightKg: Double,
        todayEpochDay: Long,
    ): TargetBreakdown? {
        // Sem estimativa de energia não há conta para explicar — acontece nos macros
        // manuais, onde os números são do utilizador e não derivam de nada.
        val e = targets.energy ?: return null
        val steps = mutableListOf<TargetBreakdown.Step>()

        val lean = e.leanMassKg
        if (lean != null) {
            steps += TargetBreakdown.Step(
                kind = TargetBreakdown.Kind.BMR_FROM_LEAN,
                values = listOf(lean),
                exact = e.bmr,
            )
        } else {
            val age = NutritionCalc.ageYears(profile.birthEpochDay, todayEpochDay)

            steps += TargetBreakdown.Step(
                kind = TargetBreakdown.Kind.BMR_MIFFLIN,
                values = listOf(
                    weightKg,
                    profile.heightCm.toDouble(),
                    age.toDouble(),
                    NutritionCalc.mifflinSexTerm(profile.sex),
                ),
                exact = e.bmr,
            )
        }

        steps += TargetBreakdown.Step(
            kind = TargetBreakdown.Kind.ACTIVITY,
            values = listOf(e.bmr, profile.activityLevel.multiplier),
            exact = e.tdee,
        )

        // Parte-se do gasto já arredondado, e não do valor exato, para que as parcelas
        // somem no ecrã: a pessoa consegue refazer a conta de cabeça.
        val tdeeShown = e.tdee.roundToInt()
        val afterRate = tdeeShown + profile.goalRateKcal
        steps += TargetBreakdown.Step(
            kind = TargetBreakdown.Kind.RATE,
            values = listOf(tdeeShown.toDouble(), profile.goalRateKcal.toDouble()),
            exact = afterRate.toDouble(),
        )

        // O passo do chão só aparece quando mudou alguma coisa; caso contrário seria uma
        // linha a dizer que o número ficou igual. Também apanha a diferença de
        // arredondamento entre o gasto exato e o mostrado, e nesse caso a linha é honesta.
        if (afterRate != targets.kcal) {
            steps += TargetBreakdown.Step(
                kind = TargetBreakdown.Kind.FLOOR,
                values = listOf(afterRate.toDouble()),
                exact = targets.kcal.toDouble(),
            )
        }

        // A proteína não sai das calorias, e por isso vem no fim e não na cadeia. Só aparece
        // quando subiu por causa do treino: quem não treina vê a conta que sempre viu.
        if (TargetWarning.PROTEIN_FLOOR_TRAINED in targets.warnings) {
            val lean = e.leanMassKg ?: 0.0
            val perKg = ProteinFloor.perKgLean(
                treinaForca = true,
                deficitFraction = ProteinFloor.deficitFraction(profile.goalRateKcal, e.tdee),
            )
            steps += TargetBreakdown.Step(
                kind = TargetBreakdown.Kind.PROTEIN_TRAINED,
                values = listOf(perKg, lean),
                exact = targets.proteinG.toDouble(),
            )
        }

        return TargetBreakdown(steps = steps, finalKcal = targets.kcal)
    }
}
