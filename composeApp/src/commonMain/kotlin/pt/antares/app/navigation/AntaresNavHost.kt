package pt.antares.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.toRoute
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.model.MealSlot
import pt.antares.app.feature.barcode.BarcodeResolveViewModel
import pt.antares.app.feature.barcode.BarcodeResult
import pt.antares.app.feature.barcode.BarcodeScanScreen
import pt.antares.app.feature.diary.DiaryScreen
import pt.antares.app.feature.exercise.AddExerciseScreen
import pt.antares.app.feature.fooddata.FoodDetailScreen
import pt.antares.app.feature.fooddata.FoodEditScreen
import pt.antares.app.feature.fooddata.FoodSearchScreen
import pt.antares.app.feature.recipe.RecipeDetailScreen
import pt.antares.app.feature.recipe.RecipeEditScreen
import pt.antares.app.feature.recipe.RecipePickViewModel
import pt.antares.app.feature.settings.AttributionsScreen
import pt.antares.app.feature.stats.NutritionStatsScreen
import pt.antares.app.feature.about.AboutScreen
import pt.antares.app.feature.achievements.AchievementsScreen
import pt.antares.app.feature.me.AppMenuScreen
import pt.antares.app.feature.me.MeScreen
import pt.antares.app.feature.onboarding.OnboardingScreen
import pt.antares.app.feature.profile.ui.BodyCompositionScreen
import pt.antares.app.feature.profile.ui.DietBreakScreen
import pt.antares.app.feature.profile.ui.MeasurementHistoryScreen
import pt.antares.app.feature.profile.ui.ShowMathsScreen
import pt.antares.app.feature.profile.ui.HealthProfileScreen
import pt.antares.app.feature.profile.ui.ProfileSettingsScreen
import pt.antares.app.feature.profile.ui.WeightHistoryScreen
import pt.antares.app.feature.running.RunScreen
import pt.antares.app.feature.coach.CoachHistoryScreen
import pt.antares.app.feature.coach.CoachReportScreen
import pt.antares.app.feature.health.HealthPermissionsScreen
import pt.antares.app.feature.running.ui.RunDetailScreen
import pt.antares.app.feature.running.ui.RunHistoryScreen
import pt.antares.app.feature.running.ui.RunLiveScreen
import pt.antares.app.feature.running.ui.RunSummaryScreen
import pt.antares.app.feature.today.TodayScreen
import pt.antares.app.feature.fasting.ui.FastingHistoryScreen
import pt.antares.app.feature.fasting.ui.FastingScreen
import pt.antares.app.feature.workout.WorkoutScreen
import pt.antares.app.feature.workout.ui.ExerciseCreateScreen
import pt.antares.app.feature.workout.ui.ExerciseDetailScreen
import pt.antares.app.feature.workout.ui.ExerciseLibraryScreen
import pt.antares.app.feature.workout.data.SessionPickBus
import pt.antares.app.feature.workout.ui.RoutineEditScreen
import pt.antares.app.feature.workout.ui.RoutineItemPickViewModel
import pt.antares.app.feature.workout.ui.WorkoutDetailScreen
import pt.antares.app.feature.workout.ui.WeeklyScheduleScreen
import pt.antares.app.feature.workout.ui.WorkoutHistoryScreen
import pt.antares.app.feature.workout.ui.WorkoutSessionScreen
import pt.antares.app.feature.workout.ui.WorkoutStatsScreen
import pt.antares.app.feature.workout.ui.WorkoutSummaryScreen

/**
 * O grafo de navegação inteiro, num sítio só. É deliberadamente longo: os ecrãs não
 * navegam, recebem funções e chamam-nas, e por isso nenhum deles conhece outro. Quem quiser
 * saber o que leva a onde lê este ficheiro e mais nenhum.
 */
