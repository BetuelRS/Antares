package pt.antares.app.feature.workout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pt.antares.app.core.database.daos.ExerciseLibraryDao
import pt.antares.app.core.database.daos.RoutineDao
import pt.antares.app.core.database.entities.WorkoutSetEntity
import pt.antares.app.core.util.Ids
import pt.antares.app.feature.workout.WorkoutAlerts
import pt.antares.app.feature.workout.data.SessionPickBus
import pt.antares.app.feature.workout.data.WorkoutSessionRepository

data class SessionExerciseUi(
    val exerciseId: String,
    val name: String,
    val targetSets: Int,
    val repsMin: Int,
    val repsMax: Int,
    val restSec: Int,
    val supersetGroup: Int?,
    val ghost: List<WorkoutSetEntity>,
    val sets: List<WorkoutSetEntity>,
) {
    /** O aquecimento não conta para o plano — é para isso que se marca a série como tal. */
    val setsDone: Int get() = sets.count { !it.isWarmup }

    val isComplete: Boolean get() = targetSets > 0 && setsDone >= targetSets
}

data class SessionUiState(
    val loading: Boolean = true,
    val sessionId: String? = null,
    val exercises: List<SessionExerciseUi> = emptyList(),
    /** O exercício que ocupa o ecrã. Os outros ficam recolhidos, com nome e progresso. */
    val currentExerciseId: String? = null,
    val finishedSessionId: String? = null,
    val discarded: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutSessionViewModel(
    private val repository: WorkoutSessionRepository,
    private val routineDao: RoutineDao,
    private val exerciseDao: ExerciseLibraryDao,
    private val alerts: WorkoutAlerts,
    pickBus: SessionPickBus,
) : ViewModel() {

    private val addedExtras = MutableStateFlow<List<String>>(emptyList())
    private val terminal = MutableStateFlow(SessionUiState(loading = true))

    /** O exercício que a pessoa escolheu. A `null` manda a ordem do plano. */
    private val picked = MutableStateFlow<String?>(null)

    private val _restRemaining = MutableStateFlow<Int?>(null)
    val restRemaining: StateFlow<Int?> = _restRemaining
    private var restJob: Job? = null

    init {

        pickBus.picked.onEach { addExercise(it) }.launchIn(viewModelScope)
    }

    val state: StateFlow<SessionUiState> = repository.observeActive()
        .flatMapLatest { session ->
            if (session == null) {
                MutableStateFlow(SessionUiState(loading = false, sessionId = null))
            } else {
                combine(
                    repository.observeSets(session.id),
                    addedExtras,
                    picked,
                ) { sets, extras, escolhido -> Triple(sets, extras, escolhido) }
                    .mapLatest { (sets, extras, escolhido) ->
                        buildState(session.id, session.routineId, sets, extras, escolhido)
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionUiState(loading = true))

    /**
     * Monta a lista de exercícios do treino em curso, juntando três origens: o plano da
     * rotina, o que se acrescentou a meio, e o que já tem séries escritas.
     */
    private suspend fun buildState(
        sessionId: String,
        routineId: String?,
        sets: List<WorkoutSetEntity>,
        extras: List<String>,
        escolhido: String?,
    ): SessionUiState {

        // `LinkedHashSet` porque a ordem é o plano e as repetições têm de desaparecer: um
        // exercício da rotina que já tem séries não pode aparecer duas vezes. A ordem de
        // inserção é a que a pessoa espera — primeiro o planeado, depois o improvisado.
        val routineItems = routineId?.let { routineDao.itemsOf(it) }.orEmpty()
        val orderedIds = LinkedHashSet<String>()
        routineItems.forEach { orderedIds.add(it.exerciseId) }
        extras.forEach { orderedIds.add(it) }
        // A terceira volta apanha o que sobrou de uma rotina entretanto editada: as séries
        // ficaram e o exercício já não está no plano.
        sets.map { it.exerciseId }.forEach { orderedIds.add(it) }

        val names = exerciseDao.namesByIds(orderedIds.toList())
            .associate { it.id to it.namePt.ifBlank { it.nameEn } }
        val setsByEx = sets.groupBy { it.exerciseId }
        val itemByEx = routineItems.associateBy { it.exerciseId }

        val exercises = orderedIds.map { exId ->
            val item = itemByEx[exId]
            SessionExerciseUi(
                exerciseId = exId,
                name = names[exId] ?: exId,
                // Alvos de recurso para exercícios fora do plano: 3×8-12 com 90 s é o
                // esquema mais comum, e serve de ponto de partida em vez de campos vazios.
                targetSets = item?.targetSets ?: 3,
                repsMin = item?.targetRepsMin ?: 8,
                repsMax = item?.targetRepsMax ?: 12,
                restSec = item?.restSec ?: 90,
                supersetGroup = item?.supersetGroup,
                ghost = repository.ghostSets(exId, sessionId),
                sets = setsByEx[exId].orEmpty().sortedBy { it.setIndex },
            )
        }
        // Um treino faz-se um exercício de cada vez. A escolha da pessoa manda enquanto esse
        // exercício existir — uma rotina editada a meio pode levá-lo embora —, e sem escolha
        // manda a ordem do plano, que aponta ao primeiro por acabar. Com tudo feito fica o
        // último, para o ecrã não ficar sem nenhum aberto.
        val current = escolhido?.takeIf { id -> exercises.any { it.exerciseId == id } }
            ?: exercises.firstOrNull { !it.isComplete }?.exerciseId
            ?: exercises.lastOrNull()?.exerciseId

        return SessionUiState(
            loading = false,
            sessionId = sessionId,
            exercises = exercises,
            currentExerciseId = current,
        )
    }

    fun select(exerciseId: String) { picked.value = exerciseId }

    fun ensureStarted(routineId: String?) {
        viewModelScope.launch {
            repository.startOrResume(routineId)
            alerts.setSessionOngoing(true)
        }
    }

    /**
     * O descanso entre séries é contado em dois sítios: aqui, para o número no ecrã, e no
     * sistema, para a notificação. O ecrã pode morrer com a app em segundo plano; o alarme
     * do sistema não.
     */
    private fun startRest(seconds: Int) {
        if (seconds <= 0) return
        restJob?.cancel()
        alerts.scheduleRestEnd(seconds)
        _restRemaining.value = seconds
        restJob = viewModelScope.launch {
            var left = seconds
            while (left > 0) {
                delay(1000)
                left--
                _restRemaining.value = left.takeIf { it > 0 }
            }
        }
    }

    fun skipRest() {
        restJob?.cancel()
        _restRemaining.value = null
        alerts.cancelRestEnd()
    }

    fun addExercise(exerciseId: String) {
        if (exerciseId !in addedExtras.value) addedExtras.value = addedExtras.value + exerciseId
    }

    fun logSet(exercise: SessionExerciseUi, weightKg: Double, reps: Int, rpe: Double?, warmup: Boolean) {
        val sessionId = state.value.sessionId ?: return
        viewModelScope.launch {
            repository.putSet(
                id = Ids.newUuid(),
                sessionId = sessionId,
                exerciseId = exercise.exerciseId,
                // O índice sai do número de séries já feitas, e é por isso que apagar uma
                // série a meio deixa o próximo índice a repetir um número já usado.
                setIndex = exercise.sets.size,
                weightKg = weightKg,
                reps = reps,
                rpe = rpe,
                isWarmup = warmup,
            )
        }

        // Nem o aquecimento nem os exercícios em supersérie disparam descanso: no superset
        // passa-se logo ao exercício seguinte, que é o que o torna um superset.
        if (!warmup && exercise.supersetGroup == null) startRest(exercise.restSec)

        // Com as séries do plano feitas, largar a escolha devolve o comando à ordem do plano —
        // e é assim que o ecrã avança sozinho para o próximo por acabar. Enquanto faltarem
        // séries, fica pregado a este, mesmo que outro atrás dele esteja por acabar.
        val feitas = exercise.setsDone + if (warmup) 0 else 1
        picked.value = if (!warmup && feitas >= exercise.targetSets) null else exercise.exerciseId
    }

    fun updateSet(set: WorkoutSetEntity, weightKg: Double, reps: Int) {
        viewModelScope.launch {
            repository.putSet(
                id = set.id,
                sessionId = set.sessionId,
                exerciseId = set.exerciseId,
                setIndex = set.setIndex,
                weightKg = weightKg,
                reps = reps,
                rpe = set.rpe,
                isWarmup = set.isWarmup,
            )
        }
    }

    fun deleteSet(setId: String) = viewModelScope.launch { repository.deleteSet(setId) }

    fun finish() {
        val id = state.value.sessionId ?: return
        viewModelScope.launch {
            repository.finish(id)
            clearAlerts()
            terminal.value = SessionUiState(finishedSessionId = id)
        }
    }

    fun discard() {
        val id = state.value.sessionId ?: return
        viewModelScope.launch {
            repository.discard(id)
            clearAlerts()
            terminal.value = SessionUiState(discarded = true)
        }
    }

    private fun clearAlerts() {
        restJob?.cancel()
        _restRemaining.value = null
        alerts.cancelRestEnd()
        alerts.setSessionOngoing(false)
    }

    val exit: StateFlow<SessionUiState> = terminal
}
