package pt.antares.app.core

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Uma dúvida escrita no código nunca é respondida por quem a lê a seguir — é lida como
 * facto. Este teste cobra a regra E3: as dúvidas vão para uma lista à parte, para o dono
 * responder de uma vez.
 *
 * Não se confunde com a proibição de comentários. Um comentário que **decide** é bem-vindo;
 * o que fica marcado é o que adia a decisão.
 */
class DuvidasForaDoCodigoTest {

    // O `XXX` fica de fora: aparece em máscaras e exemplos de formato, e marcá-lo dava
    // falsos positivos em texto que não é dúvida nenhuma.
    private val marcas = listOf("TODO", "FIXME", "HACK", "DÚVIDA", "DUVIDA")

    private val fontes: List<File>
        get() = listOf(
            "src/commonMain/kotlin",
            "src/androidMain/kotlin",
            "../supabase/functions",
        )
            .map(::File)
            .filter { it.exists() }
            .flatMap { raiz ->
                raiz.walkTopDown().filter { it.isFile && it.extension in setOf("kt", "ts") }
            }

    @Test
    fun `nenhuma duvida fica marcada no codigo`() {
        val encontradas = mutableListOf<String>()

        for (ficheiro in fontes) {
            ficheiro.readLines().forEachIndexed { indice, linha ->
                // Só dentro de comentários: `TODO()` é uma função do Kotlin que rebenta de
                // propósito, e `todoItem` é um nome legítimo de variável.
                val comentario = linha.substringAfter("//", "").ifEmpty { linha.substringAfter("*", "") }
                if (comentario.isEmpty()) return@forEachIndexed

                val marca = marcas.firstOrNull { comentario.contains(it) } ?: return@forEachIndexed
                encontradas += "${ficheiro.name}:${indice + 1} · $marca · ${linha.trim().take(80)}"
            }
        }

        assertTrue(
            encontradas.isEmpty(),
            "há dúvidas marcadas no código, e a regra E3 manda-as para uma lista à parte:\n" +
                encontradas.joinToString("\n"),
        )
    }
}
