package pt.antares.app.core.crash

import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class CrashCatcherTest {

    private var original: Thread.UncaughtExceptionHandler? = null

    @Before
    fun setup() {
        original = Thread.getDefaultUncaughtExceptionHandler()
    }

    @After
    fun tearDown() {
        Thread.setDefaultUncaughtExceptionHandler(original)
    }

    private class StoreEmMemoria : CrashStore {
        var texto: String? = null
        override fun write(report: String) { texto = report }
        override fun read(): String? = texto
        override fun clear() { texto = null }
    }

    private class HandlerAnterior : Thread.UncaughtExceptionHandler {
        var chamado = false
        var recebeu: Throwable? = null
        override fun uncaughtException(t: Thread, e: Throwable) {
            chamado = true
            recebeu = e
        }
    }

    @Test
    fun `grava o crash e entrega-o ao handler anterior`() {
        val anterior = HandlerAnterior()
        Thread.setDefaultUncaughtExceptionHandler(anterior)
        val store = StoreEmMemoria()
        CrashCatcher.install(store, versao = "0.9.18", agora = { 1_700_000_000_000L })

        val erro = IllegalStateException("o ecrã do ciclo não abriu")
        Thread.getDefaultUncaughtExceptionHandler()!!
            .uncaughtException(Thread.currentThread(), erro)

        val relatorio = assertNotNull(store.texto, "o crash não foi gravado")
        assertContains(relatorio, "0.9.18")
        assertContains(relatorio, "IllegalStateException")
        assertContains(relatorio, "o ecrã do ciclo não abriu")
        assertContains(relatorio, "1700000000000")

        assertTrue(anterior.chamado, "o handler engoliu a exceção em vez de a entregar")
        assertEquals(erro, anterior.recebeu, "entregou uma exceção diferente da que recebeu")
    }

    @Test
    fun `um store que falha nao provoca um segundo crash`() {

        val anterior = HandlerAnterior()
        Thread.setDefaultUncaughtExceptionHandler(anterior)
        val storePartido = object : CrashStore {
            override fun write(report: String) = throw RuntimeException("disco cheio")
            override fun read(): String? = null
            override fun clear() = Unit
        }
        CrashCatcher.install(storePartido, versao = "0.9.18")

        val erro = RuntimeException("o original")
        Thread.getDefaultUncaughtExceptionHandler()!!
            .uncaughtException(Thread.currentThread(), erro)

        assertTrue(anterior.chamado, "o erro a gravar comeu o crash original")
        assertEquals(erro, anterior.recebeu)
    }

    @Test
    fun `o ficheiro sobrevive a leitura e limpa-se quando se pede`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = FileCrashStore(context)
        store.clear()
        assertNull(store.read(), "começou com lixo de outro teste")

        store.write("Antares 0.9.18\nerro: qualquer coisa")
        assertContains(assertNotNull(store.read()), "0.9.18")

        store.write("segundo")
        assertEquals("segundo", store.read())

        store.clear()
        assertNull(store.read())
    }
}
