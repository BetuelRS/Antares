package pt.antares.app.core.fooddata

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BarcodeTest {

    @Test
    fun `EAN-13 valido passa intacto`() {

        assertEquals("3017620422003", Barcode.normalize("3017620422003"))
    }

    @Test
    fun `UPC-A de 12 digitos vira EAN-13 com zero a frente`() {

        assertEquals("0038000138416", Barcode.normalize("038000138416"))
    }

    @Test
    fun `ITF-14 de caixa cai no codigo do item`() {

        assertEquals("3017620422003", Barcode.normalize("13017620422003"))
    }

    @Test
    fun `codigo com digito de controlo errado e recusado`() {

        assertNull(Barcode.normalize("3017620422004"))
        assertNull(Barcode.normalize("3017620422002"))
    }

    @Test
    fun `lixo nao passa`() {
        assertNull(Barcode.normalize(null))
        assertNull(Barcode.normalize(""))
        assertNull(Barcode.normalize("   "))
        assertNull(Barcode.normalize("abc123"))
        assertNull(Barcode.normalize("12345"))
    }

    @Test
    fun `EAN-8 legitimo mantem-se`() {

        assertTrue(Barcode.isChecksumValid("96385074"))
        assertEquals("96385074", Barcode.normalize("96385074"))
    }

    @Test
    fun `UPC-E expande para EAN-13`() {

        assertEquals("0012100003416", Barcode.normalize("01234116"))
    }

    @Test
    fun `oito digitos que fecham como EAN-8 ficam EAN-8`() {

        assertEquals("01234565", Barcode.normalize("01234565"))
    }

    @Test
    fun `variantes incluem a forma sem o zero para produtos americanos`() {

        val v = Barcode.searchVariants("038000138416")
        assertEquals("0038000138416", v.first())
        assertTrue(v.contains("038000138416"), "falta a variante sem o zero")
    }

    @Test
    fun `variantes de um EAN-13 europeu nao inventam alternativas`() {
        assertEquals(listOf("3017620422003"), Barcode.searchVariants("3017620422003"))
    }
}
