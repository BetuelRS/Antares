package pt.antares.app.feature.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import pt.antares.app.feature.fooddata.RefeicaoGuardada
import pt.antares.app.feature.fooddata.juntarRefeicoes
import pt.antares.app.feature.recipe.RecipeRepository

/**
 * O que o ecrã das refeições guardadas precisa de saber, e nada mais.
 *
 * **Não reaproveita o `FoodSearchViewModel`**, apesar de a lista ser a mesma: aquele carrega
 * seis fluxos, duas pesquisas com espera e o estado da folha da AI. Um ecrã que só lista não
 * paga isso — e partilhar o ViewModel amarrava os dois ecrãs um ao outro para sempre.
 */
class MinhasRefeicoesViewModel(
    templateRepository: MealTemplateRepository,
    recipeRepository: RecipeRepository,
) : ViewModel() {

    val refeicoes: StateFlow<List<RefeicaoGuardada>> =
        combine(
            templateRepository.observeResumos(),
            recipeRepository.observeSummaries(),
        ) { modelos, receitas -> juntarRefeicoes(modelos, receitas) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
