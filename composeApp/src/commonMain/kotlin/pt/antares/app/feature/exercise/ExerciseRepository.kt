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
import pt.antares.app.core.util.MINUTES_PER_DAY
import pt.antares.app.core.util.currentMinuteOfDay
import pt.antares.app.core.util.todayEpochDay
import pt.antares.app.generated.resources.Res
import kotlin.math.roundToInt

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
                // O relógio só sabe a hora de hoje. Num dia passado fica sem hora — a mesma
                // regra da comida, e pela mesma razão: o relógio de agora não é testemunha
                // de ontem.
                startedAtMin = currentMinuteOfDay().takeIf { epochDay == todayEpochDay() },
                refId = null,
                updatedAt = now(),
            ),
        )
    }

    /**
     * Corrige a duração e a hora de um registo escrito à mão.
     *
     * As calorias **escalam com a duração** em vez de voltarem ao [MetCalc] com o peso de
     * hoje: a fórmula é linear na duração, portanto a escala dá o mesmo número — mas com o
     * peso do dia em que se registou. Mexer só na hora deixa-as quietas, que é o que o
     * instantâneo do MET nesta entidade já exige.
     *
     * Duração antiga a zero mantém as calorias: escalar a partir de zero não é uma conta, e
     * um registo assim veio da análise por texto, que dá calorias sem saber o tempo.
     */
    suspend fun updateManual(id: String, durationMin: Int, startedAtMin: Int?) = withContext(io) {
        require(durationMin in 1..MAX_DURATION_MIN) { "duração fora do intervalo: $durationMin" }
        require(startedAtMin == null || startedAtMin in 0 until MINUTES_PER_DAY) {
            "hora fora do dia: $startedAtMin"
        }
        val log = dao.byId(id) ?: return@withContext
        if (log.origin != ExerciseOrigin.MANUAL) return@withContext

        val kcal = if (log.durationMin > 0) {
            (log.kcal.toDouble() * durationMin / log.durationMin).roundToInt()
        } else {
            log.kcal
        }
        dao.upsert(
            log.copy(
                durationMin = durationMin,
                startedAtMin = startedAtMin,
                kcal = kcal,
                updatedAt = now(),
            ),
        )
    }

    suspend fun recentMetIds(limite: Int = RECENTES): List<String> =
        withContext(io) { dao.recentMetIds(limite) }

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

    companion object {

        // Dez horas. Acima disto já não é uma atividade, é um dia — e um engano de dedo no
        // campo de duração propaga-se ao orçamento do dia sem nada o travar.
        const val MAX_DURATION_MIN = 600

        // Quantas atividades diferentes cabem na secção dos recentes.
        const val RECENTES = 5
    }
}
