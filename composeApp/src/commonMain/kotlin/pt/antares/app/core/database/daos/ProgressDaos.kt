package pt.antares.app.core.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import pt.antares.app.core.database.entities.GoalHistoryEntity
import pt.antares.app.core.database.entities.CycleEntity
import pt.antares.app.core.database.entities.ProgressPhotoEntity
import pt.antares.app.core.database.entities.SearchMissEntity

@Dao
interface GoalHistoryDao {
    @Upsert
    suspend fun upsert(entry: GoalHistoryEntity)

    @Query("SELECT * FROM goal_history WHERE deleted = 0 ORDER BY setOnEpochDay ASC")
    fun observeAll(): Flow<List<GoalHistoryEntity>>

    @Query("SELECT * FROM goal_history WHERE deleted = 0 ORDER BY setOnEpochDay ASC")
    suspend fun all(): List<GoalHistoryEntity>

    @Query("SELECT * FROM goal_history WHERE deleted = 0 ORDER BY setOnEpochDay DESC LIMIT 1")
    suspend fun latest(): GoalHistoryEntity?

    @Query("UPDATE goal_history SET deleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("UPDATE goal_history SET deleted = 0, updatedAt = :now WHERE id = :id")
    suspend fun restore(id: String, now: Long)

    @Query("SELECT * FROM goal_history WHERE deleted = 0")
    suspend fun exportRows(): List<GoalHistoryEntity>
}

@Dao
interface SearchMissDao {

    // Inserir-ou-incrementar numa instrução só, em vez de ler, decidir e escrever: a
    // gravação acontece durante a escrita na pesquisa e não pode competir consigo mesma.
    @Query(
        """
        INSERT INTO search_miss (query, count, lastSeenEpochDay)
        VALUES (:query, 1, :epochDay)
        ON CONFLICT(query) DO UPDATE SET
            count = count + 1,
            lastSeenEpochDay = :epochDay
        """,
    )
    suspend fun record(query: String, epochDay: Long)

    @Query("SELECT * FROM search_miss ORDER BY count DESC, lastSeenEpochDay DESC LIMIT :limit")
    suspend fun top(limit: Int = 100): List<SearchMissEntity>

    // A mesma consulta, mas a observar: quem cria o alimento que faltava sai deste ecrã e
    // volta a ele, e um `LaunchedEffect(Unit)` já não corre outra vez.
    @Query("SELECT * FROM search_miss ORDER BY count DESC, lastSeenEpochDay DESC LIMIT :limit")
    fun observeTop(limit: Int = 100): Flow<List<SearchMissEntity>>


    // Apagar uma só, para a falha sair da lista assim que o alimento que faltava é criado.
    // Sem lápide: a lista existe para dizer o que falta ao catálogo, e o que já não falta
    // não tem de ficar registado em lado nenhum.
    @Query("DELETE FROM search_miss WHERE query = :query")
    suspend fun delete(query: String)

    @Query("DELETE FROM search_miss")
    suspend fun clear()
}

// Fotos e ciclos apagam-se a sério, sem lápide: a linha da foto sem o ficheiro não vale
// nada, e um ciclo apagado tem de libertar o dia para o índice único do `cycle_log`.
@Dao
interface ProgressPhotoDao {
    @Upsert
    suspend fun upsert(photo: ProgressPhotoEntity)

    @Query("SELECT * FROM progress_photo ORDER BY epochDay ASC")
    fun observeAll(): Flow<List<ProgressPhotoEntity>>

    @Query("SELECT * FROM progress_photo ORDER BY epochDay ASC")
    suspend fun all(): List<ProgressPhotoEntity>

    @Query("SELECT * FROM progress_photo WHERE id = :id")
    suspend fun byId(id: String): ProgressPhotoEntity?

    @Query("DELETE FROM progress_photo WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface CycleDao {
    @Upsert
    suspend fun upsert(entry: CycleEntity)

    @Query("SELECT * FROM cycle_log ORDER BY startEpochDay ASC")
    fun observeAll(): Flow<List<CycleEntity>>

    @Query("SELECT * FROM cycle_log ORDER BY startEpochDay ASC")
    suspend fun all(): List<CycleEntity>

    @Query("SELECT * FROM cycle_log ORDER BY startEpochDay DESC LIMIT 1")
    suspend fun latest(): CycleEntity?

    @Query("SELECT * FROM cycle_log WHERE startEpochDay = :startEpochDay")
    suspend fun byStart(startEpochDay: Long): CycleEntity?

    @Query("DELETE FROM cycle_log WHERE id = :id")
    suspend fun delete(id: String)
}
