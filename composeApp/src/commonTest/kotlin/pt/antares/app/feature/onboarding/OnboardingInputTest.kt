package pt.antares.app.feature.onboarding

import pt.antares.app.core.model.UnitSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OnboardingInputTest {

    private val metrico = UnitSystem.METRIC
    private val imperial = UnitSystem.IMPERIAL

    @Test
    fun `virgula e ponto sao a mesma coisa`() {
        assertEquals(78.5, OnboardingInput.parseDecimal("78,5"))
        assertEquals(78.5, OnboardingInput.parseDecimal("78.5"))
    }

    @Test
    fun `texto que nao e numero nao vira zero`() {

        assertNull(OnboardingInput.parseDecimal(""))
        assertNull(OnboardingInput.parseDecimal("   "))
        assertNull(OnboardingInput.parseDecimal("setenta"))
    }

    @Test
    fun `setenta libras nao e setenta quilos`() {
        val emQuilos = OnboardingInput.weightKg(metrico, "150")!!
        val emLibras = OnboardingInput.weightKg(imperial, "150")!!
        assertEquals(150.0, emQuilos)
        assertTrue(emLibras in 67.9..68.1, "150 lb deviam ser ≈68 kg, deram $emLibras")
    }

    @Test
    fun `pesos absurdos nao passam, em qualquer unidade`() {
        assertNull(OnboardingInput.weightKg(metrico, "12"))
        assertNull(OnboardingInput.weightKg(metrico, "500"))

        assertNull(OnboardingInput.weightKg(imperial, "50"))
        assertNull(OnboardingInput.weightKg(imperial, "900"))
    }

    @Test
    fun `um peso que so e valido em libras passa em libras e falha em quilos`() {

        assertNull(OnboardingInput.weightKg(metrico, "320"))
        assertTrue(OnboardingInput.weightKg(imperial, "320") != null)
    }

    @Test
    fun `altura em cm`() {
        assertEquals(178, OnboardingInput.heightCm(metrico, "178", "", ""))
        assertNull(OnboardingInput.heightCm(metrico, "60", "", ""))
        assertNull(OnboardingInput.heightCm(metrico, "300", "", ""))
        assertNull(OnboardingInput.heightCm(metrico, "", "", ""))
    }

    @Test
    fun `altura em pes e polegadas`() {

        assertEquals(178, OnboardingInput.heightCm(imperial, "", "5", "10"))

        assertEquals(183, OnboardingInput.heightCm(imperial, "", "6", ""))
    }

    @Test
    fun `treze polegadas nao e uma altura`() {

        assertNull(OnboardingInput.heightCm(imperial, "", "5", "13"))
    }

    @Test
    fun `alturas impossiveis em imperial tambem sao recusadas`() {
        assertNull(OnboardingInput.heightCm(imperial, "", "2", "0"))
        assertNull(OnboardingInput.heightCm(imperial, "", "9", "0"))
        assertNull(OnboardingInput.heightCm(imperial, "", "", ""))
    }

    @Test
    fun `o campo de cm e ignorado em imperial e vice-versa`() {

        assertEquals(178, OnboardingInput.heightCm(imperial, "999", "5", "10"))
        assertEquals(178, OnboardingInput.heightCm(metrico, "178", "9", "9"))
    }

    @Test
    fun `o peso-alvo em branco e uma resposta valida`() {
        assertNull(OnboardingInput.goalWeightKg(metrico, ""))
        assertTrue(OnboardingInput.goalWeightAcceptable(metrico, ""))
        assertTrue(OnboardingInput.goalWeightAcceptable(metrico, "   "))
    }

    @Test
    fun `um peso-alvo escrito mal nao passa em silencio`() {

        assertFalse(OnboardingInput.goalWeightAcceptable(metrico, "7"))
        assertFalse(OnboardingInput.goalWeightAcceptable(metrico, "abc"))
        assertTrue(OnboardingInput.goalWeightAcceptable(metrico, "72,5"))
    }

    @Test
    fun `o peso-alvo respeita as unidades`() {
        assertEquals(72.0, OnboardingInput.goalWeightKg(metrico, "72"))
        val emLibras = OnboardingInput.goalWeightKg(imperial, "160")!!
        assertTrue(emLibras in 72.5..72.6, "160 lb deviam ser ≈72,6 kg, deram $emLibras")
    }

    @Test
    fun `querer perder e escrever um alvo mais alto e contraditorio`() {
        assertTrue(OnboardingInput.goalContradictsDirection(losing = true, currentKg = 90.0, goalKg = 95.0))
        assertFalse(OnboardingInput.goalContradictsDirection(losing = true, currentKg = 90.0, goalKg = 80.0))
    }

    @Test
    fun `querer ganhar e escrever um alvo mais baixo e contraditorio`() {
        assertTrue(OnboardingInput.goalContradictsDirection(losing = false, currentKg = 60.0, goalKg = 55.0))
        assertFalse(OnboardingInput.goalContradictsDirection(losing = false, currentKg = 60.0, goalKg = 70.0))
    }

    @Test
    fun `arredondamento nao e contradicao`() {

        assertFalse(OnboardingInput.goalContradictsDirection(losing = true, currentKg = 80.0, goalKg = 80.1))
    }

    @Test
    fun `sem numeros nao ha contradicao a apontar`() {
        assertFalse(OnboardingInput.goalContradictsDirection(losing = true, currentKg = null, goalKg = 80.0))
        assertFalse(OnboardingInput.goalContradictsDirection(losing = true, currentKg = 80.0, goalKg = null))
    }
}
