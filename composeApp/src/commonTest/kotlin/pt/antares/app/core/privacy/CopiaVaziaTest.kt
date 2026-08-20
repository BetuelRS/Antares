package pt.antares.app.core.privacy

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

/**
 * A 2.1.0 escrevia a primeira cópia no primeiro segundo de uma instalação limpa: 526 bytes,
 * vinte e seis tabelas, zero linhas — e o cartão a dizer «última cópia: hoje».
 *
 * Uma cópia que não protege nada é pior do que não haver cópia nenhuma, porque **cala o
 * aviso**. Só se viu ao instalar o APK de lançamento numa máquina limpa; nenhum dos testes
 * da versão o apanhou, porque todos partiam de uma app já usada.
 */
class CopiaVaziaTest {

    private val agora = 1_800_000_000_000L

    @Test
    fun `nao copia antes do arranque estar feito`() {
        assertFalse(
            AutoBackup.deveCorrer(arranqueFeito = false, ultimaMs = 0L, agoraMs = agora),
            "a app copiou antes de haver o que copiar",
        )
    }

    @Test
    fun `o arranque por acabar manda mais do que o tempo passado`() {

        // Mesmo com anos desde a última cópia: se o arranque não está feito, não há dados
        // para lá pôr. O tempo passado não cria conteúdo.
        assertFalse(
            AutoBackup.deveCorrer(
                arranqueFeito = false,
                ultimaMs = agora - 365.days.inWholeMilliseconds,
                agoraMs = agora,
            ),
        )
    }

    @Test
    fun `com o arranque feito e sem copia nenhuma, copia ja`() {

        // É a condição que sustenta desligar a cópia da Google: quem atualiza da 2.0.4 tem o
        // arranque feito e fica com a primeira cópia local no mesmo instante, sem esperar
        // pelos três dias.
        assertTrue(AutoBackup.deveCorrer(arranqueFeito = true, ultimaMs = 0L, agoraMs = agora))
    }

    @Test
    fun `nao repete antes da cadencia`() {
        val ontem = agora - 1.days.inWholeMilliseconds
        assertFalse(AutoBackup.deveCorrer(arranqueFeito = true, ultimaMs = ontem, agoraMs = agora))
    }

    @Test
    fun `copia outra vez passada a cadencia`() {
        val ha3Dias = agora - AutoBackup.DIAS_ENTRE_COPIAS.days.inWholeMilliseconds
        assertTrue(AutoBackup.deveCorrer(arranqueFeito = true, ultimaMs = ha3Dias, agoraMs = agora))
    }
}
