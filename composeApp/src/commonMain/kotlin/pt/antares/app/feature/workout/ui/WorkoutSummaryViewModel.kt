package pt.antares.app.feature.workout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.antares.app.core.calc.ComparacaoDeTreino
import pt.antares.app.core.calc.ComparacaoDoTreino
import pt.antares.app.core.calc.PrDetector
import pt.antares.app.core.calc.SetEntry
import pt.antares.app.core.calc.TreinoComparavel
import pt.antares.app.core.calc.VolumeCalc
import pt.antares.app.core.database.daos.ExerciseLibraryDao
import pt.antares.app.feature.workout.data.WorkoutSessionRepository

data class WorkoutSummaryState(
    val loading: Boolean = true,
    val durationMin: Int = 0,
    val volume: Double = 0.0,
    val setCount: Int = 0,
    val prLabels: List<String> = emptyList(),

    /** Nulo num treino livre, que não nasceu de rotina nenhuma. */
    val nomeDaRotina: String? = null,

    /**
     * A comparação com os treinos anteriores da mesma rotina.
     *
     * Vazia por duas razões diferentes, e o ecrã distingue-as: um **treino livre** não tem
     * rotina e por isso não tem com que se comparar — e diz-se —, e a **primeira vez** de uma
     * rotina ainda não tem passado.
     */
    val comparacao: ComparacaoDeTreino = ComparacaoDeTreino(ultimaVez = null, media = null),
    val treinoLivre: Boolean = false,
)

class WorkoutSummaryViewModel(
    private val sessionRepository: WorkoutSessionRepository,
    private val exerciseDao: ExerciseLibraryDao,
) : ViewModel() {

    private val _state = MutableStateFlow(WorkoutSummaryState())
    val state: StateFlow<WorkoutSummaryState> = _state

    fun load(sessionId: String) {
        viewModelScope.launch {
            val session = sessionRepository.sessionById(sessionId)
            val sets = sessionRepository.setsForSession(sessionId)
            val working = sets.filter { !it.isWarmup }
            val volume = VolumeCalc.volume(sets.map { SetEntry(it.weightKg, it.reps, it.isWarmup) })
            val duration = session?.let {
                val end = it.endedAt ?: it.startedAt
                ((end - it.startedAt) / MS_POR_MINUTO).toInt().coerceAtLeast(0)
            } ?: 0

            val byExercise = sets.groupBy { it.exerciseId }
            val prExerciseIds = byExercise.filter { (exId, exSets) ->
                val current = exSets.map { SetEntry(it.weightKg, it.reps, it.isWarmup) }
                val previousSets = sessionRepository.doneSetsForExercise(exId, sessionId)
                    .map { SetEntry(it.weightKg, it.reps, it.isWarmup) }
                PrDetector.detect(PrDetector.best(previousSets), current).any
            }.keys
            val names = exerciseDao.namesByIds(prExerciseIds.toList())
                .associate { it.id to it.namePt.ifBlank { it.nameEn } }
            val prLabels = prExerciseIds.mapNotNull { names[it] }

            val routineId = session?.routineId
            val hoje = TreinoComparavel(duration, volume, working.size)
            val anteriores = routineId?.let {
                sessionRepository.anterioresDaRotina(
                    routineId = it,
                    excepto = sessionId,
                    limite = ComparacaoDoTreino.TREINOS_DA_MEDIA,
                )
            }.orEmpty()

            // O nome lê-se **antes** do `update` e não lá dentro: a lambda do `update` é um
            // ciclo de comparar-e-trocar, e o que estiver lá dentro corre outra vez sempre
            // que outra escrita chegue primeiro. Uma consulta à base repetida por isso é uma
            // consulta a mais, e nenhum teste a via.
            val nomeDaRotina = routineId?.let { sessionRepository.nomeDaRotina(it) }

            _state.update {
                it.copy(
                    loading = false,
                    durationMin = duration,
                    volume = volume,
                    setCount = working.size,
                    prLabels = prLabels,
                    nomeDaRotina = nomeDaRotina,
                    comparacao = ComparacaoDoTreino.de(
                        hoje = hoje,
                        anteriores = anteriores.map { linha ->
                            TreinoComparavel(
                                duracaoMin = duracaoMin(linha.startedAt, linha.endedAt),
                                volume = linha.volume,
                                series = linha.series,
                            )
                        },
                    ),
                    // O treino é livre quando não tem rotina, e não quando a rotina foi
                    // apagada: essa continua a dar nome ao passado.
                    treinoLivre = routineId == null,
                )
            }
        }
    }

    private fun duracaoMin(startedAt: Long, endedAt: Long?): Int =
        (((endedAt ?: startedAt) - startedAt) / MS_POR_MINUTO).toInt().coerceAtLeast(0)

    private companion object {
        const val MS_POR_MINUTO = 60_000L
    }
}
