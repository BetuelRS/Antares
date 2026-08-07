package pt.antares.app.core.calc

import pt.antares.app.core.database.entities.UserProfileEntity
import pt.antares.app.core.model.ActivityLevel
import pt.antares.app.core.model.BodyFatSource
import pt.antares.app.core.model.GoalType
import pt.antares.app.core.model.MacroStrategy
import pt.antares.app.core.model.Sex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileMigrationTest {

    private val today = 20639L
    private val birth30y = today - 10958L

    private fun profile(
        sex: Sex = Sex.MALE,
        activity: ActivityLevel = ActivityLevel.MODERATE,
        goal: GoalType = GoalType.MAINTAIN,
        rate: Int = 0,
        bodyFatPct: Double? = null,
        bodyFatSource: BodyFatSource? = null,
        addBack: Boolean = true,
    ) = UserProfileEntity(
        sex = sex,
        birthEpochDay = birth30y,
        heightCm = 178,
        activityLevel = activity,
        goalType = goal,
        goalRateKcal = rate,
        macroStrategy = MacroStrategy.BALANCED,
        customProteinG = null,
        customCarbsG = null,
        customFatG = null,
        exerciseAddBack = addBack,
        bodyFatPct = bodyFatPct,
        bodyFatSource = bodyFatSource,
        updatedAt = 0L,
    )

    @Test
    fun `a conta antiga e mesmo a antiga`() {

        assertEquals(2740, ProfileMigration.legacyDailyKcal(profile(), 80.0, today))
    }

    @Test
    fun `a conta antiga usa o piso fixo e mais nenhum`() {

        val p = profile(activity = ActivityLevel.SEDENTARY, goal = GoalType.LOSE, rate = -1000)
        assertEquals(1601, ProfileMigration.legacyDailyKcal(p, 120.0, today))
    }

    @Test
    fun `a meta desce quando o multiplicador desce`() {
        val change = ProfileMigration.detectGoalChange(profile(), 80.0, today)
        assertNotNull(change)
        assertEquals(2740, change.oldKcal)
        assertEquals(2563, change.newKcal)
        assertTrue(change.deltaKcal < 0)
        assertTrue(GoalChangeReason.ACTIVITY_MEANING_CHANGED in change.reasons)
    }

    @Test
    fun `sedentario nao muda de multiplicador e nao ha aviso`() {

        val change = ProfileMigration.detectGoalChange(
            profile(activity = ActivityLevel.SEDENTARY), 80.0, today,
        )
        assertNull(change)
    }

    @Test
    fun `mudar de formula tambem e uma razao`() {
        val p = profile(
            activity = ActivityLevel.SEDENTARY,
            bodyFatPct = 30.0, bodyFatSource = BodyFatSource.MEASURED,
        )
        val change = ProfileMigration.detectGoalChange(p, 80.0, today)
        assertNotNull(change)

        assertTrue(GoalChangeReason.BMR_FORMULA_CHANGED in change.reasons)
        assertTrue(GoalChangeReason.ACTIVITY_MEANING_CHANGED !in change.reasons)
    }

    @Test
    fun `o piso novo aparece como razao quando e ele que trava`() {
        val p = profile(activity = ActivityLevel.SEDENTARY, goal = GoalType.LOSE, rate = -1000)
        val change = ProfileMigration.detectGoalChange(p, 120.0, today)
        assertNotNull(change)
        assertTrue(GoalChangeReason.ENERGY_FLOOR_CHANGED in change.reasons)

        assertTrue(change.deltaKcal > 0, "delta=${change.deltaKcal}")
    }

    @Test
    fun `diferenca pequena nao gera aviso`() {

        assertTrue(ProfileMigration.NOTICE_THRESHOLD_KCAL > 0)
        val change = ProfileMigration.detectGoalChange(
            profile(activity = ActivityLevel.SEDENTARY), 70.0, today,
        )
        assertNull(change)
    }

    @Test
    fun `quem tinha o add-back desligado e avisado mesmo sem a meta mexer`() {

        val change = ProfileMigration.detectGoalChange(
            profile(activity = ActivityLevel.SEDENTARY, addBack = false), 80.0, today,
        )
        assertNotNull(change)
        assertEquals(listOf(GoalChangeReason.EXERCISE_ADD_BACK_FORCED_ON), change.reasons)
        assertEquals(change.oldKcal, change.newKcal)
    }

    @Test
    fun `quem ja o tinha ligado nao e incomodado por causa dele`() {
        val change = ProfileMigration.detectGoalChange(
            profile(activity = ActivityLevel.SEDENTARY, addBack = true), 80.0, today,
        )
        assertNull(change)
    }

    @Test
    fun `a migracao nao mexe no perfil`() {

        val p = profile()
        val before = p.copy()
        ProfileMigration.detectGoalChange(p, 80.0, today)
        assertEquals(before, p)
    }
}
