package pt.antares.app.feature.workout.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import pt.antares.app.core.database.daos.ExerciseLibraryDao
import pt.antares.app.core.database.daos.RoutineDao
import pt.antares.app.core.database.daos.RoutineScheduleDao
import pt.antares.app.core.database.daos.RunDao
import pt.antares.app.core.database.daos.UltimaCorridaRow
import pt.antares.app.core.database.daos.WorkoutSessionDao
import pt.antares.app.core.database.daos.WorkoutSetDao
import pt.antares.app.core.database.entities.RoutineScheduleEntity
import pt.antares.app.core.database.entities.WorkoutSessionEntity
import pt.antares.app.core.model.SessionStatus
import pt.antares.app.core.util.epochDayToLocalDate
import pt.antares.app.core.util.epochMillisToLocalDate
import pt.antares.app.core.util.toEpochDay
import pt.antares.app.core.util.todayEpochDay
import pt.antares.app.core.util.weekStartEpochDay

/** Uma rotina no cartão de destaque, com o que chega para decidir se é esta que se treina. */
data class RotinaEmDestaque(
    val id: String,
    val nome: String,
    val exercicios: List<String>,
    val totalDeExercicios: Int,
    val ultimaDuracaoMin: Int?,
)

/**
 * O que ocupa o cartão principal, por ordem de quem ganha o lugar.
 *
 * A ordem não é de gosto: o plano da semana é uma decisão escrita pela pessoa e ganha sempre
 * a uma dedução a partir do histórico.
 */
sealed interface DestaqueDoTreino {

    /** Há rotina marcada para hoje no plano da semana. */
    data class DeHoje(val rotina: RotinaEmDestaque) : DestaqueDoTreino

    /**
     * Sem plano para hoje: a última que foi treinada, com o dia em que o foi.
     *
     * Vai o dia e não o número de dias passados: «há 1 dias» é o que sai de um contador sem
     * plural, e o ecrã já tem um formatador que diz «ontem» e «sáb, 30 ago».
     */
    data class Ultima(val rotina: RotinaEmDestaque, val ultimaVezEpochDay: Long) : DestaqueDoTreino

    /**
     * Sem plano e sem histórico. Não há o que propor, e o cartão diz isso em vez de escolher
     * uma rotina ao acaso — a app semeia sete, e nenhuma delas é mais tua do que as outras.
     */
    data object Convite : DestaqueDoTreino
}

/** A semana de treino: que dias tiveram treino, e quanto trabalho deram. */
data class SemanaDeTreino(
    val inicioEpochDay: Long,
    val diasComTreino: List<Long>,
    val volume: Double,
    val series: Int,
)

data class RotinaNaLista(
    val id: String,
    val nome: String,
    val totalDeExercicios: Int,
    val ultimaVezEpochDay: Long?,
)

data class TreinoNaLista(
    val id: String,
    /** Nulo num treino livre, que não nasceu de rotina nenhuma. */
    val nomeDaRotina: String?,
    val epochDay: Long,
    val duracaoMin: Int,
    val series: Int,
    val volume: Double,
)

/**
 * A corrida, vista do painel de treino.
 *
 * São os dois atividade, e a corrida deixou de ter separador próprio — o
 * `estudo/esbocos/20-sistema-de-desenho.html` argumenta-o e é o que a barra nova faz. O que
 * entra aqui são **dois factos e um caminho**, e não o hub da corrida outra vez: o que ele
 * mostra é a `estudo/areas/11-corrida.md`, e tem versão própria no plano.
 */
data class CorridaNaSemana(
    val metrosNaSemana: Double,
    val ultima: UltimaCorrida?,
)

data class UltimaCorrida(
    val nome: String,
    val epochDay: Long,
    val metros: Double,
)

data class CentroDeTreino(
    val carregado: Boolean = false,
    /** O instante em que a sessão a decorrer começou, para o ecrã poder contar o tempo. */
    val sessaoActivaDesde: Long? = null,
    val destaque: DestaqueDoTreino = DestaqueDoTreino.Convite,
    val semana: SemanaDeTreino = SemanaDeTreino(0L, emptyList(), 0.0, 0),
    val rotinas: List<RotinaNaLista> = emptyList(),
    val ultimos: List<TreinoNaLista> = emptyList(),
    val corrida: CorridaNaSemana = CorridaNaSemana(0.0, null),
)

