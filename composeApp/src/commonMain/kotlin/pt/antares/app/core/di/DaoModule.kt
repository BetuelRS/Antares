package pt.antares.app.core.di

import org.koin.dsl.module
import pt.antares.app.core.database.AntaresDb

/**
 * Os DAOs, um a um.
 *
 * Cada um é registado à parte e não a base inteira: assim uma classe declara os DAOs de
 * que precisa em vez de receber acesso a tudo o que está gravado.
 */
val daoModule = module {
    // Cada DAO é registado à parte, e não a base inteira: assim uma classe declara os DAOs
    // de que precisa em vez de receber acesso a tudo.
    single { get<AntaresDb>().userProfileDao() }
    single { get<AntaresDb>().weightLogDao() }
    single { get<AntaresDb>().dailyTargetOverrideDao() }
    single { get<AntaresDb>().foodDao() }
    single { get<AntaresDb>().foodLogDao() }
    single { get<AntaresDb>().foodNutrientDao() }
    single { get<AntaresDb>().foodMarkDao() }

    single { get<AntaresDb>().dbInfoDao() }
    single { get<AntaresDb>().waterLogDao() }
    single { get<AntaresDb>().recipeDao() }
    single { get<AntaresDb>().recipeIngredientDao() }
    single { get<AntaresDb>().recipeStepDao() }
    single { get<AntaresDb>().exerciseLogDao() }
    single { get<AntaresDb>().exerciseLibraryDao() }
    single { get<AntaresDb>().routineDao() }
    single { get<AntaresDb>().workoutSessionDao() }
    single { get<AntaresDb>().workoutSetDao() }
    single { get<AntaresDb>().sessionExerciseNoteDao() }
    single { get<AntaresDb>().exerciseLoadDao() }
    single { get<AntaresDb>().routineScheduleDao() }
    single { get<AntaresDb>().fastingProtocolDao() }
    single { get<AntaresDb>().fastingSessionDao() }
    single { get<AntaresDb>().runDao() }
    single { get<AntaresDb>().trackPointDao() }
    single { get<AntaresDb>().coachReportDao() }
    single { get<AntaresDb>().mealTemplateDao() }
    single { get<AntaresDb>().mealTemplateItemDao() }
    single { get<AntaresDb>().bodyMeasurementDao() }
    single { get<AntaresDb>().goalHistoryDao() }
    single { get<AntaresDb>().searchMissDao() }
    single { get<AntaresDb>().progressPhotoDao() }

    single { get<AntaresDb>().cycleDao() }
    single { get<AntaresDb>().demoDao() }
}
