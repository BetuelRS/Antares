package pt.antares.app.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import pt.antares.app.feature.running.RunScreen
import pt.antares.app.feature.running.ui.RunDetailScreen
import pt.antares.app.feature.running.ui.RunHistoryScreen
import pt.antares.app.feature.running.ui.RunLiveScreen
import pt.antares.app.feature.running.ui.RunSummaryScreen

/**
 * Correr, andar e pedalar. A corrida a decorrer é a única rota da app que não se pode
 * abandonar sem perder dados, e por isso passa sempre pelo resumo.
 */
internal fun NavGraphBuilder.rotasDeCorrida(navController: NavHostController) {
    composable<Route.Run> {
        RunScreen(
            onBack = { navController.popBackStack() },
            onOpenLive = { navController.navigate(Route.RunLive) },
            onOpenHistory = { navController.navigate(Route.RunHistory) },
        )
    }
    composable<Route.RunLive> {
        RunLiveScreen(
            onFinish = { navController.navigate(Route.RunSummary) { popUpTo(Route.Run) } },
        )
    }
    composable<Route.RunSummary> {
        RunSummaryScreen(
            onSaved = { navController.popBackStack(Route.Run, inclusive = false) },
            onDiscarded = { navController.popBackStack(Route.Run, inclusive = false) },
        )
    }
    composable<Route.RunHistory> {
        RunHistoryScreen(
            onRun = { id -> navController.navigate(Route.RunDetail(id)) },
            onBack = { navController.popBackStack() },
        )
    }
    composable<Route.RunDetail> { entry ->
        val route = entry.toRoute<Route.RunDetail>()
        RunDetailScreen(
            runId = route.runId,
            onBack = { navController.popBackStack() },
        )
    }
}
