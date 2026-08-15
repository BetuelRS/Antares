package pt.antares.app.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.navigation.toRoute
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.model.MealSlot
import pt.antares.app.feature.barcode.BarcodeResolveViewModel
import pt.antares.app.feature.barcode.BarcodeResult
import pt.antares.app.feature.barcode.BarcodeScanScreen

/**
 * Ler um código de barras.
 *
 * Uma rota só, e um ficheiro só para ela: é a que tem mais lógica de navegação de toda
 * a app, porque o resultado da leitura decide para onde se vai — e na leitura contínua
 * não se vai a lado nenhum.
 */
internal fun NavGraphBuilder.rotasDeCodigoDeBarras(navController: NavHostController) {
    composable<Route.BarcodeScan> { entry ->
        val route = entry.toRoute<Route.BarcodeScan>()
        val viewModel: BarcodeResolveViewModel = koinViewModel()
        val result by viewModel.result.collectAsState()
        val continuous by viewModel.continuous.collectAsState()
        val logged by viewModel.logged.collectAsState()
        val notFound by viewModel.notFound.collectAsState()

        LaunchedEffect(Unit) {
            viewModel.configure(MealSlot.valueOf(route.slot), route.epochDay)
        }

        LaunchedEffect(result) {
            when (val r = result) {
                is BarcodeResult.Found -> navController.navigate(
                    Route.FoodDetail(r.foodId, route.slot, route.epochDay),
                ) { popUpTo<Route.BarcodeScan> { inclusive = true } }
                is BarcodeResult.NotFound -> navController.navigate(
                    Route.FoodEdit(
                        barcode = r.barcode,
                        slot = route.slot,
                        epochDay = route.epochDay,
                    ),
                ) { popUpTo<Route.BarcodeScan> { inclusive = true } }
                else -> Unit
            }
        }

        BarcodeScanScreen(
            onDetected = viewModel::resolve,

            networkError = result is BarcodeResult.NetworkError,
            onRetry = viewModel::reset,
            onBack = { navController.popBackStack() },
            continuous = continuous,
            onToggleContinuous = viewModel::toggleContinuous,
            logged = logged,
            notFoundCodes = notFound,
            onCreateMissing = { codigo ->
                // Sai da lista antes de abrir a criação: a leitura contínua continua a
                // correr, e voltar a este ecrã com o produto já criado não pode
                // reencontrar a mesma linha por resolver.
                viewModel.forgetNotFound(codigo)
                navController.navigate(
                    Route.FoodEdit(
                        barcode = codigo,
                        slot = route.slot,
                        epochDay = route.epochDay,
                    ),
                )
            },
        )
    }
}
