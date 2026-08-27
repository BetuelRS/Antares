package pt.antares.app.feature.fooddata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.antares.app.core.database.entities.FoodEntity
import pt.antares.app.core.database.entities.MealTemplateEntity
import pt.antares.app.core.model.MealSlot
import pt.antares.app.core.util.TextNormalize
import pt.antares.app.feature.recipe.RecipeRepository
import pt.antares.app.feature.recipe.RecipeSummary
import pt.antares.app.feature.templates.MealTemplateRepository

/**
 * Os separadores da pesquisa. Eram seis.
 *
 * **Nada saiu — mudou de sítio.** Os recentes e os favoritos sobem para dentro do [SEARCH],
 * que já abria nos mais registados e agora abre nos três; as receitas e os modelos juntam-se
 * em [REFEICOES], que é o mesmo par que a 2.18.0 vai unificar por dentro.
 *
 * Seis separadores numa fila que rola é uma escolha entre seis antes de escrever a primeira
 * letra, e quatro deles respondiam à mesma pergunta: «o que é que eu já comi?».
 */
enum class SearchTab { SEARCH, MINE, REFEICOES }

data class FoodSearchState(
    val query: String = "",
    val tab: SearchTab = SearchTab.SEARCH,
    val results: List<FoodEntity> = emptyList(),
    val searching: Boolean = false,

    /**
     * Os mesmos resultados, com os estados do mesmo alimento juntos numa linha.
     *
     * Deriva-se aqui e nao no ecra: e a lista que se desenha, e calcula-la a cada
     * recomposicao era refazer o agrupamento a cada letra que se escreve.
     */
    val grupos: List<GrupoDeEstados> = emptyList(),

    /** Que grupos estao abertos, por identificador do alimento principal. */
    val estadosAbertos: Set<String> = emptySet(),

    // Os resultados de fora são estado à parte, com o seu próprio indicador: chegam muito
    // depois dos locais, e misturá-los faria a lista saltar debaixo do dedo.
    val onlineResults: List<FoodEntity> = emptyList(),
    val searchingOnline: Boolean = false,

    // A pesquisa em linha está desligada, e o ecrã tem de o dizer. Sem isto lia-se como
    // «não há resultados», que é a app a esconder uma escolha da própria pessoa.
    val pesquisaDesligada: Boolean = false,

    // O aviso de que a procura sai do telemóvel, ainda por mostrar. Trava a primeira
    // procura em linha até haver resposta: mostrá-lo depois seria contar o que já foi.
    val pedirAvisoDaOff: Boolean = false,

    val selected: Set<String> = emptySet(),

    // Quais dos resultados a pessoa marcou. Vem à parte desde a v27: o favorito saiu da
    // linha do alimento, e a linha da lista já não o sabe olhando para si própria.
    val favoritos: Set<String> = emptySet(),
)