/**
 * O modelo de leitura do centro de treino.
 *
 * Vive num repositório e não no ViewModel porque são nove fontes a juntar-se, e o `combine`
 * do Kotlin pára nas cinco: acima disso o ecrã passava a montar a sua própria consulta.
 *
 * **Nada aqui é um dado novo.** Todos estes números já eram calculados noutro sítio da app —
 * o que faltava era chegarem a este ecrã.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutHubRepository(
    private val routineDao: RoutineDao,
    private val sessionDao: WorkoutSessionDao,
    private val setDao: WorkoutSetDao,
    private val scheduleDao: RoutineScheduleDao,
    private val exerciseDao: ExerciseLibraryDao,
    private val runDao: RunDao,
) {

    fun observe(
        hoje: Long = todayEpochDay(),
        zona: TimeZone = TimeZone.currentSystemDefault(),
    ): Flow<CentroDeTreino> {
        val rotinasComContagem = combine(
            routineDao.observeRoutines(),
            routineDao.observeItemCounts(),
            sessionDao.observeLastDoneByRoutine(),
        ) { rotinas, contagens, ultimas -> Triple(rotinas, contagens, ultimas) }

        val treinos = combine(
            sessionDao.observeByStatus(SessionStatus.DONE),
            setDao.observeSetCounts(),
        ) { sessoes, series -> sessoes to series }

        // A semana da corrida é a mesma dos treinos — a ISO do `weekStartEpochDay` —, senão
        // o ecrã dizia duas semanas diferentes ao mesmo tempo. Os limites vão em
        // milissegundos porque é assim que a corrida guarda a hora a que começou.
        val semanaDaCorrida = weekStartEpochDay(hoje)
        val corridaDaSemana = combine(
            runDao.observeDistanceBetween(
                deMs = inicioDoDia(semanaDaCorrida, zona),
                ateMs = inicioDoDia(semanaDaCorrida + DIAS_DA_SEMANA, zona),
            ),
            runDao.observeLast(),
        ) { metros, ultima -> metros to ultima }

        return combine(
            rotinasComContagem,
            treinos,
            scheduleDao.observeAll(),
            sessionDao.observeActive(),
            corridaDaSemana,
        ) { (rotinas, contagens, ultimas), (sessoes, series), horario, activa, daCorrida ->
            val porRotina = contagens.associate { it.routineId to it.total }
            val ultimaPorRotina = ultimas.associate { it.routineId to it.startedAt }
            val seriesPorSessao = series.associate { it.sessionId to it.total }

            // Uma consulta para a lista toda, como o histórico já faz. Fora do `associate`
            // para não a repetir por sessão.
            val volumes = setDao.sessionVolumes().associate { it.sessionId to it.volume }
            val nomes = rotinas.associate { it.id to it.name }

            CentroDeTreino(
                carregado = true,
                sessaoActivaDesde = activa?.startedAt,
                destaque = destaque(
                    hoje = hoje,
                    zona = zona,
                    rotinas = rotinas.map { it.id to it.name },
                    porRotina = porRotina,
                    ultimaPorRotina = ultimaPorRotina,
                    horario = horario,
                    sessoes = sessoes,
                ),
                semana = semana(hoje, zona, sessoes, seriesPorSessao, volumes),
                rotinas = rotinas.map {
                    RotinaNaLista(
                        id = it.id,
                        nome = it.name,
                        totalDeExercicios = porRotina[it.id] ?: 0,
                        ultimaVezEpochDay = ultimaPorRotina[it.id]?.let { ms -> diaDe(ms, zona) },
                    )
                },
                corrida = corrida(daCorrida, zona),
                ultimos = sessoes.take(ULTIMOS_TREINOS).map { s ->
                    TreinoNaLista(
                        id = s.id,
                        nomeDaRotina = s.routineId?.let { nomes[it] },
                        epochDay = diaDe(s.startedAt, zona),
                        duracaoMin = duracaoMin(s),
                        series = seriesPorSessao[s.id] ?: 0,
                        volume = volumes[s.id] ?: 0.0,
                    )
                },
            )
        }
    }

    private suspend fun destaque(
        hoje: Long,
        zona: TimeZone,
        rotinas: List<Pair<String, String>>,
        porRotina: Map<String, Int>,
        ultimaPorRotina: Map<String, Long>,
        horario: List<RoutineScheduleEntity>,
        sessoes: List<WorkoutSessionEntity>,
    ): DestaqueDoTreino {
        val diaIso = epochDayToLocalDate(hoje).dayOfWeek.isoDayNumber
        val marcada = horario.firstOrNull { it.dayOfWeek == diaIso }?.routineId
        val existe = rotinas.associate { it.first to it.second }

        // O plano da semana pode apontar para uma rotina apagada: o dia fica lá e a rotina
        // já não existe. Aí vale o mesmo que não haver plano nenhum.
        marcada?.let { id ->
            existe[id]?.let { nome ->
                return DestaqueDoTreino.DeHoje(emDestaque(id, nome, porRotina, sessoes))
            }
        }

        val ultimaId = ultimaPorRotina.entries
            .filter { existe.containsKey(it.key) }
            .maxByOrNull { it.value }
            ?: return DestaqueDoTreino.Convite

        val nome = existe[ultimaId.key] ?: return DestaqueDoTreino.Convite
        return DestaqueDoTreino.Ultima(
            rotina = emDestaque(ultimaId.key, nome, porRotina, sessoes),
            ultimaVezEpochDay = diaDe(ultimaId.value, zona),
        )
    }

    private suspend fun emDestaque(
        routineId: String,
        nome: String,
        porRotina: Map<String, Int>,
        sessoes: List<WorkoutSessionEntity>,
    ): RotinaEmDestaque {
        val itens = routineDao.itemsOf(routineId)
        val nomes = exerciseDao.namesByIds(itens.map { it.exerciseId }.distinct())
            .associate { it.id to it.namePt.ifBlank { it.nameEn } }

        // A duração sai da última vez que **esta** rotina foi feita, e não da última sessão:
        // «~52 min» de um treino de pernas não descreve um treino de braços.
        val ultima = sessoes.firstOrNull { it.routineId == routineId }

        return RotinaEmDestaque(
            id = routineId,
            nome = nome,
            exercicios = itens.take(EXERCICIOS_NO_DESTAQUE).map { nomes[it.exerciseId] ?: it.exerciseId },
            totalDeExercicios = porRotina[routineId] ?: itens.size,
            ultimaDuracaoMin = ultima?.let { duracaoMin(it) }?.takeIf { it > 0 },
        )
    }

    private fun semana(
        hoje: Long,
        zona: TimeZone,
        sessoes: List<WorkoutSessionEntity>,
        seriesPorSessao: Map<String, Int>,
        volumes: Map<String, Double>,
    ): SemanaDeTreino {
        val inicio = weekStartEpochDay(hoje)
        val fim = inicio + DIAS_DA_SEMANA - 1
        val daSemana = sessoes.filter { diaDe(it.startedAt, zona) in inicio..fim }
        return SemanaDeTreino(
            inicioEpochDay = inicio,
            diasComTreino = daSemana.map { diaDe(it.startedAt, zona) }.distinct(),
            volume = daSemana.sumOf { volumes[it.id] ?: 0.0 },
            series = daSemana.sumOf { seriesPorSessao[it.id] ?: 0 },
        )
    }

    /**
     * Junta o que as duas consultas da corrida devolvem. A distância da semana já vem somada
     * da base; aqui só se converte o instante da última corrida no dia dela, que é o que o
     * formatador de datas do ecrã recebe.
     */
    private fun corrida(
        semana: Pair<Double, UltimaCorridaRow?>,
        zona: TimeZone,
    ): CorridaNaSemana = CorridaNaSemana(
        metrosNaSemana = semana.first,
        ultima = semana.second?.let {
            UltimaCorrida(
                nome = it.name,
                epochDay = diaDe(it.startedAt, zona),
                metros = it.distanceM,
            )
        },
    )

    private fun diaDe(ms: Long, zona: TimeZone): Long =
        epochMillisToLocalDate(ms, zona).toEpochDay()

    private fun inicioDoDia(dia: Long, zona: TimeZone): Long =
        epochDayToLocalDate(dia).atStartOfDayIn(zona).toEpochMilliseconds()

    private fun duracaoMin(s: WorkoutSessionEntity): Int =
        (((s.endedAt ?: s.startedAt) - s.startedAt) / MS_POR_MINUTO).toInt().coerceAtLeast(0)

    private companion object {
        const val ULTIMOS_TREINOS = 3
        const val EXERCICIOS_NO_DESTAQUE = 4
        const val DIAS_DA_SEMANA = 7
        const val MS_POR_MINUTO = 60_000L
    }
}
