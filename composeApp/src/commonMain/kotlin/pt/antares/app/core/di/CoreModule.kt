package pt.antares.app.core.di

import kotlinx.coroutines.Dispatchers
import org.koin.core.qualifier.named
import org.koin.dsl.module
import pt.antares.app.core.database.AntaresDb
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import pt.antares.app.core.database.daos.CoachReportDao
import pt.antares.app.core.database.daos.DailyTargetOverrideDao
import pt.antares.app.core.database.daos.ExerciseLogDao
import pt.antares.app.core.database.daos.RunDao
import pt.antares.app.core.database.daos.WorkoutSessionDao
import pt.antares.app.core.database.entities.ExerciseLogEntity
import pt.antares.app.core.health.DayNutrition
import pt.antares.app.core.health.HealthPublisher
import pt.antares.app.core.health.HealthRepository
import pt.antares.app.core.health.NoHealthGateway
import pt.antares.app.core.health.OutboundBodyComposition
import pt.antares.app.core.health.OutboundKind
import pt.antares.app.core.health.OutboundSession
import pt.antares.app.core.health.TimeWindow
import pt.antares.app.core.notifications.NoopCoachNotifier
import pt.antares.app.core.util.toEpochDay
import pt.antares.app.core.ai.AiClient
import pt.antares.app.core.ai.AiRepository
import pt.antares.app.core.ai.SupabaseAiClient
import pt.antares.app.core.coach.CoachRepository
import pt.antares.app.core.database.daos.FoodLogDao
import pt.antares.app.core.database.daos.UserProfileDao
import pt.antares.app.core.database.daos.WaterLogDao
import pt.antares.app.core.database.daos.WeightLogDao
import pt.antares.app.core.database.entities.CoachReportEntity
import pt.antares.app.core.database.entities.BodyMeasurementEntity
import pt.antares.app.core.database.entities.GoalHistoryEntity
import pt.antares.app.core.database.entities.DailyTargetOverrideEntity
import pt.antares.app.core.datastore.AppPreferences
import pt.antares.app.core.database.entities.FoodLogEntity
import pt.antares.app.core.database.entities.UserProfileEntity
import pt.antares.app.core.database.entities.WaterLogEntity
import pt.antares.app.core.database.entities.WeightLogEntity
import pt.antares.app.core.network.supabase.AnonymousSession
import pt.antares.app.core.network.createAntaresHttpClient
import pt.antares.app.core.network.off.OffApi
import pt.antares.app.core.database.daos.CycleDao
import pt.antares.app.core.database.daos.ProgressPhotoDao
import pt.antares.app.core.database.daos.SearchMissDao
import pt.antares.app.core.database.entities.CycleEntity
import pt.antares.app.core.database.entities.ProgressPhotoEntity
import pt.antares.app.core.database.entities.SearchMissEntity
import pt.antares.app.core.privacy.BackupImporter
import pt.antares.app.core.privacy.DataExporter
import pt.antares.app.core.privacy.ExportSource
import pt.antares.app.core.privacy.PrivacyRepository
import pt.antares.app.core.privacy.RoomBackupDb
import pt.antares.app.feature.about.AppChangelog
import pt.antares.app.feature.diary.DiaryRepository
import pt.antares.app.feature.exercise.ExerciseRepository
import pt.antares.app.feature.fooddata.FoodRepository
import pt.antares.app.feature.fooddata.FoodSeeder
import pt.antares.app.feature.fooddata.OffRepository
import pt.antares.app.feature.profile.data.ProfileRepository
import pt.antares.app.feature.recipe.RecipeRepository
import pt.antares.app.feature.fasting.data.FastingProtocolSeeder
import pt.antares.app.feature.fasting.data.FastingRepository
import pt.antares.app.feature.running.data.RunRepository
import pt.antares.app.feature.stats.NutritionStatsRepository
import pt.antares.app.feature.workout.data.ExerciseLibraryRepository
import pt.antares.app.feature.workout.data.ExerciseSeeder
import pt.antares.app.feature.workout.data.RoutineRepository
import pt.antares.app.feature.workout.data.RoutineTemplateSeeder
import pt.antares.app.feature.workout.data.SessionPickBus
import pt.antares.app.feature.workout.data.WorkoutHistoryRepository
import pt.antares.app.feature.workout.data.WorkoutSessionRepository

