package pt.antares.app.core.util

import kotlin.test.Test
import kotlin.test.assertEquals

class TextNormalizeTest {

    @Test
    fun `remove acentos comuns em PT`() {
        assertEquals("pao", TextNormalize.normalize("pão"))
        assertEquals("acucar", TextNormalize.normalize("açúcar"))
        assertEquals("feijao preto", TextNormalize.normalize("feijão preto"))
    }

    @Test
    fun `converte maiusculas para minusculas`() {
        assertEquals("frango grelhado", TextNormalize.normalize("Frango Grelhado"))
    }

    @Test
    fun `mantem texto ja normalizado igual`() {
        assertEquals("arroz branco", TextNormalize.normalize("arroz branco"))
    }
}
