package pt.antares.app.core.di

import org.koin.dsl.module
import pt.antares.app.core.database.daos.CoachReportDao
import pt.antares.app.core.database.daos.DailyTargetOverrideDao
import pt.antares.app.core.database.daos.ExerciseLogDao
import pt.antares.app.core.database.daos.RunDao
import pt.antares.app.core.database.daos.WorkoutSessionDao
import pt.antares.app.core.database.entities.ExerciseLogEntity
import pt.antares.app.core.database.daos.FoodLogDao
import pt.antares.app.core.database.daos.UserProfileDao
import pt.antares.app.core.database.daos.WaterLogDao
import pt.antares.app.core.database.daos.WeightLogDao
import pt.antares.app.core.database.entities.CoachReportEntity
import pt.antares.app.core.database.entities.BodyMeasurementEntity
import pt.antares.app.core.database.entities.GoalHistoryEntity
import pt.antares.app.core.database.entities.DailyTargetOverrideEntity
import pt.antares.app.core.database.entities.FoodLogEntity
import pt.antares.app.core.database.entities.UserProfileEntity
import pt.antares.app.core.database.entities.WaterLogEntity
import pt.antares.app.core.database.entities.WeightLogEntity
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
import pt.antares.app.core.database.daos.RoutineDao
import pt.antares.app.core.database.daos.BodyMeasurementDao
import pt.antares.app.core.database.daos.GoalHistoryDao
import pt.antares.app.core.database.entities.FoodEntity
import pt.antares.app.core.database.daos.FoodDao
import pt.antares.app.core.database.entities.RecipeEntity
import pt.antares.app.core.database.daos.RecipeDao
import pt.antares.app.core.database.entities.RecipeIngredientEntity
import pt.antares.app.core.database.daos.RecipeIngredientDao
import pt.antares.app.core.database.entities.ExerciseEntity
import pt.antares.app.core.database.daos.ExerciseLibraryDao
import pt.antares.app.core.database.entities.RoutineEntity
import pt.antares.app.core.database.entities.RoutineItemEntity
import pt.antares.app.core.database.entities.RoutineScheduleEntity
import pt.antares.app.core.database.daos.RoutineScheduleDao
import pt.antares.app.core.database.entities.WorkoutSessionEntity
import pt.antares.app.core.database.entities.WorkoutSetEntity
import pt.antares.app.core.database.daos.WorkoutSetDao
import pt.antares.app.core.database.entities.FastingProtocolEntity
import pt.antares.app.core.database.daos.FastingProtocolDao
import pt.antares.app.core.database.entities.FastingSessionEntity
import pt.antares.app.core.database.daos.FastingSessionDao
import pt.antares.app.core.database.entities.RunEntity
import pt.antares.app.core.database.entities.MealTemplateEntity
import pt.antares.app.core.database.daos.MealTemplateDao
import pt.antares.app.core.database.entities.MealTemplateItemEntity
import pt.antares.app.core.database.daos.MealTemplateItemDao

/**
 * Exportar, restaurar e apagar.
 *
 * A lista de fontes é a mesma para os três caminhos, e é isso que o `GdprTableParityTest`
 * guarda: uma tabela nova que não entre aqui fica de fora da exportação sem ninguém dar por
 * isso — e o direito ao apagamento exige que tudo o que se apaga possa antes ser exportado.
 */
