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
import pt.antares.app.core.calc.CargaDoCorpo
import pt.antares.app.core.calc.ExercisePr
import pt.antares.app.core.calc.PrDetector
import pt.antares.app.core.calc.SetEntry
import pt.antares.app.core.database.entities.WorkoutSessionEntity
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
    /**
     * O peso planeado na rotina. Serve de ponto de partida **quando não há fantasma** — um
     * exercício novo, sem histórico. Até à 2.23.0 gravava-se, mostrava-se no editor e mais
     * nada: o campo abria vazio apesar de a rotina saber o número.
     */
    val targetWeightKg: Double? = null,
    val restSec: Int,
    val supersetGroup: Int?,
    val ghost: List<WorkoutSetEntity>,
    val sets: List<WorkoutSetEntity>,
    /** O material do exercício. Só numa barra é que a calculadora de discos diz alguma coisa. */
    val equipamento: String? = null,
    /** Verdade nos 111 exercícios `body only`: a carga vem do corpo e não de um campo. */
    val dePesoDoCorpo: Boolean = false,
    /** Que percentagem do corpo conta neste exercício. 100 por omissão. */
    val percentagemDoCorpo: Int = CargaDoCorpo.PERCENTAGEM_POR_OMISSAO,
    /** A nota deste exercício neste treino. Vazia quando não há nenhuma. */
    val nota: String = "",
    /** O melhor 1RM estimado de sempre, em kg. Nulo quando nenhuma série o permite estimar. */
    val melhorOneRmKg: Double? = null,
    /** Uma série de hoje já bateu o melhor de sempre deste exercício. */
    val recordeHoje: Boolean = false,
) {
    /** O aquecimento não conta para o plano — é para isso que se marca a série como tal. */
    val setsDone: Int get() = sets.count { !it.isWarmup }

    val isComplete: Boolean get() = targetSets > 0 && setsDone >= targetSets
}

/**
 * As quatro origens do ecrã da sessão, juntas para o `combine`.
 *
 * Com nomes e não por destructuring: o Kotlin pára no `Triple`, e o detekt não deixa passar
 * uma desmontagem de quatro — com razão, porque `(a, b, c, d)` numa linha é a forma mais
 * fácil de trocar duas sem que nada se queixe.
 */
private data class FontesDaSessao(
    val sets: List<WorkoutSetEntity>,
    val extras: List<String>,
    val escolhido: String?,
    val notas: Map<String, String>,
    val percentagens: Map<String, Int>,
)

