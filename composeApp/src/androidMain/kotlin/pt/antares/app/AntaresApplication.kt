package pt.antares.app

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import com.antares.app.BuildConfig
import pt.antares.app.core.crash.CrashCatcher
import pt.antares.app.core.crash.FileCrashStore
import pt.antares.app.core.di.IoDispatcher
import pt.antares.app.core.di.coreModule
import pt.antares.app.core.di.databaseModule
import pt.antares.app.core.di.viewModelModule
import pt.antares.app.core.notifications.NotificationScheduler
import kotlinx.coroutines.CoroutineDispatcher

class AntaresApplication : Application() {

    private val io: CoroutineDispatcher by inject(IoDispatcher)

    override fun onCreate() {
        super.onCreate()

        CrashCatcher.install(FileCrashStore(this), BuildConfig.VERSION_NAME)

        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidLogger()
                androidContext(this@AntaresApplication)
                modules(coreModule, databaseModule, viewModelModule)
            }
        }

        val appScope = CoroutineScope(SupervisorJob() + io)

        runCatching { pt.antares.app.core.util.DayTicker.start(appScope) }

        runCatching { NotificationScheduler.scheduleAll(this) }
    }
}
