package pt.antares.app.core.di

import org.koin.dsl.module
import pt.antares.app.feature.diary.DiaryRepository
import pt.antares.app.feature.exercise.ExerciseRepository
import pt.antares.app.feature.fooddata.FoodRepository
import pt.antares.app.feature.fooddata.FoodSeeder
import pt.antares.app.feature.profile.data.ProfileRepository
import pt.antares.app.feature.recipe.RecipeRepository
import pt.antares.app.feature.fasting.data.FastingProtocolSeeder
import pt.antares.app.feature.fasting.data.FastingRepository
import pt.antares.app.feature.running.data.RunRepository
import pt.antares.app.feature.stats.NutritionStatsRepository
import pt.antares.app.feature.workout.data.ExerciseLibraryRepository
import pt.antares.app.feature.workout.data.ExerciseSeeder
import pt.antares.app.feature.workout.data.RoutineRepository
import pt.antares.app.feature.workout.data.RoutineTemplateSeeder
import pt.antares.app.feature.workout.data.SessionPickBus
import pt.antares.app.feature.workout.data.WorkoutHistoryRepository
import pt.antares.app.feature.workout.data.WorkoutSessionRepository
import pt.antares.app.core.demo.DemoDataWriter
import pt.antares.app.feature.profile.data.BodyMeasurementRepository
import pt.antares.app.feature.profile.data.GoalMigrationRepository
import pt.antares.app.feature.templates.MealTemplateRepository
import pt.antares.app.feature.progress.ProgressRepository
import pt.antares.app.feature.progress.ProgressPhotoRepository
import pt.antares.app.feature.profile.data.CycleRepository

/**
 * Os repositórios e os semeadores.
 *
 * Recebem funções e não outros repositórios sempre que possível — é isso que os deixa
 * testáveis sem construir meia app à volta, e o que impede o grafo de dar voltas sobre si.
 */
val repositoryModule = module {
    single { DemoDataWriter(get(), get(IoDispatcher)) }

    single { ProfileRepository(get(), get(), get(), get(), get(), get(), get(IoDispatcher)) }
    single {
        BodyMeasurementRepository(
            dao = get(),
            profileDao = get(),
            io = get(IoDispatcher),
        )
    }
    single {
        GoalMigrationRepository(
            profileDao = get(),
            weightDao = get(),
            preferences = get(),
            io = get(IoDispatcher),
        )
    }
    single { FoodRepository(get(), get(), get(), get(), get(IoDispatcher)) }
    single { DiaryRepository(get(), get(), get(IoDispatcher)) }
    single {
        MealTemplateRepository(
            foodLogDao = get(),
            templateDao = get(),
            itemDao = get(),
            io = get(IoDispatcher),
        )
    }
    single { RecipeRepository(get(), get(), get(), get(), get(IoDispatcher)) }
    single { NutritionStatsRepository(get(), get(), get(IoDispatcher)) }
    single { ProgressRepository(get(), get(), get(), get(), get(IoDispatcher)) }
    single { ProgressPhotoRepository(get(), get(), get(), get(IoDispatcher)) }
    single { CycleRepository(get(), get(IoDispatcher)) }
    single { ExerciseRepository(get(), get(IoDispatcher)) }

    single { FoodSeeder(get(), get(IoDispatcher), get(), get()) }
    single { ExerciseSeeder(get(), get(IoDispatcher), get()) }
    single { FastingProtocolSeeder(get(), get(IoDispatcher)) }
    single { FastingRepository(get(), get(), get(), get(IoDispatcher)) }
    single { RunRepository(get(), get(), get(IoDispatcher)) }
    single { ExerciseLibraryRepository(get(), get(), get(IoDispatcher)) }
    single { RoutineRepository(get(), get(), get(), get(IoDispatcher)) }
    single { RoutineTemplateSeeder(get(), get(), get(IoDispatcher)) }
    single { WorkoutSessionRepository(get(), get(), get(), get(), get(), get(IoDispatcher)) }
    single { WorkoutHistoryRepository(get(), get(), get(), get(IoDispatcher)) }
    single { SessionPickBus() }
}
