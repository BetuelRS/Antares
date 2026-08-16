package pt.antares.app.testing

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import org.junit.After
import org.junit.Before
import pt.antares.app.core.database.AntaresDb
import pt.antares.app.core.database.entities.ExerciseLogEntity
import pt.antares.app.core.database.entities.WeightLogEntity
import pt.antares.app.core.datastore.AppPreferences
import pt.antares.app.core.datastore.createPreferencesDataStore
import pt.antares.app.core.health.HealthGateway
import pt.antares.app.core.health.HealthPublisher
import pt.antares.app.core.health.HealthRepository
import pt.antares.app.feature.fasting.NoopFastingNotifier
import pt.antares.app.feature.diary.DiaryRepository
import pt.antares.app.feature.diary.DiaryViewModel
import pt.antares.app.feature.exercise.ExerciseRepository
import pt.antares.app.feature.fasting.data.FastingRepository
import pt.antares.app.feature.profile.data.ProfileRepository
import pt.antares.app.feature.running.data.RunRepository
import pt.antares.app.feature.stats.NutritionStatsRepository
import pt.antares.app.feature.templates.MealTemplateRepository
import pt.antares.app.feature.today.TodayViewModel
import pt.antares.app.feature.workout.data.RoutineRepository
import pt.antares.app.feature.workout.data.WorkoutHistoryRepository
import pt.antares.app.feature.workout.data.WorkoutSessionRepository
import java.io.File
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
abstract class ViewModelHarness {

    protected lateinit var db: AntaresDb
        private set

    protected lateinit var prefs: AppPreferences
        private set

    protected lateinit var dispatcher: TestDispatcher
        private set

    protected lateinit var scope: TestScope
        private set

    private lateinit var prefsFile: File

    @Before
    fun setUpHarness() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        dispatcher = StandardTestDispatcher()
        scope = TestScope(dispatcher)

        Dispatchers.setMain(dispatcher)

        db = Room.inMemoryDatabaseBuilder(context, AntaresDb::class.java)
            .setQueryCoroutineContext(dispatcher)
            .build()

        prefsFile = File(context.cacheDir, "prefs-${UUID.randomUUID()}.preferences_pb")
        prefs = AppPreferences(createPreferencesDataStore { prefsFile.absolutePath })
    }

    @After
    fun tearDownHarness() {
        // O `scope` do harness fica com o que os ViewModels lançaram e o teste não esperou —
        // um `stateIn(WhileSubscribed)` que ainda vai buscar a primeira leitura, por exemplo.
        // Fechar a base com uma consulta a caminho fazia o Room abrir outra em memória para a
        // servir, e essa nunca era fechada: o `CloseGuard` do Robolectric queixava-se disso
        // no fim da suite, num teste qualquer que estivesse a correr quando o recolector
        // passasse — e a queixa não dizia de onde vinha.
        scope.cancel()
        Dispatchers.resetMain()
        db.close()
        prefsFile.delete()
    }

    protected fun diaryRepository() = Fabricas.diaryRepository(db, dispatcher)

    protected fun profileRepository() = Fabricas.profileRepository(db, dispatcher)

    protected fun statsRepository() = Fabricas.statsRepository(db, dispatcher)

    protected fun diaryViewModel() = DiaryViewModel(
        diaryRepository = diaryRepository(),
        profileRepository = profileRepository(),
        fastingRepository = Fabricas.fastingRepository(db, dispatcher),
        exerciseRepository = ExerciseRepository(db.exerciseLogDao(), dispatcher),
        preferences = prefs,
        templateRepository = Fabricas.mealTemplateRepository(db, dispatcher),
        statsRepository = statsRepository(),
    )

    protected fun todayViewModel(gateway: HealthGateway = CountingHealthGateway()): TodayViewModel =
        Fabricas.todayViewModel(db, prefs, dispatcher, gateway)
}
