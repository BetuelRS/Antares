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

    // A chave tem valor por omissão em todos os métodos: o perfil é único, e quem chama
    // nunca deve ter de saber que a tabela tem chave.
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

    @Query("UPDATE weight_log SET deleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("SELECT * FROM weight_log WHERE deleted = 0 ORDER BY epochDay DESC")
    fun observeAll(): Flow<List<WeightLogEntity>>

    // A pesagem mais recente é o peso de que toda a app depende: entra no basal, nas metas
    // e no orçamento do dia. Sem nenhuma, essas contas não correm de todo.
    @Query("SELECT * FROM weight_log WHERE deleted = 0 ORDER BY epochDay DESC LIMIT 1")
    fun observeLatest(): Flow<WeightLogEntity?>

    @Query("SELECT * FROM weight_log WHERE deleted = 0 ORDER BY epochDay DESC LIMIT 1")
    suspend fun latest(): WeightLogEntity?

    @Query("SELECT * FROM weight_log WHERE deleted = 0 AND epochDay = :epochDay")
    suspend fun byDay(epochDay: Long): WeightLogEntity?

    /**
     * Ignora o filtro de apagados de propósito. O índice único em `epochDay` conta as
     * lápides, por isso quem escreve tem de encontrar a linha morta desse dia e escrever
     * por cima — inserir uma nova falha contra algo que os outros métodos não veem.
     */
    @Query("SELECT * FROM weight_log WHERE epochDay = :epochDay")
    suspend fun byDayForWrite(epochDay: Long): WeightLogEntity?

    @Query("SELECT * FROM weight_log WHERE deleted = 0 AND epochDay BETWEEN :from AND :to ORDER BY epochDay ASC")
    suspend fun range(from: Long, to: Long): List<WeightLogEntity>

    // Sem filtrar apagados: uma pesagem importada e depois apagada não pode voltar na
    // importação seguinte.
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

    /**
     * A medição mais recente **que traz massa gorda**, que não é a mesma coisa que a mais
     * recente: medir só a cintura hoje não apaga a percentagem de gordura da semana
     * passada. É esta que alimenta a cópia guardada no perfil.
     */
    @Query(
        """
        SELECT * FROM body_measurement_log
        WHERE deleted = 0 AND bodyFatPct IS NOT NULL
        ORDER BY epochDay DESC LIMIT 1
        """,
    )
    suspend fun latestWithBodyFat(): BodyMeasurementEntity?

    @Query("SELECT * FROM body_measurement_log WHERE deleted = 0 AND epochDay = :epochDay")
    suspend fun byDay(epochDay: Long): BodyMeasurementEntity?

    // Como no peso e na água: o índice único conta as lápides, e quem escreve tem de as ver.
    @Query("SELECT * FROM body_measurement_log WHERE epochDay = :epochDay")
    suspend fun byDayForWrite(epochDay: Long): BodyMeasurementEntity?

    @Query("SELECT * FROM body_measurement_log WHERE deleted = 0 ORDER BY epochDay ASC")
    suspend fun all(): List<BodyMeasurementEntity>

    @Query("UPDATE body_measurement_log SET deleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("SELECT * FROM body_measurement_log WHERE deleted = 0")
    suspend fun exportRows(): List<BodyMeasurementEntity>
}
