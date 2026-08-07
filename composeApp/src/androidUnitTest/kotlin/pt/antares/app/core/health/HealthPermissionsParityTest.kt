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

    private fun pedidas(): Set<String> = gateway().let { it.readPermissions + it.writePermissions }

    @Test
    fun `o gateway pede alguma coisa`() {

        assertTrue(pedidas().isNotEmpty(), "o gateway não pede permissão nenhuma")
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
