package pt.antares.app.core.calc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import pt.antares.app.core.model.RegraDeProgressao
import pt.antares.app.core.model.UnitSystem

class ProgressaoTest {

    private val alvo = AlvoDoExercicio(series = 3, repsMin = 8, repsMax = 12, pesoKg = 60.0)

    @Test
    fun `sem regra nao ha proposta nenhuma`() {
        assertNull(Progressao.proximo(alvo, tresSeries(60.0, 12), RegraDeProgressao.NENHUMA, 2.5))
    }

    @Test
    fun `sem ultima vez nao ha de onde subir`() {
        assertNull(Progressao.proximo(alvo, emptyList(), RegraDeProgressao.LINEAR, 2.5))
    }

    @Test
    fun `linear sobe o peso quando se fez o topo em todas as series`() {
        val p = Progressao.proximo(alvo, tresSeries(60.0, 12), RegraDeProgressao.LINEAR, 2.5)!!
        assertEquals(62.5, p.pesoKg)
        assertEquals(12, p.reps)
        assertTrue(p.subiu)
    }

    @Test
    fun `linear repete o peso quando uma serie ficou abaixo do topo`() {
        val ultima = listOf(feita(60.0, 12), feita(60.0, 12), feita(60.0, 11))
        val p = Progressao.proximo(alvo, ultima, RegraDeProgressao.LINEAR, 2.5)!!
        assertEquals(60.0, p.pesoKg)
        assertEquals(12, p.reps)
        assertTrue(!p.subiu)
    }

    @Test
    fun `faltar uma serie das planeadas nao conta como completo`() {
        val ultima = listOf(feita(60.0, 12), feita(60.0, 12))
        val p = Progressao.proximo(alvo, ultima, RegraDeProgressao.LINEAR, 2.5)!!
        assertEquals(60.0, p.pesoKg)
        assertTrue(!p.subiu)
    }

    @Test
    fun `dupla sobe o peso e volta ao minimo do intervalo`() {
        val p = Progressao.proximo(alvo, tresSeries(60.0, 12), RegraDeProgressao.DUPLA, 2.5)!!
        assertEquals(62.5, p.pesoKg)
        assertEquals(8, p.reps)
        assertTrue(p.subiu)
    }

    @Test
    fun `dupla pede uma repeticao a mais no mesmo peso`() {
        val ultima = listOf(feita(60.0, 10), feita(60.0, 9), feita(60.0, 9))
        val p = Progressao.proximo(alvo, ultima, RegraDeProgressao.DUPLA, 2.5)!!
        assertEquals(60.0, p.pesoKg)

        // Uma a mais do que a **pior** série, e não do que a melhor: o alvo é o número que
        // todas têm de alcançar, e subir a partir da melhor deixava as outras para trás.
        assertEquals(10, p.reps)
        assertTrue(!p.subiu)
    }

    @Test
    fun `dupla nunca pede menos do que o minimo do intervalo`() {
        val ultima = listOf(feita(60.0, 5), feita(60.0, 5), feita(60.0, 5))
        val p = Progressao.proximo(alvo, ultima, RegraDeProgressao.DUPLA, 2.5)!!
        assertEquals(8, p.reps)
    }

    @Test
    fun `dupla nao pede mais do que o maximo do intervalo`() {
        val ultima = listOf(feita(60.0, 12), feita(60.0, 12), feita(60.0, 11))
        val p = Progressao.proximo(alvo, ultima, RegraDeProgressao.DUPLA, 2.5)!!
        assertEquals(12, p.reps)
        assertTrue(!p.subiu)
    }

    @Test
    fun `pesos diferentes na mesma sessao nao dao proposta`() {
        val ultima = listOf(feita(60.0, 12), feita(60.0, 12), feita(50.0, 12))
        assertNull(Progressao.proximo(alvo, ultima, RegraDeProgressao.LINEAR, 2.5))
    }

    @Test
    fun `peso zero nao da proposta`() {
        assertNull(Progressao.proximo(alvo, tresSeries(0.0, 12), RegraDeProgressao.LINEAR, 2.5))
    }

    @Test
    fun `incremento nao positivo nunca sobe`() {
        val p = Progressao.proximo(alvo, tresSeries(60.0, 12), RegraDeProgressao.LINEAR, 0.0)!!
        assertEquals(60.0, p.pesoKg)
        assertTrue(!p.subiu)
    }

    @Test
    fun `um intervalo fechado faz a dupla comportar-se como a linear`() {
        val fixo = AlvoDoExercicio(series = 3, repsMin = 5, repsMax = 5, pesoKg = 100.0)
        val p = Progressao.proximo(fixo, tresSeries(100.0, 5), RegraDeProgressao.DUPLA, 2.5)!!
        assertEquals(102.5, p.pesoKg)
        assertEquals(5, p.reps)
        assertTrue(p.subiu)
    }

    @Test
    fun `o incremento por omissao e o degrau da barra em cada unidade`() {
        assertEquals(2.5, Progressao.incrementoPorOmissao(UnitSystem.METRIC))

        // Cinco libras, e não 2,5 kg: o disco mais pequeno do conjunto imperial é de 2,5 lb,
        // e 2,5 kg seriam 5,51 lb — um peso que não se monta com os discos que existem.
        assertEquals(5.0, Progressao.incrementoPorOmissao(UnitSystem.IMPERIAL) / 0.45359237, 1e-9)
    }

    private fun feita(peso: Double, reps: Int) = SerieDaUltimaVez(peso, reps)
    private fun tresSeries(peso: Double, reps: Int) = List(3) { feita(peso, reps) }
}

class ResumoDaUltimaVezTest {

    @Test
    fun `sem series nao ha resumo`() {
        assertNull(resumoDaUltimaVez(emptyList()))
    }

    @Test
    fun `tudo igual da a forma curta`() {
        val r = resumoDaUltimaVez(List(3) { SerieDaUltimaVez(60.0, 10) })
        assertEquals(UltimaVez.Uniforme(series = 3, reps = 10, pesoKg = 60.0), r)
    }

    @Test
    fun `mesmo peso com repeticoes diferentes guarda a ordem em que se fizeram`() {
        val series = listOf(SerieDaUltimaVez(60.0, 12), SerieDaUltimaVez(60.0, 10), SerieDaUltimaVez(60.0, 9))
        assertEquals(UltimaVez.MesmoPeso(listOf(12, 10, 9), 60.0), resumoDaUltimaVez(series))
    }

    @Test
    fun `pesos diferentes devolvem as series como estao`() {
        val series = listOf(SerieDaUltimaVez(60.0, 12), SerieDaUltimaVez(50.0, 12))
        assertEquals(UltimaVez.Mista(series), resumoDaUltimaVez(series))
    }

    @Test
    fun `uma serie so continua a ser uniforme`() {
        assertEquals(UltimaVez.Uniforme(1, 8, 80.0), resumoDaUltimaVez(listOf(SerieDaUltimaVez(80.0, 8))))
    }
}
