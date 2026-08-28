package pt.antares.app.core.calc

import pt.antares.app.core.database.entities.UserProfileEntity
import pt.antares.app.core.model.ActivityLevel
import pt.antares.app.core.model.BodyFatSource
import pt.antares.app.core.model.GoalType
import pt.antares.app.core.model.MacroStrategy
import pt.antares.app.core.model.Sex
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TargetBreakdownSweepTest {

    private val today = 20639L

    private fun profileOf(
        sex: Sex,
        activity: ActivityLevel,
        rate: Int,
        ageYears: Int,
        heightCm: Int,
        bodyFatPct: Double?,
    ) = UserProfileEntity(
        sex = sex,
        birthEpochDay = today - ageYears * 365L - 100L,
        heightCm = heightCm,
        activityLevel = activity,
        goalType = when {
            rate < 0 -> GoalType.LOSE
            rate > 0 -> GoalType.GAIN
            else -> GoalType.MAINTAIN
        },
        goalRateKcal = rate,
        macroStrategy = MacroStrategy.BALANCED,
        customProteinG = null, customCarbsG = null, customFatG = null,
        bodyFatPct = bodyFatPct,
        bodyFatSource = bodyFatPct?.let { BodyFatSource.MEASURED },
        updatedAt = 0L,
    )

    private data class Case(
        val label: String,
        val profile: UserProfileEntity,
        val weightKg: Double,
    )

    private fun cases(): List<Case> {
        val out = mutableListOf<Case>()

        val weights = listOf(48.0, 55.3, 62.55, 70.0, 80.55, 84.2, 97.75, 120.4, 145.0)
        val heights = listOf(150, 158, 163, 171, 178, 185, 199)
        val ages = listOf(16, 19, 30, 47, 66, 80)
        val rates = listOf(-1000, -750, -550, -250, -110, 0, 110, 250, 400)
        val fats = listOf(null, 8.0, 15.0, 22.5, 31.0, 45.0)

        for (sex in Sex.entries) {
            for (activity in ActivityLevel.entries) {
                for ((i, w) in weights.withIndex()) {
                    val h = heights[i % heights.size]
                    val age = ages[i % ages.size]
                    for (rate in rates) {
                        for (fat in fats) {
                            out += Case(
                                label = "$sex/$activity/w=$w/h=$h/age=$age/rate=$rate/fat=$fat",
                                profile = profileOf(sex, activity, rate, age, h, fat),
                                weightKg = w,
                            )
                        }
                    }
                }
            }
        }
        return out
    }

    private fun eachBreakdown(body: (Case, TargetBreakdown) -> Unit) {
        val all = cases()
        assertTrue(all.size > 2000, "o varrimento encolheu: só ${all.size} casos")
        for (c in all) {
            val targets = NutritionCalc.dailyTargets(c.profile, c.weightKg, today)
            val b = TargetBreakdownCalc.of(c.profile, targets, c.weightKg, today)
            assertTrue(b != null, "sem conta nenhuma para mostrar em ${c.label}")
            body(c, b!!)
        }
    }

    @Test
    fun `a conta acaba sempre exatamente na meta que a app mostra`() {
        eachBreakdown { c, b ->
            val targets = NutritionCalc.dailyTargets(c.profile, c.weightKg, today)
            assertEquals(targets.kcal, b.finalKcal, "meta diferente da conta em ${c.label}")
            assertEquals(targets.kcal, b.steps.last().result, "último passo ≠ meta em ${c.label}")
        }
    }

    @Test
    fun `cada passo comeca onde o anterior acabou`() {
        eachBreakdown { c, b ->
            // Os passos de margem anotam o basal em vez de o transformar: o número deles é
            // o «mais ou menos», e não uma etapa nova da conta. Ver TargetBreakdown.Kind.anota.
            val elos = b.steps.filterNot { it.kind.anota }
            for (i in 1 until elos.size) {
                val anterior = elos[i - 1]
                val entrada = elos[i].values[0]

                val ok = abs(entrada - anterior.exact) < 1e-6 ||
                    entrada.roundToInt() == anterior.result && abs(entrada - anterior.result) < 1e-6
                assertTrue(
                    ok,
                    "passo ${elos[i].kind} entra em $entrada, " +
                        "anterior acabou em ${anterior.exact} — ${c.label}",
                )
            }
        }
    }

    private fun comoAparece(v: Double, casas: Int = 1): Double {
        var escala = 1L
        repeat(casas) { escala *= 10 }
        return (v * escala).roundToLong() / escala.toDouble()
    }

    @Test
    fun `a linha do basal fecha com os termos que mostra`() {
        eachBreakdown { c, b ->
            val passo = b.steps.first()
            when (passo.kind) {
                TargetBreakdown.Kind.BMR_MIFFLIN -> {
                    val (peso, altura, idade, sexo) = passo.values

                    val soma = comoAparece(10 * peso) + comoAparece(6.25 * altura) -
                        comoAparece(5 * idade) + comoAparece(sexo)
                    assertEquals(
                        comoAparece(passo.exact),
                        comoAparece(soma),
                        "a soma escrita no ecrã não dá o basal escrito no ecrã — ${c.label}",
                    )

                    assertTrue(
                        sexo == 5.0 || sexo == -161.0,
                        "constante de sexo $sexo em ${c.label}",
                    )
                }
                TargetBreakdown.Kind.BMR_FROM_LEAN -> {
                    val lean = passo.values[0]

                    assertEquals(comoAparece(lean), lean, "massa magra com casas escondidas — ${c.label}")

                    val katch = comoAparece(370 + 21.6 * lean)
                    val cunningham = comoAparece(500 + 22.0 * lean)
                    assertTrue(
                        abs(passo.exact - katch) < 1e-6 || abs(passo.exact - cunningham) < 1e-6,
                        "basal ${passo.exact} não é nenhuma das fórmulas de massa magra — ${c.label}",
                    )
                }
                else -> assertTrue(false, "o primeiro passo tem de ser um basal — ${c.label}")
            }
        }
    }

    @Test
    fun `a multiplicacao da atividade fecha com os numeros mostrados`() {
        eachBreakdown { c, b ->
            val passo = b.steps.first { it.kind == TargetBreakdown.Kind.ACTIVITY }

            val basal = comoAparece(passo.values[0])
            val mult = comoAparece(passo.values[1], 2)
            assertEquals(
                passo.result,
                (basal * mult).roundToInt(),
                "a multiplicação escrita no ecrã não dá o gasto escrito no ecrã — ${c.label}",
            )
        }
    }

    @Test
    fun `a linha do ritmo e uma soma de inteiros`() {
        eachBreakdown { c, b ->
            val passo = b.steps.first { it.kind == TargetBreakdown.Kind.RATE }
            val gasto = passo.values[0]
            val ritmo = passo.values[1]
            assertEquals(gasto, gasto.roundToInt().toDouble(), "gasto com decimais em ${c.label}")
            assertEquals(ritmo, ritmo.roundToInt().toDouble(), "ritmo com decimais em ${c.label}")
            assertEquals(
                passo.result,
                gasto.roundToInt() + ritmo.roundToInt(),
                "a soma do ritmo não fecha em ${c.label}",
            )
        }
    }

    @Test
    fun `o passo do piso aparece se e so se a conta nao dava a meta`() {
        eachBreakdown { c, b ->
            val rate = b.steps.first { it.kind == TargetBreakdown.Kind.RATE }
            val temPiso = b.steps.any { it.kind == TargetBreakdown.Kind.FLOOR }
            assertEquals(
                rate.result != b.finalKcal,
                temPiso,
                "piso ${if (temPiso) "a mais" else "em falta"} em ${c.label}",
            )
        }
    }

    @Test
    fun `nenhum passo mostra um numero absurdo`() {

        eachBreakdown { c, b ->
            for (passo in b.steps) {
                assertTrue(passo.exact.isFinite(), "${passo.kind} não é finito em ${c.label}")
                for (v in passo.values) {
                    assertTrue(v.isFinite(), "${passo.kind} tem valor não finito em ${c.label}")
                }
            }
            assertTrue(b.finalKcal > 0, "meta não positiva em ${c.label}")
            assertTrue(b.finalKcal < 10_000, "meta absurda (${b.finalKcal}) em ${c.label}")
        }
    }
}
