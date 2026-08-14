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

/**
 * Escrita e remoção em massa dos dados de demonstração, para o ecrã de administração.
 *
 * Tudo aqui assenta numa convenção só: os registos de demonstração têm o identificador a
 * começar por `demo-`. É isso que permite apagá-los todos sem tocar em nada da pessoa, e
 * é por isso que o motor que os gera nunca pode escrever um identificador sem esse prefixo.
 *
 * As inserções abortam em conflito em vez de substituírem: uma colisão significa que
 * havia lá alguma coisa, e substituir apagaria dados reais em silêncio.
 */
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

    // As remoções são a sério e não em suave, ao contrário de todo o resto da app: uma
    // lápide de demonstração continuaria a ocupar o dia nos índices únicos.
    @Query("DELETE FROM weight_log WHERE id LIKE 'demo-%'") suspend fun deleteWeights()

    @Query("DELETE FROM body_measurement_log WHERE id LIKE 'demo-%'") suspend fun deleteMeasurements()

    @Query("DELETE FROM food_log WHERE id LIKE 'demo-%'") suspend fun deleteFoodLogs()

    @Query("DELETE FROM water_log WHERE id LIKE 'demo-%'") suspend fun deleteWater()

    @Query("DELETE FROM workout_set WHERE id LIKE 'demo-%'") suspend fun deleteSets()

    @Query("DELETE FROM workout_session WHERE id LIKE 'demo-%'") suspend fun deleteSessions()

    @Query("DELETE FROM run WHERE id LIKE 'demo-%'") suspend fun deleteRuns()

    @Query("DELETE FROM fasting_session WHERE id LIKE 'demo-%'") suspend fun deleteFasts()

    @Query("DELETE FROM weight_log WHERE deleted = 1") suspend fun purgeWeightTombstones()

    @Query("DELETE FROM water_log WHERE deleted = 1") suspend fun purgeWaterTombstones()

    @Query("DELETE FROM body_measurement_log WHERE deleted = 1")
    suspend fun purgeMeasurementTombstones()

    /**
     * Apaga as lápides das três tabelas que têm índice único num dia
     * (`weight_log`, `water_log` e `body_measurement_log`).
     *
     * O índice é `Index(value = ["epochDay"], unique = true)` e não sabe nada da
     * coluna `deleted`: uma linha apagada em suave continua a ocupar o dia. Como
     * o motor escreve uma linha por dia dos últimos 730 com `OnConflictStrategy.ABORT`,
     * uma só lápide chega para abortar a geração inteira.
     *
     * Só é seguro porque quem chama já confirmou que `realCount()` é zero: sem
     * linhas vivas, uma lápide não é dado de ninguém. E não há sincronização a
     * precisar delas (ver `AntaresDb.DropSyncMeta` e o `NoSyncTest`).
     */
    @Transaction
    suspend fun purgeTombstonesBlockingDemo() {
        purgeWeightTombstones()
        purgeWaterTombstones()
        purgeMeasurementTombstones()
    }

    // As séries antes das sessões: nada as liga na base, mas apagar pela ordem inversa
    // deixaria séries órfãs se a transação falhasse a meio.
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
    // Serve os testes: os registos de demonstração nascem com `dirty` a zero, e um valor
    // diferente de zero denuncia que a app os tratou como escritos pela pessoa.
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

    /**
     * Alimentos para a demonstração.
     *
     * Exige `microsJson` porque sem micros os ecrãs de nutrição não têm o que
     * mostrar, e põe as tabelas portuguesas à frente para a demonstração parecer
     * uma semana de alguém. O `ORDER BY` é estável, portanto a mesma semente dá
     * sempre a mesma demonstração.
     */
    @Query(
        """
        SELECT * FROM foods
        WHERE deleted = 0 AND kcal > 0 AND microsJson IS NOT NULL
        ORDER BY
            (CASE
                WHEN id LIKE 'ptx%' OR id LIKE 'pt-%' OR id LIKE 'tca-%' THEN 0
                ELSE 1
             END) ASC,
            id ASC
        LIMIT :limite
        """,
    )
    suspend fun catalogoParaDemo(limite: Int): List<pt.antares.app.core.database.entities.FoodEntity>

    @Query("SELECT id FROM exercise WHERE deleted = 0 ORDER BY id LIMIT :limite")
    suspend fun exerciciosParaDemo(limite: Int): List<String>

    @Query("SELECT id FROM fasting_protocol WHERE deleted = 0 ORDER BY id LIMIT 1")
    suspend fun protocoloParaDemo(): String?
}
