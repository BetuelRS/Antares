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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NavyUncertaintyTest {

    private val today = 20639L

    private fun perfil(
        bodyFatPct: Double?,
        source: BodyFatSource?,
        formula: BmrFormula? = null,
    ) = UserProfileEntity(
        sex = Sex.MALE,
        birthEpochDay = today - 10958L,
        heightCm = 178,
        activityLevel = ActivityLevel.MODERATE,
        goalType = GoalType.LOSE,
        goalRateKcal = -500,
        macroStrategy = MacroStrategy.BALANCED,
        customProteinG = null, customCarbsG = null, customFatG = null,
        bodyFatPct = bodyFatPct,
        bodyFatSource = source,
        bmrFormulaOverride = formula,
        updatedAt = 0L,
    )

    @Test
    fun `a fita da o mais ou menos que a conta manda`() {
        // 3,6 pp sobre 80 kg são 2,88 kg de massa magra, e a Katch-McArdle converte cada
        // quilo em 21,6 kcal.
        val e = NutritionCalc.energy(perfil(20.0, BodyFatSource.NAVY), weightKg = 80.0, todayEpochDay = today)

        assertEquals(62, e.bmrUncertaintyKcal?.roundToInt())
    }

    @Test
    fun `uma medicao a serio nao leva o erro da fita`() {
        val e = NutritionCalc.energy(
            perfil(20.0, BodyFatSource.MEASURED),
            weightKg = 80.0,
            todayEpochDay = today,
        )
        assertNull(
            e.bmrUncertaintyKcal,
            "o erro é do método das circunferências; colá-lo a uma absorciometria era " +
                "inventar imprecisão que não existe",
        )
    }

    @Test
    fun `sem massa gorda utilizavel nao ha intervalo nenhum`() {
        val semNada = NutritionCalc.energy(perfil(null, null), 80.0, today)
        assertNull(semNada.bmrUncertaintyKcal)

        // Pelo IMC a massa magra é recusada antes de chegar à Katch-McArdle, e a Mifflin
        // sai do peso e da altura — que a fita não mede.
        val peloImc = NutritionCalc.energy(perfil(20.0, BodyFatSource.BMI), 80.0, today)
        assertNull(peloImc.bmrUncertaintyKcal)
        assertEquals(BmrFormula.MIFFLIN_ST_JEOR, peloImc.formula)
    }

    @Test
    fun `a Cunningham tem o seu proprio intervalo`() {
        val e = NutritionCalc.energy(
            perfil(20.0, BodyFatSource.NAVY, formula = BmrFormula.CUNNINGHAM),
            weightKg = 80.0,
            todayEpochDay = today,
        )
        assertEquals(BmrFormula.CUNNINGHAM, e.formula)
        assertEquals(63, e.bmrUncertaintyKcal?.roundToInt(), "22 kcal por quilo em vez de 21,6")
    }

    @Test
    fun `quem pesa mais tem mais incerteza em kcal`() {
        val leve = NutritionCalc.energy(perfil(20.0, BodyFatSource.NAVY), 55.0, today)
        val pesado = NutritionCalc.energy(perfil(20.0, BodyFatSource.NAVY), 110.0, today)

        assertTrue(
            pesado.bmrUncertaintyKcal!! > leve.bmrUncertaintyKcal!!,
            "os mesmos pontos percentuais valem mais quilos em quem pesa mais",
        )
    }

    @Test
    fun `o intervalo acompanha a formula em vez de a repetir`() {
        // Se alguém mudar o coeficiente da Katch-McArdle, isto muda com ele — que é a razão
        // de o intervalo sair da própria função e não de uma constante copiada.
        val delta = 80.0 * NavyUncertainty.BODY_FAT_STANDARD_ERROR_PP / 100.0
        val esperado = NutritionCalc.bmrKatchMcArdle(delta) - NutritionCalc.bmrKatchMcArdle(0.0)

        assertEquals(esperado, NavyUncertainty.bmrKcal(BmrFormula.KATCH_MCARDLE, 80.0))
    }
}