val privacyModule = module {
    single {
        val routineDao = get<RoutineDao>()
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
                    restore = { linhas -> linhas.forEach { get<BodyMeasurementDao>().upsert(it) } },
                ) { get<BodyMeasurementDao>().exportRows() },
                ExportSource(
                    "goal_history",
                    GoalHistoryEntity.serializer(),
                    restore = { linhas -> linhas.forEach { get<GoalHistoryDao>().upsert(it) } },
                ) { get<GoalHistoryDao>().exportRows() },
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
                    FoodEntity.serializer(),
                    restore = { linhas -> linhas.forEach { get<FoodDao>().upsert(it) } },
                ) { get<FoodDao>().exportRows() },
                ExportSource(
                    "recipe",
                    RecipeEntity.serializer(),
                    restore = { linhas -> linhas.forEach { get<RecipeDao>().upsert(it) } },
                ) { get<RecipeDao>().exportRows() },
                ExportSource(
                    "recipe_ingredient",
                    RecipeIngredientEntity.serializer(),
                    restore = { linhas -> linhas.forEach { get<RecipeIngredientDao>().upsert(it) } },
                ) { get<RecipeIngredientDao>().exportRows() },
                ExportSource(
                    "exercise_log",
                    ExerciseLogEntity.serializer(),
                    restore = { linhas -> linhas.forEach { get<ExerciseLogDao>().upsert(it) } },
                ) { get<ExerciseLogDao>().exportRows() },
                ExportSource(
                    "exercise",
                    ExerciseEntity.serializer(),
                    restore = { linhas -> linhas.forEach { get<ExerciseLibraryDao>().upsert(it) } },
                ) { get<ExerciseLibraryDao>().exportRows() },
                ExportSource(
                    "routine",
                    RoutineEntity.serializer(),
                    restore = { linhas -> linhas.forEach { routineDao.upsertRoutine(it) } },
                ) { routineDao.exportRows() },
                ExportSource(
                    "routine_item",
                    RoutineItemEntity.serializer(),
                    restore = { linhas -> linhas.forEach { routineDao.upsertItem(it) } },
                ) { routineDao.exportItems() },
                ExportSource(
                    "routine_schedule",
                    RoutineScheduleEntity.serializer(),
                    restore = { linhas -> linhas.forEach { get<RoutineScheduleDao>().upsert(it) } },
                ) { get<RoutineScheduleDao>().exportRows() },
                ExportSource(
                    "workout_session",
                    WorkoutSessionEntity.serializer(),
                    restore = { linhas -> linhas.forEach { get<WorkoutSessionDao>().upsertSession(it) } },
                ) { get<WorkoutSessionDao>().exportRows() },
                ExportSource(
                    "workout_set",
                    WorkoutSetEntity.serializer(),
                    restore = { linhas -> linhas.forEach { get<WorkoutSetDao>().upsertSet(it) } },
                ) { get<WorkoutSetDao>().exportRows() },
                ExportSource(
                    "fasting_protocol",
                    FastingProtocolEntity.serializer(),
                    restore = { linhas -> linhas.forEach { get<FastingProtocolDao>().upsert(it) } },
                ) { get<FastingProtocolDao>().exportRows() },
                ExportSource(
                    "fasting_session",
                    FastingSessionEntity.serializer(),
                    restore = { linhas -> linhas.forEach { get<FastingSessionDao>().upsert(it) } },
                ) { get<FastingSessionDao>().exportRows() },
                ExportSource(
                    "run",
                    RunEntity.serializer(),
                    restore = { linhas -> linhas.forEach { get<RunDao>().upsert(it) } },
                ) { get<RunDao>().exportRows() },
                ExportSource(
                    "meal_template",
                    MealTemplateEntity.serializer(),
                    restore = { linhas -> linhas.forEach { get<MealTemplateDao>().upsert(it) } },
                ) { get<MealTemplateDao>().exportRows() },
                ExportSource(
                    "meal_template_item",
                    MealTemplateItemEntity.serializer(),
                    restore = { linhas -> linhas.forEach { get<MealTemplateItemDao>().upsert(it) } },
                ) { get<MealTemplateItemDao>().exportRows() },
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

    // Uma só instância: o estado da cópia é lido em três ecrãs, e duas instâncias davam
    // duas respostas diferentes à mesma pergunta enquanto uma cópia estivesse a correr.
    single { pt.antares.app.core.privacy.AutoBackup(get(), get(), get(), get(), get()) }
}