@OptIn(FlowPreview::class)
class FoodSearchViewModel(
    private val repository: FoodRepository,
    private val offRepository: OffRepository,
    recipeRepository: RecipeRepository,
    private val templateRepository: MealTemplateRepository,
    private val diaryRepository: pt.antares.app.feature.diary.DiaryRepository,
    private val preferences: pt.antares.app.core.datastore.AppPreferences,
) : ViewModel() {

    val recipes: StateFlow<List<RecipeSummary>> = recipeRepository.observeSummaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val templates: StateFlow<List<MealTemplateEntity>> = templateRepository.observeTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _templateApplied = MutableStateFlow(false)
    val templateApplied: StateFlow<Boolean> = _templateApplied

    private val _state = MutableStateFlow(FoodSearchState())
    val state: StateFlow<FoodSearchState> = _state

    private val _openFood = MutableStateFlow<String?>(null)
    val openFood: StateFlow<String?> = _openFood

    val recents: StateFlow<List<FoodEntity>> = repository.observeRecents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val favorites: StateFlow<List<FoodEntity>> = repository.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val myFoods: StateFlow<List<FoodEntity>> = repository.observeMyFoods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val mostLogged: StateFlow<List<FoodEntity>> = repository.observeMostLogged(limit = 20)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * As sugestões que aparecem enquanto se escreve. Saem só do que a pessoa já usou, em
     * memória e sem tocar na base: têm de aparecer a cada tecla, antes de a pesquisa a
     * sério correr.
     */
    val suggestions: StateFlow<List<FoodEntity>> =
        combine(_state, mostLogged, recents) { s, top, rec ->
            val procura = TextNormalize.normalize(s.query.trim())
            if (procura.isEmpty()) {
                emptyList()
            } else {
                (top + rec)
                    .distinctBy { it.id }
                    .filter { TextNormalize.normalize(it.namePt.ifBlank { it.nameEn }).contains(procura) }
                    .take(MAX_SUGGESTIONS)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val queryFlow = MutableStateFlow("")

    init {

        // Os favoritos entram no estado por identificador, e não por lista de alimentos: o
        // que a linha da lista precisa de saber é «este está marcado?», e a lista inteira
        // dos favoritos já vive no separador que a mostra.
        favorites
            .onEach { lista -> _state.update { it.copy(favoritos = lista.map { f -> f.id }.toSet()) } }
            .launchIn(viewModelScope)

        // Duas pesquisas sobre o mesmo texto, com esperas e mínimos diferentes: a local é
        // barata e responde a partir de duas letras, a de rede custa um pedido a um serviço
        // de terceiros e só arranca às três. Os dois fluxos são independentes de propósito —
        // a de rede a falhar não pode levar consigo os resultados que já estão no ecrã.
        queryFlow
            .debounce(300)
            .onEach { q ->
                if (q.length < 2) {
                    _state.update {
                        it.copy(
                            results = emptyList(),
                            grupos = emptyList(),
                            estadosAbertos = emptySet(),
                            searching = false,
                        )
                    }
                } else {
                    _state.update { it.copy(searching = true) }
                    val results = repository.search(q)
                    _state.update {
                        it.copy(
                            results = results,
                            grupos = agruparEstados(results),
                            // Uma procura nova fecha o que estava aberto: os grupos sao
                            // outros, e um identificador que sobrevivesse abria um grupo
                            // que a pessoa nunca tocou.
                            estadosAbertos = emptySet(),
                            searching = false,
                        )
                    }
                }
            }
            .launchIn(viewModelScope)

        queryFlow
            .debounce(400)
            .onEach { q ->
                if (q.length < 3) {
                    _state.update { it.copy(onlineResults = emptyList(), searchingOnline = false) }
                } else {
                    // O indicador sobe antes de qualquer suspensão. Ler a preferência
                    // primeiro deixava uma janela em que a app ia procurar e o ecrã dizia
                    // que estava parado — e quem esperasse por esse estado via a lista
                    // antiga a passar por resultado novo.
                    _state.update { it.copy(searchingOnline = true) }

                    // O aviso trava a procura em vez de a acompanhar: contactar a Open Food
                    // Facts e só depois avisar que se contactou não é um aviso, é um relato.
                    if (!preferences.avisoDaOffVistoOnce()) {
                        _state.update { it.copy(searchingOnline = false, pedirAvisoDaOff = true) }
                        return@onEach
                    }

                    val online = offRepository.procurar(q)
                    val encontrados = (online as? OffSearch.Resultados)?.foods

                    // Tira da lista de fora o que já está em baixo na local: um produto
                    // guardado numa leitura anterior aparecia duas vezes.
                    val localIds = _state.value.results.map { it.id }.toSet()
                    _state.update {
                        it.copy(
                            onlineResults = encontrados.orEmpty().filter { f -> f.id !in localIds },
                            searchingOnline = false,
                            pesquisaDesligada = online is OffSearch.Desligada,
                        )
                    }

                    // Uma procura que não chegou a sair não é uma falha do catálogo: só se
                    // regista o que faltava quando a Open Food Facts respondeu mesmo.
                    if (online is OffSearch.Resultados) recordMissIfWorthIt(q, encontrados?.size)
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Regista a pesquisa que não deu nada — nem local nem em linha. É o que diz ao dono
     * que alimentos faltam ao catálogo. Fica no telemóvel; o ecrã de administração lê-o.
     */
    private fun recordMissIfWorthIt(query: String, onlineHits: Int?) {
        if (!SearchMissRule.shouldRecord(query, _state.value.results.size, onlineHits)) return
        val canonica = SearchMissRule.normalize(query) ?: return
        viewModelScope.launch { repository.recordSearchMiss(canonica) }
    }

    fun setQuery(query: String) {
        _state.update { it.copy(query = query) }
        queryFlow.value = query
    }

    /**
     * A resposta ao aviso da Open Food Facts. É aqui que o interruptor ganha o seu primeiro
     * valor: dizer que não se quer usar desliga-o, em vez de perguntar outra vez amanhã.
     */
    fun responderAoAvisoDaOff(aceita: Boolean) {
        viewModelScope.launch {
            preferences.marcarAvisoDaOffVisto()
            if (!aceita) preferences.setPesquisaEmLinha(false)
            _state.update { it.copy(pedirAvisoDaOff = false, pesquisaDesligada = !aceita) }

            // Reacende a procura com o texto que já lá está: sem isto, quem aceitou tinha de
            // apagar uma letra e voltar a escrevê-la para o pedido sair.
            if (aceita) setQuery(_state.value.query)
        }
    }

    fun setTab(tab: SearchTab) = _state.update { it.copy(tab = tab) }

    // Escolher um resultado de fora guarda-o primeiro no catálogo local: a partir daí é um
    // alimento como os outros, e o ecrã de detalhe abre sem depender de rede.
    fun selectOnline(food: FoodEntity) {
        viewModelScope.launch {
            repository.cacheOnline(food)
            _openFood.value = food.id
        }
    }

    fun consumeOpenFood() {
        _openFood.value = null
    }

    fun applyTemplate(templateId: String, slot: MealSlot, epochDay: Long) {
        viewModelScope.launch {
            templateRepository.applyTemplate(templateId, slot, epochDay)
            _templateApplied.value = true
        }
    }

    fun consumeTemplateApplied() {
        _templateApplied.value = false
    }

    fun toggleSelect(foodId: String) = _state.update { s ->
        s.copy(
            selected = if (foodId in s.selected) s.selected - foodId else s.selected + foodId,
        )
    }

    fun clearSelection() = _state.update { it.copy(selected = emptySet()) }

    /**
     * Abre ou fecha os outros estados de um alimento.
     *
     * Fica no estado e não no ecrã porque a lista se redesenha a cada letra escrita e a cada
     * favorito marcado, e um grupo aberto tinha de sobreviver a isso — fechá-lo por baixo do
     * dedo era o mesmo defeito que a lista a saltar.
     */
    fun alternarEstados(principalId: String) = _state.update { s ->
        s.copy(
            estadosAbertos = if (principalId in s.estadosAbertos) {
                s.estadosAbertos - principalId
            } else {
                s.estadosAbertos + principalId
            },
        )
    }

    fun logSelected(slot: MealSlot, epochDay: Long) {
        val ids = _state.value.selected
        if (ids.isEmpty()) return
        viewModelScope.launch {
            ids.forEach { id ->
                val food = repository.byId(id) ?: return@forEach
                // Registo em lote usa a porção habitual de cada alimento — não há campo
                // onde a escrever quando se marcam vários de uma vez.
                val gramas = diaryRepository.defaultPortionFor(food)
                diaryRepository.logFood(food, gramas, slot, epochDay)
                repository.touchLastUsed(food.id, amountG = gramas)
            }
            _state.update { it.copy(selected = emptySet()) }

            _templateApplied.value = true
        }
    }

    fun deleteTemplate(templateId: String) {
        viewModelScope.launch { templateRepository.deleteTemplate(templateId) }
    }

    fun restoreTemplate(templateId: String) {
        viewModelScope.launch { templateRepository.restoreTemplate(templateId) }
    }

    private companion object {

        const val MAX_SUGGESTIONS = 6
    }
}
