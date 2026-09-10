package pt.antares.app.core.health

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class HealthPermissionsParityTest {

    private val manifest = File("src/androidMain/AndroidManifest.xml").readText()

    private fun gateway(): HealthConnectGateway =
        HealthConnectGateway(ApplicationProvider.getApplicationContext<Context>())

    private fun pedidas(): Set<String> =
        gateway().let { it.readPermissions + it.writePermissions + it.heartRatePermissions }

    @Test
    fun `o gateway pede alguma coisa`() {

        assertTrue(pedidas().isNotEmpty(), "o gateway não pede permissão nenhuma")
    }

    /**
     * A frequência cardíaca entrou na 2.31.0 com conjunto próprio, e este teste guarda que
     * continua fora do da importação. O `hasReadPermissions` exige o conjunto todo: com ela
     * lá dentro, quem já tinha concedido as seis de antes deixava de importar peso e treinos,
     * em silêncio, até ir às definições conceder uma permissão que nunca lhe foi pedida.
     */
    @Test
    fun `a frequencia cardiaca nao entra no conjunto da importacao`() {
        val g = gateway()
        assertTrue(g.heartRatePermissions.isNotEmpty(), "a frequência cardíaca não pede permissão nenhuma")
        assertEquals(emptySet(), g.heartRatePermissions intersect g.readPermissions)
    }

    @Test
    fun `o manifesto declara tudo o que o gateway pede`() {
        val emFalta = pedidas().filterNot { manifest.contains(it) }.sorted()
        assertEquals(
            emptyList(),
            emFalta,
            "permissões do Health Connect que o gateway pede e o manifesto não " +
                "declara. Não são concedíveis, e como a verificação faz " +
                "`containsAll`, o import e o publish ficam mudos no telemóvel",
        )
    }

    @Test
    fun `o manifesto nao declara permissoes de saude que ninguem pede`() {

        val declaradas = Regex("""android\.permission\.health\.[A-Z_]+""")
            .findAll(manifest)
            .map { it.value }
            .toSet()
        val aMais = (declaradas - pedidas()).sorted()
        assertEquals(
            emptyList(),
            aMais,
            "permissões de saúde declaradas que o gateway nunca pede",
        )
    }
}
