package pt.antares.app.core.nutrition

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DailyGapTest {

    private fun totais(
        byKey: Map<String, Double>,
        medidoPorChave: Map<String, Double>,
        totalKcal: Double = 2000.0,
    ) = MicroTotals(
        byKey = byKey,
        measuredKcalByKey = medidoPorChave,
        totalKcal = totalKcal,
        measuredAnyKcal = totalKcal,
    )

    private val referencia: (String) -> Double? = { chave ->
        when (chave) {
            Nutrients.IRON -> 14.0
            Nutrients.VIT_C -> 80.0
            else -> null
        }
    }

    @Test
    fun `aponta o nutriente que esta mais longe da referencia`() {
        val gap = DailyGap.worst(
            totals = totais(
                byKey = mapOf(Nutrients.IRON to 2.0, Nutrients.VIT_C to 30.0),
                medidoPorChave = mapOf(Nutrients.IRON to 2000.0, Nutrients.VIT_C to 2000.0),
            ),
            referenceFor = referencia,
        )

        assertEquals(Nutrients.IRON, gap?.key)
        assertEquals(12.0, gap?.missing)
    }

    @Test
    fun `nao chama falta ao que nao foi medido`() {

        val gap = DailyGap.worst(
            totals = totais(
                byKey = mapOf(Nutrients.IRON to 1.0),
                medidoPorChave = mapOf(Nutrients.IRON to 200.0),
            ),
            referenceFor = referencia,
        )
        assertNull(gap)
    }

    @Test
    fun `um dia bem coberto nao inventa uma lacuna`() {
        val gap = DailyGap.worst(
            totals = totais(
                byKey = mapOf(Nutrients.IRON to 13.0, Nutrients.VIT_C to 79.0),
                medidoPorChave = mapOf(Nutrients.IRON to 2000.0, Nutrients.VIT_C to 2000.0),
            ),
            referenceFor = referencia,
        )
        assertNull(gap)
    }

    @Test
    fun `um dia sem nada registado nao diz nada`() {

        assertNull(
            DailyGap.worst(
                totals = totais(byKey = emptyMap(), medidoPorChave = emptyMap(), totalKcal = 0.0),
                referenceFor = referencia,
            ),
        )
    }

    @Test
    fun `o sodio nunca e uma lacuna`() {

        val gap = DailyGap.worst(
            totals = totais(
                byKey = mapOf(Nutrients.SODIUM to 100.0),
                medidoPorChave = mapOf(Nutrients.SODIUM to 2000.0),
            ),
            referenceFor = { if (it == Nutrients.SODIUM) 2000.0 else null },
        )
        assertNull(gap)
    }

    @Test
    fun `a fracao e a falta nunca ficam negativas`() {
        val cheio = DailyGap(Nutrients.IRON, consumed = 20.0, reference = 14.0)
        assertEquals(0.0, cheio.missing)
        assertEquals(1.0, cheio.fraction)
    }
}
