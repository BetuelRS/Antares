package pt.antares.app.feature.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pt.antares.app.core.calc.RecipeNutrition
import pt.antares.app.core.confecao.MetodoDeConfecao
import pt.antares.app.core.database.entities.RecipeIngredientEntity
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

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeEditViewModel(
    private val repository: RecipeRepository,
) : ViewModel() {

    private val recipeId = MutableStateFlow<String?>(null)
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
                    emptyList<MetodoDeConfecao>() to null
                } else {
                    repository.metodosPara(id) to repository.pesoFinalSugerido(id, mt)
                },
            )
        }

    val state: StateFlow<RecipeEditState> =
        combine(recipeId, campos, rows, saved, metodo) { id, campos, rws, sv, mt ->
            Quinteto(id, campos, rws, sv, mt)
        }.combine(confecao) { q, (metodos, sugerido) ->
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
                metodos = metodos,
                // A sugestão só aparece com o campo vazio: com um peso escrito, propor
                // outro era discutir com quem tem a balança à frente.
                pesoSugerido = sugerido.takeIf { yieldG == null },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecipeEditState())

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

    fun ensureRecipeThen(navigate: (String) -> Unit) {
        viewModelScope.launch {
            val id = recipeId.value ?: repository.createRecipe(
                name.value.ifBlank { " " },
                yieldText.value.replace(',', '.').toDoubleOrNull(),
            ).also { recipeId.value = it }
            navigate(id)
        }
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
