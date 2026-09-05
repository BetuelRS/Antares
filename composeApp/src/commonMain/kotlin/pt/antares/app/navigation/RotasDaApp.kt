package pt.antares.app.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import pt.antares.app.feature.settings.AttributionsScreen
import pt.antares.app.feature.about.AboutScreen
import pt.antares.app.feature.me.AppMenuScreen
import pt.antares.app.feature.me.DestinosDaApp
import pt.antares.app.feature.me.DestinosDoCorpo
import pt.antares.app.feature.me.DestinosDoSobre
import pt.antares.app.feature.coach.CoachHistoryScreen
import pt.antares.app.feature.coach.CoachReportScreen
import pt.antares.app.feature.today.DestinosDoHoje
import pt.antares.app.feature.today.TodayScreen
import pt.antares.app.feature.fasting.ui.FastingHistoryScreen
import pt.antares.app.feature.fasting.ui.FastingScreen
import pt.antares.app.feature.settings.SettingsScreen
import pt.antares.app.feature.admin.AdminScreen
import pt.antares.app.feature.backup.BackupScreen
import pt.antares.app.feature.crash.CrashScreen

/**
 * O resto: o ecrã de hoje, o menu, o jejum, o treinador e o que a app diz sobre si mesma.
 */
internal fun NavGraphBuilder.rotasDaApp(navController: NavHostController) {
    composable<Route.Today> {
        TodayScreen(
            destinos = DestinosDoHoje(
                peso = { navController.navigate(Route.WeightHistory) },
                refeicao = { navController.navigateToTab(Route.Diary) },
                treino = { navController.navigateToTab(Route.Workout) },
                jejum = { navController.navigate(Route.Fasting) { launchSingleTop = true } },
                // Um `navigate` e não um `navigateToTab`: a corrida deixou de ser separador, e
                // saltar para ela pelo caminho dos separadores apagava a pilha até ao Hoje.
                corrida = { navController.navigate(Route.Run) { launchSingleTop = true } },
                treinador = { navController.navigate(Route.CoachReport()) },
                perfil = { navController.navigate(Route.ProfileSettings) },
                // O arranque é um destino como os outros: quem lá chega daqui volta ao Hoje
                // pelo caminho que ele já tem — o `onFinished` faz `popUpTo(Onboarding)`.
                arranque = { navController.navigate(Route.Onboarding) },
            ),
            onQuickLog = { slot, epochDay, mode, query ->
                navController.navigate(Route.FoodSearch(slot.name, epochDay, mode.name, query))
            },

            onOpenGap = { chave -> navController.navigate(Route.RichIn(chave)) },
        )
    }
    composable<Route.Mais> {
        AppMenuScreen(
            corpo = DestinosDoCorpo(
                perfil = { navController.navigate(Route.HealthProfile) },
                refeicoes = { navController.navigate(Route.MinhasRefeicoes) },
                estatisticas = { navController.navigate(Route.NutritionStats) },
                ricoEm = { navController.navigate(Route.RichIn()) },
                treinador = { navController.navigate(Route.CoachHistory) },
            ),
            app = DestinosDaApp(
                definicoes = { navController.navigate(Route.Settings) },
                copia = { navController.navigate(Route.Backup) },
                destinos = { navController.navigate(Route.Destinos) },
                saude = { navController.navigate(Route.HealthPermissions) },
            ),
            sobre = DestinosDoSobre(
                atribuicoes = { navController.navigate(Route.Attributions) },
                sobre = { navController.navigate(Route.About) },
                falhas = { navController.navigate(Route.CrashLog) },
            ),
        )
    }
    composable<Route.Settings> {
        SettingsScreen(
            onBack = { navController.popBackStack() },
            onAdminClick = { navController.navigate(Route.Admin) },
        )
    }
    composable<Route.Admin> {
        AdminScreen(onBack = { navController.popBackStack() })
    }
    composable<Route.Attributions> {
        AttributionsScreen(onBack = { navController.popBackStack() })
    }
    composable<Route.About> {
        AboutScreen(onBack = { navController.popBackStack() })
    }
    composable<Route.Backup> {
        BackupScreen(onBack = { navController.popBackStack() })
    }
    composable<Route.Destinos> {
        pt.antares.app.feature.privacidade.DestinosScreen(
            onBack = { navController.popBackStack() },
        )
    }
    composable<Route.CrashLog> {
        CrashScreen(onBack = { navController.popBackStack() })
    }

    composable<Route.CoachHistory> {
        CoachHistoryScreen(
            onOpen = { id -> navController.navigate(Route.CoachReport(id)) },
            onBack = { navController.popBackStack() },
        )
    }
    composable<Route.CoachReport> { entry ->
        val route = entry.toRoute<Route.CoachReport>()
        CoachReportScreen(
            reportId = route.reportId,
            onBack = { navController.popBackStack() },
        )
    }
    composable<Route.Fasting> {
        FastingScreen(
            onBack = { navController.popBackStack() },
            onOpenHistory = { navController.navigate(Route.FastingHistory) },
            onOpenDiary = {
                navController.navigate(Route.Diary) {
                    popUpTo(Route.Today)
                    launchSingleTop = true
                }
            },
        )
    }
    composable<Route.FastingHistory> {
        FastingHistoryScreen(onBack = { navController.popBackStack() })
    }
}
