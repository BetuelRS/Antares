package pt.antares.app.feature.workout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pt.antares.app.core.util.todayEpochDay
import pt.antares.app.feature.workout.data.ExerciseLibraryRepository
import pt.antares.app.feature.workout.model.Exercise

data class LibraryFilters(
    val query: String = "",
    val muscle: String? = null,
    val equipment: String? = null,

    /**
     * Substituiu o filtro de nível na 2.27.0. O nível é uma classificação da base de origem
     * e ninguém procura por ela; quem criou um exercício à mão procura-o.
     */
    val soMeus: Boolean = false,
)

/** Um exercício na lista, e se está marcado. */
data class ExercicioNaLista(val exercicio: Exercise, val favorito: Boolean)

data class ExerciseLibraryState(
    val resultados: List<ExercicioNaLista> = emptyList(),
    val favoritos: List<ExercicioNaLista> = emptyList(),
    val maisFeitos: List<ExercicioNaLista> = emptyList(),
    val filtros: LibraryFilters = LibraryFilters(),
) {
    /**
     * «Os teus» só aparecem **antes de se procurar**, e é a mesma regra e a mesma razão dos
     * recentes do ecrã de exercício avulso: assim que alguém escreve ou filtra, a pergunta
     * passou a ser outra, e uma secção fixa no topo empurra a resposta para fora do ecrã.
     */
    val mostrarTeus: Boolean
        get() = (favoritos.isNotEmpty() || maisFeitos.isNotEmpty()) &&
            filtros.query.isBlank() && filtros.muscle == null &&
            filtros.equipment == null && !filtros.soMeus
}

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseLibraryViewModel(
    private val repository: ExerciseLibraryRepository,
) : ViewModel() {

    private val _filters = MutableStateFlow(LibraryFilters())

    private val favoritos = repository.observeFavoritos()

    private val resultados = _filters
        .flatMapLatest { f -> repository.observeFiltered(f.query, f.muscle, f.equipment, f.soMeus) }

    val state: StateFlow<ExerciseLibraryState> =
        combine(
            resultados,
            favoritos,
            repository.observeMaisFeitos(todayEpochDay()),
            _filters,
        ) { lista, marcados, uso, filtros ->

            // O catálogo inteiro chega aqui uma vez por emissão, e é dele que saem as três
            // listas: procurar o exercício de cada linha de uso numa segunda consulta eram
            // cinco idas à base para desenhar cinco linhas.
            val comMarca = lista.map { ExercicioNaLista(it, it.id in marcados) }
            val porId = comMarca.associateBy { it.exercicio.id }

            val favoritosOrdenados = comMarca.filter { it.favorito }

            // Sem repetir o que já está nos favoritos: quem marcou o supino não precisa de o
            // ver duas vezes no mesmo ecrã, e a segunda linha rouba o lugar a outro.
            val maisFeitos = uso
                .asSequence()
                .filter { it.exerciseId !in marcados }
                .mapNotNull { porId[it.exerciseId] }
                .take(ExerciseLibraryRepository.MAIS_FEITOS)
                .toList()

            ExerciseLibraryState(
                resultados = comMarca,
                favoritos = favoritosOrdenados,
                maisFeitos = maisFeitos,
                filtros = filtros,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseLibraryState())

    fun setQuery(q: String) { _filters.value = _filters.value.copy(query = q) }
    fun setMuscle(m: String?) { _filters.value = _filters.value.copy(muscle = m) }
    fun setEquipment(e: String?) { _filters.value = _filters.value.copy(equipment = e) }
    fun setSoMeus(v: Boolean) { _filters.value = _filters.value.copy(soMeus = v) }


    fun alternarFavorito(exerciseId: String, favorito: Boolean) {
        viewModelScope.launch { repository.marcarFavorito(exerciseId, favorito) }
    }
}
