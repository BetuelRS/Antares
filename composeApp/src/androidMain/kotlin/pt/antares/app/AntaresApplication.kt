package pt.antares.app

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import okio.Path.Companion.toOkioPath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.android.get
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pt.antares.app.core.datastore.AppPreferences
import pt.antares.app.core.privacy.AutoBackup

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

        // A cópia de segurança automática, no arranque e não num trabalho agendado: um
        // `WorkManager` que o fabricante mata não escreve nada e ninguém dá por isso — e
        // desde a 2.1.0 esta é a única cópia que existe, porque a da Google está desligada.
        //
        // Espera pelo fim do arranque em vez de correr já. Quem atualiza tem-no feito e a
        // cópia sai no mesmo instante; quem instala de fresco só a tem quando houver o que
        // copiar — e sai nesse momento, sem esperar por outro arranque da app, senão o
        // cartão do Hoje ficava vermelho no primeiro minuto de uso.
        //
        // O `runCatching` é pela mesma razão dos outros: não conseguir escrever a cópia não
        // é motivo para a app não abrir, e o cartão de estado diz-o na cara de quem entrar.
        runCatching {
            appScope.launch {
                get<AppPreferences>().onboardingDone.first { feito -> feito }
                get<AutoBackup>().correrSeNecessario()

                // A seguir à cópia, e nunca antes: a varredura apaga fotografias de pratos,
                // e se a app morresse a meio deste arranque era melhor tê-las apagado
                // depois de a cópia dos números estar escrita do que antes.
                get<pt.antares.app.core.util.FotosDeRefeicao>().varrer()
            }
        }

        instalarCacheDeImagens()
    }

    /**
     * A cache em disco das imagens dos exercícios, escrita à mão em vez de deixada por
     * omissão. Numa app que se apresenta como offline, o que já se viu uma vez tem de
     * continuar a ver-se sem rede — e o tamanho é uma decisão, não um acaso: são 1300
     * exercícios com imagens, e sem teto a pasta cresce até onde o telemóvel deixar.
     */
    private fun instalarCacheDeImagens() {
        SingletonImageLoader.setSafe { context ->
            ImageLoader.Builder(context)
                .diskCache {
                    DiskCache.Builder()
                        .directory(context.filesDir.resolve(PASTA_DE_IMAGENS).toOkioPath())
                        .maxSizeBytes(CACHE_MAX_BYTES)
                        .build()
                }
                .build()
        }
    }

    private companion object {
        const val PASTA_DE_IMAGENS = "image_cache"

        // 64 MB dá para algumas centenas de imagens de exercício, e é pouco ao lado do que
        // uma galeria de fotografias ocupa.
        const val CACHE_MAX_BYTES = 64L * 1024 * 1024
    }
}
