package pt.antares.app.core.designsystem

import androidx.compose.ui.text.font.FontWeight
import pt.antares.app.feature.about.AppChangelog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InlineBoldTest {

    @Test
    fun `texto sem marcacao passa inteiro`() {
        val r = inlineBold("uma linha simples")
        assertEquals("uma linha simples", r.text)
        assertTrue(r.spanStyles.isEmpty(), "não devia ter estilo nenhum")
    }

    @Test
    fun `os asteriscos desaparecem do texto visivel`() {
        val r = inlineBold("o **ecrã do ciclo** não abria")
        assertEquals("o ecrã do ciclo não abria", r.text)
    }

    @Test
    fun `o trecho marcado fica a negrito`() {
        val r = inlineBold("o **ecrã do ciclo** não abria")
        val negrito = r.spanStyles.single()
        assertEquals(FontWeight.Bold, negrito.item.fontWeight)
        assertEquals("ecrã do ciclo", r.text.substring(negrito.start, negrito.end))
    }

    @Test
    fun `duas marcas na mesma linha funcionam`() {
        val r = inlineBold("**um** no meio **outro**")
        assertEquals("um no meio outro", r.text)
        assertEquals(2, r.spanStyles.size)
    }

    @Test
    fun `a linha pode comecar e acabar marcada`() {
        val r = inlineBold("**tudo marcado**")
        assertEquals("tudo marcado", r.text)
        assertEquals(1, r.spanStyles.size)
    }

    @Test
    fun `marca sem par fecha-se sozinha sem perder conteudo`() {
        val r = inlineBold("uma **marca aberta que nunca fecha")
        assertEquals("uma marca aberta que nunca fecha", r.text)
    }

    @Test
    fun `texto vazio nao rebenta`() {
        assertEquals("", inlineBold("").text)
    }

    @Test
    fun `nenhuma entrada do changelog mostra asteriscos`() {
        val comAsterisco = AppChangelog.versions.flatMap { versao ->
            (versao.highlights + versao.title).map { versao.name to it }
        }.filter { (_, linha) -> inlineBold(linha).text.contains('*') }

        assertTrue(
            comAsterisco.isEmpty(),
            "estas entradas mostrariam asteriscos em cru: $comAsterisco",
        )
    }
}
