package pt.antares.app.core.calc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EatingPatternsTest {

    private val segunda = 20_003L

    private fun dia(offset: Long, kcal: Double, proteinG: Double = 100.0) =
        EatingPatterns.Day(segunda + offset, kcal, proteinG)

    private fun semanas(kcalSemana: Double, kcalFds: Double, proteinaSemana: Double = 100.0, proteinaFds: Double = 100.0) =
        (0 until 28).map {
            val fds = EatingPatterns.isWeekend(segunda + it)
            dia(it.toLong(), if (fds) kcalFds else kcalSemana, if (fds) proteinaFds else proteinaSemana)
        }

    @Test
    fun `sabado e domingo sao fim de semana, o resto nao`() {

        val esperado = listOf(false, false, false, false, false, true, true)
        for (i in 0..6) {
            assertEquals(esperado[i], EatingPatterns.isWeekend(segunda + i), "dia $i")
        }
    }

    @Test
    fun `poucos dias nao dao padrao`() {

        val poucos = (0 until 5).map { dia(it.toLong(), if (EatingPatterns.isWeekend(segunda + it)) 3000.0 else 1800.0) }
        assertEquals(emptyList(), EatingPatterns.detect(poucos))
    }

    @Test
    fun `dias sem registo nao contam como dias`() {

        val quaseVazio = (0 until 28).map { dia(it.toLong(), if (it < 5) 2000.0 else 0.0) }
        assertEquals(emptyList(), EatingPatterns.detect(quaseVazio))
    }

    @Test
    fun `sem fins de semana registados nao se compara com o fim de semana`() {
        val soSemana = (0 until 40)
            .filterNot { EatingPatterns.isWeekend(segunda + it) }
            .map { dia(it.toLong(), 2000.0) }
        assertTrue(EatingPatterns.detect(soSemana).none { it.kind == EatingPatterns.Kind.WEEKEND_HIGHER })
    }

    @Test
    fun `mais calorias ao fim de semana e um facto que se diz`() {
        val padroes = EatingPatterns.detect(semanas(kcalSemana = 1800.0, kcalFds = 2600.0))
        val fds = padroes.first { it.kind == EatingPatterns.Kind.WEEKEND_HIGHER }
        assertEquals(800, fds.value)
    }

    @Test
    fun `menos calorias ao fim de semana tambem`() {
        val padroes = EatingPatterns.detect(semanas(kcalSemana = 2600.0, kcalFds = 1800.0))
        assertTrue(padroes.any { it.kind == EatingPatterns.Kind.WEEKEND_LOWER })
    }

    @Test
    fun `uma diferenca pequena nao se menciona`() {

        val padroes = EatingPatterns.detect(semanas(kcalSemana = 2000.0, kcalFds = 2100.0))
        assertTrue(padroes.none { it.kind == EatingPatterns.Kind.WEEKEND_HIGHER })
    }

    @Test
    fun `a queda de proteina ao fim de semana conta`() {
        val padroes = EatingPatterns.detect(
            semanas(2000.0, 2000.0, proteinaSemana = 130.0, proteinaFds = 90.0),
        )
        val queda = padroes.first { it.kind == EatingPatterns.Kind.WEEKEND_PROTEIN_DROP }
        assertEquals(40, queda.value)
    }

    @Test
    fun `a proteina subir ao fim de semana nao e noticia`() {

        val padroes = EatingPatterns.detect(
            semanas(2000.0, 2000.0, proteinaSemana = 90.0, proteinaFds = 130.0),
        )
        assertTrue(padroes.none { it.kind == EatingPatterns.Kind.WEEKEND_PROTEIN_DROP })
    }

    @Test
    fun `um dia concentrado numa refeicao e apontado`() {
        val dias = (0 until 28).map {
            EatingPatterns.Day(
                epochDay = segunda + it,
                kcal = 2000.0,
                proteinG = 100.0,
                kcalBySlot = mapOf("DINNER" to 1200.0, "LUNCH" to 500.0, "BREAKFAST" to 300.0),
            )
        }
        val padrao = EatingPatterns.detect(dias).first { it.kind == EatingPatterns.Kind.MEAL_CONCENTRATION }
        assertEquals("DINNER", padrao.label)
        assertEquals(60, padrao.value)
    }

    @Test
    fun `um dia repartido nao tem concentracao nenhuma`() {
        val dias = (0 until 28).map {
            EatingPatterns.Day(
                epochDay = segunda + it,
                kcal = 2000.0,
                proteinG = 100.0,
                kcalBySlot = mapOf("DINNER" to 700.0, "LUNCH" to 700.0, "BREAKFAST" to 600.0),
            )
        }
        assertTrue(EatingPatterns.detect(dias).none { it.kind == EatingPatterns.Kind.MEAL_CONCENTRATION })
    }

    @Test
    fun `sem refeicoes registadas nao se inventa concentracao`() {
        val dias = (0 until 28).map { dia(it.toLong(), 2000.0) }
        assertTrue(EatingPatterns.detect(dias).none { it.kind == EatingPatterns.Kind.MEAL_CONCENTRATION })
    }

    @Test
    fun `comer igual todos os dias nao produz padrao nenhum`() {

        assertEquals(emptyList(), EatingPatterns.detect(semanas(2000.0, 2000.0)))
    }

    @Test
    fun `nenhum padrao traz texto - so numeros`() {

        val dias = (0 until 28).map {
            EatingPatterns.Day(
                epochDay = segunda + it,
                kcal = if (EatingPatterns.isWeekend(segunda + it)) 2800.0 else 1800.0,
                proteinG = if (EatingPatterns.isWeekend(segunda + it)) 80.0 else 130.0,
                kcalBySlot = mapOf("DINNER" to 1500.0, "LUNCH" to 500.0),
            )
        }
        val padroes = EatingPatterns.detect(dias)
        assertTrue(padroes.isNotEmpty())
        for (p in padroes) {
            assertTrue(p.value > 0)
            p.label?.let { assertTrue(it.none { ch -> ch == ' ' }, "o label virou prosa: $it") }
        }
    }
}
