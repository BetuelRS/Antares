package pt.antares.app.feature.running

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import pt.antares.app.feature.running.domain.ActivityType
import pt.antares.app.feature.running.domain.AvisoDaCorrida
import pt.antares.app.feature.running.domain.GeoSample
import pt.antares.app.feature.running.domain.RunEngine
import pt.antares.app.feature.running.domain.RunPrCalc
import pt.antares.app.feature.running.domain.Split

/**
 * As voltas marcadas à mão, e o que a voz tem para dizer.
 *
 * O que estes testes defendem é sobretudo **uma coisa que uma volta podia partir sem dar
 * erro**: os recordes. As voltas vivem na mesma lista dos quilómetros, e o `RunPrCalc` só
 * conta parciais com pelo menos 999 m — se uma volta mexesse na âncora dos quilómetros, o
 * parcial seguinte media 600 m e a corrida ficava sem recorde nenhum, calada.
 */
class VoltasEAvisosTest {

    /** Um metro de latitude são cerca de 1/111 320 de grau. Andar em linha recta para norte. */
    private fun ponto(metros: Double, segundos: Long) = GeoSample(
        lat = 38.7 + metros / 111_320.0,
        lon = -9.14,
        altM = 10.0,
        accM = 5.0,
        tMs = segundos * 1000L,
    )

    private fun motor() = RunEngine(ActivityType.RUN, weightKg = 70.0)

    /** Anda `metros` em passos de 20 m, um a cada 6 s — que dá um ritmo de 5:00/km. */
    private fun andar(e: RunEngine, ateMetros: Double, deMetros: Double = 0.0, deSegundos: Long = 0L): Long {
        var m = deMetros
        var s = deSegundos
        while (m < ateMetros) {
            m += 20.0
            s += 6L
            e.onSample(ponto(m, s))
        }
        return s
    }

    @Test
    fun `uma volta marcada nao mexe nos quilometros nem nos recordes`() {
        val e = motor()
        e.onSample(ponto(0.0, 0))
        var s = andar(e, 1400.0)
        e.volta()
        andar(e, 2100.0, deMetros = 1400.0, deSegundos = s)
        val r = e.finish()

        val quilometros = r.splits.filter { !it.manual }
        assertTrue(quilometros.size >= 2, "quilómetros=${quilometros.map { it.distanceM }}")

        // O segundo quilómetro continua a medir mil metros: a volta aos 1 400 m fechou-se
        // com âncoras próprias e não tocou nas dos quilómetros.
        assertTrue(
            quilometros[1].distanceM in 990.0..1010.0,
            "o segundo quilómetro mede ${quilometros[1].distanceM} — a volta mexeu na âncora",
        )

        // E é isto que a volta punha em risco: o recorde de 2 km deixa de existir se os
        // quilómetros deixarem de ser quilómetros.
        assertNotNull(
            RunPrCalc.timeForKm(r.splits, 2),
            "a corrida ficou sem recorde de 2 km por ter uma volta marcada",
        )
    }

    @Test
    fun `um recorde nunca sai de uma volta marcada`() {
        // Uma volta de 1 200 m tem mais de 999 e entraria na conta como se fosse um
        // quilómetro: o tempo saía por uma distância que ninguém correu.
        val voltaLonga = Split(
            index = 1,
            distanceM = 1200.0,
            movingMs = 100_000L,
            paceSecPerKm = 83,
            kcal = 80,
            manual = true,
        )
        assertNull(RunPrCalc.timeForKm(listOf(voltaLonga), 1))
    }

    @Test
    fun `as duas series contam-se em separado`() {
        val e = motor()
        e.onSample(ponto(0.0, 0))
        val s = andar(e, 1400.0)
        e.volta()
        andar(e, 2100.0, deMetros = 1400.0, deSegundos = s)
        val r = e.finish()

        val kms = r.splits.filter { !it.manual }.map { it.index }
        val voltas = r.splits.filter { it.manual }.map { it.index }

        // O quilómetro 2 é o segundo quilómetro, e não o terceiro elemento da lista.
        assertEquals(listOf(1, 2), kms.take(2), "os quilómetros ficaram renumerados pela volta")
        assertEquals(1, voltas.first(), "a primeira volta não é a volta 1")
    }

