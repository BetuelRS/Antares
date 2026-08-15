package pt.antares.app.core

import pt.antares.app.core.demo.DEMO_ID_PREFIX
import pt.antares.app.core.demo.DemoData
import pt.antares.app.core.demo.DemoDataEngine
import pt.antares.app.core.demo.DemoFood
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DemoDataEngineTest {

    private val catalogo = listOf(
        DemoFood("f-aveia", "Aveia", 389, 16.9, 66.3, 6.9),
        DemoFood("f-frango", "Peito de frango", 165, 31.0, 0.0, 3.6),
        DemoFood("f-arroz", "Arroz cozido", 130, 2.7, 28.2, 0.3),
        DemoFood("f-azeite", "Azeite", 884, 0.0, 0.0, 100.0),
        DemoFood("f-leite", "Leite meio-gordo", 47, 3.3, 4.8, 1.6, liquido = true),
    )
    private val exercicios = listOf("ex-supino", "ex-agachamento", "ex-remada", "ex-peso-morto")

    private fun gerar(semente: Long = DemoDataEngine.SEMENTE_PADRAO): DemoData =
        DemoDataEngine.gerar(
            semente = semente,
            diaFinal = 20_666L,
            catalogo = catalogo,
            exercicios = exercicios,
            protocoloJejumId = "prot-16-8",
        )

    @Test
    fun `a mesma semente da exatamente a mesma base`() {

        val a = gerar()
        val b = gerar()

        assertEquals(a.pesos, b.pesos)
        assertEquals(a.refeicoes, b.refeicoes)
        assertEquals(a.treinos, b.treinos)
        assertEquals(a.series, b.series)
        assertEquals(a.corridas, b.corridas)
        assertEquals(a.jejuns, b.jejuns)
        assertEquals(a.medidas, b.medidas)
        assertEquals(a.aguas, b.aguas)
    }

    @Test
    fun `sementes diferentes dao bases diferentes`() {

        assertTrue(
            gerar().refeicoes != gerar(semente = 7L).refeicoes,
            "mudar a semente não mudou nada — o parâmetro não está a ser usado",
        )
    }

    @Test
    fun `os updatedAt nao vem do relogio`() {

        val pesos = gerar().pesos
        assertTrue(pesos.all { it.updatedAt == it.epochDay * 86_400_000L })
    }

    @Test
    fun `a curva do peso comeca aos 110 e acaba aos 70`() {
        val pesos = gerar().pesos.sortedBy { it.epochDay }
        assertTrue(pesos.size > 400, "dois anos com ~72 % de adesão dão bem mais do que isto")

        assertTrue(
            pesos.first().weightKg in 108.0..111.5,
            "começa em ${pesos.first().weightKg} kg e devia começar perto dos 110",
        )
        assertTrue(
            pesos.last().weightKg in 68.5..72.0,
            "acaba em ${pesos.last().weightKg} kg e devia acabar perto dos 70",
        )
    }

    @Test
    fun `a curva tem recaidas, nao e uma reta a descer`() {

        val porMes = gerar().pesos
            .groupBy { it.epochDay / 30 }
            .toSortedMap()
            .map { (_, dias) -> dias.map { it.weightKg }.average() }

        val subidas = porMes.zipWithNext().count { (antes, depois) -> depois > antes + 0.3 }
        assertTrue(
            subidas >= 2,
            "só $subidas meses acabaram acima do anterior — a curva não tem recaídas nenhumas",
        )

        assertTrue(porMes.first() - porMes.last() > 35, "os dois anos têm de contar uma perda de ~40 kg")
    }

    @Test
    fun `as calorias contam a mesma historia que o peso`() {

        val dados = gerar()
        val kcalPorMes = dados.refeicoes
            .groupBy { it.epochDay / 30 }
            .mapValues { (_, logs) ->
                logs.groupBy { it.epochDay }
                    .map { (_, dia) -> dia.sumOf { it.kcalSnapshot } }
                    .average()
            }
        val pesoPorMes = dados.pesos
            .groupBy { it.epochDay / 30 }
            .mapValues { (_, p) -> p.map { it.weightKg }.average() }

        val variacoes = pesoPorMes.toSortedMap().toList().zipWithNext()
            .map { (antes, depois) -> depois.first to (depois.second - antes.second) }
        val mesQueMaisSubiu = variacoes.maxBy { it.second }.first
        val mesQueMaisDesceu = variacoes.minBy { it.second }.first

        assertTrue(
            kcalPorMes.getValue(mesQueMaisSubiu) > kcalPorMes.getValue(mesQueMaisDesceu),
            "no mês em que o peso mais subiu comeu-se menos do que no mês em que mais desceu",
        )
    }

    @Test
    fun `ha dias sem registo nenhum`() {

        val dados = gerar()
        val diasComComida = dados.refeicoes.map { it.epochDay }.toSet()
        assertTrue(
            diasComComida.size < DemoDataEngine.DIAS,
            "todos os dias têm registo — ninguém regista dois anos seguidos sem falhar",
        )
        assertTrue(diasComComida.size > DemoDataEngine.DIAS * 0.7, "faltam demasiados dias")
    }

    @Test
    fun `a forca sobe enquanto o peso desce`() {

        val series = gerar().series.sortedBy { it.updatedAt }
        assertTrue(series.isNotEmpty())
        val primeiroAno = series.take(series.size / 2).map { it.weightKg }.average()
        val segundoAno = series.drop(series.size / 2).map { it.weightKg }.average()
        assertTrue(
            segundoAno > primeiroAno,
            "as cargas não subiram: $primeiroAno kg no primeiro ano, $segundoAno no segundo",
        )
    }

    @Test
    fun `as corridas ficam mais rapidas e mais longas`() {
        val corridas = gerar().corridas.sortedBy { it.startedAt }
        assertTrue(corridas.size > 100)
        val cedo = corridas.take(20)
        val tarde = corridas.takeLast(20)
        assertTrue(tarde.map { it.distanceM }.average() > cedo.map { it.distanceM }.average())
        assertTrue(
            tarde.map { it.avgPaceSecPerKm }.average() < cedo.map { it.avgPaceSecPerKm }.average(),
            "correr com menos 35 kg tem de dar um ritmo melhor",
        )
    }

    @Test
    fun `as corridas nao trazem percurso inventado`() {

        assertTrue(gerar().corridas.all { it.polyline.isEmpty() })
    }

    @Test
    fun `tudo o que sai tem id demo`() {

        val d = gerar()
        val ids = d.pesos.map { it.id } + d.medidas.map { it.id } + d.refeicoes.map { it.id } +
            d.aguas.map { it.id } + d.treinos.map { it.id } + d.series.map { it.id } +
            d.corridas.map { it.id } + d.jejuns.map { it.id }

        assertEquals(
            emptyList(),
            ids.filterNot { it.startsWith(DEMO_ID_PREFIX) },
            "ids sem o prefixo `demo-` ficam na base para sempre quando se desligar",
        )
    }

    @Test
    fun `sem catalogo nao inventa alimentos`() {

        val semNada = DemoDataEngine.gerar(diaFinal = 20_666L)
        assertTrue(semNada.refeicoes.isEmpty())
        assertTrue(semNada.series.isEmpty(), "sem biblioteca de exercícios não há séries")
        assertTrue(semNada.jejuns.isEmpty(), "sem protocolo não há jejuns")
        assertTrue(semNada.pesos.isNotEmpty(), "o peso não depende de catálogo nenhum")
        assertTrue(semNada.corridas.isNotEmpty())
    }

    @Test
    fun `os ids nao colidem entre si`() {

        val d = gerar()
        fun unico(nome: String, ids: List<String>) =
            assertEquals(ids.size, ids.toSet().size, "ids repetidos em $nome")

        unico("pesos", d.pesos.map { it.id })
        unico("medidas", d.medidas.map { it.id })
        unico("refeições", d.refeicoes.map { it.id })
        unico("águas", d.aguas.map { it.id })
        unico("treinos", d.treinos.map { it.id })
        unico("séries", d.series.map { it.id })
        unico("corridas", d.corridas.map { it.id })
        unico("jejuns", d.jejuns.map { it.id })

        assertEquals(d.pesos.size, d.pesos.map { it.epochDay }.toSet().size, "dois pesos no mesmo dia")
        assertEquals(d.aguas.size, d.aguas.map { it.epochDay }.toSet().size, "duas águas no mesmo dia")
        assertEquals(d.medidas.size, d.medidas.map { it.epochDay }.toSet().size, "duas medidas no mesmo dia")
    }
}
