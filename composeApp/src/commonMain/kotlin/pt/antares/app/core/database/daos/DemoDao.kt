package pt.antares.app.core.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import pt.antares.app.core.database.entities.BodyMeasurementEntity
import pt.antares.app.core.database.entities.FastingSessionEntity
import pt.antares.app.core.database.entities.FoodLogEntity
import pt.antares.app.core.database.entities.RunEntity
import pt.antares.app.core.database.entities.WaterLogEntity
import pt.antares.app.core.database.entities.WeightLogEntity
import pt.antares.app.core.database.entities.WorkoutSessionEntity
import pt.antares.app.core.database.entities.WorkoutSetEntity

@Dao
interface DemoDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertWeights(rows: List<WeightLogEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMeasurements(rows: List<BodyMeasurementEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertFoodLogs(rows: List<FoodLogEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertWater(rows: List<WaterLogEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSessions(rows: List<WorkoutSessionEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSets(rows: List<WorkoutSetEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRuns(rows: List<RunEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertFasts(rows: List<FastingSessionEntity>)

    @Query("DELETE FROM weight_log WHERE id LIKE 'demo-%'") suspend fun deleteWeights()

    @Query("DELETE FROM body_measurement_log WHERE id LIKE 'demo-%'") suspend fun deleteMeasurements()

    @Query("DELETE FROM food_log WHERE id LIKE 'demo-%'") suspend fun deleteFoodLogs()

    @Query("DELETE FROM water_log WHERE id LIKE 'demo-%'") suspend fun deleteWater()

    @Query("DELETE FROM workout_set WHERE id LIKE 'demo-%'") suspend fun deleteSets()

    @Query("DELETE FROM workout_session WHERE id LIKE 'demo-%'") suspend fun deleteSessions()

    @Query("DELETE FROM run WHERE id LIKE 'demo-%'") suspend fun deleteRuns()

    @Query("DELETE FROM fasting_session WHERE id LIKE 'demo-%'") suspend fun deleteFasts()

    @Transaction
    suspend fun deleteAllDemo() {
        deleteWeights()
        deleteMeasurements()
        deleteFoodLogs()
        deleteWater()
        deleteSets()
        deleteSessions()
        deleteRuns()
        deleteFasts()
    }

    @Query(
        """
        SELECT
            (SELECT COUNT(*) FROM weight_log WHERE id LIKE 'demo-%') +
            (SELECT COUNT(*) FROM body_measurement_log WHERE id LIKE 'demo-%') +
            (SELECT COUNT(*) FROM food_log WHERE id LIKE 'demo-%') +
            (SELECT COUNT(*) FROM water_log WHERE id LIKE 'demo-%') +
            (SELECT COUNT(*) FROM workout_session WHERE id LIKE 'demo-%') +
            (SELECT COUNT(*) FROM workout_set WHERE id LIKE 'demo-%') +
            (SELECT COUNT(*) FROM run WHERE id LIKE 'demo-%') +
            (SELECT COUNT(*) FROM fasting_session WHERE id LIKE 'demo-%')
        """,
    )
    suspend fun demoCount(): Int

    @Query(
        """
        SELECT
            (SELECT COUNT(*) FROM weight_log WHERE id LIKE 'demo-%' AND dirty = 1) +
            (SELECT COUNT(*) FROM body_measurement_log WHERE id LIKE 'demo-%' AND dirty = 1) +
            (SELECT COUNT(*) FROM food_log WHERE id LIKE 'demo-%' AND dirty = 1) +
            (SELECT COUNT(*) FROM water_log WHERE id LIKE 'demo-%' AND dirty = 1) +
            (SELECT COUNT(*) FROM workout_session WHERE id LIKE 'demo-%' AND dirty = 1) +
            (SELECT COUNT(*) FROM workout_set WHERE id LIKE 'demo-%' AND dirty = 1) +
            (SELECT COUNT(*) FROM run WHERE id LIKE 'demo-%' AND dirty = 1) +
            (SELECT COUNT(*) FROM fasting_session WHERE id LIKE 'demo-%' AND dirty = 1)
        """,
    )
    suspend fun sujosDeDemo(): Int

    @Query(
        """
        SELECT
            (SELECT COUNT(*) FROM weight_log WHERE id NOT LIKE 'demo-%' AND deleted = 0) +
            (SELECT COUNT(*) FROM body_measurement_log WHERE id NOT LIKE 'demo-%' AND deleted = 0) +
            (SELECT COUNT(*) FROM food_log WHERE id NOT LIKE 'demo-%' AND deleted = 0) +
            (SELECT COUNT(*) FROM water_log WHERE id NOT LIKE 'demo-%' AND deleted = 0) +
            (SELECT COUNT(*) FROM workout_session WHERE id NOT LIKE 'demo-%' AND deleted = 0) +
            (SELECT COUNT(*) FROM run WHERE id NOT LIKE 'demo-%' AND deleted = 0) +
            (SELECT COUNT(*) FROM fasting_session WHERE id NOT LIKE 'demo-%' AND deleted = 0)
        """,
    )
    suspend fun realCount(): Int

    @Query("SELECT * FROM foods WHERE deleted = 0 AND kcal > 0 ORDER BY id LIMIT :limite")
    suspend fun catalogoParaDemo(limite: Int): List<pt.antares.app.core.database.entities.FoodEntity>

    @Query("SELECT id FROM exercise WHERE deleted = 0 ORDER BY id LIMIT :limite")
    suspend fun exerciciosParaDemo(limite: Int): List<String>

    @Query("SELECT id FROM fasting_protocol WHERE deleted = 0 ORDER BY id LIMIT 1")
    suspend fun protocoloParaDemo(): String?
}
