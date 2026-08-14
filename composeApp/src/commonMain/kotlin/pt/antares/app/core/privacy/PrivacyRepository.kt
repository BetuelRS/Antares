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

    /**
     * Apaga tudo e deixa a app como acabada de instalar. É o direito ao apagamento, e é
     * definitivo: não há cópia num servidor de onde recuperar.
     */
    suspend fun deleteEverything(): WipeOutcome = withContext(io) {
        try {
            db.clearAllTables()

            // As fotos são ficheiros e não linhas: `clearAllTables` não lhes toca, e ficavam
            // no telemóvel depois de a pessoa ter mandado apagar tudo.
            photos.deleteAll()
            prefs.clearAll()
            // Volta a semear o catálogo, que também foi apagado: sem isto a app fica sem
            // alimentos nem exercícios e não há como voltar a tê-los senão reinstalando.
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
