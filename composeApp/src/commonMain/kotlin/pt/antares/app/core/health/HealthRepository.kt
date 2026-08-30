package pt.antares.app.core.health

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import pt.antares.app.core.calc.MetCalc
import pt.antares.app.core.database.entities.ExerciseLogEntity
import pt.antares.app.core.database.entities.WeightLogEntity
import pt.antares.app.core.model.ExerciseOrigin
import pt.antares.app.core.model.WeightSource
import pt.antares.app.core.util.Ids
import pt.antares.app.core.util.epochMillisToMinuteOfDay

data class HealthImport(
    val weights: Int = 0,
    val sessions: Int = 0,

    // Contam-se para o ecrã poder explicar uma importação que trouxe pouco: sem este
    // número, "0 treinos" parecia avaria em vez de trabalho já feito.
    val skippedDuplicates: Int = 0,

    val bodyMeasurements: Int = 0,
) {
    val isEmpty: Boolean get() = weights == 0 && sessions == 0 && bodyMeasurements == 0
}

/**
 * Traz para dentro o que outras apps escreveram no Health Connect: pesagens de uma balança
 * ligada, treinos de um relógio, passos.
 *
 * Recebe escritores em vez dos DAOs para poder ser testado sem base de dados — a lógica
 * que interessa aqui é a de não duplicar, não a de gravar.
 */
