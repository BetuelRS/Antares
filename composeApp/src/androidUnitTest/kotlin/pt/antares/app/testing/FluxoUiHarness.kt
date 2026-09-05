package pt.antares.app.testing

import android.content.ContentProvider
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.junit.After
import org.junit.Before
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import org.robolectric.Robolectric
import pt.antares.app.core.database.AntaresDb
import pt.antares.app.core.datastore.AppPreferences
import pt.antares.app.core.privacy.AutoBackup
import pt.antares.app.core.privacy.BackupStore
import pt.antares.app.core.privacy.DataExporter
import pt.antares.app.core.util.LocalPhotoStore
import pt.antares.app.feature.about.AppChangelog
import pt.antares.app.feature.backup.CopiaViewModel
import pt.antares.app.core.datastore.createPreferencesDataStore
import java.io.File
import java.util.UUID

/**
 * Base dos testes que exercitam um **fluxo inteiro** pela interface: tocar onde a pessoa
 * toca e confirmar o que ela vê a seguir.
 *
 * Difere do [ViewModelHarness] em duas coisas, e as duas por causa do Compose:
 *
 * - O despachante é o `Unconfined`, e não um de teste. O `runComposeUiTest` traz o seu
 *   próprio relógio, e dois relógios a controlar as mesmas corrotinas travam-se um ao
 *   outro. Com o `Unconfined` cada escrita acaba antes de o toque devolver, e a
 *   recomposição já encontra o estado novo.
 * - Os recursos gerados precisam de um `ContentProvider` do Compose que só existe quando a
 *   app arranca a sério. Sem o [arrancaOsRecursos], qualquer `stringResource` rebenta.
 *
 * Os textos leem-se com o [Textos], porque um `stringResource` só funciona dentro da
 * composição — e um teste que procure a frase escrita à mão passa a falhar sempre que
 * alguém corrigir uma vírgula.
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class FluxoUiHarness {

    protected lateinit var db: AntaresDb
        private set

    protected lateinit var prefs: AppPreferences
        private set

    protected val io: CoroutineDispatcher = Dispatchers.Unconfined

    private lateinit var prefsFile: File

    private lateinit var context: Context

    @Before
    fun arrancaOsRecursos() {
        @Suppress("UNCHECKED_CAST")
        val provider = Class.forName("org.jetbrains.compose.resources.AndroidContextProvider")
            as Class<ContentProvider>
        Robolectric.buildContentProvider(provider).create()

        // O que os ViewModels lançam vai para o `viewModelScope`, que corre no `Main`. Sem
        // isto ficava à espera do looper do Robolectric e o teste media a ordem errada.
        Dispatchers.setMain(Dispatchers.Unconfined)

        context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AntaresDb::class.java)
            // O Room pede `limitedParallelism` ao contexto que recebe, e o `Unconfined`
            // recusa-o. Fica o `Default`: as escritas saem da linha da composição, e por
            // isso as afirmações sobre a base esperam pelo resultado em vez de o assumir.
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()

        prefsFile = File(context.cacheDir, "fluxo-${UUID.randomUUID()}.preferences_pb")
        prefs = AppPreferences(createPreferencesDataStore { prefsFile.absolutePath })
    }

    /**
     * Arranca um Koin com o mínimo. Alguns ecrãs trazem cartões que pedem o seu próprio
     * ViewModel lá dentro — o Hoje traz o cartão do relatório semanal — e sem contexto
     * rebentam com «KoinApplication has not been started». Só se registam os ViewModels
     * que não são passados por parâmetro: o resto entra pela porta da frente, à vista.
     *
     * O `ProfileRepository` está aqui por causa do `rememberUnitSystem`: qualquer ecrã que
     * mostre um peso, uma distância ou uma porção precisa de saber em que unidades a pessoa
     * as quer, e vai buscá-lo pelo Koin em vez de o arrastar por doze ViewModels.
     */
    protected fun arrancaKoin(vararg extras: Module) {
        stopKoin()
        startKoin {
            modules(
                module {
                    viewModel { Fabricas.coachViewModel(db, prefs, io) }
                    single { Fabricas.profileRepository(db, io) }

                    // O cartão da cópia de segurança entra no Hoje e no menu, e pede o seu
                    // ViewModel lá dentro. Sem fontes: o cartão só mostra contagens quando
                    // alguém as pede, e nenhum destes testes as pede.
                    viewModel {
                        val exportador = DataExporter(
                            sources = emptyList(),
                            appVersion = AppChangelog.CURRENT,
                        )
                        CopiaViewModel(
                            AutoBackup(
                                prefs,
                                exportador,
                                db.progressPhotoDao(),
                                LocalPhotoStore(context, io),
                                BackupStore(context, io),
                            ),
                            exportador,
                        )
                    }
                },
                *extras,
            )
        }
    }

    @After
    fun fechaTudo() {
        // Os ViewModels primeiro, e **antes** do `resetMain`. Um ViewModel construído à mão
        // no teste fica com o `viewModelScope` vivo depois de o teste acabar, e esse scope
        // despacha para o `Main`: uma leitura da base que ainda vinha a caminho resumia no
        // `Main` enquanto a classe seguinte lhe chamava o `setMain`, e o
        // `kotlinx-coroutines-test` rebenta com «is used concurrently with setting it».
        //
        // Falhou assim no CI e não aqui — a corrida é entre máquinas de velocidades
        // diferentes, e a vítima era a classe a seguir, que não tinha culpa nenhuma.
        viewModels.clear()
        stopKoin()
        Dispatchers.resetMain()
        db.close()
        prefsFile.delete()
    }

    private val viewModels = ViewModelStore()

    /**
     * Entrega o ViewModel ao harness, que o fecha no fim do teste.
     *
     * Os testes de fluxo constroem os ViewModels à mão — é a porta da frente, e é o que os
     * torna legíveis —, e um ViewModel que ninguém fecha continua a trabalhar depois de o
     * teste dele acabar.
     */
    protected fun <T : ViewModel> vivo(vm: T): T = vm.also {
        viewModels.put("vm-${viewModels.keys().size}", it)
    }

    /**
     * Guarda os textos lidos durante a composição, para as afirmações lhes chegarem de
     * fora. Preenche-se com [ler] dentro do `setContent`.
     */
    protected class Textos {
        private val lidos = LinkedHashMap<StringResource, String>()

        @Composable
        fun ler(vararg recursos: StringResource) {
            recursos.forEach { lidos[it] = stringResource(it) }
        }

        /**
         * O mesmo, para um texto com argumentos. Existe porque metade das frases da app leva
         * um número ou um nome lá dentro, e escrevê-las à mão no teste é o que faz um teste
         * passar a falhar quando alguém corrige uma vírgula.
         */
        @Composable
        fun lerFormatado(recurso: StringResource, vararg args: Any) {
            lidos[recurso] = stringResource(recurso, *args)
        }

        operator fun get(recurso: StringResource): String =
            lidos[recurso] ?: error("texto não foi lido dentro do setContent")
    }
}
