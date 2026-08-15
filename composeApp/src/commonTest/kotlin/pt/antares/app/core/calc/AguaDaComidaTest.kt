package pt.antares.app.core.calc

import pt.antares.app.core.nutrition.MicroTotals
import pt.antares.app.core.nutrition.Nutrients
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AguaDaComidaTest {

    private fun totais(agua: Double, medidoKcal: Double, totalKcal: Double) = MicroTotals(
        byKey = mapOf(Nutrients.WATER to agua),
        measuredKcalByKey = mapOf(Nutrients.WATER to medidoKcal),
        totalKcal = totalKcal,
        measuredAnyKcal = medidoKcal,
    )

    @Test
    fun umGramaDeAguaEUmMililitro() {
        assertEquals(740, AguaDaComida.mlDoDia(totais(agua = 739.6, medidoKcal = 2000.0, totalKcal = 2000.0)))
    }

    @Test
    fun semCoberturaSuficienteNaoDaNumero() {
        // Um terço do prato com teor de água medido. O total existiria, mas falaria de um
        // terço do dia a fingir que fala do dia.
        assertNull(AguaDaComida.mlDoDia(totais(agua = 500.0, medidoKcal = 700.0, totalKcal = 2100.0)))
    }

    @Test
    fun mesmoNoLimiarDaCoberturaAindaConta() {
        val ml = AguaDaComida.mlDoDia(totais(agua = 300.0, medidoKcal = 1000.0, totalKcal = 2000.0))
        assertEquals(300, ml, "metade do prato medido é o mínimo, e o mínimo passa")
    }

    @Test
    fun diaSemComidaNaoDaNumero() {
        assertNull(AguaDaComida.mlDoDia(totais(agua = 0.0, medidoKcal = 0.0, totalKcal = 0.0)))
    }

    @Test
    fun comidaSemAguaDeclaradaNaoDaNumero() {
        val semAgua = MicroTotals(
            byKey = mapOf(Nutrients.IRON to 8.0),
            measuredKcalByKey = mapOf(Nutrients.IRON to 2000.0),
            totalKcal = 2000.0,
            measuredAnyKcal = 2000.0,
        )
        assertNull(
            AguaDaComida.mlDoDia(semAgua),
            "nenhum alimento do dia declarou água: zero mililitros seria uma afirmação, e " +
                "o que há é ausência de medição",
        )
    }
}
