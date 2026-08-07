package pt.antares.app.feature.profile.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import pt.antares.app.core.calc.GoalChange
import pt.antares.app.core.calc.ProfileMigration
import pt.antares.app.core.database.daos.UserProfileDao
import pt.antares.app.core.database.daos.WeightLogDao
import pt.antares.app.core.datastore.AppPreferences
import pt.antares.app.core.util.todayEpochDay
import pt.antares.app.feature.about.AppChangelog

class GoalMigrationRepository(
    private val profileDao: UserProfileDao,
    private val weightDao: WeightLogDao,
    private val preferences: AppPreferences,
    private val io: CoroutineDispatcher,
) {

    val noticePending: Flow<Boolean> = preferences.goalEngineNoticePending

    suspend fun onAppStart(currentVersion: String = AppChangelog.CURRENT) = withContext(io) {
        val seen = preferences.lastSeenVersion.first()
        if (seen.isEmpty() && profileDao.get() != null) {
            preferences.setGoalEngineNoticePending(true)
        }
        if (seen != currentVersion) preferences.setLastSeenVersion(currentVersion)
    }

    suspend fun pendingGoalChange(day: Long = todayEpochDay()): GoalChange? = withContext(io) {
        if (!preferences.goalEngineNoticePending.first()) return@withContext null
        val profile = profileDao.get() ?: return@withContext null
        val weight = weightDao.latest()?.weightKg ?: ProfileRepository.DEFAULT_WEIGHT_KG
        val change = ProfileMigration.detectGoalChange(profile, weight, day)
        if (change == null) preferences.setGoalEngineNoticePending(false)
        change
    }

    suspend fun acknowledge() = withContext(io) {
        profileDao.get()?.takeIf { !it.exerciseAddBack }?.let { profile ->
            profileDao.upsert(
                profile.copy(
                    exerciseAddBack = true,
                    updatedAt = Clock.System.now().toEpochMilliseconds(),
                    dirty = true,
                ),
            )
        }
        preferences.setGoalEngineNoticePending(false)
    }
}