class HealthRepository(
    private val gateway: HealthGateway,
    private val weights: WeightWriter,
    private val exercise: ExerciseWriter,
    private val ownWindows: OwnSessionWindows,
    private val latestWeightKg: suspend () -> Double?,
    private val lastImportAt: suspend () -> Long,
    private val setLastImportAt: suspend (Long) -> Unit,
    private val io: CoroutineDispatcher,
    private val now: () -> Long,
    private val epochDayOf: (Long) -> Long,
    private val newId: () -> String = { Ids.newUuid() },

    private val measurements: MeasurementWriter? = null,
) {

    fun interface MeasurementWriter {
        suspend fun record(epochDay: Long, bodyFatPct: Double)
    }

    interface WeightWriter {
        suspend fun importedRefs(): Set<String>
        suspend fun existsOnDay(epochDay: Long): Boolean
        suspend fun insert(entry: WeightLogEntity)
    }

    interface ExerciseWriter {
        suspend fun importedRefs(): Set<String>
        suspend fun insert(log: ExerciseLogEntity)
    }

    fun interface OwnSessionWindows {
        suspend fun since(fromMs: Long): List<TimeWindow>
    }

    fun availability(): HealthAvailability = gateway.availability()

    suspend fun hasPermissions(): Boolean = gateway.hasReadPermissions()

    val permissions: Set<String> get() = gateway.readPermissions

    suspend fun stepsToday(startOfDayMs: Long, endOfDayMs: Long): Long? = withContext(io) {
        if (!gateway.hasReadPermissions()) null else gateway.steps(startOfDayMs, endOfDayMs)
    }

    /** Do mais antigo para o mais recente, e sem incluir hoje — o dia ainda vai a meio. */
    suspend fun stepsPerDay(todayStartMs: Long, days: Int): List<Long> = withContext(io) {
        if (!gateway.hasReadPermissions()) return@withContext emptyList()
        (days downTo 1).mapNotNull { atras ->
            val inicio = todayStartMs - atras * DAY_MS
            gateway.steps(inicio, inicio + DAY_MS)
        }
    }

    /**
     * Importa o que há de novo desde a última vez. Sem serviço ou sem permissão devolve
     * uma importação vazia em vez de falhar: isto também corre no arranque, sozinho.
     */
    suspend fun importNow(): HealthImport = withContext(io) {
        if (gateway.availability() != HealthAvailability.AVAILABLE) return@withContext HealthImport()
        if (!gateway.hasReadPermissions()) return@withContext HealthImport()

        // A marca de água é tirada antes de ler, e não depois: entre a leitura e a gravação
        // pode entrar um registo novo, e assim ele fica para a importação seguinte em vez
        // de se perder.
        val startedAt = now()

        // Na primeira importação não se traz o histórico todo: podem ser anos de treinos de
        // outra app, e encher o diário de uma vez não é o que ninguém espera ao dar
        // permissão.
        val since = lastImportAt().takeIf { it > 0 } ?: (startedAt - FIRST_IMPORT_WINDOW_MS)

        val importedWeights = importWeights(since)
        val importedMeasurements = importBodyComposition(since)
        val (importedSessions, skipped) = importSessions(since)

        setLastImportAt(startedAt)

        HealthImport(
            weights = importedWeights,
            sessions = importedSessions,
            skippedDuplicates = skipped,
            bodyMeasurements = importedMeasurements,
        )
    }

    private suspend fun importBodyComposition(since: Long): Int {
        val writer = measurements ?: return 0
        var count = 0
        for (m in gateway.bodyComposition(since)) {
            val pct = m.bodyFatPct ?: continue
            writer.record(epochDayOf(m.timestampMs), pct)
            count++
        }
        return count
    }

    private suspend fun importWeights(since: Long): Int {
        val known = weights.importedRefs()
        var count = 0
        for (w in gateway.weights(since)) {
            // Duas defesas: o identificador impede reimportar o mesmo registo, e o dia
            // impede sobrepor uma pesagem que a pessoa escreveu à mão. A dela ganha.
            if (w.uid in known) continue
            val day = epochDayOf(w.timestampMs)
            if (weights.existsOnDay(day)) continue
            weights.insert(
                WeightLogEntity(
                    id = newId(),
                    epochDay = day,
                    weightKg = w.kg,
                    note = null,
                    source = WeightSource.HEALTH_CONNECT,
                    sourceRef = w.uid,
                    updatedAt = now(),
                ),
            )
            count++
        }
        return count
    }

    private suspend fun importSessions(since: Long): Pair<Int, Int> {
        val known = exercise.importedRefs()

        // A folga alarga a janela dos treinos próprios para trás: um relógio pode publicar
        // horas depois, e sem isso o treino da app já não estaria na lista de comparação.
        val own = ownWindows.since(since - OWN_WINDOW_SLACK_MS)
        val weightKg = latestWeightKg() ?: DEFAULT_WEIGHT_KG

        var imported = 0
        var skipped = 0

        for (s in gateway.sessions(since)) {
            if (s.uid in known) continue

            if (HealthDedupe.isDuplicate(TimeWindow(s.startMs, s.endMs), own)) {
                skipped++
                continue
            }

            // Sessão de menos de um minuto é engano de arranque, não treino.
            val durationMin = ((s.endMs - s.startMs) / 60_000L).toInt()
            if (durationMin <= 0) continue

            val met = s.met

            // As calorias medidas pelo relógio ganham às calculadas: ele tem sensor de
            // frequência cardíaca, e a tabela de METs é uma média de população.
            val kcal = s.kcal ?: MetCalc.kcal(met ?: 0.0, weightKg, durationMin)

            exercise.insert(
                ExerciseLogEntity(
                    id = newId(),
                    epochDay = epochDayOf(s.startMs),
                    startedAtMin = epochMillisToMinuteOfDay(s.startMs),
                    origin = ExerciseOrigin.HEALTH_CONNECT,
                    label = s.title?.takeIf { it.isNotBlank() } ?: s.activity,
                    metId = null,
                    met = met,
                    durationMin = durationMin,
                    kcal = kcal,
                    refId = s.uid,
                    updatedAt = now(),
                ),
            )
            imported++
        }
        return imported to skipped
    }

    companion object {

        const val DEFAULT_WEIGHT_KG = 70.0

        private const val DAY_MS = 24L * 60 * 60 * 1000

        // Duas semanas de passos, que é o que a sugestão de nível de atividade precisa.
        const val ACTIVITY_WINDOW_DAYS = 14

        // Um mês na primeira importação: chega para o histórico recente fazer sentido sem
        // encher o diário com anos de outra app.
        const val FIRST_IMPORT_WINDOW_MS = 30L * 24 * 60 * 60 * 1000

        const val OWN_WINDOW_SLACK_MS = 24L * 60 * 60 * 1000
    }
}
