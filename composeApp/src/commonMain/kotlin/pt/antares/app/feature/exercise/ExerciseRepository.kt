package pt.antares.app.feature.exercise

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.ExperimentalResourceApi
import pt.antares.app.core.database.daos.ExerciseLogDao
import pt.antares.app.core.database.entities.ExerciseLogEntity
import pt.antares.app.core.exercise.MetCatalog
import pt.antares.app.core.model.ExerciseOrigin
import pt.antares.app.core.util.Ids
import pt.antares.app.generated.resources.Res

class ExerciseRepository(
    private val dao: ExerciseLogDao,
    private val io: CoroutineDispatcher,
) {
    private fun now() = Clock.System.now().toEpochMilliseconds()

    fun observeDay(epochDay: Long): Flow<List<ExerciseLogEntity>> = dao.observeDay(epochDay)
    fun observeDayKcal(epochDay: Long): Flow<Int> = dao.observeDayKcal(epochDay)

    suspend fun byId(id: String): ExerciseLogEntity? = withContext(io) { dao.byId(id) }

    suspend fun logManual(
        epochDay: Long,
        label: String,
        metId: String?,
        met: Double?,
        durationMin: Int,
        kcal: Int,
    ) = withContext(io) {
        dao.upsert(
            ExerciseLogEntity(
                id = Ids.newUuid(),
                epochDay = epochDay,
                origin = ExerciseOrigin.MANUAL,
                label = label,
                metId = metId,
                met = met,
                durationMin = durationMin,
                kcal = kcal,
                refId = null,
                updatedAt = now(),
            ),
        )
    }

    suspend fun delete(id: String) = withContext(io) { dao.softDelete(id, now()) }

    suspend fun restore(id: String) = withContext(io) { dao.restore(id, now()) }

    /**
     * A tabela de METs, lida do ficheiro empacotado a cada chamada. Não fica em memória
     * porque só o ecrã de registar exercício a usa, e são umas centenas de linhas — guardá-la
     * custaria mais do que voltar a lê-la.
     */
    suspend fun loadCatalog(): MetCatalog = withContext(io) {
        @OptIn(ExperimentalResourceApi::class)
        val bytes = Res.readBytes("files/seed_mets.csv")
        MetCatalog.parse(bytes.decodeToString())
    }
}
