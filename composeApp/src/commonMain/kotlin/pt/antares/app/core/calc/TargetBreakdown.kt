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

        BMR_UNCERTAIN,

        /** O mesmo, para o basal que não passa pela massa magra. A margem é outra. */
        BMR_MIFFLIN_INCERTO,

        ;

        /**
         * Se este passo **anota** o número anterior em vez de o transformar.
         *
         * A cadeia da conta é uma corrente: cada passo entra com o resultado do anterior.
         * Os dois passos de margem não entram nela — trazem o «mais ou menos» do basal, e o
         * número deles é a margem, não uma etapa nova do cálculo.
         *
         * Está declarado aqui e não numa lista dentro dos testes porque é uma propriedade
         * do passo: quem acrescentar um terceiro tipo de anotação tem de a marcar, e o
         * teste da corrente apanha-o se se esquecer.
         */
        val anota: Boolean
            get() = this == BMR_UNCERTAIN || this == BMR_MIFFLIN_INCERTO
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
            // Logo a seguir ao basal e não no fim: quem lê a conta tem de saber que o
            // primeiro número já traz erro antes de o ver multiplicado pelos outros.
            e.bmrUncertaintyKcal?.let { erro ->
                steps += TargetBreakdown.Step(
                    kind = TargetBreakdown.Kind.BMR_UNCERTAIN,
                    values = listOf(erro),
                    exact = e.bmr,
                )
            }
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

            // Pela mesma razão do ramo de cima, e é a metade que faltava: a app declarava a
            // margem da estimativa boa e calava-se sobre a má. Quem nunca mediu a massa
            // gorda — a maioria — via o número mais incerto da app sem margem nenhuma.
            MifflinUncertainty.bmrKcal(BmrFormula.MIFFLIN_ST_JEOR, e.bmr)?.let { erro ->
                steps += TargetBreakdown.Step(
                    kind = TargetBreakdown.Kind.BMR_MIFFLIN_INCERTO,
                    values = listOf(erro),
                    exact = e.bmr,
                )
            }
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
