package pt.antares.app.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import pt.antares.app.core.model.MealSlot
import pt.antares.app.core.util.todayEpochDay
import pt.antares.app.feature.diary.DiaryScreen
import pt.antares.app.feature.fooddata.FoodDetailScreen
import pt.antares.app.feature.fooddata.FoodEditScreen
import pt.antares.app.feature.fooddata.FoodSearchScreen
import pt.antares.app.feature.stats.NutritionStatsScreen
import pt.antares.app.feature.fooddata.RichInScreen

/**
 * Comer: o diário, a pesquisa, o detalhe do alimento e a criação de um novo.
 *
 * A refeição e o dia viajam em quase todas estas rotas, e é isso que faz um registo
 * acabar no sítio certo do diário depois de três ecrãs de distância.
 */
internal fun NavGraphBuilder.rotasDeComida(navController: NavHostController) {
    composable<Route.Diary> {
        DiaryScreen(
            onAddFood = { slot, epochDay, mode ->
                navController.navigate(Route.FoodSearch(slot.name, epochDay, mode.name))
            },
            onAddExercise = { epochDay -> navController.navigate(Route.AddExercise(epochDay)) },
            onQuickLog = { slot, epochDay, mode, query ->
                navController.navigate(Route.FoodSearch(slot.name, epochDay, mode.name, query))
            },
            // Leva o dia e a refeição do próprio registo: voltar atrás cai no dia onde se
            // estava, e não em hoje.
            onOpenFood = { foodId, slot, epochDay ->
                navController.navigate(Route.FoodDetail(foodId, slot.name, epochDay))
            },
        )
    }
    composable<Route.FoodSearch> { entry ->
        val route = entry.toRoute<Route.FoodSearch>()
        FoodSearchScreen(
            onBack = { navController.popBackStack() },
            onFoodSelected = { foodId ->
                navController.navigate(Route.FoodDetail(foodId, route.slot, route.epochDay))
            },
            onCreateCustom = {
                navController.navigate(Route.FoodEdit(slot = route.slot, epochDay = route.epochDay))
            },
            onScan = { navController.navigate(Route.BarcodeScan(route.slot, route.epochDay)) },
            onRecipeSelected = { recipeId ->
                navController.navigate(Route.RecipeDetail(recipeId, route.slot, route.epochDay))
            },
            onEditRecipe = { recipeId -> navController.navigate(Route.RecipeEdit(recipeId)) },
            onNewRecipe = { navController.navigate(Route.RecipeEdit()) },

            aiSlot = MealSlot.valueOf(route.slot),
            aiEpochDay = route.epochDay,
            initialMode = route.initial,
            initialQuery = route.query,
        )
    }
    composable<Route.FoodDetail> { entry ->
        val route = entry.toRoute<Route.FoodDetail>()
        FoodDetailScreen(
            foodId = route.foodId,
            slot = MealSlot.valueOf(route.slot),
            epochDay = route.epochDay,

            onSaved = { navController.popBackStack(Route.Diary, inclusive = false) },
            onBack = { navController.popBackStack() },
        )
    }
    composable<Route.FoodEdit> { entry ->
        val route = entry.toRoute<Route.FoodEdit>()
        FoodEditScreen(
            foodId = route.foodId,
            barcode = route.barcode,
            initialName = route.name,
            onSaved = { navController.popBackStack() },
            onBack = { navController.popBackStack() },
            // Sai do ecrã de criação antes de abrir o alimento: quem escolheu usar um
            // que já existe não quer o rascunho de volta ao carregar em voltar.
            onUseExisting = route.slot?.let { slot ->
                route.epochDay?.let { dia ->
                    { foodId: String ->
                        navController.popBackStack()
                        navController.navigate(Route.FoodDetail(foodId, slot, dia))
                    }
                }
            },
        )
    }

    composable<Route.RichIn> { entry ->
        val route = entry.toRoute<Route.RichIn>()
        RichInScreen(
            // Entra como lanche de hoje: quem chegou aqui veio de uma lacuna do dia, e o
            // lanche é a refeição que aceita qualquer coisa a qualquer hora.
            onFoodSelected = { id ->
                navController.navigate(Route.FoodDetail(id, MealSlot.SNACK.name, todayEpochDay()))
            },
            onBack = { navController.popBackStack() },
            initialKey = route.key,
        )
    }
    composable<Route.NutritionStats> {
        NutritionStatsScreen(
            onBack = { navController.popBackStack() },
            onOpenRichIn = { chave -> navController.navigate(Route.RichIn(chave)) },
        )
    }
}
