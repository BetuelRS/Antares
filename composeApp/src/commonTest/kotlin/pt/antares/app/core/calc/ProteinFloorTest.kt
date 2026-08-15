package pt.antares.app.core.calc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProteinFloorTest {

    @Test
    fun `quem nao treina forca fica onde sempre esteve`() {
        for (fracao in listOf(0.0, 0.10, 0.20, 0.35, 0.60)) {
            assertEquals(
                ProteinFloor.UNTRAINED_DEFICIT,
                ProteinFloor.perKgLean(treinaForca = false, deficitFraction = fracao),
                "o intervalo de Helms é sobre quem treina; estendê-lo a quem não treina " +
                    "era aplicar um estudo fora do que ele estudou",
            )
        }
    }

    @Test
    fun `treinar forca em defice leve sobe para o fundo do intervalo de Helms`() {
        assertEquals(2.3, ProteinFloor.perKgLean(treinaForca = true, deficitFraction = 0.05))
        assertEquals(2.3, ProteinFloor.perKgLean(treinaForca = true, deficitFraction = 0.10))
    }

    @Test
    fun `defice profundo leva ao topo que a app usa`() {
        assertEquals(2.8, ProteinFloor.perKgLean(treinaForca = true, deficitFraction = 0.25))
        assertEquals(
            2.8,
            ProteinFloor.perKgLean(treinaForca = true, deficitFraction = 0.50),
            "acima de um quarto do gasto não sobe mais: Helms vai até 3,1, mas isso é " +
                "preparação de competição com prazo e acompanhamento",
        )
    }

    @Test
    fun `entre os dois interpola em vez de saltar`() {
        val meio = ProteinFloor.perKgLean(treinaForca = true, deficitFraction = 0.175)
        assertEquals(2.55, meio, absoluteTolerance = 0.001)

        // O que isto protege: mexer um bocadinho no ritmo não pode mudar o alvo de proteína
        // num degrau que se vê no ecrã.
        val antes = ProteinFloor.perKgLean(treinaForca = true, deficitFraction = 0.199)
        val depois = ProteinFloor.perKgLean(treinaForca = true, deficitFraction = 0.201)
        assertTrue(depois - antes < 0.01, "a subida tem de ser contínua")
    }

    @Test
    fun `sem defice a fracao e zero`() {
        assertEquals(0.0, ProteinFloor.deficitFraction(rateKcal = 0, tdee = 2400.0))
        assertEquals(
            0.0,
            ProteinFloor.deficitFraction(rateKcal = 300, tdee = 2400.0),
            "quem está em superavit não tem défice nenhum",
        )
    }

    @Test
    fun `a fracao mede-se contra o gasto e nao em kcal absolutas`() {
        // As mesmas 600 kcal são um terço do dia de quem gasta 1800 e um quinto do de 3000.
        assertEquals(0.333, ProteinFloor.deficitFraction(-600, 1800.0), absoluteTolerance = 0.001)
        assertEquals(0.200, ProteinFloor.deficitFraction(-600, 3000.0), absoluteTolerance = 0.001)
    }

    @Test
    fun `sem gasto conhecido cai no lado seguro`() {
        assertEquals(
            ProteinFloor.TRAINED_LIGHT_DEFICIT,
            ProteinFloor.perKgLean(true, ProteinFloor.deficitFraction(-600, tdee = 0.0)),
            "sem gasto não se sabe a profundidade, e pedir mais proteína do que se sabe " +
                "justificar é o erro pior",
        )
    }
}
