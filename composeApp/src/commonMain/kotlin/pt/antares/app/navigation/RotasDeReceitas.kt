package pt.antares.app.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.model.MealSlot
import pt.antares.app.feature.fooddata.FoodSearchScreen
import pt.antares.app.feature.recipe.RecipeDetailScreen
import pt.antares.app.feature.recipe.RecipeEditScreen

/**
 * Receitas: criar, escolher ingredientes e ver o resultado.
 *
 * Saíram das rotas de comida por tamanho: uma receita é comida, mas o ficheiro passava
 * das 120 linhas por função e deixava de se ler de uma vez.
 */
internal fun NavGraphBuilder.rotasDeReceitas(navController: NavHostController) {
    composable<Route.RecipeEdit> { entry ->
        val route = entry.toRoute<Route.RecipeEdit>()
        RecipeEditScreen(
            recipeId = route.recipeId,
            onDone = { navController.popBackStack() },
            onBack = { navController.popBackStack() },
        )
    }
    composable<Route.RecipeDetail> { entry ->
        val route = entry.toRoute<Route.RecipeDetail>()
        RecipeDetailScreen(
            recipeId = route.recipeId,
            slot = MealSlot.valueOf(route.slot),
            epochDay = route.epochDay,
            onSaved = { navController.popBackStack(Route.Diary, inclusive = false) },
            onBack = { navController.popBackStack() },
        )
    }
}