// Qualificador do dispatcher de entrada e saída. Passa-se explicitamente a tudo o que
// toca disco ou rede, em vez de cada classe o escolher: é o que permite aos testes
// substituí-lo por um dispatcher determinista.
val IoDispatcher = named("io")

/**
 * Tudo o que vive enquanto a app viver: base de dados, repositórios, rede.
 *
 * Os repositórios recebem funções e não outros repositórios sempre que possível — é isso
 * que os deixa testáveis sem construir meia app à volta, e o que impede o grafo de
 * dependências de dar voltas sobre si mesmo.
 */
val coreModule = module {
    single(IoDispatcher) { Dispatchers.IO }

    // Cada DAO é registado à parte, e não a base inteira: assim uma classe declara os DAOs
    // de que precisa em vez de receber acesso a tudo.
    single { get<AntaresDb>().userProfileDao() }
    single { get<AntaresDb>().weightLogDao() }
    single { get<AntaresDb>().dailyTargetOverrideDao() }
    single { get<AntaresDb>().foodDao() }
    single { get<AntaresDb>().foodLogDao() }

    single { get<AntaresDb>().dbInfoDao() }
    single { get<AntaresDb>().waterLogDao() }
    single { get<AntaresDb>().recipeDao() }
    single { get<AntaresDb>().recipeIngredientDao() }
    single { get<AntaresDb>().exerciseLogDao() }
    single { get<AntaresDb>().exerciseLibraryDao() }
    single { get<AntaresDb>().routineDao() }
    single { get<AntaresDb>().workoutSessionDao() }
    single { get<AntaresDb>().workoutSetDao() }
    single { get<AntaresDb>().routineScheduleDao() }
    single { get<AntaresDb>().fastingProtocolDao() }
    single { get<AntaresDb>().fastingSessionDao() }
    single { get<AntaresDb>().runDao() }
    single { get<AntaresDb>().trackPointDao() }
    single { get<AntaresDb>().coachReportDao() }
    single { get<AntaresDb>().mealTemplateDao() }
    single { get<AntaresDb>().mealTemplateItemDao() }
    single { get<AntaresDb>().bodyMeasurementDao() }
    single { get<AntaresDb>().goalHistoryDao() }
    single { get<AntaresDb>().searchMissDao() }
    single { get<AntaresDb>().progressPhotoDao() }

    single { get<AntaresDb>().cycleDao() }
    single { get<AntaresDb>().demoDao() }

    single { pt.antares.app.core.demo.DemoDataWriter(get(), get(IoDispatcher)) }

    single { ProfileRepository(get(), get(), get(), get(), get(), get(IoDispatcher)) }
    single {
        pt.antares.app.feature.profile.data.BodyMeasurementRepository(
            dao = get(),
            io = get(IoDispatcher),
        )
    }
    single {
        pt.antares.app.feature.profile.data.GoalMigrationRepository(
            profileDao = get(),
            weightDao = get(),
            preferences = get(),
            io = get(IoDispatcher),
        )
    }
    single { FoodRepository(get(), get(), get(IoDispatcher)) }
    single { DiaryRepository(get(), get(), get(IoDispatcher)) }
    single {
        pt.antares.app.feature.templates.MealTemplateRepository(
            foodLogDao = get(),
            templateDao = get(),
            itemDao = get(),
            io = get(IoDispatcher),
        )
    }
    single { RecipeRepository(get(), get(), get(), get(), get(IoDispatcher)) }
    single { NutritionStatsRepository(get(), get(), get(IoDispatcher)) }
    single { pt.antares.app.feature.progress.ProgressRepository(get(), get(), get(), get(), get(IoDispatcher)) }
    single { pt.antares.app.feature.progress.ProgressPhotoRepository(get(), get(), get(), get(IoDispatcher)) }
    single { pt.antares.app.feature.profile.data.CycleRepository(get(), get(IoDispatcher)) }
    single { ExerciseRepository(get(), get(IoDispatcher)) }

    single { FoodSeeder(get(), get(IoDispatcher)) }
    single { ExerciseSeeder(get(), get(IoDispatcher)) }
    single { FastingProtocolSeeder(get(), get(IoDispatcher)) }
    single { FastingRepository(get(), get(), get(), get(IoDispatcher)) }
    single { RunRepository(get(), get(), get(IoDispatcher)) }
    single { ExerciseLibraryRepository(get(), get(), get(IoDispatcher)) }
    single { RoutineRepository(get(), get(), get(), get(IoDispatcher)) }
    single { RoutineTemplateSeeder(get(), get(), get(IoDispatcher)) }
    single { WorkoutSessionRepository(get(), get(), get(), get(), get(), get(IoDispatcher)) }
    single { WorkoutHistoryRepository(get(), get(), get(), get(IoDispatcher)) }
    single { SessionPickBus() }

    // Um cliente HTTP para a app toda: cada instância abre o seu conjunto de ligações, e
    // várias delas eram memória e sockets a mais para as duas ou três chamadas que a app faz.
    single { createAntaresHttpClient() }
    // A versão sai do `AppChangelog`, que o `AppChangelogTest` mantém colado ao
    // `versionName` do build. É o que impede o `User-Agent` de envelhecer sozinho.
    single { OffApi(get(), userAgent = "Antares/${AppChangelog.CURRENT} (${OffApi.CONTACT})") }
    single { OffRepository(get(), get(), get(IoDispatcher)) }

    single { AnonymousSession(get(), get(IoDispatcher)) }

    single<AiClient> { SupabaseAiClient(get(), get(IoDispatcher)) }
    single {
        val sessao: AnonymousSession = get()
        pt.antares.app.core.admin.AdminRepository(
            container = get(),
            prefs = get(),
            ensureAccount = { sessao.ensure() },
            io = get(IoDispatcher),
        )
    }
    single {
        val sessao: AnonymousSession = get()
        val foodLogDao: FoodLogDao = get()
        val weightLogDao: WeightLogDao = get()
        val prefs: AppPreferences = get()
        AiRepository(
            client = get(),

            // Funções em vez dos DAOs inteiros: a classe da AI fica sem acesso à base, e
            // o que ela pode escrever está aqui à vista.
            ensureAccount = { sessao.ensure() },
            saveFoodLog = { foodLogDao.upsert(it) },
            latestWeightKg = { weightLogDao.latest()?.weightKg },
            persistUsage = { usage, day ->
                prefs.setAiUsage(usage.used, usage.limit, usage.trial, day)
            },
            io = get(IoDispatcher),
        )
    }

    single {
        CoachRepository(
            coachDao = get(),
            foodLogDao = get(),
            weightDao = get(),
            profileDao = get(),
            overrideDao = get(),
            workoutSessionDao = get(),
            workoutSetDao = get(),
            exerciseLogDao = get(),
            fastingDao = get(),
            runDao = get(),
            statsRepository = get(),
            prefs = get(),
            notifier = getOrNull() ?: NoopCoachNotifier(),
            io = get(IoDispatcher),
        )
    }

    single {
        val weightDao = get<WeightLogDao>()
        val exerciseLogDao = get<ExerciseLogDao>()
        val workoutDao = get<WorkoutSessionDao>()
        val runDao = get<RunDao>()

        HealthRepository(
            // `getOrNull` porque o gateway só existe no módulo Android e só quando o
            // serviço está instalado. Sem ele, o [NoHealthGateway] devolve vazio em vez de
            // a app rebentar ao arrancar.
            gateway = getOrNull() ?: NoHealthGateway,
            weights = object : HealthRepository.WeightWriter {
                override suspend fun importedRefs() = weightDao.importedRefs().toSet()
                override suspend fun existsOnDay(epochDay: Long) = weightDao.byDay(epochDay) != null
                override suspend fun insert(entry: WeightLogEntity) = weightDao.upsert(entry)
            },
            exercise = object : HealthRepository.ExerciseWriter {
                override suspend fun importedRefs() = exerciseLogDao.importedRefs().toSet()
                override suspend fun insert(log: ExerciseLogEntity) = exerciseLogDao.upsert(log)
            },

            // Treinos e corridas juntos: para efeitos de duplicação são a mesma coisa, e
            // uma corrida da app também é publicada por um relógio que a acompanhe.
            ownWindows = { fromMs ->
                val workouts = workoutDao.endedSince(fromMs)
                    .mapNotNull { s -> s.endedAt?.let { TimeWindow(s.startedAt, it) } }
                val runs = runDao.runsBetween(fromMs, Long.MAX_VALUE)
                    .map { TimeWindow(it.startedAt, it.endedAt) }
                workouts + runs
            },
            latestWeightKg = { weightDao.latest()?.weightKg },
            lastImportAt = { get<AppPreferences>().lastHealthImportAtOnce() },
            setLastImportAt = { get<AppPreferences>().setLastHealthImportAt(it) },
            io = get(IoDispatcher),
            now = { Clock.System.now().toEpochMilliseconds() },
            epochDayOf = { ms ->
                Instant.fromEpochMilliseconds(ms)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date
                    .toEpochDay()
            },

            measurements = { day, pct ->
                get<pt.antares.app.feature.profile.data.BodyMeasurementRepository>()
                    .record(
                        epochDay = day,
                        bodyFatPct = pct,
                        // Vem de uma balança de bioimpedância, e por isso conta como medida:
                        // é o que a torna utilizável para o basal por massa magra.
                        bodyFatSource = pt.antares.app.core.model.BodyFatSource.MEASURED,
                    )
            },
        )
    }

    single {
        val foodDao = get<FoodLogDao>()
        val exerciseLogDao = get<ExerciseLogDao>()
        val workoutDao = get<WorkoutSessionDao>()
        val runDao = get<RunDao>()
        val measurementDao = get<pt.antares.app.core.database.daos.BodyMeasurementDao>()
        val weightDao = get<pt.antares.app.core.database.daos.WeightLogDao>()

        HealthPublisher(
            gateway = getOrNull() ?: NoHealthGateway,
            nutrition = { fromDay ->
                val stats = get<NutritionStatsRepository>()
                foodDao.loggedDaysSince(fromDay).map { day ->
                    val t = foodDao.dayTotals(day)

                    DayNutrition(day, t.kcal, t.proteinG, t.carbsG, t.fatG, stats.totals(day, day).byKey)
                }
            },
            sessions = { fromMs ->
                val runs = runDao.runsBetween(fromMs, Long.MAX_VALUE).map {
                    OutboundSession(
                        clientId = it.id,
                        kind = OutboundKind.RUN,
                        title = it.name,
                        startMs = it.startedAt,
                        endMs = it.endedAt,
                        kcal = it.kcal,
                    )
                }
                val workouts = workoutDao.endedSince(fromMs).mapNotNull { s ->
                    val end = s.endedAt ?: return@mapNotNull null

                    // As calorias do treino não estão na sessão: vivem na linha de exercício
                    // que ela gerou, e é por ali que se vão buscar.
                    val kcal = exerciseLogDao.byRef(s.id)?.kcal ?: 0
                    OutboundSession(
                        clientId = s.id,
                        kind = OutboundKind.WORKOUT,
                        title = s.note?.takeIf { it.isNotBlank() } ?: "Treino",
                        startMs = s.startedAt,
                        endMs = end,
                        kcal = kcal,
                    )
                }
                runs + workouts
            },

            bodyComposition = { fromDay ->
                val pesos = weightDao.exportRows().sortedBy { it.epochDay }
                measurementDao.all()
                    .filter { it.epochDay >= fromDay }
                    .map { m ->

                        val peso = pesos.lastOrNull { it.epochDay <= m.epochDay }?.weightKg
                        OutboundBodyComposition(
                            epochDay = m.epochDay,
                            bodyFatPct = m.bodyFatPct,
                            leanMassKg = if (peso != null && m.bodyFatPct != null) {
                                peso * (1.0 - m.bodyFatPct!! / 100.0)
                            } else {
                                null
                            },
                        )
                    }
            },
            lastPublishAt = { get<AppPreferences>().lastHealthPublishAtOnce() },
            setLastPublishAt = { get<AppPreferences>().setLastHealthPublishAt(it) },
            io = get(IoDispatcher),
            now = { Clock.System.now().toEpochMilliseconds() },
        )
    }

    single {
        val routineDao = get<pt.antares.app.core.database.daos.RoutineDao>()
        DataExporter(

            sources = listOf(
                ExportSource(
                    "user_profile",
                    UserProfileEntity.serializer(),
                    restore = { linhas -> linhas.forEach { get<UserProfileDao>().upsert(it) } },
                ) { get<UserProfileDao>().exportRows() },
                ExportSource(
                    "weight_log",
                    WeightLogEntity.serializer(),
                    restore = { linhas -> linhas.forEach { get<WeightLogDao>().upsert(it) } },
                ) { get<WeightLogDao>().exportRows() },
                ExportSource(
                    "body_measurement_log",
                    BodyMeasurementEntity.serializer(),
                    restore = { linhas -> linhas.forEach { get<pt.antares.app.core.database.daos.BodyMeasurementDao>().upsert(it) } },
                ) { get<pt.antares.app.core.database.daos.BodyMeasurementDao>().exportRows() },
                ExportSource(
                    "goal_history",
                    GoalHistoryEntity.serializer(),
                    restore = { linhas -> linhas.forEach { get<pt.antares.app.core.database.daos.GoalHistoryDao>().upsert(it) } },
                ) { get<pt.antares.app.core.database.daos.GoalHistoryDao>().exportRows() },
                ExportSource(
                    "daily_target_override",
                    DailyTargetOverrideEntity.serializer(),
                    restore = { linhas -> linhas.forEach { get<DailyTargetOverrideDao>().upsert(it) } },
                ) { get<DailyTargetOverrideDao>().exportRows() },
                ExportSource(
                    "food_log",
                    FoodLogEntity.serializer(),
                    restore = { linhas -> linhas.forEach { get<FoodLogDao>().upsert(it) } },
                ) { get<FoodLogDao>().exportRows() },
                ExportSource(
                    "water_log",
                    WaterLogEntity.serializer(),
                    restore = { linhas -> linhas.forEach { get<WaterLogDao>().upsert(it) } },
                ) { get<WaterLogDao>().exportRows() },
                ExportSource(
                    "coach_report",
                    CoachReportEntity.serializer(),
                    restore = { linhas -> linhas.forEach { get<CoachReportDao>().upsert(it) } },
                ) { get<CoachReportDao>().exportRows() },
                ExportSource(
                    "foods",
                    pt.antares.app.core.database.entities.FoodEntity.serializer(),
                    restore = { linhas -> linhas.forEach { get<pt.antares.app.core.database.daos.FoodDao>().upsert(it) } },
                ) { get<pt.antares.app.core.database.daos.FoodDao>().exportRows() },
                ExportSource(
                    "recipe",
                    pt.antares.app.core.database.entities.RecipeEntity.serializer(),
                    restore = { linhas -> linhas.forEach { get<pt.antares.app.core.database.daos.RecipeDao>().upsert(it) } },
                ) { get<pt.antares.app.core.database.daos.RecipeDao>().exportRows() },
                ExportSource(
                    "recipe_ingredient",
                    pt.antares.app.core.database.entities.RecipeIngredientEntity.serializer(),
                    restore = { linhas -> linhas.forEach { get<pt.antares.app.core.database.daos.RecipeIngredientDao>().upsert(it) } },
                ) { get<pt.antares.app.core.database.daos.RecipeIngredientDao>().exportRows() },
                ExportSource(
                    "exercise_log",
                    ExerciseLogEntity.serializer(),
                    restore = { linhas -> linhas.forEach { get<ExerciseLogDao>().upsert(it) } },
                ) { get<ExerciseLogDao>().exportRows() },
                ExportSource(
                    "exercise",
                    pt.antares.app.core.database.entities.ExerciseEntity.serializer(),
                    restore = { linhas -> linhas.forEach { get<pt.antares.app.core.database.daos.ExerciseLibraryDao>().upsert(it) } },
                ) { get<pt.antares.app.core.database.daos.ExerciseLibraryDao>().exportRows() },
                ExportSource(
                    "routine",
                    pt.antares.app.core.database.entities.RoutineEntity.serializer(),
                    restore = { linhas -> linhas.forEach { routineDao.upsertRoutine(it) } },
                ) { routineDao.exportRows() },
                ExportSource(
                    "routine_item",
                    pt.antares.app.core.database.entities.RoutineItemEntity.serializer(),
                    restore = { linhas -> linhas.forEach { routineDao.upsertItem(it) } },
                ) { routineDao.exportItems() },
                ExportSource(
                    "routine_schedule",
                    pt.antares.app.core.database.entities.RoutineScheduleEntity.serializer(),
                    restore = { linhas -> linhas.forEach { get<pt.antares.app.core.database.daos.RoutineScheduleDao>().upsert(it) } },
                ) { get<pt.antares.app.core.database.daos.RoutineScheduleDao>().exportRows() },
                ExportSource(
                    "workout_session",
                    pt.antares.app.core.database.entities.WorkoutSessionEntity.serializer(),
                    restore = { linhas -> linhas.forEach { get<WorkoutSessionDao>().upsertSession(it) } },
                ) { get<WorkoutSessionDao>().exportRows() },
                ExportSource(
                    "workout_set",
                    pt.antares.app.core.database.entities.WorkoutSetEntity.serializer(),
                    restore = { linhas -> linhas.forEach { get<pt.antares.app.core.database.daos.WorkoutSetDao>().upsertSet(it) } },
                ) { get<pt.antares.app.core.database.daos.WorkoutSetDao>().exportRows() },
                ExportSource(
                    "fasting_protocol",
                    pt.antares.app.core.database.entities.FastingProtocolEntity.serializer(),
                    restore = { linhas -> linhas.forEach { get<pt.antares.app.core.database.daos.FastingProtocolDao>().upsert(it) } },
                ) { get<pt.antares.app.core.database.daos.FastingProtocolDao>().exportRows() },
                ExportSource(
                    "fasting_session",
                    pt.antares.app.core.database.entities.FastingSessionEntity.serializer(),
                    restore = { linhas -> linhas.forEach { get<pt.antares.app.core.database.daos.FastingSessionDao>().upsert(it) } },
                ) { get<pt.antares.app.core.database.daos.FastingSessionDao>().exportRows() },
                ExportSource(
                    "run",
                    pt.antares.app.core.database.entities.RunEntity.serializer(),
                    restore = { linhas -> linhas.forEach { get<RunDao>().upsert(it) } },
                ) { get<RunDao>().exportRows() },
                ExportSource(
                    "meal_template",
                    pt.antares.app.core.database.entities.MealTemplateEntity.serializer(),
                    restore = { linhas -> linhas.forEach { get<pt.antares.app.core.database.daos.MealTemplateDao>().upsert(it) } },
                ) { get<pt.antares.app.core.database.daos.MealTemplateDao>().exportRows() },
                ExportSource(
                    "meal_template_item",
                    pt.antares.app.core.database.entities.MealTemplateItemEntity.serializer(),
                    restore = { linhas -> linhas.forEach { get<pt.antares.app.core.database.daos.MealTemplateItemDao>().upsert(it) } },
                ) { get<pt.antares.app.core.database.daos.MealTemplateItemDao>().exportRows() },
                ExportSource(
                    "cycle_log",
                    CycleEntity.serializer(),
                    restore = { linhas -> linhas.forEach { get<CycleDao>().upsert(it) } },
                ) { get<CycleDao>().all() },
                ExportSource(
                    "progress_photo",
                    ProgressPhotoEntity.serializer(),
                    restore = { linhas -> linhas.forEach { get<ProgressPhotoDao>().upsert(it) } },
                ) { get<ProgressPhotoDao>().all() },
                ExportSource(
                    "search_miss",
                    SearchMissEntity.serializer(),

                    restore = null,
                ) { get<SearchMissDao>().top(limit = Int.MAX_VALUE) },
            ),
            appVersion = AppChangelog.CURRENT,
        )
    }

    single {
        val exportador: DataExporter = get()
        BackupImporter(
            sources = exportador.sources,
            io = get(IoDispatcher),
            db = RoomBackupDb(get()),
        )
    }

    single { PrivacyRepository(get(), get(), get(), get(), get(), get(), get(), get(IoDispatcher)) }
}
