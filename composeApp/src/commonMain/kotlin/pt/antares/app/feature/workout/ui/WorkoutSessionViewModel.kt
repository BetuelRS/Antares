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
)

data class SessionUiState(
    val loading: Boolean = true,
    val sessionId: String? = null,
    val exercises: List<SessionExerciseUi> = emptyList(),
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
                ) { sets, extras -> Triple(session.id, sets, extras) }
                    .mapLatest { (sessionId, sets, extras) -> buildState(sessionId, session.routineId, sets, extras) }
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
        return SessionUiState(loading = false, sessionId = sessionId, exercises = exercises)
    }

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