    @Test
    fun `em pausa nao se marca volta`() {
        val e = motor()
        e.onSample(ponto(0.0, 0))
        andar(e, 200.0)
        e.pausar()
        assertNull(e.volta(), "marcou uma volta com a corrida em pausa")
    }

    @Test
    fun `dois toques seguidos nao dao uma volta de zero metros`() {
        val e = motor()
        e.onSample(ponto(0.0, 0))
        andar(e, 200.0)
        assertNotNull(e.volta())
        assertNull(e.volta(), "a segunda volta nasceu sem um metro andado desde a primeira")
    }

    /**
     * O caso que só apareceu a correr a app: **termina-se uma corrida em pausa**, e é a
     * única maneira de a terminar desde a 2.29.0. Com a guarda da pausa dentro do fecho, a
     * volta que ia a meio nunca se fechava — o último troço de um treino por séries
     * desaparecia, e nenhum dos testes o via porque nenhum deles pausava antes de acabar.
     */
    @Test
    fun `a volta aberta fecha-se mesmo com a corrida em pausa`() {
        val e = motor()
        e.onSample(ponto(0.0, 0))
        val s = andar(e, 200.0)
        e.volta()
        andar(e, 400.0, deMetros = 200.0, deSegundos = s)
        e.pausar()

        assertEquals(
            2,
            e.finish().splits.count { it.manual },
            "terminar em pausa deitou fora a volta que ia a meio",
        )
    }

    @Test
    fun `a volta aberta fecha-se na meta, e so quem marcou voltas ganha uma`() {
        val comVolta = motor()
        comVolta.onSample(ponto(0.0, 0))
        val s = andar(comVolta, 200.0)
        comVolta.volta()
        andar(comVolta, 400.0, deMetros = 200.0, deSegundos = s)
        assertEquals(
            2,
            comVolta.finish().splits.count { it.manual },
            "a volta que ia a meio não se fechou na meta",
        )

        val semVolta = motor()
        semVolta.onSample(ponto(0.0, 0))
        andar(semVolta, 400.0)
        assertTrue(
            semVolta.finish().splits.none { it.manual },
            "quem nunca tocou no botão ganhou uma volta do princípio ao fim",
        )
    }

    @Test
    fun `a voz anuncia o ultimo parcial e salta os do meio`() {
        val parciais = listOf(
            Split(1, 1000.0, 300_000L, 300, 70),
            Split(2, 1000.0, 300_000L, 300, 70),
        )
        // Dois fecharam-se de uma vez — um túnel — e quem já vai no segundo não quer ouvir
        // o primeiro.
        assertEquals(2, AvisoDaCorrida.porAnunciar(parciais, jaAnunciados = 0)?.index)
        assertNull(AvisoDaCorrida.porAnunciar(parciais, jaAnunciados = 2))
    }

    @Test
    fun `o ritmo falado parte-se em minutos e segundos`() {
        val r = AvisoDaCorrida.ritmo(342)
        assertEquals(5, r?.minutos)
        assertEquals(42, r?.segundos, "«5 e 42» é como se diz um ritmo em voz alta")
    }

    @Test
    fun `sem ritmo nao ha o que dizer`() {
        // O motor devolve zero enquanto não houver um metro andado, e o ecrã lê isso como
        // «ainda não». A voz tem de ler o mesmo, em vez de anunciar 0:00.
        assertNull(AvisoDaCorrida.ritmo(0))
    }

    @Test
    fun `o tempo falado parte-se em horas e minutos, sem segundos`() {
        assertEquals(AvisoDaCorrida.Tempo(horas = 0, minutos = 17), AvisoDaCorrida.tempo(17 * 60_000L + 42_000L))
        assertEquals(AvisoDaCorrida.Tempo(horas = 1, minutos = 5), AvisoDaCorrida.tempo(65 * 60_000L))
    }
}
