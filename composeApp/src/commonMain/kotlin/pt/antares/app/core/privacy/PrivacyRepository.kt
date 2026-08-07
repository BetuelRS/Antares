package pt.antares.app.core.privacy

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import pt.antares.app.core.database.AntaresDb
import pt.antares.app.core.datastore.AppPreferences
import pt.antares.app.core.util.LocalPhotoStore
import pt.antares.app.feature.fasting.data.FastingProtocolSeeder
import pt.antares.app.feature.fooddata.FoodSeeder
import pt.antares.app.feature.workout.data.ExerciseSeeder
import pt.antares.app.feature.workout.data.RoutineTemplateSeeder

sealed interface WipeOutcome {
    data object Success : WipeOutcome

    data class Failed(val message: String) : WipeOutcome
}

class PrivacyRepository(
    private val db: AntaresDb,
    private val prefs: AppPreferences,
    private val foodSeeder: FoodSeeder,
    private val exerciseSeeder: ExerciseSeeder,
    private val templateSeeder: RoutineTemplateSeeder,
    private val fastingProtocolSeeder: FastingProtocolSeeder,
    private val photos: LocalPhotoStore,
    private val io: CoroutineDispatcher,
) {

    suspend fun deleteEverything(): WipeOutcome = withContext(io) {
        try {
            db.clearAllTables()

            photos.deleteAll()
            prefs.clearAll()
            foodSeeder.seedIfNeeded()
            exerciseSeeder.seedIfNeeded()
            templateSeeder.seedIfNeeded()
            fastingProtocolSeeder.seedIfNeeded()
            WipeOutcome.Success
        } catch (e: Throwable) {
            WipeOutcome.Failed(e.message ?: "falhou a apagar")
        }
    }
}