@Composable
fun AntaresNavHost(
    navController: NavHostController,
    startDestination: Route,
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable<Route.Today> {
            TodayScreen(
                onLogWeight = { navController.navigate(Route.WeightHistory) },

                onAddMeal = { navController.navigateToTab(Route.Diary) },
                onOpenWorkout = { navController.navigateToTab(Route.Workout) },
                onOpenFasting = { navController.navigate(Route.Fasting) { launchSingleTop = true } },
                onOpenRun = { navController.navigateToTab(Route.Run) },
                onOpenCoach = { navController.navigate(Route.CoachReport()) },
                onQuickLog = { slot, epochDay, mode, query ->
                    navController.navigate(Route.FoodSearch(slot.name, epochDay, mode.name, query))
                },

                onOpenGap = { chave -> navController.navigate(Route.RichIn(chave)) },
            )
        }
        composable<Route.Diary> {
            DiaryScreen(
                onAddFood = { slot, epochDay, mode ->
                    navController.navigate(Route.FoodSearch(slot.name, epochDay, mode.name))
                },
                onAddExercise = { epochDay -> navController.navigate(Route.AddExercise(epochDay)) },
                onQuickLog = { slot, epochDay, mode, query ->
                    navController.navigate(Route.FoodSearch(slot.name, epochDay, mode.name, query))
                },
            )
        }
        composable<Route.Workout> {
            WorkoutScreen(
                onLibrary = { navController.navigate(Route.ExerciseLibrary()) },
                onRoutine = { routineId -> navController.navigate(Route.RoutineEdit(routineId)) },
                onStartEmpty = { navController.navigate(Route.WorkoutSession()) },
                onResume = { navController.navigate(Route.WorkoutSession()) },
                onHistory = { navController.navigate(Route.WorkoutHistory) },
                onStats = { navController.navigate(Route.WorkoutStats) },
                onSchedule = { navController.navigate(Route.WorkoutSchedule) },
            )
        }
        composable<Route.WorkoutSchedule> {
            WeeklyScheduleScreen(onBack = { navController.popBackStack() })
        }
        composable<Route.Run> {
            RunScreen(
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
        composable<Route.HealthPermissions> {
            HealthPermissionsScreen(
                viewModel = koinViewModel(),
                onBack = { navController.popBackStack() },
            )
        }
        composable<Route.Cycle> {
            pt.antares.app.feature.profile.ui.CycleScreen(onBack = { navController.popBackStack() })
        }
        composable<Route.ProgressPhotos> {
            pt.antares.app.feature.progress.ProgressPhotosScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable<Route.Me> {
            MeScreen(
                onSettingsMenu = { navController.navigate(Route.AppMenu) },
                onProfileClick = { navController.navigate(Route.HealthProfile) },
                onWeightClick = { navController.navigate(Route.WeightHistory) },
                onPhotosClick = { navController.navigate(Route.ProgressPhotos) },
                onStatsClick = { navController.navigate(Route.NutritionStats) },
                onRichInClick = { navController.navigate(Route.RichIn()) },
                onAchievementsClick = { navController.navigate(Route.Achievements) },
                onCoachClick = { navController.navigate(Route.CoachHistory) },
            )
        }
        composable<Route.AppMenu> {
            AppMenuScreen(
                onSettingsClick = { navController.navigate(Route.Settings) },
                onHealthClick = { navController.navigate(Route.HealthPermissions) },
                onAttributionsClick = { navController.navigate(Route.Attributions) },
                onAboutClick = { navController.navigate(Route.About) },
                onBackupClick = { navController.navigate(Route.Backup) },
                onCrashClick = { navController.navigate(Route.CrashLog) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<Route.Settings> {
            pt.antares.app.feature.settings.SettingsScreen(
                onBack = { navController.popBackStack() },
                onAdminClick = { navController.navigate(Route.Admin) },
            )
        }
        composable<Route.Admin> {
            pt.antares.app.feature.admin.AdminScreen(onBack = { navController.popBackStack() })
        }
        composable<Route.Achievements> {
            AchievementsScreen(onBack = { navController.popBackStack() })
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
            ProfileSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable<Route.WeightHistory> {
            WeightHistoryScreen(onBack = { navController.popBackStack() })
        }

        composable<Route.FoodSearch> { entry ->
            val route = entry.toRoute<Route.FoodSearch>()
            FoodSearchScreen(
                onBack = { navController.popBackStack() },
                onFoodSelected = { foodId ->
                    navController.navigate(Route.FoodDetail(foodId, route.slot, route.epochDay))
                },
                onCreateCustom = { navController.navigate(Route.FoodEdit()) },
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
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }

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
                        Route.FoodEdit(barcode = r.barcode),
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
                notFoundCount = notFound,
            )
        }

        composable<Route.RecipeEdit> { entry ->
            val route = entry.toRoute<Route.RecipeEdit>()
            RecipeEditScreen(
                recipeId = route.recipeId,
                onAddIngredient = { recipeId -> navController.navigate(Route.RecipeIngredientPick(recipeId)) },
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        composable<Route.RecipeIngredientPick> { entry ->
            val route = entry.toRoute<Route.RecipeIngredientPick>()
            val pickViewModel: RecipePickViewModel = koinViewModel()
            FoodSearchScreen(
                onBack = { navController.popBackStack() },
                onFoodSelected = { foodId ->
                    pickViewModel.add(route.recipeId, foodId) { navController.popBackStack() }
                },
                onCreateCustom = {},
                onScan = {},
                pickMode = true,
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
        composable<Route.RichIn> { entry ->
            val route = entry.toRoute<Route.RichIn>()
            pt.antares.app.feature.fooddata.RichInScreen(
                onFoodSelected = { id -> navController.navigate(Route.FoodDetail(id, MealSlot.SNACK.name, pt.antares.app.core.util.todayEpochDay())) },
                onBack = { navController.popBackStack() },
                initialKey = route.key,
            )
        }
        composable<Route.NutritionStats> {
            NutritionStatsScreen(onBack = { navController.popBackStack() })
        }
        composable<Route.Attributions> {
            AttributionsScreen(onBack = { navController.popBackStack() })
        }
        composable<Route.About> {
            AboutScreen(onBack = { navController.popBackStack() })
        }
        composable<Route.Backup> {
            pt.antares.app.feature.backup.BackupScreen(onBack = { navController.popBackStack() })
        }
        composable<Route.CrashLog> {
            pt.antares.app.feature.crash.CrashScreen(onBack = { navController.popBackStack() })
        }

        composable<Route.AddExercise> { entry ->
            val route = entry.toRoute<Route.AddExercise>()
            AddExerciseScreen(
                epochDay = route.epochDay,
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }

        composable<Route.ExerciseLibrary> { entry ->
            val route = entry.toRoute<Route.ExerciseLibrary>()
            val pickVm: RoutineItemPickViewModel = koinViewModel()
            val pickBus: SessionPickBus = koinInject()
            val scope = rememberCoroutineScope()
            ExerciseLibraryScreen(
                pickMode = route.pickMode,
                onExercise = { id ->
                    when {
                        route.pickMode && route.routineId != null -> {

                            pickVm.add(route.routineId, id) { navController.popBackStack() }
                        }
                        route.sessionPick -> {

                            scope.launch { pickBus.emit(id) }
                            navController.popBackStack()
                        }
                        else -> navController.navigate(Route.ExerciseDetail(id))
                    }
                },
                onCreateCustom = { navController.navigate(Route.ExerciseCreate) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<Route.ExerciseDetail> { entry ->
            val route = entry.toRoute<Route.ExerciseDetail>()
            ExerciseDetailScreen(
                exerciseId = route.exerciseId,
                onDeleted = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        composable<Route.ExerciseCreate> {
            ExerciseCreateScreen(
                onCreated = { id ->
                    navController.navigate(Route.ExerciseDetail(id)) {
                        popUpTo<Route.ExerciseCreate> { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable<Route.RoutineEdit> { entry ->
            val route = entry.toRoute<Route.RoutineEdit>()
            RoutineEditScreen(
                routineId = route.routineId,
                onAddExercise = { rid -> navController.navigate(Route.ExerciseLibrary(pickMode = true, routineId = rid)) },
                onStart = { rid -> navController.navigate(Route.WorkoutSession(rid)) },
                onDeleted = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }

        composable<Route.WorkoutSession> { entry ->
            val route = entry.toRoute<Route.WorkoutSession>()
            WorkoutSessionScreen(
                routineId = route.routineId,
                onAddExercise = { navController.navigate(Route.ExerciseLibrary(pickMode = true, sessionPick = true)) },
                onFinished = { sessionId ->
                    navController.navigate(Route.WorkoutSummary(sessionId)) {
                        popUpTo<Route.Workout>()
                    }
                },
                onDiscarded = { navController.popBackStack(Route.Workout, inclusive = false) },
            )
        }
        composable<Route.WorkoutSummary> { entry ->
            val route = entry.toRoute<Route.WorkoutSummary>()
            WorkoutSummaryScreen(
                sessionId = route.sessionId,
                onDone = { navController.popBackStack(Route.Workout, inclusive = false) },
            )
        }
        composable<Route.WorkoutHistory> {
            WorkoutHistoryScreen(
                onSession = { id -> navController.navigate(Route.WorkoutDetail(id)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<Route.WorkoutDetail> { entry ->
            val route = entry.toRoute<Route.WorkoutDetail>()
            WorkoutDetailScreen(sessionId = route.sessionId, onBack = { navController.popBackStack() })
        }
        composable<Route.WorkoutStats> {
            WorkoutStatsScreen(onBack = { navController.popBackStack() })
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
}
