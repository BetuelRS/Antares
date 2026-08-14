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

        // Antes de tudo o resto: uma falha na própria arranque tem de ficar registada, e
        // instalar isto depois do Koin deixaria a janela mais provável por cobrir.
        CrashCatcher.install(FileCrashStore(this), BuildConfig.VERSION_NAME)

        // A guarda é para os testes: o Robolectric pode criar a aplicação mais do que uma
        // vez no mesmo processo, e o Koin recusa arrancar duas vezes.
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidLogger()
                androidContext(this@AntaresApplication)
                modules(coreModule, databaseModule, viewModelModule)
            }
        }

        // `SupervisorJob` para uma destas falhar não levar a outra consigo.
        val appScope = CoroutineScope(SupervisorJob() + io)

        // Os `runCatching` são deliberados: nenhuma destas duas coisas vale impedir a app de
        // abrir. Sem alarme da meia-noite o ecrã fica no dia errado até ser reaberto; sem
        // notificações agendadas não há lembretes. Nenhum dos dois é motivo para não arrancar.
        runCatching { pt.antares.app.core.util.DayTicker.start(appScope) }

        runCatching { NotificationScheduler.scheduleAll(this) }
    }
}
