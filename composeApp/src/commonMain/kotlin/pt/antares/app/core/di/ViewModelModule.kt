package pt.antares.app.core.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import pt.antares.app.AppViewModel
import pt.antares.app.feature.barcode.BarcodeResolveViewModel
import pt.antares.app.feature.diary.DiaryViewModel
import pt.antares.app.feature.exercise.AddExerciseViewModel
import pt.antares.app.feature.fasting.ui.FastingHistoryViewModel
import pt.antares.app.feature.fasting.ui.FastingViewModel
import pt.antares.app.feature.running.ui.RunDetailViewModel
import pt.antares.app.feature.running.ui.RunHistoryViewModel
import pt.antares.app.feature.running.ui.RunSummaryViewModel
import pt.antares.app.feature.running.ui.RunViewModel
import pt.antares.app.feature.fooddata.FoodDetailViewModel
import pt.antares.app.feature.fooddata.FoodEditViewModel
import pt.antares.app.feature.fooddata.FoodSearchViewModel
import pt.antares.app.feature.onboarding.OnboardingViewModel
import pt.antares.app.feature.profile.ui.ProfileSettingsViewModel
import pt.antares.app.feature.recipe.RecipeDetailViewModel
import pt.antares.app.feature.recipe.RecipeEditViewModel
import pt.antares.app.feature.recipe.RecipePickViewModel
import pt.antares.app.feature.stats.NutritionStatsViewModel
import pt.antares.app.feature.profile.ui.WeightViewModel
import pt.antares.app.core.privacy.PrivacyViewModel
import pt.antares.app.feature.today.TodayViewModel
import pt.antares.app.feature.workout.ui.ExerciseCreateViewModel
import pt.antares.app.feature.workout.ui.ExerciseDetailViewModel
import pt.antares.app.feature.workout.ui.ExerciseLibraryViewModel
import pt.antares.app.feature.workout.ui.RoutineEditViewModel
import pt.antares.app.feature.workout.ui.RoutineItemPickViewModel
import pt.antares.app.feature.workout.ui.WeeklyScheduleViewModel
import pt.antares.app.feature.workout.ui.WorkoutHubViewModel
import pt.antares.app.feature.workout.ui.WorkoutDetailViewModel
import pt.antares.app.feature.workout.ui.WorkoutHistoryViewModel
import pt.antares.app.feature.workout.ui.WorkoutSessionViewModel
import pt.antares.app.feature.workout.ui.WorkoutStatsViewModel
import pt.antares.app.feature.workout.ui.WorkoutSummaryViewModel

/**
 * Os ViewModels, todos com ciclo de vida próprio — cada ecrã recebe o seu e ele morre com
 * o ecrã. É o oposto do [coreModule], onde tudo é único e vive enquanto a app viver.
 *
 * Os `get()` sem tipo resolvem-se pela assinatura do construtor: acrescentar um parâmetro
 * a um ViewModel obriga a acrescentar um `get()` aqui, e o erro só aparece ao correr.
 */
val viewModelModule = module {
    viewModel { AppViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { OnboardingViewModel(get(), get()) }
    viewModel { TodayViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { WeightViewModel(get()) }
    viewModel { ProfileSettingsViewModel(get(), get()) }
    viewModel { pt.antares.app.feature.profile.ui.HealthProfileViewModel(get(), get(), get()) }
    viewModel { pt.antares.app.feature.profile.ui.BodyCompositionViewModel(get(), get()) }
    viewModel { pt.antares.app.feature.profile.ui.GoalChangeViewModel(get()) }
    viewModel { pt.antares.app.feature.profile.ui.ShowMathsViewModel(get()) }
    viewModel { pt.antares.app.feature.profile.ui.MeasurementHistoryViewModel(get()) }
    viewModel { pt.antares.app.feature.profile.ui.DietBreakViewModel(get()) }
    viewModel { pt.antares.app.feature.settings.SettingsViewModel(get()) }
    viewModel { pt.antares.app.feature.admin.AdminViewModel(get()) }
    viewModel { pt.antares.app.feature.admin.DemoViewModel(get()) }
    viewModel { pt.antares.app.feature.crash.CrashViewModel(get(), get(IoDispatcher)) }
    viewModel { pt.antares.app.feature.progress.ProgressViewModel(get(), get(), get(), get()) }
    viewModel { pt.antares.app.feature.progress.ProgressPhotosViewModel(get()) }
    viewModel { pt.antares.app.feature.profile.ui.CycleViewModel(get()) }
    viewModel { PrivacyViewModel(get(), get(), get(), get(), get(), get()) }

    viewModel { DiaryViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { FoodSearchViewModel(get(), get(), get(), get(), get()) }
    viewModel { FoodDetailViewModel(get(), get(), get(), get()) }
    viewModel { FoodEditViewModel(get(), get()) }

    viewModel { BarcodeResolveViewModel(get(), get(), get()) }
    viewModel { RecipeEditViewModel(get()) }
    viewModel { RecipeDetailViewModel(get(), get(), get()) }
    viewModel { pt.antares.app.feature.fooddata.RichInViewModel(get(), get(), get()) }
    viewModel { RecipePickViewModel(get()) }
    viewModel { NutritionStatsViewModel(get(), get()) }

    viewModel { AddExerciseViewModel(get(), get(), get()) }

    viewModel { ExerciseLibraryViewModel(get()) }
    viewModel { ExerciseDetailViewModel(get(), get()) }
    viewModel { ExerciseCreateViewModel(get()) }
    viewModel { WorkoutHubViewModel(get(), get()) }
    viewModel { WeeklyScheduleViewModel(get()) }
    viewModel { RoutineEditViewModel(get()) }
    viewModel { RoutineItemPickViewModel(get()) }
    viewModel { WorkoutSessionViewModel(get(), get(), get(), get(), get()) }
    viewModel { WorkoutSummaryViewModel(get(), get()) }
    viewModel { WorkoutHistoryViewModel(get()) }
    viewModel { WorkoutDetailViewModel(get()) }
    viewModel { WorkoutStatsViewModel(get()) }

    viewModel { FastingViewModel(get()) }
    viewModel { FastingHistoryViewModel(get()) }

    viewModel { RunViewModel(get(), get()) }
    viewModel { RunSummaryViewModel(get(), get()) }
    viewModel { RunHistoryViewModel(get()) }
    viewModel { RunDetailViewModel(get()) }

    viewModel { pt.antares.app.feature.ai.AiViewModel(get()) }
    viewModel { pt.antares.app.feature.coach.CoachViewModel(get()) }

    viewModel { pt.antares.app.feature.health.HealthViewModel(get(), get()) }
}
