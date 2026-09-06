package pt.antares.app.feature.workout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import pt.antares.app.core.calc.AlvoDoExercicio
import pt.antares.app.core.calc.Progressao
import pt.antares.app.core.calc.ProximoAlvo
import pt.antares.app.core.calc.SerieDaUltimaVez
import pt.antares.app.core.model.RegraDeProgressao
import pt.antares.app.core.model.UnitSystem
import pt.antares.app.feature.profile.data.ProfileRepository
import pt.antares.app.feature.workout.data.RoutineRepository
import pt.antares.app.feature.workout.data.RoutineWithItems

/**
 * A rotina no editor, e o que a progressão propõe para cada linha.
 *
 * As propostas são calculadas aqui e não no ecrã porque dependem da unidade da pessoa — o
 * degrau por omissão é 2,5 kg ou 5 lb —, e desenhar uma linha não tem de saber disso.
 */
data class RotinaNoEditor(
    val detalhe: RoutineWithItems,

    /** Por identificador de item da rotina. Ausente onde a regra não tem resposta. */
    val propostas: Map<String, ProximoAlvo> = emptyMap(),

    /** Por identificador de exercício: o que se fez da última vez, para a linha o poder dizer. */
    val ultimas: Map<String, List<SerieDaUltimaVez>> = emptyMap(),

    val unidades: UnitSystem = UnitSystem.METRIC,

    /**
     * O degrau que esta rotina usa mesmo: o que foi escolhido, ou o da unidade.
     *
     * Campo e não um `get()` que o recalcule: é o mesmo número que calcula as propostas e que
     * as explica no ecrã, e duas contas iguais em dois sítios são duas contas que podem passar
     * a ser diferentes.
     */
    val incrementoKg: Double = Progressao.DEGRAU_KG,
)

@OptIn(ExperimentalCoroutinesApi::class)
class RoutineEditViewModel(
    private val repository: RoutineRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val routineId = MutableStateFlow<String?>(null)

    /**
     * A rotina, e a progressão por cima dela.
     *
     * As últimas séries são lidas a cada emissão do detalhe e não uma vez ao abrir: acrescentar
     * um exercício à rotina muda a pergunta, e uma lista carregada uma só vez deixava a linha
     * nova sem o «da última vez» que todas as outras têm.
     */
    val estado: StateFlow<RotinaNoEditor?> = routineId
        .filterNotNull()
        .flatMapLatest { repository.observeDetail(it) }
        .mapLatest { detalhe ->
            if (detalhe == null) return@mapLatest null

            val unidades = profileRepository.profileOnce()?.unitSystem ?: UnitSystem.METRIC
            val ultimas = repository.ultimasSeriesDaRotina(detalhe.routine.id)
            val incremento = detalhe.routine.incrementoKg
                ?: Progressao.incrementoPorOmissao(unidades)

            RotinaNoEditor(
                detalhe = detalhe,
                propostas = detalhe.items.mapNotNull { view ->
                    val item = view.item
                    val proposta = Progressao.proximo(
                        alvo = AlvoDoExercicio(
                            series = item.targetSets,
                            repsMin = item.targetRepsMin,
                            repsMax = item.targetRepsMax,
                            pesoKg = item.targetWeightKg,
                        ),
                        ultima = ultimas[item.exerciseId].orEmpty(),
                        regra = detalhe.routine.progressao,
                        incrementoKg = incremento,
                    )
                    proposta?.let { item.id to it }
                }.toMap(),
                ultimas = ultimas,
                unidades = unidades,
                incrementoKg = incremento,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted

    fun start(id: String) { routineId.value = id }

    fun rename(name: String) {
        val id = routineId.value ?: return
        viewModelScope.launch { repository.rename(id, name) }
    }

    /**
     * A regra e o degrau, gravados juntos porque se escolhem juntos.
     *
     * O incremento nulo é o que diz «usa o da minha unidade», e é diferente de gravar 2,5:
     * gravado, ficava 2,5 kg para sempre, mesmo para quem mudasse para libras no dia seguinte.
     */
    fun setProgressao(regra: RegraDeProgressao, incrementoKg: Double?) {
        val id = routineId.value ?: return
        viewModelScope.launch { repository.setProgressao(id, regra, incrementoKg) }
    }

    fun updateTargets(itemId: String, sets: Int, repsMin: Int, repsMax: Int, weightKg: Double?, restSec: Int) =
        viewModelScope.launch { repository.updateTargets(itemId, sets, repsMin, repsMax, weightKg, restSec) }

    /**
     * Grava a ordem que o dedo deixou. Quem chama guarda a ordem **anterior** para a poder
     * desfazer: mover era a única acção do editor sem desfazer, e é mais fácil de fazer por
     * engano do que apagar — as setas eram pequenas e estavam coladas ao menu.
     */
    fun reordenar(ordem: List<String>) {
        viewModelScope.launch { repository.reorderItems(ordem) }
    }

    /**
     * A ordem em que os exercícios estão agora, para se poder voltar a ela.
     *
     * Lê o [estado], que é o que o ecrã colecciona. Lia um segundo `StateFlow` derivado dele —
     * e como ninguém o coleccionava, ficava parado no `null` inicial: o desfazer do arrastar
     * gravava uma ordem vazia, sem nada no ecrã a dizê-lo.
     */
    fun ordemActual(): List<String> = estado.value?.detalhe?.items.orEmpty().map { it.item.id }

    fun duplicar(nome: String, aoTerminar: (String) -> Unit) {
        val id = routineId.value ?: return
        viewModelScope.launch { repository.duplicateRoutine(id, nome)?.let(aoTerminar) }
    }

    fun setSuperset(itemId: String, group: Int?) =
        viewModelScope.launch { repository.setSuperset(itemId, group) }

    fun deleteItem(itemId: String) = viewModelScope.launch { repository.deleteItem(itemId) }

    fun restoreItem(itemId: String) = viewModelScope.launch { repository.restoreItem(itemId) }

    fun deleteRoutine() {
        val id = routineId.value ?: return
        viewModelScope.launch {
            repository.deleteRoutine(id)
            _deleted.value = true
        }
    }

    /** O identificador é guardado pelo ecrã antes de ele fechar: aqui já não há estado. */
    fun restoreRoutine(id: String) = viewModelScope.launch { repository.restoreRoutine(id) }
}

class RoutineItemPickViewModel(
    private val repository: RoutineRepository,
) : ViewModel() {

    private val _jaNaRotina = MutableStateFlow<Set<String>>(emptySet())

    /**
     * O que já está na rotina que se está a montar, para a lista o poder marcar.
     *
     * Marca e não esconde: o defeito concreto 2 da `estudo/areas/09-treino-biblioteca.md` é
     * a lista mostrar os 873 sem dizer que um deles já lá está. Esconder resolvia isso e
     * tirava a quem repete o mesmo exercício de propósito na mesma rotina a única forma de o
     * fazer — e sem o dizer, que é pior.
     */
    val jaNaRotina: StateFlow<Set<String>> = _jaNaRotina

    fun carregar(routineId: String) {
        viewModelScope.launch {
            _jaNaRotina.value = repository.exerciciosDaRotina(routineId)
        }
    }

    fun add(routineId: String, exerciseId: String, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.addItem(routineId, exerciseId)
            onDone()
        }
    }
}
