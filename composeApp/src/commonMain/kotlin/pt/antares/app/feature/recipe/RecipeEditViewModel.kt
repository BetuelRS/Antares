package pt.antares.app.feature.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.antares.app.core.calc.RecipeNutrition
import pt.antares.app.core.confecao.MetodoDeConfecao
import pt.antares.app.core.database.entities.FoodEntity
import pt.antares.app.core.database.entities.RecipeIngredientEntity
import pt.antares.app.core.database.entities.RecipeStepEntity
import pt.antares.app.core.model.UnitSystem
import pt.antares.app.core.util.UnitConversions
import kotlin.math.roundToInt

data class RecipeEditState(
    val recipeId: String? = null,
    val name: String = "",
    val yieldText: String = "",
    val servingsText: String = "",
    val rows: List<IngredientRow> = emptyList(),
    val nutrition: RecipeNutrition = RecipeNutrition(0, 0.0, 0.0, 0.0, 0.0),
    val saved: Boolean = false,

    /** O método com que o prato foi cozinhado, ou nulo na receita que não vai ao lume. */
    val metodo: String? = null,

    /**
     * Os métodos que **alguma** família presente conhece. Vazio esconde a pergunta — uma
     * salada não tem nada a perguntar.
     */
    val metodos: List<MetodoDeConfecao> = emptyList(),

    /**
     * O peso final que as tabelas prevêem, para quem ainda não pôs a panela na balança.
     *
     * Nunca se grava sozinho: é uma sugestão ao lado do campo vazio, e o peso que a pessoa
     * mede ganha sempre a uma mediana de pratos que não são este.
     */
    val pesoSugerido: Double? = null,

    /**
     * O peso escrito à mão que **nenhum** método de confeção explica.
     *
     * Nulo é o caso normal: ou não há cobertura para comparar, ou o que está escrito cabe
     * no que as tabelas prevêem. Quando não é nulo, traz o intervalo que elas prevêem, para
     * o ecrã poder dizer o número em vez de dizer «parece errado».
     *
     * **Avisa e nunca corrige.** Quem pesou o tacho tem razão contra uma mediana de pratos
     * que não são este; o que a app pode fazer é não deixar passar em silêncio um 2000 onde
     * se quis escrever 200.
     */
    val pesoForaDoPrevisto: ClosedFloatingPointRange<Double>? = null,
) {
    val yieldGrams: Double? get() = yieldText.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }

    val servings: Int? get() = servingsText.toIntOrNull()?.takeIf { it in 1..MAX_DOSES }

    /**
     * Quanto pesa uma dose. Sai do peso total — o final se estiver escrito, o cru se não —,
     * e é o que permite registar «uma dose» em vez de adivinhar as gramas de uma lasanha.
     */
    val gramsPerServing: Double?
        get() = servings?.let { n -> nutrition.basisGrams.takeIf { it > 0 }?.div(n) }

    val valid: Boolean get() = name.trim().length >= 2 && rows.isNotEmpty()
}

// Cinquenta doses é uma cozinha industrial, não uma receita de quem regista o que come.
private const val MAX_DOSES = 50

// O que um ingrediente novo pesa até alguém escrever outra coisa. Era já o que o ecrã de
// escolha usava; fica no mesmo valor para a folha não mudar o resultado de nada.
private const val GRAMAS_POR_OMISSAO = 100.0

private const val MAX_RESULTADOS = 20

// Um passo é uma instrução, não um parágrafo de livro. O tecto existe para a lista se
// continuar a ler de relance com o tacho ao lume.
private const val MAX_TEXTO_DE_PASSO = 280

/**
 * A procura de ingredientes que se abre por cima da receita.
 *
 * Existe para acrescentar ingredientes **sem sair do ecrã**. Até aqui cada ingrediente era
 * uma viagem a um ecrã de pesquisa inteiro e uma volta atrás: uma receita de oito
 * ingredientes eram oito idas e oito regressos, e a receita saía do ecrã de cada vez.
 *
 * A folha não se fecha ao escolher, e é isso que a torna útil: escolhe-se, vê-se a linha
 * aparecer por baixo, e escolhe-se outra vez.
 */
