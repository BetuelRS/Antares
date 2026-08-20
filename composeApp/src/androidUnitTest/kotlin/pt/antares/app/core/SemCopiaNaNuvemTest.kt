package pt.antares.app.core

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A app deixou de mandar dados para a cópia da Google na 2.1.0, por decisão do dono. Este
 * teste substitui o `BackupRulesTest`, que verificava o contrário: que as regras da nuvem
 * estavam bem escritas.
 *
 * O que ele guarda não é a decisão em si — é a condição que a torna honesta. Desligar a
 * Google só não tira nada a ninguém porque entrou uma cópia automática local que sobrevive
 * à desinstalação. Se essa cópia desaparecer e o `allowBackup="false"` ficar, a app fica
 * sem rede nenhuma, e é isso que estas asserções não deixam acontecer em silêncio.
 */
class SemCopiaNaNuvemTest {

    private val manifest = File("src/androidMain/AndroidManifest.xml").readText()

    @Test
    fun `a copia da Google esta desligada`() {
        assertTrue(
            manifest.contains("android:allowBackup=\"false\""),
            "o allowBackup voltou a true — os dados voltam a sair para a Google",
        )
    }

    @Test
    fun `as regras da nuvem nao voltaram`() {

        // Com o allowBackup a false estes atributos não fazem nada, mas a sua presença diz
        // a quem lê o manifesto que ainda há uma cópia na nuvem. E se alguém repuser o
        // allowBackup, voltam a valer sem ninguém reparar.
        assertFalse(manifest.contains("android:fullBackupContent"))
        assertFalse(manifest.contains("android:dataExtractionRules"))

        for (ficheiro in listOf("backup_rules.xml", "data_extraction_rules.xml")) {
            assertFalse(
                File("src/androidMain/res/xml/$ficheiro").exists(),
                "$ficheiro voltou — foi apagado na 2.1.0",
            )
        }
    }

    @Test
    fun `a permissao de escrita antiga tem tecto`() {

        // Sem o maxSdkVersion, o Android 10 e seguintes veem uma app a pedir acesso ao
        // armazenamento todo. A Play Store recusa, e a app deixa de poder ser publicada.
        val pedida = manifest.contains("android.permission.WRITE_EXTERNAL_STORAGE")
        if (!pedida) return
        val bloco = manifest.substringAfter("android.permission.WRITE_EXTERNAL_STORAGE")
            .substringBefore("/>")
        assertTrue(
            bloco.contains("android:maxSdkVersion=\"28\""),
            "o WRITE_EXTERNAL_STORAGE ficou sem maxSdkVersion",
        )
    }

    @Test
    fun `existe uma copia local a substituir a da nuvem`() {

        val auto = File("src/commonMain/kotlin/pt/antares/app/core/privacy/AutoBackup.kt")
        assertTrue(auto.exists(), "a cópia automática local desapareceu")
        // As duas constantes e não o caminho já junto: o `BackupStore` monta-o com
        // interpolação, e procurar a frase inteira era procurar texto que nunca existiu no
        // ficheiro — o teste passava a acusar sempre.
        val loja = File("src/androidMain/kotlin/pt/antares/app/core/privacy/BackupStore.android.kt")
            .readText()
        for (parte in listOf("const val DOCUMENTOS = \"Documents\"", "const val PASTA = \"Antares\"")) {
            assertTrue(
                loja.contains(parte),
                "a cópia deixou de ir para a pasta Documentos/Antares — se for para dentro " +
                    "da app, desaparece com a desinstalação e não substitui a da Google",
            )
        }
    }
}
