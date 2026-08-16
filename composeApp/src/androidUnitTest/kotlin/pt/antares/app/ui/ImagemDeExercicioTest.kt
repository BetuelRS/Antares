package pt.antares.app.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * As imagens dos exercícios vêm da rede, numa app que se apresenta como offline.
 *
 * Eram um `AsyncImage` cru: sem esboço, sem estado de erro e sem cache escrita à mão. Sem
 * rede, isso dá um retângulo vazio sem explicação — e um retângulo vazio lê-se como uma app
 * partida, não como uma imagem que precisa de ligação.
 */
class ImagemDeExercicioTest {

    private val componente =
        File("src/commonMain/kotlin/pt/antares/app/feature/workout/ui/ExerciseImage.kt").readText()

    private fun ecrasDeTreino(): List<File> =
        File("src/commonMain/kotlin/pt/antares/app/feature/workout").walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name != "ExerciseImage.kt" }
            .toList()

    @Test
    fun `a imagem de um exercicio distingue os tres estados`() {
        assertTrue("State.Error" in componente, "sem ramo de erro, uma falha de rede fica em branco")
        assertTrue("State.Success" in componente)
        assertTrue(
            "Esboco" in componente,
            "sem esboço, o sítio da imagem é um buraco enquanto ela não chega",
        )
    }

    @Test
    fun `nenhum ecra de treino desenha a imagem por fora do componente`() {
        // O caminho fácil é escrever `AsyncImage(model = url)` e seguir. É esse que se
        // fecha: a decisão dos três estados vive num sítio só.
        val infratores = ecrasDeTreino()
            .filter { Regex("""\bAsyncImage\(""").containsMatchIn(it.readText()) }
            .map { it.name }

        assertEquals(
            emptyList(),
            infratores,
            "usa o `ExerciseImage`, que trata do esboço e da falta de ligação: $infratores",
        )
    }

    @Test
    fun `a cache de imagens em disco esta escrita e tem teto`() {
        val app = File("src/androidMain/kotlin/pt/antares/app/AntaresApplication.kt").readText()

        assertTrue(
            "DiskCache" in app,
            "sem cache em disco, o que já se viu uma vez desaparece sem rede — numa app que " +
                "promete funcionar offline",
        )
        assertTrue(
            "maxSizeBytes" in app,
            "sem teto, a pasta de imagens cresce até onde o telemóvel deixar",
        )
    }
}
