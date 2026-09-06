package pt.antares.app.feature.running

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.feature.running.domain.Split

/**
 * A frase que a corrida diz em voz alta.
 *
 * É a única parte desta versão que não se lê — ouve-se —, e por isso é a que menos se
 * consegue conferir a olho no aparelho: o `logcat` regista que o motor de voz falou, e não o
 * que ele disse. Aqui prova-se a frase inteira, nas duas línguas.
 */
@RunWith(RobolectricTestRunner::class)
class FraseDoAvisoTest {

    private fun contexto(idioma: String): Context {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val config = Configuration(base.resources.configuration)
        config.setLocale(Locale(idioma))
        return base.createConfigurationContext(config)
    }

    private fun quilometro(indice: Int, ritmoSec: Int) =
        Split(index = indice, distanceM = 1000.0, movingMs = 300_000L, paceSecPerKm = ritmoSec, kcal = 70)

    @Test
    fun `um quilometro diz o numero, o ritmo e o tempo total`() {
        val frase = fraseDoAviso(contexto("pt"), quilometro(3, 342), movingMs = 17 * 60_000L)
        assertEquals("Quilómetro 3. Ritmo 5 e 42 por quilómetro. Tempo total 17 minutos.", frase)
    }

    @Test
    fun `e em ingles diz o mesmo`() {
        val frase = fraseDoAviso(contexto("en"), quilometro(3, 342), movingMs = 17 * 60_000L)
        assertEquals("Kilometre 3. Pace 5 42 per kilometre. Total time 17 minutes.", frase)
    }

    @Test
    fun `os segundos do ritmo levam o zero a frente`() {
        // «Ritmo 5 e 5» seriam cinco minutos e cinco segundos ou cinco e cinquenta? O zero
        // é o que separa as duas leituras, e a voz lê-o.
        val frase = fraseDoAviso(contexto("pt"), quilometro(1, 305), movingMs = 60_000L)
        assertTrue(frase.contains("5 e 05"), "o ritmo saiu sem o zero à frente: $frase")
    }

    @Test
    fun `uma volta diz volta e nao quilometro`() {
        val volta = Split(
            index = 2,
            distanceM = 400.0,
            movingMs = 90_000L,
            paceSecPerKm = 225,
            kcal = 28,
            manual = true,
        )
        val frase = fraseDoAviso(contexto("pt"), volta, movingMs = 90_000L)
        assertTrue(frase.startsWith("Volta 2."), "a volta anunciou-se como quilómetro: $frase")
        assertFalse(frase.contains("Quilómetro"))
    }

    @Test
    fun `sem ritmo a frase continua a ser uma frase`() {
        // O motor devolve zero enquanto não houver um metro andado neste parcial. Sem esta
        // regra a frase ficava com «Ritmo 0 e 00» no meio, que é um número inventado.
        val semRitmo = quilometro(1, 0)
        val frase = fraseDoAviso(contexto("pt"), semRitmo, movingMs = 60_000L)
        assertEquals("Quilómetro 1. Tempo total 1 minuto.", frase)
    }

    @Test
    fun `passada a hora, o tempo diz horas e minutos`() {
        val frase = fraseDoAviso(contexto("pt"), quilometro(12, 300), movingMs = 65 * 60_000L)
        assertTrue(frase.endsWith("Tempo total 1 hora e 5 minutos."), frase)
    }

    @Test
    fun `um minuto e um minuto, e nao um minutos`() {
        // O plural existe nos dois idiomas e é o género de coisa que passa despercebida
        // quando ninguém lê a frase em voz alta.
        val frase = fraseDoAviso(contexto("pt"), quilometro(1, 300), movingMs = 60_000L)
        assertTrue(frase.endsWith("Tempo total 1 minuto."), frase)
    }
}