data class SessionUiState(
    val loading: Boolean = true,
    val sessionId: String? = null,
    /** O instante em que o treino começou, para a barra do topo contar o tempo. */
    val startedAt: Long? = null,
    /** O nome da rotina, que passa a ser o título. Nulo num treino livre. */
    val routineName: String? = null,
    /**
     * O peso mais recente que a pessoa registou. **Nulo quando nunca registou nenhum**, e aí
     * os exercícios de peso do corpo dizem-no em vez de inventarem um número.
     */
    val pesoDoCorpoKg: Double? = null,
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

    private val cacheDeMelhores = mutableMapOf<String, Map<String, ExercisePr>>()

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
                    repository.observeNotes(session.id),
                    repository.observePercentagens(),
                ) { sets, extras, escolhido, notas, percentagens ->
                    FontesDaSessao(sets, extras, escolhido, notas, percentagens)
                }
                    .mapLatest { f -> buildState(session, f) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionUiState(loading = true))

    /**
     * Monta a lista de exercícios do treino em curso, juntando três origens: o plano da
     * rotina, o que se acrescentou a meio, e o que já tem séries escritas.
     */
    private suspend fun buildState(
        session: WorkoutSessionEntity,
        fontes: FontesDaSessao,
    ): SessionUiState {
        val sessionId = session.id
        val routineId = session.routineId
        val sets = fontes.sets
        val extras = fontes.extras
        val escolhido = fontes.escolhido
        val notas = fontes.notas

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

        val linhas = exerciseDao.namesByIds(orderedIds.toList())
        val names = linhas.associate { it.id to it.namePt.ifBlank { it.nameEn } }
        val equipamentos = linhas.associate { it.id to it.equipment }
        val setsByEx = sets.groupBy { it.exerciseId }
        val itemByEx = routineItems.associateBy { it.exerciseId }

        val anteriores = melhoresAnteriores(sessionId)

        val exercises = orderedIds.map { exId ->
            val item = itemByEx[exId]
            val hoje = setsByEx[exId].orEmpty().map { SetEntry(it.weightKg, it.reps, it.isWarmup) }
            val antes = anteriores[exId]
            SessionExerciseUi(
                exerciseId = exId,
                name = names[exId] ?: exId,
                // Alvos de recurso para exercícios fora do plano: 3×8-12 com 90 s é o
                // esquema mais comum, e serve de ponto de partida em vez de campos vazios.
                targetSets = item?.targetSets ?: 3,
                repsMin = item?.targetRepsMin ?: 8,
                repsMax = item?.targetRepsMax ?: 12,
                targetWeightKg = item?.targetWeightKg,
                restSec = item?.restSec ?: 90,
                supersetGroup = item?.supersetGroup,
                ghost = repository.ghostSets(exId, sessionId),
                sets = setsByEx[exId].orEmpty().sortedBy { it.setIndex },
                equipamento = equipamentos[exId],
                dePesoDoCorpo = CargaDoCorpo.eDePesoDoCorpo(equipamentos[exId]),
                percentagemDoCorpo = fontes.percentagens[exId] ?: CargaDoCorpo.PERCENTAGEM_POR_OMISSAO,
                nota = notas[exId].orEmpty(),
                // O melhor de sempre, já com o de hoje dentro: é o número que interessa a
                // quem está a decidir o peso da série seguinte, e não o de antes de começar.
                melhorOneRmKg = listOfNotNull(antes?.bestOneRm, PrDetector.best(hoje)?.bestOneRm)
                    .maxOrNull(),
                recordeHoje = PrDetector.detect(antes, hoje).any,
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
            startedAt = session.startedAt,
            pesoDoCorpoKg = repository.pesoDoCorpoKg(),
            routineName = routineId?.let { routineDao.routineById(it)?.name },
            exercises = exercises,
            currentExerciseId = current,
        )
    }

    /**
     * O melhor de sempre de cada exercício, **fora deste treino**, lido uma vez por treino.
     *
     * Duas razões para não ser uma consulta por série gravada. Uma: são os treinos já
     * terminados, e nenhum termina enquanto este decorre — o valor não pode mudar. Outra: uma
     * consulta por exercício eram seis idas à base por toque, que é o custo que a 2.20.0 já
     * tinha tirado da lista de rotinas.
     */
    private suspend fun melhoresAnteriores(sessionId: String): Map<String, ExercisePr> {
        cacheDeMelhores[sessionId]?.let { return it }
        val lidos = repository.previousBests(sessionId)
        cacheDeMelhores.clear()
        cacheDeMelhores[sessionId] = lidos
        return lidos
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

    /**
     * Uma serie nova nasce **sem RPE**, e nao com um por preencher. Ate a 2.21.0 o RPE era um
     * terceiro campo na linha de registo e quase ninguem o escrevia; hoje escreve-se depois,
     * pelo menu da serie, com o [updateRpe]. Um parametro que so recebe `null` era um
     * parametro a dizer que ainda ha um campo, e nao ha.
     */
    fun logSet(
        exercise: SessionExerciseUi,
        weightKg: Double,
        reps: Int,
        warmup: Boolean,
        bodyweightKg: Double? = null,
    ) {
        val sessionId = state.value.sessionId ?: return
        viewModelScope.launch {
            repository.putSet(
                id = Ids.newUuid(),
                sessionId = sessionId,
                exerciseId = exercise.exerciseId,
                // O índice sai do maior já usado, e não da contagem: contar fazia a série
                // seguinte repetir um número depois de se apagar uma a meio, e o histórico
                // ordena por ele. Apagar deixa buracos na numeração, que ninguém vê — o
                // ecrã mostra a ordem, não o número.
                setIndex = (exercise.sets.maxOfOrNull { it.setIndex } ?: -1) + 1,
                weightKg = weightKg,
                reps = reps,
                rpe = null,
                isWarmup = warmup,
                bodyweightKg = bodyweightKg,
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
                // Corrigir o peso não apaga a memória de que parte dele era o corpo — **a não
                // ser que ela deixe de caber**. Uma série corrigida para 40 kg com 78 vindos
                // do corpo não é um facto sobre coisa nenhuma; nesse caso o que fica é o
                // total, sem a repartição, que é o que a app sabe.
                bodyweightKg = set.bodyweightKg?.takeIf { it <= weightKg },
            )
        }
    }

    fun deleteSet(setId: String) = viewModelScope.launch { repository.deleteSet(setId) }

    /**
     * O RPE de uma série já gravada. Existe à parte do [updateSet] porque o RPE saiu da linha
     * de registo na 2.21.0 e passou a escrever-se depois, no menu da série: quem o usa
     * escreve-o quando quer, e quem não o usa deixou de ter um campo a ocupar um terço da
     * linha para nada.
     */
    fun updateRpe(set: WorkoutSetEntity, rpe: Double?) {
        viewModelScope.launch {
            repository.putSet(
                id = set.id,
                sessionId = set.sessionId,
                exerciseId = set.exerciseId,
                setIndex = set.setIndex,
                weightKg = set.weightKg,
                reps = set.reps,
                rpe = rpe,
                isWarmup = set.isWarmup,
                bodyweightKg = set.bodyweightKg,
            )
        }
    }

    fun savePercentagem(exerciseId: String, percentagem: Int) {
        viewModelScope.launch { repository.guardarPercentagem(exerciseId, percentagem) }
    }

    fun saveNote(exerciseId: String, nota: String) {

        val id = state.value.sessionId ?: return
        viewModelScope.launch { repository.saveNote(id, exerciseId, nota) }
    }

    fun restoreSet(setId: String) = viewModelScope.launch { repository.restoreSet(setId) }

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
