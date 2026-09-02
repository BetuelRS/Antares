package pt.antares.app.testing

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.datetime.Clock
import pt.antares.app.core.coach.CoachRepository
import pt.antares.app.core.database.AntaresDb
import pt.antares.app.core.database.entities.ExerciseLogEntity
import pt.antares.app.core.database.entities.WeightLogEntity
import pt.antares.app.core.datastore.AppPreferences
import pt.antares.app.core.health.HealthGateway
import pt.antares.app.core.health.HealthPublisher
import pt.antares.app.core.health.HealthRepository
import pt.antares.app.feature.coach.CoachViewModel
import pt.antares.app.feature.diary.DiaryRepository
import pt.antares.app.feature.exercise.ExerciseRepository
import pt.antares.app.feature.fasting.NoopFastingNotifier
import pt.antares.app.feature.fasting.data.FastingRepository
import pt.antares.app.feature.profile.data.BodyMeasurementRepository
import pt.antares.app.feature.profile.data.ProfileRepository
import pt.antares.app.feature.profile.ui.HealthProfileViewModel
import pt.antares.app.feature.running.data.RunRepository
import pt.antares.app.feature.stats.NutritionStatsRepository
import pt.antares.app.feature.templates.MealTemplateRepository
import pt.antares.app.feature.today.TodayViewModel
import pt.antares.app.feature.workout.data.RoutineRepository
import pt.antares.app.feature.workout.data.WorkoutHistoryRepository
import pt.antares.app.feature.workout.data.WorkoutSessionRepository

/**
 * As montagens que os dois harnesses partilham. Vivem fora de ambos porque o
 * [ViewModelHarness] e o [FluxoUiHarness] diferem só no despachante e no relógio — o grafo
 * de dependências é o mesmo, e duplicá-lo faria as duas suites divergirem sem ninguém dar
 * por isso.
 */
object Fabricas {

    fun diaryRepository(db: AntaresDb, io: CoroutineDispatcher) =
        DiaryRepository(db.foodLogDao(), db.waterLogDao(), io)

    fun profileRepository(db: AntaresDb, io: CoroutineDispatcher) = ProfileRepository(
        db.userProfileDao(),
        db.weightLogDao(),
        db.dailyTargetOverrideDao(),
        db.foodLogDao(),
        db.goalHistoryDao(),
        db.workoutSessionDao(),
        io,
    )

    fun statsRepository(db: AntaresDb, io: CoroutineDispatcher) =
        NutritionStatsRepository(db.foodLogDao(), db.dbInfoDao(), io)

    fun workoutSessionRepository(db: AntaresDb, io: CoroutineDispatcher) =
        WorkoutSessionRepository(
            db.workoutSessionDao(),
            db.workoutSetDao(),
            db.exerciseLogDao(),
            db.weightLogDao(),
            db.routineDao(),
            db.sessionExerciseNoteDao(),
            db.exerciseLoadDao(),
            io,
        )

    fun workoutHistoryRepository(db: AntaresDb, io: CoroutineDispatcher) =
        WorkoutHistoryRepository(db.workoutSessionDao(), db.workoutSetDao(), db.exerciseLibraryDao(), io)

    fun routineRepository(db: AntaresDb, io: CoroutineDispatcher) =
        RoutineRepository(db.routineDao(), db.exerciseLibraryDao(), db.routineScheduleDao(), io)

    /**
     * O Health Connect ligado a nada: escreve na base de teste e nunca lê janelas de fora.
     * Serve os dois modelos que dependem dele, e é por isso que está aqui — a montagem
     * duplicada foi a razão de as duas suites divergirem antes.
     */
    fun healthRepository(
        db: AntaresDb,
        io: CoroutineDispatcher,
        gateway: HealthGateway = CountingHealthGateway(),
    ): HealthRepository {
        var lastImport = 0L
        return HealthRepository(
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
            io = io,
            now = { Clock.System.now().toEpochMilliseconds() },
            epochDayOf = { it / 86_400_000L },
        )
    }

    fun healthProfileViewModel(
        db: AntaresDb,
        io: CoroutineDispatcher,
    ) = HealthProfileViewModel(
        repository = profileRepository(db, io),
        measurements = BodyMeasurementRepository(db.bodyMeasurementDao(), db.userProfileDao(), io),
        health = healthRepository(db, io),
    )

    fun todayViewModel(
        db: AntaresDb,
        prefs: AppPreferences,
        io: CoroutineDispatcher,
        gateway: HealthGateway = CountingHealthGateway(),
    ): TodayViewModel {
        var lastPublish = 0L
        val health = healthRepository(db, io, gateway)
        val publisher = HealthPublisher(
            gateway = gateway,
            nutrition = { emptyList() },
            sessions = { emptyList() },
            bodyComposition = { emptyList() },
            lastPublishAt = { lastPublish },
            setLastPublishAt = { lastPublish = it },
            io = io,
            now = { Clock.System.now().toEpochMilliseconds() },
        )
        return TodayViewModel(
            profileRepository = profileRepository(db, io),
            diaryRepository = diaryRepository(db, io),
            exerciseRepository = ExerciseRepository(db.exerciseLogDao(), io),
            workoutSessionRepository = workoutSessionRepository(db, io),
            workoutHistoryRepository = workoutHistoryRepository(db, io),
            routineRepository = routineRepository(db, io),
            fastingRepository = fastingRepository(db, io),
            runRepository = RunRepository(db.runDao(), db.exerciseLogDao(), io),
            preferences = prefs,
            statsRepository = statsRepository(db, io),
            health = health,
            healthPublisher = publisher,
        )
    }

    fun fastingRepository(db: AntaresDb, io: CoroutineDispatcher) = FastingRepository(
        db.fastingProtocolDao(),
        db.fastingSessionDao(),
        NoopFastingNotifier(),
        io,
    )

    fun mealTemplateRepository(db: AntaresDb, io: CoroutineDispatcher) = MealTemplateRepository(
        db.foodLogDao(),
        db.mealTemplateDao(),
        db.mealTemplateItemDao(),
        io,
    )

    fun coachRepository(db: AntaresDb, prefs: AppPreferences, io: CoroutineDispatcher) =
        CoachRepository(
            coachDao = db.coachReportDao(),
            foodLogDao = db.foodLogDao(),
            weightDao = db.weightLogDao(),
            profileDao = db.userProfileDao(),
            overrideDao = db.dailyTargetOverrideDao(),
            workoutSessionDao = db.workoutSessionDao(),
            workoutSetDao = db.workoutSetDao(),
            exerciseLogDao = db.exerciseLogDao(),
            fastingDao = db.fastingSessionDao(),
            runDao = db.runDao(),
            statsRepository = statsRepository(db, io),
            prefs = prefs,
            io = io,
        )

    fun coachViewModel(db: AntaresDb, prefs: AppPreferences, io: CoroutineDispatcher) =
        CoachViewModel(coachRepository(db, prefs, io), profileRepository(db, io))
}
