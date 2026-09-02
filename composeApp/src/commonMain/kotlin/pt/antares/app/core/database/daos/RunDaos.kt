package pt.antares.app.core.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import pt.antares.app.core.database.entities.RunEntity
import pt.antares.app.core.database.entities.TrackPointEntity

/** A última corrida, sem o percurso: a linha do painel de treino precisa de três campos. */
data class UltimaCorridaRow(
    val name: String,
    val startedAt: Long,
    val distanceM: Double,
)

@Dao
interface RunDao {

    @Upsert
    suspend fun upsert(run: RunEntity)

    @Query("SELECT * FROM run WHERE status = 'DONE' AND deleted = 0 ORDER BY startedAt DESC")
    fun observeHistory(): Flow<List<RunEntity>>

    @Query("SELECT * FROM run WHERE status = 'DONE' AND deleted = 0 ORDER BY startedAt ASC")
    suspend fun allDone(): List<RunEntity>

    // Duas leituras estreitas para o painel de treino, e não o `observeHistory`: a
    // `RunEntity` traz a `polyline` e os parciais de cada corrida, e ali mostram-se uma
    // distância e um nome. É a mesma escolha das três contagens do treino — somar na base.
    @Query(
        "SELECT COALESCE(SUM(distanceM), 0) FROM run WHERE status = 'DONE' AND deleted = 0 " +
            "AND startedAt >= :deMs AND startedAt < :ateMs",
    )
    fun observeDistanceBetween(deMs: Long, ateMs: Long): Flow<Double>

    @Query(
        "SELECT name, startedAt, distanceM FROM run WHERE status = 'DONE' AND deleted = 0 " +
            "ORDER BY startedAt DESC LIMIT 1",
    )
    fun observeLast(): Flow<UltimaCorridaRow?>

    @Query(
        "SELECT * FROM run WHERE status = 'DONE' AND deleted = 0 " +
            "AND startedAt BETWEEN :fromMs AND :toMs ORDER BY startedAt ASC",
    )
    suspend fun runsBetween(fromMs: Long, toMs: Long): List<RunEntity>

    // Sem filtrar estado nem apagados: é por aqui que uma corrida por terminar, deixada
    // para trás por a app ter sido morta a meio, se recupera.
    @Query("SELECT * FROM run WHERE id = :id")
    suspend fun byId(id: String): RunEntity?

    @Query("UPDATE run SET deleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("UPDATE run SET deleted = 0, updatedAt = :now WHERE id = :id")
    suspend fun restore(id: String, now: Long)

    @Query("SELECT * FROM run WHERE deleted = 0")
    suspend fun exportRows(): List<RunEntity>
}

/**
 * Só inserir, contar e apagar em bloco. Não há leitura de pontos: o percurso desenha-se a
 * partir da polyline resumida na corrida, e uma corrida de uma hora são milhares de linhas
 * que nenhum ecrã percorre.
 */
@Dao
interface TrackPointDao {

    @Insert
    suspend fun insert(point: TrackPointEntity)

    // A inserção em bloco é o que o registo de GPS usa: gravar ponto a ponto durante uma
    // hora abriria uma transação por segundo.
    @Insert
    suspend fun insertAll(points: List<TrackPointEntity>)

    @Query("SELECT COUNT(*) FROM track_point WHERE runId = :runId")
    suspend fun countForRun(runId: String): Int

    @Query("DELETE FROM track_point WHERE runId = :runId")
    suspend fun deleteForRun(runId: String)
}
