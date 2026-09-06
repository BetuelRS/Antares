package pt.antares.app.core.privacy

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import pt.antares.app.core.database.entities.RoutineEntity
import pt.antares.app.core.model.RegraDeProgressao

/**
 * Uma cópia de segurança feita **antes** da v41 não traz a regra de progressão nem o degrau, e
 * tem de continuar a restaurar-se.
 *
 * O importador lê com `ignoreUnknownKeys` — o que resolve o campo a mais —, mas o campo **a
 * menos** só se resolve porque a entidade lhe dá omissão. Um campo novo sem omissão fazia
 * rebentar o restauro de toda a gente que exportou antes desta versão, e o sítio onde isso
 * aparece é aqui e não no compilador.
 */
class RotinaDeCopiaAntigaTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun `uma rotina exportada pela 2_27_0 restaura-se sem regra nenhuma`() {
        val antiga = """
            {"id":"r1","name":"Empurrar A","note":null,"position":0,
             "updatedAt":1730000000000,"deleted":false}
        """.trimIndent()

        val rotina = json.decodeFromString<RoutineEntity>(antiga)

        assertEquals("Empurrar A", rotina.name)
        assertEquals(RegraDeProgressao.NENHUMA, rotina.progressao)
        assertNull(rotina.incrementoKg)
    }

    @Test
    fun `a regra viaja pelo nome, e nao pela posicao na lista`() {
        val comRegra = """
            {"id":"r1","name":"Empurrar A","note":null,"position":0,
             "progressao":"DUPLA","incrementoKg":2.0,
             "updatedAt":1730000000000,"deleted":false}
        """.trimIndent()

        val rotina = json.decodeFromString<RoutineEntity>(comRegra)

        assertEquals(RegraDeProgressao.DUPLA, rotina.progressao)
        assertEquals(2.0, rotina.incrementoKg)
    }
}
