package pt.antares.app.feature.fooddata

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class DedupeNormalizeTest {

    @Test
    fun `maiusculas e acentos nao fazem alimentos diferentes`() {
        assertEquals(normalizeForDedupe("Bolacha Maria"), normalizeForDedupe("bolacha maria"))
        assertEquals(normalizeForDedupe("Salmão grelhado"), normalizeForDedupe("salmao grelhado"))
        assertEquals(normalizeForDedupe("Pastéis de nata"), normalizeForDedupe("pasteis de nata"))
        assertEquals(normalizeForDedupe("Açúcar"), normalizeForDedupe("acucar"))
    }

    @Test
    fun `pontuacao e espacos a mais nao contam`() {
        assertEquals(normalizeForDedupe("Arroz, branco  cozido"), normalizeForDedupe("arroz branco cozido"))
        assertEquals(normalizeForDedupe("  Batata cozida  "), normalizeForDedupe("Batata cozida"))
    }

    @Test
    fun `alimentos mesmo diferentes continuam diferentes`() {

        assertNotEquals(normalizeForDedupe("Batata cozida"), normalizeForDedupe("Batata frita"))
        assertNotEquals(normalizeForDedupe("Leite meio-gordo"), normalizeForDedupe("Leite gordo"))
        assertNotEquals(normalizeForDedupe("Queijo flamengo"), normalizeForDedupe("Queijo fresco"))
        assertNotEquals(normalizeForDedupe("Bacalhau cozido"), normalizeForDedupe("Bacalhau assado"))
    }

    @Test
    fun `nao colapsa palavras diferentes num so`() {
        assertNotEquals(normalizeForDedupe("pao"), normalizeForDedupe("pao de forma"))
    }

    @Test
    fun `nomes vazios ou so pontuacao nao rebentam`() {
        assertEquals("", normalizeForDedupe(""))
        assertEquals("", normalizeForDedupe("   "))
        assertEquals("", normalizeForDedupe("---"))
    }
}
