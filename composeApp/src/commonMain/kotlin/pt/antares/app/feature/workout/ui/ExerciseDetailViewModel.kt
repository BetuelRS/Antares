package pt.antares.app.feature.workout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.antares.app.core.calc.DesempenhoDoExercicio
import pt.antares.app.feature.workout.data.ExerciseLibraryRepository
import pt.antares.app.feature.workout.data.WorkoutHistoryRepository
import pt.antares.app.feature.workout.model.Exercise

data class ExerciseDetailState(
    val loading: Boolean = true,
    val exercise: Exercise? = null,
    val progress: List<Float> = emptyList(),

    /** Nulo quando nunca se fez este exercício: o cartão não aparece em vez de aparecer a zeros. */
    val desempenho: DesempenhoDoExercicio? = null,

    val favorito: Boolean = false,

    /** Quantas rotinas vivas usam este exercício. Só se lê ao pedir para apagar. */
    val rotinasCom: Int = 0,
    val aConfirmarApagar: Boolean = false,
    val deleted: Boolean = false,
)

class ExerciseDetailViewModel(
    private val repository: ExerciseLibraryRepository,
    private val historyRepository: WorkoutHistoryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ExerciseDetailState())
    val state: StateFlow<ExerciseDetailState> = _state

    fun load(id: String) {
        viewModelScope.launch {
            val ex = repository.byId(id)
            val progress = historyRepository.exerciseVolumeSeries(id)
            val desempenho = historyRepository.desempenhoDoExercicio(id)
            val favorito = repository.eFavorito(id)
            _state.update {
                it.copy(
                    loading = false,
                    exercise = ex,
                    progress = progress,
                    desempenho = desempenho,
                    favorito = favorito,
                )
            }
        }
    }

    fun alternarFavorito() {
        val ex = _state.value.exercise ?: return
        val novo = !_state.value.favorito
        // O ecrã muda já e a base a seguir: a estrela é a resposta ao toque, e esperar pela
        // escrita fazia-a piscar num aparelho lento.
        _state.update { it.copy(favorito = novo) }
        viewModelScope.launch { repository.marcarFavorito(ex.id, novo) }
    }

    /**
     * Apagar um exercício criado à mão passa a perguntar, e a pergunta diz **quantas rotinas
     * o usam** — é o defeito concreto 3 da `estudo/areas/09-treino-biblioteca.md`: era a única
     * acção destrutiva da app sem confirmação nem desfazer, num ecrã onde tudo o resto tem uma
     * das duas.
     *
     * A contagem lê-se aqui e não ao abrir o ecrã: é uma consulta que só interessa a quem
     * carregou no caixote, e abrir um exercício é o que se faz mil vezes mais.
     */
    fun pedirParaApagar() {
        val ex = _state.value.exercise ?: return
        if (!ex.isCustom) return
        viewModelScope.launch {
            val rotinas = historyRepository.rotinasCom(ex.id)
            _state.update { it.copy(rotinasCom = rotinas, aConfirmarApagar = true) }
        }
    }

    fun cancelarApagar() {
        _state.update { it.copy(aConfirmarApagar = false) }
    }

    fun confirmarApagar() {
        val ex = _state.value.exercise ?: return
        if (!ex.isCustom) return
        viewModelScope.launch {
            repository.deleteCustom(ex.id)
            _state.update { it.copy(aConfirmarApagar = false, deleted = true) }
        }
    }
}
