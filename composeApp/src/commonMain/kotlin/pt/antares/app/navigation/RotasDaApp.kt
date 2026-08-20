package pt.antares.app.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import pt.antares.app.feature.settings.AttributionsScreen
import pt.antares.app.feature.about.AboutScreen
import pt.antares.app.feature.me.AppMenuScreen
import pt.antares.app.feature.coach.CoachHistoryScreen
import pt.antares.app.feature.coach.CoachReportScreen
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
            onLogWeight = { navController.navigate(Route.WeightHistory) },

            onAddMeal = { navController.navigateToTab(Route.Diary) },
            onOpenWorkout = { navController.navigateToTab(Route.Workout) },
            onOpenFasting = { navController.navigate(Route.Fasting) { launchSingleTop = true } },
            onOpenRun = { navController.navigateToTab(Route.Run) },
            onOpenCoach = { navController.navigate(Route.CoachReport()) },
            onOpenProfile = { navController.navigate(Route.ProfileSettings) },
            onQuickLog = { slot, epochDay, mode, query ->
                navController.navigate(Route.FoodSearch(slot.name, epochDay, mode.name, query))
            },

            onOpenGap = { chave -> navController.navigate(Route.RichIn(chave)) },
        )
    }
    composable<Route.AppMenu> {
        AppMenuScreen(
            onSettingsClick = { navController.navigate(Route.Settings) },
            onHealthClick = { navController.navigate(Route.HealthPermissions) },
            onAttributionsClick = { navController.navigate(Route.Attributions) },
            onAboutClick = { navController.navigate(Route.About) },
            onBackupClick = { navController.navigate(Route.Backup) },
            onDestinosClick = { navController.navigate(Route.Destinos) },
            onCrashClick = { navController.navigate(Route.CrashLog) },
            onBack = { navController.popBackStack() },
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
