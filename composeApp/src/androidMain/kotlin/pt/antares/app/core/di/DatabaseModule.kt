package pt.antares.app.core.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import pt.antares.app.core.database.AntaresDb
import pt.antares.app.core.database.DatabaseFactory
import pt.antares.app.core.database.buildAntaresDb
import pt.antares.app.core.datastore.AppPreferences
import pt.antares.app.core.datastore.createPreferencesDataStore
import pt.antares.app.core.network.supabase.SupabaseContainer
import pt.antares.app.core.network.supabase.createAntaresSupabase
import pt.antares.app.core.health.HealthConnectGateway
import pt.antares.app.core.health.HealthGateway
import pt.antares.app.core.notifications.AndroidCoachNotifier
import pt.antares.app.core.notifications.CoachNotifier
import pt.antares.app.feature.fasting.AndroidFastingNotifier
import pt.antares.app.feature.fasting.FastingNotifier
import pt.antares.app.feature.running.AndroidRunController
import pt.antares.app.feature.running.RunController
import pt.antares.app.feature.workout.AndroidWorkoutAlerts
import pt.antares.app.core.util.LocalPhotoStore
import pt.antares.app.feature.workout.WorkoutAlerts

val databaseModule = module {
    single { DatabaseFactory(androidContext()) }
    single<AntaresDb> { get<DatabaseFactory>().create().buildAntaresDb() }

    single<WorkoutAlerts> { AndroidWorkoutAlerts(androidContext()) }

    single<FastingNotifier> { AndroidFastingNotifier(androidContext()) }

    single<RunController> { AndroidRunController(androidContext(), get(), get()) }

    single { SupabaseContainer(createAntaresSupabase()) }

    single { LocalPhotoStore(androidContext(), get(IoDispatcher)) }

    single(pt.antares.app.core.di.FotosDePrato) {
        LocalPhotoStore(androidContext(), get(IoDispatcher), LocalPhotoStore.DIR_REFEICOES)
    }

    single {
        pt.antares.app.core.util.FotosDeRefeicao(get(), get(pt.antares.app.core.di.FotosDePrato))
    }

    single { pt.antares.app.core.privacy.BackupStore(androidContext(), get(IoDispatcher)) }

    // Dentro do armazenamento privado, ao contrário da cópia de segurança: um catálogo é
    // reconstruível a partir da rede, e nada se perde quando a app é desinstalada.
    single {
        pt.antares.app.core.catalogo.ArmazemDoCatalogo(
            androidContext().filesDir.resolve("catalogo"),
            get(IoDispatcher),
        )
    }

    single<pt.antares.app.core.crash.CrashStore> {
        pt.antares.app.core.crash.FileCrashStore(androidContext())
    }

    single<HealthGateway> { HealthConnectGateway(androidContext()) }

    single<CoachNotifier> { AndroidCoachNotifier(androidContext()) }

    single {
        AppPreferences(
            createPreferencesDataStore {
                androidContext().filesDir.resolve("antares.preferences_pb").absolutePath
            },
        )
    }
}
