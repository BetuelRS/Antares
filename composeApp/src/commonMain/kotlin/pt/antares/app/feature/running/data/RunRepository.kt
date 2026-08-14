package pt.antares.app.feature.running.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import pt.antares.app.core.database.daos.ExerciseLogDao
import pt.antares.app.core.database.daos.RunDao
import pt.antares.app.core.database.entities.ExerciseLogEntity
import pt.antares.app.core.database.entities.RunEntity
import pt.antares.app.core.model.ExerciseOrigin
import pt.antares.app.core.util.Ids
import pt.antares.app.core.util.epochMillisToLocalDate
import pt.antares.app.core.util.toEpochDay
import pt.antares.app.feature.running.domain.ActivityType
import pt.antares.app.feature.running.domain.PolylineCodec
import pt.antares.app.feature.running.domain.RunMetrics
import pt.antares.app.feature.running.domain.Split
import pt.antares.app.feature.running.domain.TrackPruner

class RunRepository(
    private val runDao: RunDao,
    private val exerciseLogDao: ExerciseLogDao,
    private val io: CoroutineDispatcher,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun observeHistory(): Flow<List<RunEntity>> = runDao.observeHistory()

    suspend fun byId(id: String): RunEntity? = withContext(io) { runDao.byId(id) }

    suspend fun allDone(): List<RunEntity> = withContext(io) { runDao.allDone() }

    suspend fun delete(id: String) = withContext(io) {
        runDao.softDelete(id, Clock.System.now().toEpochMilliseconds())
    }

    suspend fun save(
        type: ActivityType,
        metrics: RunMetrics,
        path: List<Pair<Double, Double>>,
        splits: List<Split>,
        name: String,
        note: String,
    ): String = withContext(io) {
        val now = Clock.System.now().toEpochMilliseconds()
        // O percurso é reduzido antes de ser codificado: uma hora de GPS a um ponto por
        // segundo são milhares de coordenadas, e o traço no mapa é o mesmo com uma fração
        // delas. Os pontos crus continuam na `track_point` para quem os quiser.
        val poly = PolylineCodec.encode(TrackPruner.prune(path))
        val splitsJson = json.encodeToString(ListSerializer(Split.serializer()), splits)
        val id = Ids.newUuid()
        runDao.upsert(
            RunEntity(
                id = id,
                type = type,
                // O início deduz-se do fim menos o tempo decorrido, porque a corrida só é
                // gravada quando termina — a app não guarda nada enquanto ela decorre.
                startedAt = now - metrics.elapsedMs,
                endedAt = now,
                distanceM = metrics.distanceM,
                movingS = metrics.movingMs / 1000,
                elapsedS = metrics.elapsedMs / 1000,
                avgPaceSecPerKm = metrics.avgPaceSecPerKm,
                kcal = metrics.kcal,
                elevGainM = metrics.elevGainM,
                polyline = poly,
                splitsJson = splitsJson,
                name = name,
                note = note,
                status = pt.antares.app.feature.running.domain.RunStatus.DONE,
                updatedAt = now,
            ),
        )

        // Uma corrida de zero calorias — parada logo a seguir a começar — não entra no
        // orçamento do dia, mas fica no histórico na mesma.
        if (metrics.kcal > 0) {
            exerciseLogDao.upsert(
                ExerciseLogEntity(
                    id = Ids.newUuid(),
                    epochDay = epochMillisToLocalDate(now - metrics.elapsedMs).toEpochDay(),
                    origin = ExerciseOrigin.RUN,
                    label = name,
                    metId = null,
                    met = null,
                    // Tempo em movimento e não decorrido: as pausas não gastam calorias.
                    durationMin = (metrics.movingMs / 60_000L).toInt(),
                    kcal = metrics.kcal,
                    refId = id,
                    updatedAt = now,
                ),
            )
        }
        id
    }

    fun decodePath(entity: RunEntity): List<Pair<Double, Double>> = PolylineCodec.decode(entity.polyline)

    fun splitsOf(entity: RunEntity): List<Split> =
        if (entity.splitsJson.isBlank()) emptyList()
        else json.decodeFromString(ListSerializer(Split.serializer()), entity.splitsJson)
}
