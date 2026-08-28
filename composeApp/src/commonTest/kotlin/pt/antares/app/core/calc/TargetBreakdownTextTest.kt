package pt.antares.app.core.calc

import pt.antares.app.core.database.entities.UserProfileEntity
import pt.antares.app.core.model.ActivityLevel
import pt.antares.app.core.model.BodyFatSource
import pt.antares.app.core.model.GoalType
import pt.antares.app.core.model.MacroStrategy
import pt.antares.app.core.model.Sex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TargetBreakdownTextTest {

    private val today = 20639L

    private fun profile(
        sex: Sex = Sex.MALE,
        activity: ActivityLevel = ActivityLevel.MODERATE,
        rate: Int = -110,
        bodyFatPct: Double? = null,
        ageYears: Int = 30,
        heightCm: Int = 178,
    ) = UserProfileEntity(
        sex = sex,
        birthEpochDay = today - ageYears * 365L - 8L,
        heightCm = heightCm,
        activityLevel = activity,
        goalType = if (rate < 0) GoalType.LOSE else GoalType.MAINTAIN,
        goalRateKcal = rate,
        macroStrategy = MacroStrategy.BALANCED,
        customProteinG = null, customCarbsG = null, customFatG = null,
        bodyFatPct = bodyFatPct,
        bodyFatSource = bodyFatPct?.let { BodyFatSource.MEASURED },
        updatedAt = 0L,
    )

    private fun steps(p: UserProfileEntity, weightKg: Double): List<TargetBreakdown.Step> {
        val t = NutritionCalc.dailyTargets(p, weightKg, today)
        return TargetBreakdownCalc.of(p, t, weightKg, today)!!.steps
    }

    private fun argsOf(
        p: UserProfileEntity,
        weightKg: Double,
        kind: TargetBreakdown.Kind,
        comma: Boolean,
    ): List<String> =
        TargetBreakdownText.args(steps(p, weightKg).first { it.kind == kind }, comma)

    @Test
    fun `a linha da massa magra e a que o emulador mostrou`() {

        val p = profile(bodyFatPct = 15.625)
        val a = argsOf(p, 80.0, TargetBreakdown.Kind.BMR_FROM_LEAN, comma = true)
        assertEquals(listOf("67,5", "21,6", "370", "1828"), a)
    }

    @Test
    fun `em ingles a mesma linha leva ponto`() {

        val p = profile(bodyFatPct = 15.625)
        val a = argsOf(p, 80.0, TargetBreakdown.Kind.BMR_FROM_LEAN, comma = false)
        assertEquals(listOf("67.5", "21.6", "370", "1828"), a)
    }

    @Test
    fun `a multiplicacao da atividade leva duas casas`() {
        val p = profile(bodyFatPct = 15.625)
        val a = argsOf(p, 80.0, TargetBreakdown.Kind.ACTIVITY, comma = true)

        assertEquals(listOf("1828", "1,45", "2651"), a)
        assertTrue("1,5" !in a, "o multiplicador arredondado a uma casa faz a linha mentir")
    }

    @Test
    fun `a linha do ritmo mostra o sinal solto`() {
        val p = profile(bodyFatPct = 15.625)
        val a = argsOf(p, 80.0, TargetBreakdown.Kind.RATE, comma = true)
        assertEquals(listOf("2651", "− 110", "2541"), a)
    }

    @Test
    fun `o termo do sexo do homem e mais cinco, escrito`() {
        val a = argsOf(profile(), 80.0, TargetBreakdown.Kind.BMR_MIFFLIN, comma = true)

        assertEquals(listOf("800", "1112,5", "150", "+ 5", "1767,5"), a)
    }

    @Test
    fun `o termo do sexo da mulher e menos cento e sessenta e um`() {
        val a = argsOf(profile(sex = Sex.FEMALE), 80.0, TargetBreakdown.Kind.BMR_MIFFLIN, comma = true)
        assertEquals("− 161", a[3])
    }

    @Test
    fun `com 80 virgula 55 kg o termo do sexo continua a ser cinco`() {

        val a = argsOf(profile(), 80.55, TargetBreakdown.Kind.BMR_MIFFLIN, comma = true)
        assertEquals("+ 5", a[3])
        assertEquals("805,5", a[0])
    }

    @Test
    fun `o menos e o sinal tipografico e nao o hifen`() {

        val a = argsOf(profile(bodyFatPct = 15.625), 80.0, TargetBreakdown.Kind.RATE, comma = true)
        assertTrue(a[1].startsWith("−"), "esperava o sinal de menos U+2212, veio '${a[1]}'")
        assertTrue('-' !in a[1], "veio um hífen do teclado em vez do sinal de menos")
    }

    @Test
    fun `em portugues nenhum numero sai com ponto decimal`() {
        forEachStep { step ->
            for (arg in TargetBreakdownText.args(step, comma = true)) {
                assertTrue(
                    '.' !in arg,
                    "'$arg' saiu com ponto decimal em português (${step.kind})",
                )
            }
        }
    }

    @Test
    fun `nas outras linguas nenhum numero sai com virgula`() {
        forEachStep { step ->
            for (arg in TargetBreakdownText.args(step, comma = false)) {
                assertTrue(
                    ',' !in arg,
                    "'$arg' saiu com vírgula fora do português (${step.kind})",
                )
            }
        }
    }

    @Test
    fun `nenhuma linha fica sem os argumentos que a frase espera`() {

        val esperados = mapOf(
            TargetBreakdown.Kind.BMR_FROM_LEAN to 4,
            TargetBreakdown.Kind.BMR_MIFFLIN to 5,
            TargetBreakdown.Kind.ACTIVITY to 3,
            TargetBreakdown.Kind.RATE to 3,
            TargetBreakdown.Kind.FLOOR to 0,
            // Os dois passos de margem: o basal e o «mais ou menos» dele.
            TargetBreakdown.Kind.BMR_UNCERTAIN to 2,
            TargetBreakdown.Kind.BMR_MIFFLIN_INCERTO to 2,
        )
        forEachStep { step ->
            assertEquals(
                esperados[step.kind],
                TargetBreakdownText.args(step, comma = true).size,
                "número de argumentos errado em ${step.kind}",
            )
        }
    }

    private fun forEachStep(body: (TargetBreakdown.Step) -> Unit) {
        val pesos = listOf(48.0, 62.55, 80.55, 97.75, 145.0)
        val gorduras = listOf(null, 8.0, 22.5, 45.0)
        var vistos = 0
        for (sex in Sex.entries) {
            for (activity in ActivityLevel.entries) {
                for (peso in pesos) {
                    for (gordura in gorduras) {
                        for (rate in listOf(-1000, -110, 0, 250)) {
                            val p = profile(sex = sex, activity = activity, rate = rate, bodyFatPct = gordura)
                            for (step in steps(p, peso)) {
                                body(step)
                                vistos++
                            }
                        }
                    }
                }
            }
        }
        assertTrue(vistos > 1000, "o varrimento encolheu: só $vistos passos")
    }
}
