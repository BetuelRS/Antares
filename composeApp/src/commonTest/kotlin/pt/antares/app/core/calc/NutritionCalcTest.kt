package pt.antares.app.core.calc

import pt.antares.app.core.database.entities.UserProfileEntity
import pt.antares.app.core.model.ActivityLevel
import pt.antares.app.core.model.BodyFatSource
import pt.antares.app.core.model.LifeStage
import pt.antares.app.core.model.GoalType
import pt.antares.app.core.model.MacroStrategy
import pt.antares.app.core.model.Sex
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NutritionCalcTest {

    private val today = 20639L

    private val birth30y = today - 10958L

    private fun profile(
        sex: Sex = Sex.MALE,
        heightCm: Int = 178,
        activity: ActivityLevel = ActivityLevel.MODERATE,
        goal: GoalType = GoalType.MAINTAIN,
        rate: Int = 0,
        strategy: MacroStrategy = MacroStrategy.BALANCED,
        customP: Int? = null,
        customC: Int? = null,
        customF: Int? = null,
        bodyFatPct: Double? = null,
        bodyFatSource: BodyFatSource? = null,
        goalWeightKg: Double? = null,
        birthEpochDay: Long = birth30y,
        lifeStage: LifeStage = LifeStage.NONE,
    ) = UserProfileEntity(
        sex = sex,
        birthEpochDay = birthEpochDay,
        heightCm = heightCm,
        activityLevel = activity,
        goalType = goal,
        goalRateKcal = rate,
        macroStrategy = strategy,
        customProteinG = customP,
        customCarbsG = customC,
        customFatG = customF,
        bodyFatPct = bodyFatPct,
        bodyFatSource = bodyFatSource,
        goalWeightKg = goalWeightKg,
        lifeStage = lifeStage,
        updatedAt = 0L,
    )

    @Test
    fun `fixture1 BMR homem 80kg 178cm 30anos`() {

        assertEquals(1767.5, NutritionCalc.bmr(Sex.MALE, 80.0, 178, 30), 0.01)
    }

    @Test
    fun `fixture2 BMR mulher 60kg 165cm 25anos`() {

        assertEquals(1345.25, NutritionCalc.bmr(Sex.FEMALE, 60.0, 165, 25), 0.01)
    }

    @Test
    fun `fixture3 TDEE = BMR x multiplicador`() {
        assertEquals(2562.875, NutritionCalc.tdee(1767.5, ActivityLevel.MODERATE.multiplier), 0.01)
    }

    @Test
    fun `os multiplicadores de atividade sao os de NEAT`() {
        assertEquals(1.20, ActivityLevel.SEDENTARY.multiplier, 0.001)
        assertEquals(1.30, ActivityLevel.LIGHT.multiplier, 0.001)
        assertEquals(1.45, ActivityLevel.MODERATE.multiplier, 0.001)
        assertEquals(1.60, ActivityLevel.HIGH.multiplier, 0.001)

        assertEquals(1.70, ActivityLevel.ATHLETE.multiplier, 0.001)

        for (level in ActivityLevel.entries) {
            assertTrue(
                level.multiplier <= level.legacyMultiplier,
                "$level: NEAT (${level.multiplier}) não pode exceder o clássico (${level.legacyMultiplier})",
            )
        }
    }

    @Test
    fun `fixture4 homem manutencao balanced`() {
        val t = NutritionCalc.dailyTargets(profile(), weightKg = 80.0, todayEpochDay = today)
        val expectedKcal = (1767.5 * ActivityLevel.MODERATE.multiplier).roundToInt()
        assertEquals(expectedKcal, t.kcal)
        assertEquals((1.8 * 80).roundToInt(), t.proteinG)
        assertEquals((0.8 * 80).roundToInt(), t.fatG)

        val sum = t.proteinG * 4 + t.carbsG * 4 + t.fatG * 9
        assertTrue(abs(sum - t.kcal) <= 4, "sum=$sum kcal=${t.kcal}")
        assertTrue(t.warnings.isEmpty())
    }

    @Test
    fun `fixture5 lose usa proteina 2g por kg`() {
        val t = NutritionCalc.dailyTargets(
            profile(goal = GoalType.LOSE, rate = -500),
            weightKg = 80.0,
            todayEpochDay = today,
        )
        assertEquals(160, t.proteinG)
        assertEquals((1767.5 * ActivityLevel.MODERATE.multiplier - 500).roundToInt(), t.kcal)
    }

    @Test
    fun `fixture6 piso mulher 1200 clampa e avisa`() {

        val p = profile(
            sex = Sex.FEMALE, heightCm = 155,
            activity = ActivityLevel.SEDENTARY,
            goal = GoalType.LOSE, rate = -750,
        )
        val t = NutritionCalc.dailyTargets(p, weightKg = 48.0, todayEpochDay = today)
        assertEquals(1200, t.kcal)
        assertTrue(TargetWarning.FLOOR_CLAMPED in t.warnings)
    }

    @Test
    fun `fixture7 piso homem 1500 clampa e avisa`() {
        val p = profile(
            heightCm = 160, activity = ActivityLevel.SEDENTARY,
            goal = GoalType.LOSE, rate = -1000,
        )
        val t = NutritionCalc.dailyTargets(p, weightKg = 55.0, todayEpochDay = today)
        assertEquals(1500, t.kcal)
        assertTrue(TargetWarning.FLOOR_CLAMPED in t.warnings)
    }

    @Test
    fun `fixture8 high protein usa 2_2g por kg`() {
        val t = NutritionCalc.dailyTargets(
            profile(strategy = MacroStrategy.HIGH_PROTEIN),
            weightKg = 80.0, todayEpochDay = today,
        )
        assertEquals((2.2 * 80).roundToInt(), t.proteinG)
    }

    @Test
    fun `fixture9 keto fixa 25g hidratos e enche gordura`() {
        val t = NutritionCalc.dailyTargets(
            profile(strategy = MacroStrategy.KETO),
            weightKg = 80.0, todayEpochDay = today,
        )
        assertEquals(25, t.carbsG)
        val sum = t.proteinG * 4 + t.carbsG * 4 + t.fatG * 9
        assertTrue(abs(sum - t.kcal) <= 8, "sum=$sum kcal=${t.kcal}")
    }

    @Test
    fun `fixture10 low carb gordura 40 por cento`() {
        val t = NutritionCalc.dailyTargets(
            profile(strategy = MacroStrategy.LOW_CARB),
            weightKg = 80.0, todayEpochDay = today,
        )
        assertEquals((t.kcal * 0.40 / 9).roundToInt(), t.fatG)
    }

    @Test
    fun `fixture11 custom devolve gramas do perfil`() {
        val t = NutritionCalc.dailyTargets(
            profile(strategy = MacroStrategy.CUSTOM, customP = 150, customC = 300, customF = 70),
            weightKg = 80.0, todayEpochDay = today,
        )
        assertEquals(150, t.proteinG)
        assertEquals(300, t.carbsG)
        assertEquals(70, t.fatG)
    }

    @Test
    fun `fixture12 carbs clampam a zero com aviso quando P mais F excedem kcal`() {

        val p = profile(
            sex = Sex.FEMALE, heightCm = 150,
            activity = ActivityLevel.SEDENTARY,
            goal = GoalType.LOSE, rate = -1000,
            strategy = MacroStrategy.HIGH_PROTEIN,
        )
        val t = NutritionCalc.dailyTargets(p, weightKg = 120.0, todayEpochDay = today)
        assertEquals(0, t.carbsG)
        assertTrue(TargetWarning.CARBS_CLAMPED_TO_ZERO in t.warnings)
    }

    @Test
    fun `fixture13 idade nao vira no ano civil`() {

        val birth = today - (15.99 * 365.2425).toInt()
        assertEquals(15, NutritionCalc.ageYears(birth, today))
        val birth16 = today - (16.01 * 365.2425).toInt()
        assertEquals(16, NutritionCalc.ageYears(birth16, today))
    }

    @Test
    fun `Katch-McArdle e 370 mais 21_6 por kg de massa magra`() {

        assertEquals(1838.8, NutritionCalc.bmrKatchMcArdle(68.0), 0.01)
        assertEquals(370.0, NutritionCalc.bmrKatchMcArdle(0.0), 0.01)
    }

    @Test
    fun `com percentagem de gordura medida usa Katch-McArdle`() {
        val p = profile(bodyFatPct = 15.0, bodyFatSource = BodyFatSource.MEASURED)
        val e = NutritionCalc.energy(p, weightKg = 80.0, todayEpochDay = today)
        assertEquals(BmrFormula.KATCH_MCARDLE, e.formula)
        assertEquals(68.0, e.leanMassKg!!, 0.001)
        assertEquals(1838.8, e.bmr, 0.01)
        assertEquals(1838.8 * ActivityLevel.MODERATE.multiplier, e.tdee, 0.01)
    }

    @Test
    fun `sem percentagem de gordura usa Mifflin`() {
        val e = NutritionCalc.energy(profile(), weightKg = 80.0, todayEpochDay = today)
        assertEquals(BmrFormula.MIFFLIN_ST_JEOR, e.formula)
        assertEquals(1767.5, e.bmr, 0.01)
        assertNull(e.leanMassKg)
        assertNull(e.bodyFatSource)
    }

    @Test
    fun `a estimativa por IMC nao entra no calculo do BMR`() {

        val p = profile(bodyFatPct = 24.0, bodyFatSource = BodyFatSource.BMI)
        val e = NutritionCalc.energy(p, weightKg = 80.0, todayEpochDay = today)
        assertEquals(BmrFormula.MIFFLIN_ST_JEOR, e.formula)
        assertNull(e.leanMassKg)
    }

    @Test
    fun `a estimativa por medidas ja entra`() {

        val p = profile(bodyFatPct = 18.0, bodyFatSource = BodyFatSource.NAVY)
        val e = NutritionCalc.energy(p, weightKg = 80.0, todayEpochDay = today)
        assertEquals(BmrFormula.KATCH_MCARDLE, e.formula)
        assertEquals(BodyFatSource.NAVY, e.bodyFatSource)
    }

    @Test
    fun `proteina segue a massa magra e nao o peso total`() {

        val p = profile(
            goal = GoalType.LOSE, rate = -500,
            bodyFatPct = 40.0, bodyFatSource = BodyFatSource.MEASURED,
        )
        val t = NutritionCalc.dailyTargets(p, weightKg = 120.0, todayEpochDay = today)
        assertEquals(173, t.proteinG)
        assertTrue(t.proteinG < (2.0 * 120).roundToInt())
    }

    @Test
    fun `para composicao comum a proteina fica onde sempre esteve`() {

        val p = profile(bodyFatPct = 20.0, bodyFatSource = BodyFatSource.MEASURED)
        val t = NutritionCalc.dailyTargets(p, weightKg = 80.0, todayEpochDay = today)
        assertTrue(abs(t.proteinG - 144) <= 5, "deu ${t.proteinG}, esperava perto de 144")
    }

    @Test
    fun `o piso de proteina nunca desce abaixo do PRI da EFSA`() {

        val floor = NutritionCalc.proteinFloorG(weightKg = 100.0, leanMassKg = 40.0, inDeficit = false)
        assertTrue(floor >= (0.83 * 100).roundToInt(), "piso $floor abaixo do PRI")
    }

    @Test
    fun `o piso de proteina sobe em defice`() {
        val manutencao = NutritionCalc.proteinFloorG(80.0, leanMassKg = 64.0, inDeficit = false)
        val defice = NutritionCalc.proteinFloorG(80.0, leanMassKg = 64.0, inDeficit = true)
        assertTrue(defice > manutencao, "défice=$defice manutenção=$manutencao")
    }

    @Test
    fun `proteina custom baixa avisa mas nao e corrigida`() {

        val p = profile(
            goal = GoalType.LOSE, rate = -500,
            strategy = MacroStrategy.CUSTOM, customP = 40, customC = 200, customF = 60,
        )
        val t = NutritionCalc.dailyTargets(p, weightKg = 80.0, todayEpochDay = today)
        assertEquals(40, t.proteinG)
        assertTrue(TargetWarning.PROTEIN_BELOW_FLOOR in t.warnings)
    }

    @Test
    fun `meio quilo por semana sao 550 kcal por dia`() {

        assertEquals(550, NutritionCalc.kcalPerDayFromWeeklyKg(0.5))
        assertEquals(-550, NutritionCalc.kcalPerDayFromWeeklyKg(-0.5))
    }

    @Test
    fun `a conversao de ritmo fecha nos dois sentidos`() {
        val kcal = NutritionCalc.kcalPerDayFromWeeklyKg(0.75)
        assertEquals(0.75, NutritionCalc.weeklyKgFromKcalPerDay(kcal), 0.01)
    }

    @Test
    fun `a zona segura depende do peso e nao de um numero fixo`() {

        assertTrue(NutritionCalc.isRateAboveSafeZone(GoalType.LOSE, -1000, weightKg = 80.0))
        assertFalse(NutritionCalc.isRateAboveSafeZone(GoalType.LOSE, -1000, weightKg = 120.0))
        assertFalse(NutritionCalc.isRateAboveSafeZone(GoalType.LOSE, -500, weightKg = 80.0))
    }

    @Test
    fun `manter peso nunca e rapido demais`() {
        assertFalse(NutritionCalc.isRateAboveSafeZone(GoalType.MAINTAIN, 0, weightKg = 80.0))
    }

    @Test
    fun `ganhar tem janela mais estreita que perder`() {

        assertFalse(NutritionCalc.isRateAboveSafeZone(GoalType.LOSE, -500, 80.0))
        assertTrue(NutritionCalc.isRateAboveSafeZone(GoalType.GAIN, 500, 80.0))
    }

    @Test
    fun `ritmo acima da zona segura avisa nas metas`() {
        val t = NutritionCalc.dailyTargets(
            profile(goal = GoalType.LOSE, rate = -1000),
            weightKg = 80.0, todayEpochDay = today,
        )
        assertTrue(TargetWarning.RATE_ABOVE_SAFE_ZONE in t.warnings)
    }

    @Test
    fun `o piso relativo ao basal trava onde o piso fixo deixava passar`() {

        val p = profile(
            heightCm = 180, activity = ActivityLevel.SEDENTARY,
            goal = GoalType.LOSE, rate = -1000,
        )
        val t = NutritionCalc.dailyTargets(p, weightKg = 120.0, todayEpochDay = today)
        assertEquals(1744, t.kcal)
        assertTrue(TargetWarning.BMR_FLOOR_CLAMPED in t.warnings)
        assertTrue(TargetWarning.FLOOR_CLAMPED !in t.warnings, "travou o fixo, não o relativo")
    }

    @Test
    fun `quando o piso fixo e o mais alto e ele que aparece no aviso`() {

        val p = profile(
            heightCm = 160, activity = ActivityLevel.SEDENTARY,
            goal = GoalType.LOSE, rate = -1000,
        )
        val t = NutritionCalc.dailyTargets(p, weightKg = 55.0, todayEpochDay = today)
        assertTrue(TargetWarning.FLOOR_CLAMPED in t.warnings)
        assertTrue(TargetWarning.BMR_FLOOR_CLAMPED !in t.warnings)
    }

    @Test
    fun `as metas trazem de onde vieram`() {
        val t = NutritionCalc.dailyTargets(profile(), weightKg = 80.0, todayEpochDay = today)
        val e = t.energy!!
        assertEquals(BmrFormula.MIFFLIN_ST_JEOR, e.formula)
        assertEquals(1767.5, e.bmr, 0.01)

        assertEquals(t.kcal, (e.tdee + 0).roundToInt())
    }

    @Test
    fun `chegar ao peso-alvo em defice levanta aviso`() {

        val p = profile(goal = GoalType.LOSE, rate = -500, goalWeightKg = 74.0)
        val t = NutritionCalc.dailyTargets(p, weightKg = 74.1, todayEpochDay = today)
        assertTrue(TargetWarning.GOAL_WEIGHT_REACHED in t.warnings)
    }

    @Test
    fun `longe do alvo nao ha aviso nenhum`() {
        val p = profile(goal = GoalType.LOSE, rate = -500, goalWeightKg = 74.0)
        val t = NutritionCalc.dailyTargets(p, weightKg = 80.0, todayEpochDay = today)
        assertTrue(TargetWarning.GOAL_WEIGHT_REACHED !in t.warnings)
    }

    @Test
    fun `em manutencao chegar ao alvo nao avisa nada`() {

        val p = profile(goal = GoalType.MAINTAIN, rate = 0, goalWeightKg = 74.0)
        val t = NutritionCalc.dailyTargets(p, weightKg = 74.0, todayEpochDay = today)
        assertTrue(TargetWarning.GOAL_WEIGHT_REACHED !in t.warnings)
    }

    @Test
    fun `sem peso-alvo nunca se considera atingido`() {
        assertFalse(NutritionCalc.hasReachedGoalWeight(null, 80.0))
    }

    @Test
    fun `o piso de proteina sobe aos 65`() {

        val adulto = NutritionCalc.proteinFloorG(70.0, leanMassKg = null, inDeficit = false, ageYears = 30)
        val idoso = NutritionCalc.proteinFloorG(70.0, leanMassKg = null, inDeficit = false, ageYears = 70)
        assertEquals((0.83 * 70).roundToInt(), adulto)
        assertEquals(70, idoso)
        assertTrue(idoso > adulto)
    }

    @Test
    fun `a fronteira dos 65 e inclusiva`() {
        val aos64 = NutritionCalc.proteinFloorG(70.0, null, inDeficit = false, ageYears = 64)
        val aos65 = NutritionCalc.proteinFloorG(70.0, null, inDeficit = false, ageYears = 65)
        assertTrue(aos65 > aos64)
    }

    @Test
    fun `sem idade conhecida usa o valor de adulto`() {

        assertEquals(
            (0.83 * 70).roundToInt(),
            NutritionCalc.proteinFloorG(70.0, null, inDeficit = false, ageYears = null),
        )
    }

    @Test
    fun `gravidez tira o defice da meta`() {
        val comDefice = profile(sex = Sex.FEMALE, goal = GoalType.LOSE, rate = -500)
        val gravida = profile(
            sex = Sex.FEMALE,
            goal = GoalType.LOSE,
            rate = -500,
            lifeStage = LifeStage.PREGNANCY,
        )

        val normal = NutritionCalc.dailyTargets(comDefice, 65.0, today)
        val semDefice = NutritionCalc.dailyTargets(gravida, 65.0, today)

        assertTrue(
            semDefice.kcal > normal.kcal,
            "a meta na gravidez (${semDefice.kcal}) devia ser maior do que a com défice (${normal.kcal})",
        )
        assertTrue(TargetWarning.NO_DEFICIT_IN_PREGNANCY in semDefice.warnings)
    }

    @Test
    fun `na gravidez a meta e a manutencao`() {
        val gravida = profile(
            sex = Sex.FEMALE,
            goal = GoalType.LOSE,
            rate = -500,
            lifeStage = LifeStage.PREGNANCY,
        )
        val manutencao = profile(sex = Sex.FEMALE, goal = GoalType.MAINTAIN, rate = 0)

        assertEquals(
            NutritionCalc.dailyTargets(manutencao, 65.0, today).kcal,
            NutritionCalc.dailyTargets(gravida, 65.0, today).kcal,
        )
    }

    @Test
    fun `amamentacao tambem tira o defice`() {
        val t = NutritionCalc.dailyTargets(
            profile(sex = Sex.FEMALE, goal = GoalType.LOSE, rate = -400, lifeStage = LifeStage.LACTATION),
            65.0,
            today,
        )
        assertTrue(TargetWarning.NO_DEFICIT_IN_PREGNANCY in t.warnings)
    }

    @Test
    fun `na gravidez um superavit fica como esta`() {
        val t = NutritionCalc.dailyTargets(
            profile(sex = Sex.FEMALE, goal = GoalType.GAIN, rate = 300, lifeStage = LifeStage.PREGNANCY),
            65.0,
            today,
        )
        assertTrue(TargetWarning.NO_DEFICIT_IN_PREGNANCY !in t.warnings)
    }

    @Test
    fun `pos-menopausa nao impede um defice`() {
        val t = NutritionCalc.dailyTargets(
            profile(sex = Sex.FEMALE, goal = GoalType.LOSE, rate = -400, lifeStage = LifeStage.POSTMENOPAUSAL),
            65.0,
            today,
        )
        assertTrue(TargetWarning.NO_DEFICIT_IN_PREGNANCY !in t.warnings)
    }

    @Test
    fun `removesDeficit so vale para gravidez e amamentacao`() {
        assertTrue(NutritionCalc.removesDeficit(LifeStage.PREGNANCY))
        assertTrue(NutritionCalc.removesDeficit(LifeStage.LACTATION))
        assertTrue(!NutritionCalc.removesDeficit(LifeStage.POSTMENOPAUSAL))
        assertTrue(!NutritionCalc.removesDeficit(LifeStage.NONE))
        assertTrue(!NutritionCalc.removesDeficit(null))
    }
}