/**
 * Um passo a ser escrito ou corrigido.
 *
 * [passo] nulo é um passo novo — a folha é a mesma nos dois casos, e a diferença está só em
 * haver ou não uma linha para reescrever.
 */
data class EdicaoDePasso(
    val passo: RecipeStepEntity? = null,
    val texto: String = "",
) {
    val podeGravar: Boolean get() = texto.isNotBlank()
}

data class ProcuraDeIngrediente(
    val texto: String = "",
    val resultados: List<FoodEntity> = emptyList(),
    val aProcurar: Boolean = false,
    val acrescentados: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeEditViewModel(
    private val repository: RecipeRepository,

    // Função em vez do repositório inteiro: o que este ecrã pode ler do catálogo fica
    // escrito no módulo que o cria, e é o mesmo padrão do [AiViewModel].
    private val procurarNoCatalogo: suspend (String) -> List<FoodEntity> = { emptyList() },
) : ViewModel() {

    /** O passo que está a ser editado, ou nulo. Nulo com a folha aberta é um passo novo. */
    private val _passoEmEdicao = MutableStateFlow<EdicaoDePasso?>(null)
    val passoEmEdicao: StateFlow<EdicaoDePasso?> = _passoEmEdicao

    fun escreverPasso() {
        viewModelScope.launch {
            garantirReceita()
            _passoEmEdicao.value = EdicaoDePasso()
        }
    }

    fun editarPasso(passo: RecipeStepEntity) {
        _passoEmEdicao.value = EdicaoDePasso(passo = passo, texto = passo.texto)
    }

    fun escreverTextoDoPasso(texto: String) =
        _passoEmEdicao.update { it?.copy(texto = texto.take(MAX_TEXTO_DE_PASSO)) }

    fun fecharEdicaoDePasso() {
        _passoEmEdicao.value = null
    }

    /** Grava o que está na folha: um passo novo no fim, ou o texto de um que já existe. */
    fun gravarPasso() {
        val edicao = _passoEmEdicao.value ?: return
        val id = recipeId.value ?: return
        viewModelScope.launch {
            val existente = edicao.passo
            if (existente == null) repository.addStep(id, edicao.texto)
            else repository.updateStep(existente, edicao.texto)
            _passoEmEdicao.value = null
        }
    }

    fun removerPasso(passo: RecipeStepEntity) {
        viewModelScope.launch { repository.removeStep(passo) }
    }

    fun devolverPasso(passo: RecipeStepEntity) {
        viewModelScope.launch { repository.restoreStep(passo.id, passo.recipeId) }
    }

    /**
     * Sobe ou desce um passo uma posição.
     *
     * Nos extremos não faz nada, e o ecrã desliga o botão: um botão que não faz nada é pior
     * do que um que não está lá.
     */
    fun moverPasso(passo: RecipeStepEntity, para: Int) {
        val id = recipeId.value ?: return
        viewModelScope.launch { repository.moveStep(id, passo.posicao, para) }
    }

    private val _procura = MutableStateFlow<ProcuraDeIngrediente?>(null)
    val procura: StateFlow<ProcuraDeIngrediente?> = _procura

    private var procuraJob: Job? = null

    /** Abre a folha, criando a receita primeiro se ela ainda não existir. */
    fun abrirProcura() {
        viewModelScope.launch {
            garantirReceita()
            _procura.value = ProcuraDeIngrediente()
        }
    }

    fun fecharProcura() {
        procuraJob?.cancel()
        _procura.value = null
    }

    fun procurar(texto: String) {
        val atual = _procura.value ?: return
        _procura.value = atual.copy(texto = texto, aProcurar = true)

        procuraJob?.cancel()
        procuraJob = viewModelScope.launch {
            val achados = procurarNoCatalogo(texto).take(MAX_RESULTADOS)
            val agora = _procura.value ?: return@launch
            // A consulta pode ter demorado mais do que a letra seguinte: escrever por cima
            // de uma procura mais recente devolvia resultados de um texto que já não está lá.
            if (agora.texto != texto) return@launch
            _procura.value = agora.copy(resultados = achados, aProcurar = false)
        }
    }

    /**
     * Junta o alimento à receita e **deixa a folha aberta**.
     *
     * Fechá-la ao escolher era repor a viagem que esta versão veio tirar: quem faz uma
     * receita acrescenta ingredientes em série, não um de cada vez.
     */
    fun acrescentar(food: FoodEntity) {
        val id = recipeId.value ?: return
        viewModelScope.launch {
            repository.addIngredient(id, food.id, GRAMAS_POR_OMISSAO)
            _procura.update { it?.copy(acrescentados = it.acrescentados + 1) }
        }
    }

    private suspend fun garantirReceita(): String =
        recipeId.value ?: repository.createRecipe(
            name.value.ifBlank { " " },
            yieldText.value.replace(',', '.').toDoubleOrNull(),
        ).also { recipeId.value = it }

    private val recipeId = MutableStateFlow<String?>(null)

    /**
     * Os passos de preparação, pela ordem em que se fazem.
     *
     * Fluxo à parte do estado do ecrã: mudam por outras razões — escrever, mover, apagar —
     * e metê-los no `combine` de cinco fluxos que já lá está obrigava a recalcular a
     * nutrição a cada letra escrita num passo.
     */
    val passos: StateFlow<List<RecipeStepEntity>> = recipeId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repository.observeSteps(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val name = MutableStateFlow("")
    private val yieldText = MutableStateFlow("")
    private val servingsText = MutableStateFlow("")
    private val metodo = MutableStateFlow<String?>(null)
    private val saved = MutableStateFlow(false)

    private val rows = recipeId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.observeIngredientRows(id)
    }

    private val campos = combine(name, yieldText, servingsText) { nm, yt, st -> Triple(nm, yt, st) }

    /**
     * Os métodos e o peso sugerido saem do repositório, e por isso vêm num fluxo à parte
     * que só se recalcula quando os ingredientes ou o método mudam — e não a cada letra
     * escrita no nome.
     */
    private val confecao = combine(recipeId, rows, metodo) { id, _, mt -> id to mt }
        .flatMapLatest { (id, mt) ->
            flowOf(
                if (id == null) {
                    Confecao()
                } else {
                    Confecao(
                        metodos = repository.metodosPara(id),
                        sugerido = repository.pesoFinalSugerido(id, mt),
                        envelope = repository.envelopeDePesoFinal(id),
                    )
                },
            )
        }

    val state: StateFlow<RecipeEditState> =
        combine(recipeId, campos, rows, saved, metodo) { id, campos, rws, sv, mt ->
            Quinteto(id, campos, rws, sv, mt)
        }.combine(confecao) { q, conf ->
            val (nm, yt, st) = q.campos
            val yieldG = yt.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }
            RecipeEditState(
                recipeId = q.id,
                name = nm,
                yieldText = yt,
                servingsText = st,
                rows = q.rows,
                nutrition = repository.nutritionFrom(q.rows, yieldG, q.metodo),
                saved = q.saved,
                metodo = q.metodo,
                metodos = conf.metodos,
                // A sugestão só aparece com o campo vazio: com um peso escrito, propor
                // outro era discutir com quem tem a balança à frente.
                pesoSugerido = conf.sugerido.takeIf { yieldG == null },
                // E com o campo escrito, a app só abre a boca quando **nenhum** método
                // explica o número. Foi a metade que faltava: até aqui, escrever 2000 g em
                // 400 g de ingredientes não dizia nada, e os valores por 100 g saíam cinco
                // vezes errados sem um aviso.
                pesoForaDoPrevisto = conf.envelope
                    ?.takeIf { env -> yieldG != null && yieldG !in env },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecipeEditState())

    /** O que o repositório sabe sobre a confeção desta receita, num sítio só. */
    private data class Confecao(
        val metodos: List<MetodoDeConfecao> = emptyList(),
        val sugerido: Double? = null,
        val envelope: ClosedFloatingPointRange<Double>? = null,
    )

    // O `combine` de cinco fluxos não tem sobrecarga com destruturação, e um `Triple` de
    // `Triple`s lia-se pior do que isto.
    private data class Quinteto(
        val id: String?,
        val campos: Triple<String, String, String>,
        val rows: List<IngredientRow>,
        val saved: Boolean,
        val metodo: String?,
    )

    fun start(existingId: String?) {
        if (existingId != null && recipeId.value == null) {
            recipeId.value = existingId
            viewModelScope.launch {
                repository.recipeById(existingId)?.let { r ->
                    name.value = r.name
                    yieldText.value = r.yieldGrams?.let { formatGrams(it) } ?: ""
                    servingsText.value = r.servings?.toString() ?: ""
                    metodo.value = r.metodo
                }
            }
        }
    }

    fun setName(value: String) { name.value = value.take(80) }
    fun setYield(value: String) { yieldText.value = value.filter { it.isDigit() || it == '.' || it == ',' }.take(6) }

    fun setServings(value: String) { servingsText.value = value.filter(Char::isDigit).take(2) }

    /** Tocar no método já escolhido tira-o: é como se desmarca sem um botão só para isso. */
    fun escolherMetodo(id: String) {
        metodo.value = if (metodo.value == id) null else id
        val recipe = recipeId.value ?: return
        viewModelScope.launch { repository.updateMetodo(recipe, metodo.value) }
    }

    /** Aceitar o peso que as tabelas prevêem, escrevendo-o no campo como se fosse à mão. */
    fun aceitarPesoSugerido() {
        val sugerido = state.value.pesoSugerido ?: return
        yieldText.value = formatGrams(sugerido.roundToInt().toDouble())
    }

    fun updateGrams(
        ingredient: RecipeIngredientEntity,
        gramsText: String,
        unidades: UnitSystem = UnitSystem.METRIC,
    ) {
        val grams = gramsText.replace(',', '.').toDoubleOrNull()
            ?.let { UnitConversions.portionToStored(it, unidades, liquid = false) }
            ?.takeIf { it > 0 } ?: return
        viewModelScope.launch { repository.updateIngredientGrams(ingredient, grams) }
    }

    fun removeIngredient(ingredient: RecipeIngredientEntity) {
        viewModelScope.launch { repository.removeIngredient(ingredient) }
    }

    fun restoreIngredient(ingredientId: String) {
        viewModelScope.launch { repository.restoreIngredient(ingredientId) }
    }

    fun delete() {
        val id = recipeId.value ?: return
        viewModelScope.launch {
            repository.deleteRecipe(id)
            saved.value = true
        }
    }

    fun save() {
        viewModelScope.launch {
            val yieldG = yieldText.value.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }
            val doses = servingsText.value.toIntOrNull()?.takeIf { it in 1..MAX_DOSES }
            val id = recipeId.value
                ?: repository.createRecipe(name.value, yieldG, doses).also { recipeId.value = it }
            repository.updateRecipe(id, name.value, yieldG, doses)
            // O método já foi gravado quando se escolheu, mas uma receita criada agora não
            // existia nesse momento — e sem isto perdia-o ao gravar.
            repository.updateMetodo(id, metodo.value)
            saved.value = true
        }
    }

    private fun formatGrams(v: Double): String {
        val i = v.toInt()
        return if (v == i.toDouble()) i.toString() else v.toString()
    }
}
