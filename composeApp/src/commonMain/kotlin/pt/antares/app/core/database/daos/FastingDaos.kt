package pt.antares.app.core.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import pt.antares.app.core.database.entities.FastingProtocolEntity
import pt.antares.app.core.database.entities.FastingSessionEntity

@Dao
interface FastingProtocolDao {

    @Upsert
    suspend fun upsert(protocol: FastingProtocolEntity)

    @Upsert
    suspend fun upsertAll(protocols: List<FastingProtocolEntity>)

    @Query("SELECT * FROM fasting_protocol WHERE deleted = 0 ORDER BY fastingHours")
    fun observeAll(): Flow<List<FastingProtocolEntity>>

    @Query("SELECT * FROM fasting_protocol WHERE id = :id AND deleted = 0")
    suspend fun byId(id: String): FastingProtocolEntity?

    @Query("SELECT COUNT(*) FROM fasting_protocol")
    suspend fun count(): Int

    // A exportação leva só os protocolos criados pelo utilizador: os de origem voltam a ser
    // semeados ao instalar, e exportá-los duplicava-os na importação.
    @Query("SELECT * FROM fasting_protocol WHERE deleted = 0 AND isCustom = 1")
    suspend fun exportRows(): List<FastingProtocolEntity>
}

@Dao
interface FastingSessionDao {

    @Upsert
    suspend fun upsert(session: FastingSessionEntity)

    @Query("SELECT * FROM fasting_session WHERE id = :id")
    suspend fun byId(id: String): FastingSessionEntity?

    @Query("SELECT * FROM fasting_session WHERE status = 'ACTIVE' AND deleted = 0 ORDER BY startedAt DESC LIMIT 1")
    fun observeActive(): Flow<FastingSessionEntity?>

    @Query("SELECT * FROM fasting_session WHERE status = 'ACTIVE' AND deleted = 0 ORDER BY startedAt DESC LIMIT 1")
    suspend fun activeSession(): FastingSessionEntity?

    // `!= 'ACTIVE'` e não `== 'COMPLETED'`: o histórico mostra também os jejuns
    // interrompidos, porque é neles que está a informação de que a pessoa precisa.
    @Query("SELECT * FROM fasting_session WHERE status != 'ACTIVE' AND deleted = 0 ORDER BY startedAt DESC")
    fun observeHistory(): Flow<List<FastingSessionEntity>>

    @Query("SELECT * FROM fasting_session WHERE status != 'ACTIVE' AND deleted = 0 ORDER BY startedAt ASC")
    suspend fun finishedSessions(): List<FastingSessionEntity>

    @Query("SELECT * FROM fasting_session WHERE status != 'ACTIVE' AND deleted = 0 ORDER BY startedAt ASC")
    fun observeFinished(): Flow<List<FastingSessionEntity>>

    @Query(
        "SELECT * FROM fasting_session WHERE status != 'ACTIVE' AND deleted = 0 " +
            "AND startedAt BETWEEN :fromMs AND :toMs ORDER BY startedAt ASC",
    )
    suspend fun sessionsBetween(fromMs: Long, toMs: Long): List<FastingSessionEntity>

    @Query("SELECT * FROM fasting_session WHERE deleted = 0")
    suspend fun exportRows(): List<FastingSessionEntity>
}
