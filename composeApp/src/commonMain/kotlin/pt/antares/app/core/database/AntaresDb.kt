package pt.antares.app.core.database

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Dao
import androidx.room.DeleteColumn
import androidx.room.DeleteTable
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import androidx.room.RoomDatabaseConstructor
import androidx.room.Upsert
import pt.antares.app.core.database.daos.CoachReportDao
import pt.antares.app.core.database.daos.BodyMeasurementDao
import pt.antares.app.core.database.daos.GoalHistoryDao
import pt.antares.app.core.database.daos.CycleDao
import pt.antares.app.core.database.daos.SearchMissDao
import pt.antares.app.core.database.daos.ProgressPhotoDao
import pt.antares.app.core.database.daos.DailyTargetOverrideDao
import pt.antares.app.core.database.daos.ExerciseLibraryDao
import pt.antares.app.core.database.daos.ExerciseLogDao
import pt.antares.app.core.database.daos.FastingProtocolDao
import pt.antares.app.core.database.daos.FastingSessionDao
import pt.antares.app.core.database.daos.FoodDao
import pt.antares.app.core.database.daos.FoodNutrientDao
import pt.antares.app.core.database.daos.FoodLogDao
import pt.antares.app.core.database.daos.MealTemplateDao
import pt.antares.app.core.database.daos.MealTemplateItemDao
import pt.antares.app.core.database.daos.RecipeDao
import pt.antares.app.core.database.daos.RecipeIngredientDao
import pt.antares.app.core.database.daos.RoutineDao
import pt.antares.app.core.database.daos.RunDao
import pt.antares.app.core.database.daos.TrackPointDao
import pt.antares.app.core.database.daos.RoutineScheduleDao
import pt.antares.app.core.database.daos.UserProfileDao
import pt.antares.app.core.database.daos.WorkoutSessionDao
import pt.antares.app.core.database.daos.WorkoutSetDao
import pt.antares.app.core.database.daos.WaterLogDao
import pt.antares.app.core.database.daos.WeightLogDao
import pt.antares.app.core.database.entities.CoachReportEntity
import pt.antares.app.core.database.entities.BodyMeasurementEntity
import pt.antares.app.core.database.entities.GoalHistoryEntity
import pt.antares.app.core.database.entities.CycleEntity
import pt.antares.app.core.database.entities.SearchMissEntity
import pt.antares.app.core.database.entities.ProgressPhotoEntity
import pt.antares.app.core.database.entities.DailyTargetOverrideEntity
import pt.antares.app.core.database.entities.ExerciseLogEntity
import pt.antares.app.core.database.entities.FoodEntity
import pt.antares.app.core.database.entities.FoodFtsEntity
import pt.antares.app.core.database.entities.FoodNutrientEntity
import pt.antares.app.core.database.entities.FoodLogEntity
import pt.antares.app.core.database.entities.MealTemplateEntity
import pt.antares.app.core.database.entities.MealTemplateItemEntity
import pt.antares.app.core.database.entities.RecipeEntity
import pt.antares.app.core.database.entities.RecipeIngredientEntity
import pt.antares.app.core.database.entities.ExerciseEntity
import pt.antares.app.core.database.entities.FastingProtocolEntity
import pt.antares.app.core.database.entities.FastingSessionEntity
import pt.antares.app.core.database.entities.RunEntity
import pt.antares.app.core.database.entities.TrackPointEntity
import pt.antares.app.core.database.entities.RoutineEntity
import pt.antares.app.core.database.entities.RoutineItemEntity
import pt.antares.app.core.database.entities.UserProfileEntity
import pt.antares.app.core.database.entities.WaterLogEntity
import pt.antares.app.core.database.entities.WeightLogEntity
import pt.antares.app.core.database.entities.WorkoutSessionEntity
import pt.antares.app.core.database.entities.RoutineScheduleEntity
import pt.antares.app.core.database.entities.WorkoutSetEntity

/**
 * Chave-valor para marcas da própria base — o que a app precisa de saber sobre os seus
 * dados e não cabe em nenhuma tabela de domínio, como a versão do catálogo já semeado.
 */
@Entity(tableName = "db_info")
data class DbInfo(
    @PrimaryKey val key: String,
    val value: String,
)

@Dao
interface DbInfoDao {
    @Upsert
    suspend fun upsert(info: DbInfo)

    @Query("SELECT * FROM db_info WHERE `key` = :key")
    suspend fun get(key: String): DbInfo?
}

/**
 * A base de dados inteira. Vive no telemóvel e mais lado nenhum: não há servidor com uma
 * cópia, e o `NoSyncTest` falha se voltar a haver.
 *
 * Todas as migrações são automáticas, o que impõe uma regra a quem mexer nas entidades:
 * coluna nova nasce anulável ou com valor por omissão. Uma coluna obrigatória sem
 * omissão obriga a escrever a migração à mão, e sem ela a app rebenta ao abrir com dados
 * antigos lá dentro.
 */
