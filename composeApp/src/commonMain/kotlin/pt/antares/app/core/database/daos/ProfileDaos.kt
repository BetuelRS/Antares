package pt.antares.app.core.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import pt.antares.app.core.database.entities.BodyMeasurementEntity
import pt.antares.app.core.database.entities.DailyTargetOverrideEntity
import pt.antares.app.core.database.entities.UserProfileEntity
import pt.antares.app.core.database.entities.WeightLogEntity

@Dao
interface UserProfileDao {
    @Upsert
    suspend fun upsert(profile: UserProfileEntity)

    @Query("SELECT * FROM user_profile WHERE id = :id AND deleted = 0")
    fun observe(id: String = UserProfileEntity.SINGLETON_ID): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = :id AND deleted = 0")
    suspend fun get(id: String = UserProfileEntity.SINGLETON_ID): UserProfileEntity?

    @Query("SELECT * FROM user_profile WHERE deleted = 0")
    suspend fun exportRows(): List<UserProfileEntity>
}

@Dao
interface WeightLogDao {
    @Upsert
    suspend fun upsert(entry: WeightLogEntity)

    @Query("UPDATE weight_log SET deleted = 1, dirty = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("SELECT * FROM weight_log WHERE deleted = 0 ORDER BY epochDay DESC")
    fun observeAll(): Flow<List<WeightLogEntity>>

    @Query("SELECT * FROM weight_log WHERE deleted = 0 ORDER BY epochDay DESC LIMIT 1")
    fun observeLatest(): Flow<WeightLogEntity?>

    @Query("SELECT * FROM weight_log WHERE deleted = 0 ORDER BY epochDay DESC LIMIT 1")
    suspend fun latest(): WeightLogEntity?

    @Query("SELECT * FROM weight_log WHERE deleted = 0 AND epochDay = :epochDay")
    suspend fun byDay(epochDay: Long): WeightLogEntity?

    @Query("SELECT * FROM weight_log WHERE epochDay = :epochDay")
    suspend fun byDayForWrite(epochDay: Long): WeightLogEntity?

    @Query("SELECT * FROM weight_log WHERE deleted = 0 AND epochDay BETWEEN :from AND :to ORDER BY epochDay ASC")
    suspend fun range(from: Long, to: Long): List<WeightLogEntity>

    @Query("SELECT sourceRef FROM weight_log WHERE sourceRef IS NOT NULL")
    suspend fun importedRefs(): List<String>

    @Query("SELECT * FROM weight_log WHERE deleted = 0")
    suspend fun exportRows(): List<WeightLogEntity>
}

@Dao
interface DailyTargetOverrideDao {
    @Upsert
    suspend fun upsert(override: DailyTargetOverrideEntity)

    @Query("SELECT * FROM daily_target_override WHERE deleted = 0 AND epochDay = :epochDay")
    fun observeByDay(epochDay: Long): Flow<DailyTargetOverrideEntity?>

    @Query("SELECT * FROM daily_target_override WHERE deleted = 0 AND epochDay = :epochDay")
    suspend fun byDay(epochDay: Long): DailyTargetOverrideEntity?

    @Query("SELECT * FROM daily_target_override WHERE deleted = 0")
    suspend fun exportRows(): List<DailyTargetOverrideEntity>
}

@Dao
interface BodyMeasurementDao {
    @Upsert
    suspend fun upsert(entry: BodyMeasurementEntity)

    @Query("SELECT * FROM body_measurement_log WHERE deleted = 0 ORDER BY epochDay ASC")
    fun observeAll(): Flow<List<BodyMeasurementEntity>>

    @Query("SELECT * FROM body_measurement_log WHERE deleted = 0 ORDER BY epochDay DESC LIMIT 1")
    suspend fun latest(): BodyMeasurementEntity?

    @Query("SELECT * FROM body_measurement_log WHERE deleted = 0 ORDER BY epochDay DESC LIMIT 1")
    fun observeLatest(): Flow<BodyMeasurementEntity?>

    @Query("SELECT * FROM body_measurement_log WHERE deleted = 0 AND epochDay = :epochDay")
    suspend fun byDay(epochDay: Long): BodyMeasurementEntity?

    @Query("SELECT * FROM body_measurement_log WHERE epochDay = :epochDay")
    suspend fun byDayForWrite(epochDay: Long): BodyMeasurementEntity?

    @Query("SELECT * FROM body_measurement_log WHERE deleted = 0 ORDER BY epochDay ASC")
    suspend fun all(): List<BodyMeasurementEntity>

    @Query("UPDATE body_measurement_log SET deleted = 1, dirty = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("SELECT * FROM body_measurement_log WHERE deleted = 0")
    suspend fun exportRows(): List<BodyMeasurementEntity>
}
