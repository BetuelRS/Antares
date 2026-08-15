package pt.antares.app.core.calc

import pt.antares.app.core.database.entities.UserProfileEntity
import pt.antares.app.core.model.ActivityLevel
import pt.antares.app.core.model.BodyFatSource
import pt.antares.app.core.model.GoalType
import pt.antares.app.core.model.MacroStrategy
import pt.antares.app.core.model.Sex
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TargetBreakdownTest {

    private val today = 20639L
    private val birth30y = today - 10958L

    private fun profile(
        sex: Sex = Sex.MALE,
        activity: ActivityLevel = ActivityLevel.MODERATE,
        rate: Int = -500,
        bodyFatPct: Double? = null,
    ) = UserProfileEntity(
        sex = sex,
        birthEpochDay = birth30y,
        heightCm = 178,
        activityLevel = activity,
        goalType = if (rate < 0) GoalType.LOSE else GoalType.MAINTAIN,
        goalRateKcal = rate,
        macroStrategy = MacroStrategy.BALANCED,
        customProteinG = null, customCarbsG = null, customFatG = null,
        bodyFatPct = bodyFatPct,
        bodyFatSource = bodyFatPct?.let { BodyFatSource.MEASURED },
        updatedAt = 0L,
    )

    private fun breakdown(p: UserProfileEntity, weightKg: Double = 80.0): TargetBreakdown {
        val t = NutritionCalc.dailyTargets(p, weightKg, today)
        return TargetBreakdownCalc.of(p, t, weightKg, today).also { assertNotNull(it) }!!
    }

    @Test
    fun `o basal do primeiro passo e o mesmo que a meta usou`() {
        val p = profile()
        val t = NutritionCalc.dailyTargets(p, 80.0, today)
        val b = breakdown(p)
        assertEquals(t.energy!!.bmr.roundToInt(), b.steps.first().result)
    }

    @Test
    fun `cada passo comeca onde o anterior acabou`() {

        val b = breakdown(profile())
        for (i in 1 until b.steps.size) {
            val anterior = b.steps[i - 1].result
            val entrada = b.steps[i].values[0].roundToInt()
            assertEquals(
                anterior,
                entrada,
                "passo ${b.steps[i].kind} começa em $entrada mas o anterior acabou em $anterior",
            )
        }
    }

    @Test
    fun `o ultimo passo da exatamente a meta`() {
        val p = profile()
        val t = NutritionCalc.dailyTargets(p, 80.0, today)
        val b = breakdown(p)
        assertEquals(t.kcal, b.finalKcal)
        assertEquals(t.kcal, b.steps.last().result)
    }

    @Test
    fun `a multiplicacao da atividade fecha com duas casas`() {

        val b = breakdown(profile())
        val passo = b.steps.first { it.kind == TargetBreakdown.Kind.ACTIVITY }
        val bmr = passo.values[0]
        val mult = passo.values[1]
        assertEquals(passo.result, (bmr * mult).roundToInt())

        assertEquals(1.45, mult, 0.0001)
    }

    @Test
    fun `o termo do sexo esta no basal e sem ele a soma nao fecha`() {

        val b = breakdown(profile())
        val passo = b.steps.first { it.kind == TargetBreakdown.Kind.BMR_MIFFLIN }
        val peso = (10 * passo.values[0]).roundToInt()
        val altura = (6.25 * passo.values[1]).roundToInt()
        val idade = (5 * passo.values[2]).roundToInt()
        val semSexo = peso + altura - idade
        assertTrue(
            passo.result != semSexo,
            "o termo do sexo tem de existir, senão a página mostra uma soma que não fecha",
        )
        assertEquals(5, passo.result - semSexo)
    }

    @Test
    fun `nas mulheres o termo do sexo e negativo`() {
        val b = breakdown(profile(sex = Sex.FEMALE))
        val passo = b.steps.first { it.kind == TargetBreakdown.Kind.BMR_MIFFLIN }
        val semSexo = (10 * passo.values[0]).roundToInt() +
            (6.25 * passo.values[1]).roundToInt() -
            (5 * passo.values[2]).roundToInt()
        assertEquals(-161, passo.result - semSexo)
    }

    @Test
    fun `com massa magra a conta e a da massa magra`() {
        val b = breakdown(profile(bodyFatPct = 20.0))
        assertEquals(TargetBreakdown.Kind.BMR_FROM_LEAN, b.steps.first().kind)

        assertEquals(1752, b.steps.first().result)
    }

    @Test
    fun `quando um piso trava, isso aparece como passo`() {

        val p = profile(activity = ActivityLevel.SEDENTARY, rate = -1000)
        val b = breakdown(p, weightKg = 120.0)
        assertTrue(b.steps.any { it.kind == TargetBreakdown.Kind.FLOOR })
        assertEquals(b.finalKcal, b.steps.last().result)
    }

    @Test
    fun `sem piso a travar nao aparece passo nenhum de piso`() {
        val b = breakdown(profile())
        assertTrue(b.steps.none { it.kind == TargetBreakdown.Kind.FLOOR })
    }

    @Test
    fun `a margem da fita aparece logo a seguir ao basal`() {
        val p = profile(bodyFatPct = 20.0).copy(bodyFatSource = BodyFatSource.NAVY)
        val b = breakdown(p)

        val posicao = b.steps.indexOfFirst { it.kind == TargetBreakdown.Kind.BMR_UNCERTAIN }
        assertEquals(
            1,
            posicao,
            "vem antes da atividade e do ritmo: quem lê tem de saber que o primeiro " +
                "número já traz erro antes de o ver multiplicado pelos outros",
        )
        assertTrue(b.steps[posicao].values[0] > 0.0)
    }

    @Test
    fun `massa gorda medida a serio nao traz margem nenhuma`() {
        val b = breakdown(profile(bodyFatPct = 20.0))
        assertTrue(b.steps.none { it.kind == TargetBreakdown.Kind.BMR_UNCERTAIN })
    }

    @Test
    fun `a proteina que sobe por causa do treino diz-se na conta`() {
        val p = profile(rate = -600, bodyFatPct = 20.0)
        val t = NutritionCalc.dailyTargets(p, 80.0, today, treinaForca = true)
        val b = TargetBreakdownCalc.of(p, t, 80.0, today)!!

        val passo = b.steps.single { it.kind == TargetBreakdown.Kind.PROTEIN_TRAINED }
        assertEquals(
            t.proteinG,
            passo.result,
            "o passo tem de mostrar a meta que saiu, e não um número parecido",
        )
        assertTrue(
            passo.values[0] > ProteinFloor.UNTRAINED_DEFICIT,
            "a linha existe para explicar uma subida; sem subida não tem nada para dizer",
        )
        assertEquals(t.energy!!.leanMassKg!!, passo.values[1])
    }

    @Test
    fun `quem nao treina ve a conta que sempre viu`() {
        val p = profile(rate = -600, bodyFatPct = 20.0)
        val t = NutritionCalc.dailyTargets(p, 80.0, today)
        val b = TargetBreakdownCalc.of(p, t, 80.0, today)!!

        assertTrue(b.steps.none { it.kind == TargetBreakdown.Kind.PROTEIN_TRAINED })
    }
}
