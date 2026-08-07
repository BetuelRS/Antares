package pt.antares.app.core

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class ManifestPermissionsTest {

    private val manifest = File("src/androidMain/AndroidManifest.xml").readText()

    private val obrigatorias = mapOf(
        "INTERNET" to "pesquisa online, sincronização e AI",
        "CAMERA" to "scanner de código de barras e foto da refeição",
        "POST_NOTIFICATIONS" to "lembretes de refeição, jejum e descanso",
        "VIBRATE" to "fim do temporizador de descanso",
        "ACCESS_FINE_LOCATION" to "corrida com GPS",
        "FOREGROUND_SERVICE" to "corrida a continuar com o ecrã desligado",
        "FOREGROUND_SERVICE_LOCATION" to "idem, com o tipo que o Android 14 exige",
    )

    @Test
    fun `o manifesto declara as permissoes de que a app depende`() {
        val emFalta = obrigatorias.filterKeys { permissao ->
            !manifest.contains("android.permission.$permissao")
        }
        assertTrue(
            emFalta.isEmpty(),
            "permissões em falta no manifesto da app (não vale herdá-las de uma " +
                "dependência): " + emFalta.entries.joinToString { "${it.key} — ${it.value}" },
        )
    }

    @Test
    fun `localizacao em segundo plano continua fora`() {
        assertTrue(
            !manifest.contains("ACCESS_BACKGROUND_LOCATION"),
            "ACCESS_BACKGROUND_LOCATION entrou no manifesto — é uma promessa quebrada",
        )
    }
}
