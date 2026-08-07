package pt.antares.app.core.designsystem

import kotlin.test.Test
import kotlin.test.assertEquals

class NumberFormatTest {

    @Test
    fun `em portugues o separador e virgula`() {
        assertEquals("0,5", oneDecimal(0.5, comma = true))
        assertEquals("82,4", oneDecimal(82.4, comma = true))
    }

    @Test
    fun `nas outras linguas e ponto`() {
        assertEquals("0.5", oneDecimal(0.5, comma = false))
        assertEquals("82.4", oneDecimal(82.4, comma = false))
    }

    @Test
    fun `arredonda a uma casa`() {
        assertEquals("0,5", oneDecimal(0.47, comma = true))
        assertEquals("0,5", oneDecimal(0.45, comma = true))
        assertEquals("1,0", oneDecimal(0.96, comma = true))
    }

    @Test
    fun `o zero mostra a casa decimal`() {

        assertEquals("0,0", oneDecimal(0.0, comma = true))
    }

    @Test
    fun `negativos abaixo de um nao perdem o sinal`() {

        assertEquals("-0,4", oneDecimal(-0.4, comma = true))
        assertEquals("-0.4", oneDecimal(-0.4, comma = false))
    }

    @Test
    fun `negativos acima de um mantem o sinal uma so vez`() {
        assertEquals("-2,5", oneDecimal(-2.5, comma = true))
    }
}

class TrimmedDecimalTest {

    @Test
    fun `corta a casa decimal quando ela e zero`() {
        assertEquals("800", trimmedDecimal(800.0, comma = true))
        assertEquals("800", trimmedDecimal(800.0, comma = false))
        assertEquals("0", trimmedDecimal(0.0, comma = true))
    }

    @Test
    fun `mantem a casa decimal quando ela existe`() {
        assertEquals("1112,5", trimmedDecimal(1112.5, comma = true))
        assertEquals("1112.5", trimmedDecimal(1112.5, comma = false))
        assertEquals("1767,5", trimmedDecimal(1767.5, comma = true))
    }

    @Test
    fun `o sinal sobrevive ao corte`() {
        assertEquals("-161", trimmedDecimal(-161.0, comma = true))
        assertEquals("-0,4", trimmedDecimal(-0.4, comma = true))
    }

    @Test
    fun `nao corta um zero que faz parte do numero`() {

        assertEquals("100", trimmedDecimal(100.0, comma = true))
        assertEquals("1050", trimmedDecimal(1050.0, comma = true))
    }
}
