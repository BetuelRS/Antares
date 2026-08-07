package pt.antares.app.feature.crash

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import pt.antares.app.core.crash.CrashReport
import pt.antares.app.core.crash.CrashStore
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CrashViewModelTest {

    private class StoreEmMemoria(var texto: String? = null) : CrashStore {
        var limpezas = 0
        override fun write(report: String) { texto = report }
        override fun read(): String? = texto
        override fun clear() { texto = null; limpezas++ }
    }

    @Before
    fun setup() = Dispatchers.setMain(StandardTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private suspend fun CrashViewModel.pronto(): CrashUi = state.first { !it.aLer }

    private val relatorio = CrashReport.format(
        versao = "0.9.18",
        quando = 1L,
        thread = "main",
        tipo = "java.lang.IllegalArgumentException",
        mensagem = "Key was already used",
        stack = listOf(
            "androidx.compose.foundation.lazy.LazyListKt.check(LazyList.kt:120)",
            "pt.antares.app.feature.workout.ui.WorkoutDetailScreenKt.Detail(WorkoutDetailScreen.kt:58)",
        ),
    )

    @Test
    fun `sem crash o ecra diz que nao ha`() = runTest {
        val vm = CrashViewModel(StoreEmMemoria(), Dispatchers.Default)
        val s = vm.pronto()
        assertFalse(s.temCrash)
        assertNull(s.relatorio)
        assertNull(s.culpado)
    }

    @Test
    fun `com crash mostra o relatorio e aponta a linha desta app`() = runTest {
        val vm = CrashViewModel(StoreEmMemoria(relatorio), Dispatchers.Default)
        val s = vm.pronto()

        assertTrue(s.temCrash)
        assertTrue(s.relatorio!!.contains("Key was already used"))

        assertEquals(
            "pt.antares.app.feature.workout.ui.WorkoutDetailScreenKt.Detail(WorkoutDetailScreen.kt:58)",
            s.culpado,
        )
    }

    @Test
    fun `limpar apaga o ficheiro e o ecra`() = runTest {
        val store = StoreEmMemoria(relatorio)
        val vm = CrashViewModel(store, Dispatchers.Default)
        vm.pronto()

        vm.limpar()

        val s = vm.state.first { !it.temCrash }
        assertNull(s.relatorio)
        assertNull(store.texto)
        assertEquals(1, store.limpezas)
    }

    @Test
    fun `abrir o ecra nao apaga o crash`() = runTest {

        val store = StoreEmMemoria(relatorio)
        CrashViewModel(store, Dispatchers.Default).pronto()

        assertEquals(0, store.limpezas, "abrir o ecrã apagou o relatório")
        assertEquals(relatorio, store.texto)
    }

    @Test
    fun `nada envia o relatorio sozinho`() = runTest {

        val ecra = File("src/commonMain/kotlin/pt/antares/app/feature/crash/CrashScreen.kt").readText()
        val vm = File("src/commonMain/kotlin/pt/antares/app/feature/crash/CrashViewModel.kt").readText()

        val proibido = Regex("""httpClient|post\(|upload|functions\.invoke|Supabase""")
        assertFalse(proibido.containsMatchIn(ecra), "o ecrã do crash fala com a rede")
        assertFalse(proibido.containsMatchIn(vm), "o ViewModel do crash fala com a rede")

        assertTrue(ecra.contains("rememberFileSharer"), "não há forma de partilhar o relatório")
    }
}
