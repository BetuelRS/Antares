package pt.antares.app.core.nutrition

import pt.antares.app.core.model.LifeStage
import pt.antares.app.core.model.Sex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LifeStageDrvTest {

    private val adultoFeminino = Drv("iron_mg", male = 11.0, female = 16.0, unit = "mg")
    private val folato = Drv("vitB9_ug", male = 330.0, female = 330.0, unit = "µg")

    @Test
    fun `todo ajuste tem de dizer de onde veio`() {
        for (fase in LifeStage.entries) {
            for (ajuste in LifeStageDrv.adjustments(fase)) {
                assertTrue(
                    ajuste.source.contains("EFSA"),
                    "${ajuste.key} em $fase não cita a fonte — não pode entrar",
                )
            }
        }
    }

    @Test
    fun `nenhum ajuste e zero ou negativo`() {
        for (fase in LifeStage.entries) {
            for (ajuste in LifeStageDrv.adjustments(fase)) {
                assertTrue(ajuste.value > 0, "${ajuste.key} em $fase vale ${ajuste.value}")
            }
        }
    }

    @Test
    fun `sem fase declarada valem as referencias de adulto`() {
        assertEquals(emptyList(), LifeStageDrv.adjustments(null))
        assertEquals(emptyList(), LifeStageDrv.adjustments(LifeStage.NONE))
        assertEquals(16.0, adultoFeminino.forPerson(Sex.FEMALE, null))
        assertEquals(16.0, adultoFeminino.forPerson(Sex.FEMALE, LifeStage.NONE))
    }

    @Test
    fun `uma chave que a fase nao muda mantem o valor de adulto`() {

        val calcio = Drv("calcium_mg", 950.0, 950.0, "mg")
        assertEquals(950.0, calcio.forPerson(Sex.FEMALE, LifeStage.PREGNANCY))
        assertNull(LifeStageDrv.sourceFor("calcium_mg", LifeStage.PREGNANCY))
    }

    @Test
    fun `na gravidez o folato sobe para 600`() {

        assertEquals(600.0, folato.forPerson(Sex.FEMALE, LifeStage.PREGNANCY))
    }

    @Test
    fun `a amamentar o folato fica em 500`() {

        assertEquals(500.0, folato.forPerson(Sex.FEMALE, LifeStage.LACTATION))
    }

    @Test
    fun `o iodo sobe para 200 nas duas fases`() {
        val iodo = Drv("iodine_ug", 150.0, 150.0, "µg")
        assertEquals(200.0, iodo.forPerson(Sex.FEMALE, LifeStage.PREGNANCY))
        assertEquals(200.0, iodo.forPerson(Sex.FEMALE, LifeStage.LACTATION))
    }

    @Test
    fun `depois da menopausa o ferro desce de 16 para 11`() {

        assertEquals(11.0, adultoFeminino.forPerson(Sex.FEMALE, LifeStage.POSTMENOPAUSAL))
    }

    @Test
    fun `a fase nao mexe no valor dos homens quando nao se aplica`() {

        assertEquals(11.0, adultoFeminino.forPerson(Sex.MALE, null))
    }

    @Test
    fun `as chaves ajustadas sao as que o ecra vai assinalar`() {
        assertEquals(setOf("vitB9_ug", "iodine_ug"), LifeStageDrv.adjustedKeys(LifeStage.PREGNANCY))
        assertEquals(setOf("iron_mg"), LifeStageDrv.adjustedKeys(LifeStage.POSTMENOPAUSAL))
        assertEquals(emptySet(), LifeStageDrv.adjustedKeys(LifeStage.NONE))
    }

    @Test
    fun `a fonte de um ajuste esta disponivel para o ecra a citar`() {
        val fonte = LifeStageDrv.sourceFor("vitB9_ug", LifeStage.PREGNANCY)
        assertNotNull(fonte)
        assertTrue(fonte.contains("3893"), "a citação perdeu o número do parecer: $fonte")
    }

    @Test
    fun `nenhuma fase ajusta a mesma chave duas vezes`() {

        for (fase in LifeStage.entries) {
            val chaves = LifeStageDrv.adjustments(fase).map { it.key }
            assertEquals(chaves.distinct(), chaves, "chave repetida em $fase")
        }
    }
}
