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

/**
 * Avisa quem já usava a app que a meta mudou de cálculo, uma vez só. Sem isto, uma
 * atualização que melhora as contas parece um erro a quem vê o número diferente.
 */
class GoalMigrationRepository(
    private val profileDao: UserProfileDao,
    private val weightDao: WeightLogDao,
    private val preferences: AppPreferences,
    private val io: CoroutineDispatcher,
) {

    val noticePending: Flow<Boolean> = preferences.goalEngineNoticePending

    suspend fun onAppStart(currentVersion: String = AppChangelog.CURRENT) = withContext(io) {
        // Versão vista vazia mas com perfil já criado: é uma instalação anterior à versão
        // que passou a registar isto. Quem instala de novo tem perfil nulo neste momento e
        // não recebe aviso nenhum — não há passado para lhe explicar.
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
        // Nada mudou de facto para esta pessoa: baixa-se a marca em silêncio, sem lhe
        // mostrar um aviso sobre uma alteração que ela não vai ver.
        if (change == null) preferences.setGoalEngineNoticePending(false)
        change
    }

    /**
     * A pessoa leu o aviso. É aqui que a soma do exercício ao orçamento passa a ligada —
     * depois de explicada, e não antes.
     */
    suspend fun acknowledge() = withContext(io) {
        profileDao.get()?.takeIf { !it.exerciseAddBack }?.let { profile ->
            profileDao.upsert(
                profile.copy(
                    exerciseAddBack = true,
                    updatedAt = Clock.System.now().toEpochMilliseconds(),
                ),
            )
        }
        preferences.setGoalEngineNoticePending(false)
    }
}
