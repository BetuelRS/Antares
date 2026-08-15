package pt.antares.app.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.feature.me.MeScreen
import pt.antares.app.feature.onboarding.OnboardingScreen
import pt.antares.app.feature.profile.ui.BodyCompositionScreen
import pt.antares.app.feature.profile.ui.DietBreakScreen
import pt.antares.app.feature.profile.ui.MeasurementHistoryScreen
import pt.antares.app.feature.profile.ui.ShowMathsScreen
import pt.antares.app.feature.profile.ui.HealthProfileScreen
import pt.antares.app.feature.profile.ui.ProfileSettingsScreen
import pt.antares.app.feature.profile.ui.WeightHistoryScreen
import pt.antares.app.feature.health.HealthPermissionsScreen
import pt.antares.app.feature.profile.ui.CycleScreen
import pt.antares.app.feature.progress.ProgressPhotosScreen

/**
 * Quem usa a app: o perfil, o peso, as medições, as definições e a primeira utilização.
 */
internal fun NavGraphBuilder.rotasDePerfil(navController: NavHostController) {
    composable<Route.Me> {
        MeScreen(
            onSettingsMenu = { navController.navigate(Route.AppMenu) },
            onProfileClick = { navController.navigate(Route.HealthProfile) },
            onWeightClick = { navController.navigate(Route.WeightHistory) },
            onPhotosClick = { navController.navigate(Route.ProgressPhotos) },
            onStatsClick = { navController.navigate(Route.NutritionStats) },
            onRichInClick = { navController.navigate(Route.RichIn()) },
            onCoachClick = { navController.navigate(Route.CoachHistory) },
        )
    }
    composable<Route.HealthProfile> {
        HealthProfileScreen(
            onBack = { navController.popBackStack() },
            onEditProfile = { navController.navigate(Route.ProfileSettings) },
            onWeightHistory = { navController.navigate(Route.WeightHistory) },
            onBodyComposition = { navController.navigate(Route.BodyCompositionEdit) },
            onShowMaths = { navController.navigate(Route.ShowMaths) },
            onMeasurementHistory = { navController.navigate(Route.MeasurementHistory) },
            onDietBreak = { navController.navigate(Route.DietBreak) },
            onCycle = { navController.navigate(Route.Cycle) },
            onCoach = { navController.navigate(Route.CoachReport()) },
        )
    }
    composable<Route.ShowMaths> {
        ShowMathsScreen(onBack = { navController.popBackStack() })
    }
    composable<Route.MeasurementHistory> {
        MeasurementHistoryScreen(onBack = { navController.popBackStack() })
    }
    composable<Route.DietBreak> {
        DietBreakScreen(onBack = { navController.popBackStack() })
    }
    composable<Route.BodyCompositionEdit> {
        BodyCompositionScreen(onBack = { navController.popBackStack() })
    }
    composable<Route.ProfileSettings> {
        ProfileSettingsScreen(
            onBack = { navController.popBackStack() },
            // Sem refeição nem dia: este ecrã não regista nada no diário, e criar aqui
            // é encher o catálogo, não a refeição de hoje.
            onCreateFood = { nome -> navController.navigate(Route.FoodEdit(name = nome)) },
        )
    }
    composable<Route.WeightHistory> {
        WeightHistoryScreen(onBack = { navController.popBackStack() })
    }

    composable<Route.Cycle> {
        CycleScreen(onBack = { navController.popBackStack() })
    }
    composable<Route.ProgressPhotos> {
        ProgressPhotosScreen(
            onBack = { navController.popBackStack() },
        )
    }

    composable<Route.HealthPermissions> {
        HealthPermissionsScreen(
            viewModel = koinViewModel(),
            onBack = { navController.popBackStack() },
        )
    }
    composable<Route.Onboarding> {
        OnboardingScreen(
            onFinished = {
                navController.navigate(Route.Today) {
                    popUpTo(Route.Onboarding) { inclusive = true }
                }
            },
        )
    }
}
