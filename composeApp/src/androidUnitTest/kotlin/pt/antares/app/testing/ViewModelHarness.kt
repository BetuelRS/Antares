package pt.antares.app.testing

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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
        Dispatchers.resetMain()
        db.close()
        prefsFile.delete()
    }

    protected fun diaryRepository() = DiaryRepository(db.foodLogDao(), db.waterLogDao(), dispatcher)

    protected fun profileRepository() = ProfileRepository(
        db.userProfileDao(),
        db.weightLogDao(),
        db.dailyTargetOverrideDao(),
        db.foodLogDao(),
        db.goalHistoryDao(),
        dispatcher,
    )

    protected fun statsRepository() =
        NutritionStatsRepository(db.foodLogDao(), db.dbInfoDao(), dispatcher)

    protected fun diaryViewModel() = DiaryViewModel(
        diaryRepository = diaryRepository(),
        profileRepository = profileRepository(),
        exerciseRepository = ExerciseRepository(db.exerciseLogDao(), dispatcher),
        preferences = prefs,
        templateRepository = MealTemplateRepository(
            db.foodLogDao(),
            db.mealTemplateDao(),
            db.mealTemplateItemDao(),
            dispatcher,
        ),
        statsRepository = statsRepository(),
    )

    protected fun todayViewModel(
        gateway: HealthGateway = CountingHealthGateway(),
    ): TodayViewModel {
        var lastImport = 0L
        var lastPublish = 0L
        val health = HealthRepository(
            gateway = gateway,
            weights = object : HealthRepository.WeightWriter {
                override suspend fun importedRefs() = emptySet<String>()
                override suspend fun existsOnDay(epochDay: Long) = false
                override suspend fun insert(entry: WeightLogEntity) = db.weightLogDao().upsert(entry)
            },
            exercise = object : HealthRepository.ExerciseWriter {
                override suspend fun importedRefs() = emptySet<String>()
                override suspend fun insert(log: ExerciseLogEntity) = db.exerciseLogDao().upsert(log)
            },
            ownWindows = { emptyList() },
            latestWeightKg = { db.weightLogDao().latest()?.weightKg },
            lastImportAt = { lastImport },
            setLastImportAt = { lastImport = it },
            io = dispatcher,
            now = { Clock.System.now().toEpochMilliseconds() },
            epochDayOf = { it / 86_400_000L },
        )
        val publisher = HealthPublisher(
            gateway = gateway,
            nutrition = { emptyList() },
            sessions = { emptyList() },
            bodyComposition = { emptyList() },
            lastPublishAt = { lastPublish },
            setLastPublishAt = { lastPublish = it },
            io = dispatcher,
            now = { Clock.System.now().toEpochMilliseconds() },
        )
        return TodayViewModel(
            profileRepository = profileRepository(),
            diaryRepository = diaryRepository(),
            exerciseRepository = ExerciseRepository(db.exerciseLogDao(), dispatcher),
            workoutSessionRepository = WorkoutSessionRepository(
                db.workoutSessionDao(),
                db.workoutSetDao(),
                db.exerciseLogDao(),
                db.weightLogDao(),
                db.routineDao(),
                dispatcher,
            ),
            workoutHistoryRepository = WorkoutHistoryRepository(
                db.workoutSessionDao(),
                db.workoutSetDao(),
                db.exerciseLibraryDao(),
                dispatcher,
            ),
            routineRepository = RoutineRepository(
                db.routineDao(),
                db.exerciseLibraryDao(),
                db.routineScheduleDao(),
                dispatcher,
            ),
            fastingRepository = FastingRepository(
                db.fastingProtocolDao(),
                db.fastingSessionDao(),
                NoopFastingNotifier(),
                dispatcher,
            ),
            runRepository = RunRepository(db.runDao(), db.exerciseLogDao(), dispatcher),
            preferences = prefs,
            statsRepository = statsRepository(),
            health = health,
            healthPublisher = publisher,
        )
    }
}