@Database(
    entities = [
        DbInfo::class,
        UserProfileEntity::class,
        WeightLogEntity::class,
        DailyTargetOverrideEntity::class,
        BodyMeasurementEntity::class,
        FoodEntity::class,
        FoodFtsEntity::class,
        FoodNutrientEntity::class,
        FoodLogEntity::class,
        WaterLogEntity::class,
        RecipeEntity::class,
        RecipeIngredientEntity::class,
        ExerciseLogEntity::class,
        ExerciseEntity::class,
        RoutineEntity::class,
        RoutineItemEntity::class,
        WorkoutSessionEntity::class,
        WorkoutSetEntity::class,
        RoutineScheduleEntity::class,
        FastingProtocolEntity::class,
        FastingSessionEntity::class,
        RunEntity::class,
        TrackPointEntity::class,
        CoachReportEntity::class,
        MealTemplateEntity::class,
        MealTemplateItemEntity::class,
        GoalHistoryEntity::class,
        SearchMissEntity::class,
        ProgressPhotoEntity::class,
        CycleEntity::class,
    ],

    version = 25,
    // Os esquemas exportados são o que permite ao Room gerar as migrações automáticas e
    // aos testes verificá-las; sem eles, cada versão seria uma reinstalação.
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 8, to = 9),
        AutoMigration(from = 9, to = 10),
        AutoMigration(from = 10, to = 11),
        AutoMigration(from = 11, to = 12),
        AutoMigration(from = 12, to = 13),
        AutoMigration(from = 13, to = 14),
        AutoMigration(from = 14, to = 15),
        AutoMigration(from = 15, to = 16),
        AutoMigration(from = 16, to = 17),
        AutoMigration(from = 17, to = 18),
        AutoMigration(from = 18, to = 19),
        AutoMigration(from = 19, to = 20),
        AutoMigration(from = 20, to = 21, spec = AntaresDb.DropSyncMeta::class),
        AutoMigration(from = 21, to = 22),
        AutoMigration(from = 22, to = 23),
        AutoMigration(from = 23, to = 24, spec = AntaresDb.DropDirty::class),
        // Acrescenta `recipe.servings`, anulável: quem já tem receitas fica com elas a
        // funcionar exatamente como antes, em gramas.
        AutoMigration(from = 24, to = 25),
    ],
)
@ConstructedBy(AntaresDbConstructor::class)
abstract class AntaresDb : RoomDatabase() {

    // Único passo de migração que não é acrescento: apaga a tabela de estado de
    // sincronização. A app não sincroniza nada, e o `NoSyncTest` garante que continua assim.
    @DeleteTable(tableName = "sync_meta")
    class DropSyncMeta : AutoMigrationSpec

    /**
     * Apaga a coluna `dirty` das 23 tabelas que a tinham. Marcava linhas por enviar para
     * um servidor, e a app não sincroniza desde a v21 — era escrita em todo o lado e lida
     * em sítio nenhum. Ver a [decisão 0001].
     */
    @DeleteColumn(tableName = "user_profile", columnName = "dirty")
    @DeleteColumn(tableName = "weight_log", columnName = "dirty")
    @DeleteColumn(tableName = "daily_target_override", columnName = "dirty")
    @DeleteColumn(tableName = "body_measurement_log", columnName = "dirty")
    @DeleteColumn(tableName = "foods", columnName = "dirty")
    @DeleteColumn(tableName = "food_log", columnName = "dirty")
    @DeleteColumn(tableName = "water_log", columnName = "dirty")
    @DeleteColumn(tableName = "recipe", columnName = "dirty")
    @DeleteColumn(tableName = "recipe_ingredient", columnName = "dirty")
    @DeleteColumn(tableName = "exercise_log", columnName = "dirty")
    @DeleteColumn(tableName = "exercise", columnName = "dirty")
    @DeleteColumn(tableName = "routine", columnName = "dirty")
    @DeleteColumn(tableName = "routine_item", columnName = "dirty")
    @DeleteColumn(tableName = "workout_session", columnName = "dirty")
    @DeleteColumn(tableName = "workout_set", columnName = "dirty")
    @DeleteColumn(tableName = "routine_schedule", columnName = "dirty")
    @DeleteColumn(tableName = "fasting_protocol", columnName = "dirty")
    @DeleteColumn(tableName = "fasting_session", columnName = "dirty")
    @DeleteColumn(tableName = "run", columnName = "dirty")
    @DeleteColumn(tableName = "coach_report", columnName = "dirty")
    @DeleteColumn(tableName = "meal_template", columnName = "dirty")
    @DeleteColumn(tableName = "meal_template_item", columnName = "dirty")
    @DeleteColumn(tableName = "goal_history", columnName = "dirty")
    class DropDirty : AutoMigrationSpec

    abstract fun dbInfoDao(): DbInfoDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun weightLogDao(): WeightLogDao
    abstract fun dailyTargetOverrideDao(): DailyTargetOverrideDao
    abstract fun bodyMeasurementDao(): BodyMeasurementDao
    abstract fun goalHistoryDao(): GoalHistoryDao
    abstract fun searchMissDao(): SearchMissDao
    abstract fun progressPhotoDao(): ProgressPhotoDao
    abstract fun cycleDao(): CycleDao
    abstract fun foodDao(): FoodDao
    abstract fun foodNutrientDao(): FoodNutrientDao
    abstract fun foodLogDao(): FoodLogDao
    abstract fun waterLogDao(): WaterLogDao
    abstract fun recipeDao(): RecipeDao
    abstract fun recipeIngredientDao(): RecipeIngredientDao
    abstract fun exerciseLogDao(): ExerciseLogDao
    abstract fun exerciseLibraryDao(): ExerciseLibraryDao
    abstract fun routineDao(): RoutineDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun workoutSetDao(): WorkoutSetDao
    abstract fun routineScheduleDao(): RoutineScheduleDao
    abstract fun fastingProtocolDao(): FastingProtocolDao
    abstract fun fastingSessionDao(): FastingSessionDao
    abstract fun runDao(): RunDao
    abstract fun trackPointDao(): TrackPointDao
    abstract fun coachReportDao(): CoachReportDao
    abstract fun mealTemplateDao(): MealTemplateDao
    abstract fun mealTemplateItemDao(): MealTemplateItemDao

    abstract fun demoDao(): pt.antares.app.core.database.daos.DemoDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AntaresDbConstructor : RoomDatabaseConstructor<AntaresDb> {
    override fun initialize(): AntaresDb
}
