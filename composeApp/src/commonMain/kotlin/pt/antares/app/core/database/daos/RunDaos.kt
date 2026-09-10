package pt.antares.app.core.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import pt.antares.app.core.database.entities.RunEntity
import pt.antares.app.core.database.entities.TrackPointEntity
import pt.antares.app.feature.running.domain.ActivityType

/**
 * Uma corrida como as listas a mostram: sem a `polyline` e sem os parciais.
 *
 * Serve o painel de treino, o hub, o histórico e o cartão do Hoje — desenham nome, data,
 * distância, tempo, ritmo e kcal, e nenhum deles desenha o percurso. A `RunEntity` inteira traz o traço do
 * mapa e o `splitsJson` de cada corrida, e ler cem corridas para escrever duas linhas era o
 * defeito que a revisão da 2.20.1 tirou do painel de treino e deixou intacto ao lado.
 */
data class CorridaNaListaRow(
    val id: String,
    val name: String,
    val type: ActivityType,
    val startedAt: Long,
    val distanceM: Double,
    val movingS: Long,
    val avgPaceSecPerKm: Int,
    val kcal: Int,
)

/**
 * A semana somada pela base: três números e nenhuma corrida lida.
 *
 * Substitui a soma só da distância que a 2.20.1 tinha criado para o painel de treino. Duas
 * consultas para a mesma semana davam dois caminhos para o mesmo facto, e o esboço do hub
 * pede os três juntos.
 */
data class SemanaDaCorridaRow(
    val corridas: Int,
    val metros: Double,
    val movimentoS: Long,
)

@Dao
interface RunDao {

    @Upsert
    suspend fun upsert(run: RunEntity)

    @Query("SELECT * FROM run WHERE status = 'DONE' AND deleted = 0 ORDER BY startedAt DESC")
    fun observeHistory(): Flow<List<RunEntity>>

    @Query("SELECT * FROM run WHERE status = 'DONE' AND deleted = 0 ORDER BY startedAt ASC")
    suspend fun allDone(): List<RunEntity>

    // Leituras estreitas para o painel de treino e para o hub, e não o `observeHistory`: a
    // `RunEntity` traz a `polyline` e os parciais de cada corrida, e nenhum destes ecrãs
    // desenha o percurso. É a mesma escolha das três contagens do treino — somar na base.
    @Query(
        "SELECT COUNT(*) AS corridas, COALESCE(SUM(distanceM), 0) AS metros, " +
            "COALESCE(SUM(movingS), 0) AS movimentoS FROM run " +
            "WHERE status = 'DONE' AND deleted = 0 AND startedAt >= :deMs AND startedAt < :ateMs",
    )
    fun observeSemana(deMs: Long, ateMs: Long): Flow<SemanaDaCorridaRow>

    // As últimas e todas são a mesma linha em consultas diferentes, de propósito: o hub
    // mostra duas e o histórico mostra a lista inteira. Um `LIMIT` gigante a fazer de «sem
    // limite» seria uma consulta a fingir que é duas.
    @Query(
        "SELECT id, name, type, startedAt, distanceM, movingS, avgPaceSecPerKm, kcal FROM run " +
            "WHERE status = 'DONE' AND deleted = 0 ORDER BY startedAt DESC LIMIT :quantas",
    )
    fun observeUltimas(quantas: Int): Flow<List<CorridaNaListaRow>>

    @Query(
        "SELECT id, name, type, startedAt, distanceM, movingS, avgPaceSecPerKm, kcal FROM run " +
            "WHERE status = 'DONE' AND deleted = 0 ORDER BY startedAt DESC",
    )
    fun observeCorridas(): Flow<List<CorridaNaListaRow>>

    // Só os parciais, e é o que tira aos recordes o custo do histórico inteiro: o
    // `RunPrCalc` precisa deles e de mais nada, e a `polyline` de uma corrida de uma hora é
    // a maior coluna da tabela.
    @Query("SELECT splitsJson FROM run WHERE status = 'DONE' AND deleted = 0")
    fun observeSplitsJson(): Flow<List<String>>

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
