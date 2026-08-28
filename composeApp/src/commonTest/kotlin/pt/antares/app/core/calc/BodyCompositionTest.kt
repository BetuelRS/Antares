package pt.antares.app.core.calc

import pt.antares.app.core.model.BodyFatSource
import pt.antares.app.core.model.Sex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BodyCompositionTest {

    @Test
    fun `IMC e a conta de sempre`() {

        val bmi = BodyComposition.bmi(80.0, 178)!!
        assertEquals(25.2, (bmi * 10).toInt() / 10.0, 0.05)
        assertEquals(BmiCategory.OVERWEIGHT, BodyComposition.bmiCategory(bmi))
    }

    @Test
    fun `as fronteiras do IMC sao as da OMS`() {
        assertEquals(BmiCategory.UNDERWEIGHT, BodyComposition.bmiCategory(18.4))
        assertEquals(BmiCategory.HEALTHY, BodyComposition.bmiCategory(18.5))
        assertEquals(BmiCategory.HEALTHY, BodyComposition.bmiCategory(24.9))
        assertEquals(BmiCategory.OVERWEIGHT, BodyComposition.bmiCategory(25.0))
        assertEquals(BmiCategory.OBESE, BodyComposition.bmiCategory(30.0))
    }

    @Test
    fun `a faixa saudavel bate com os limites do IMC`() {
        val range = BodyComposition.healthyWeightRange(178)!!

        assertEquals(18.5, BodyComposition.bmi(range.start, 178)!!, 0.01)
        assertEquals(25.0, BodyComposition.bmi(range.endInclusive, 178)!!, 0.01)
    }

    @Test
    fun `altura invalida nao rebenta nem inventa`() {
        assertNull(BodyComposition.bmi(80.0, 0))
        assertNull(BodyComposition.healthyWeightRange(0))
        assertNull(BodyComposition.bmi(0.0, 178))
    }

    @Test
    fun `US Navy da um valor plausivel para um homem`() {

        val pct = BodyComposition.navyBodyFat(Sex.MALE, 178, waistCm = 84.0, neckCm = 38.0)!!
        assertTrue(pct in 12.0..22.0, "esperava 12-22%, deu $pct")
    }

    @Test
    fun `US Navy da um valor plausivel para uma mulher`() {
        val pct = BodyComposition.navyBodyFat(
            Sex.FEMALE, 165, waistCm = 74.0, neckCm = 32.0, hipCm = 96.0,
        )!!
        assertTrue(pct in 20.0..35.0, "esperava 20-35%, deu $pct")
    }

    /**
     * Os valores saem **já corrigidos do viés por sexo**.
     *
     * A fórmula crua dava 15,7 no homem e 27,4 na mulher. Potter et al. (2022), contra
     * DEXA, mostram que ela subestima a gordura dos homens em 2,6 pontos e sobrestima a das
     * mulheres em 2,3 — os números fixados aqui são os de depois da correcção, que são os
     * que a app mostra.
     */
    @Test
    fun `os valores da formula US Navy estao fixados`() {

        assertEquals(
            18.3,
            BodyComposition.navyBodyFat(Sex.MALE, 178, waistCm = 84.0, neckCm = 38.0)!!,
            0.2,
        )
        assertEquals(
            25.1,
            BodyComposition.navyBodyFat(Sex.FEMALE, 165, 74.0, 32.0, hipCm = 96.0)!!,
            0.2,
        )
    }

    /**
     * O viés corrige-se em direções opostas, e é isso que o distingue de uma margem.
     *
     * Um erro aleatório anula-se ao longo do tempo; um viés não se anula nunca. Se alguém
     * um dia "simplificar" isto para um só valor, ou lhe trocar o sinal, este teste cai.
     */
    @Test
    fun `o vies sobe nos homens e desce nas mulheres`() {
        val homem = BodyComposition.navyBodyFat(Sex.MALE, 178, waistCm = 84.0, neckCm = 38.0)!!
        val mulher = BodyComposition.navyBodyFat(Sex.FEMALE, 165, 74.0, 32.0, hipCm = 96.0)!!

        assertEquals(2.6, homem - 15.7, 0.2)
        assertEquals(-2.3, mulher - 27.4, 0.2)
    }

    @Test
    fun `cintura menor que o pescoco e recusada em vez de dar disparate`() {

        assertNull(BodyComposition.navyBodyFat(Sex.MALE, 178, waistCm = 30.0, neckCm = 38.0))
    }

    @Test
    fun `mulher sem anca nao tem estimativa Navy`() {

        assertNull(BodyComposition.navyBodyFat(Sex.FEMALE, 165, waistCm = 74.0, neckCm = 32.0))
    }

    @Test
    fun `Deurenberg cresce com o IMC e com a idade`() {
        val young = BodyComposition.deurenbergBodyFat(Sex.MALE, bmi = 25.0, ageYears = 25)!!
        val older = BodyComposition.deurenbergBodyFat(Sex.MALE, bmi = 25.0, ageYears = 55)!!
        val fatter = BodyComposition.deurenbergBodyFat(Sex.MALE, bmi = 32.0, ageYears = 25)!!
        assertTrue(older > young, "a mesma pessoa mais velha tem mais gordura estimada")
        assertTrue(fatter > young, "IMC mais alto → mais gordura estimada")
    }

    @Test
    fun `mulheres tem mais gordura estimada que homens no mesmo IMC`() {

        val m = BodyComposition.deurenbergBodyFat(Sex.MALE, 25.0, 30)!!
        val f = BodyComposition.deurenbergBodyFat(Sex.FEMALE, 25.0, 30)!!
        assertTrue(f > m)
    }

    @Test
    fun `massa magra e o peso menos a gordura`() {
        assertEquals(64.0, BodyComposition.leanMassKg(80.0, 20.0)!!, 0.001)
    }

    @Test
    fun `percentagem impossivel de gordura e recusada`() {
        assertNull(BodyComposition.leanMassKg(80.0, 0.0))
        assertNull(BodyComposition.leanMassKg(80.0, 95.0))
    }

    @Test
    fun `cintura abaixo de metade da altura e saudavel`() {
        assertEquals(WaistRisk.HEALTHY, BodyComposition.waistRisk(0.47))
        assertEquals(WaistRisk.INCREASED, BodyComposition.waistRisk(0.55))
        assertEquals(WaistRisk.HIGH, BodyComposition.waistRisk(0.62))
    }

    @Test
    fun `o valor medido tem prioridade sobre qualquer estimativa`() {
        val s = BodyComposition.stats(
            sex = Sex.MALE, weightKg = 80.0, heightCm = 178, ageYears = 30,
            bodyFatPct = 12.0, bodyFatSource = BodyFatSource.MEASURED,
            waistCm = 84.0, neckCm = 38.0,
        )
        assertEquals(12.0, s.bodyFatPct)
        assertEquals(BodyFatSource.MEASURED, s.bodyFatSource)
    }

    @Test
    fun `sem valor medido usa as medidas antes do IMC`() {
        val s = BodyComposition.stats(
            sex = Sex.MALE, weightKg = 80.0, heightCm = 178, ageYears = 30,
            waistCm = 84.0, neckCm = 38.0,
        )
        assertEquals(BodyFatSource.NAVY, s.bodyFatSource)
        assertTrue(s.leanMassKg!! < 80.0)
        assertEquals(80.0, s.leanMassKg!! + s.fatMassKg!!, 0.001)
    }

    @Test
    fun `sem medidas cai no IMC e diz que foi por IMC`() {
        val s = BodyComposition.stats(Sex.MALE, 80.0, 178, 30)
        assertEquals(BodyFatSource.BMI, s.bodyFatSource)
        assertTrue(s.bodyFatPct != null)
    }

    @Test
    fun `sem cintura nao ha razao cintura-altura`() {
        val s = BodyComposition.stats(Sex.MALE, 80.0, 178, 30)
        assertNull(s.waistToHeight)
        assertNull(s.waistRisk)
    }
}
