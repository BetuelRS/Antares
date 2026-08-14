package pt.antares.app.core.util

import kotlin.random.Random

object Ids {
    /**
     * UUID versão 4 montado à mão: não há gerador no código comum do Kotlin. Os
     * identificadores são aleatórios e não sequenciais para nada no ficheiro exportado
     * revelar quantos registos existem nem por que ordem foram criados.
     */
    fun newUuid(): String {
        val hex = "0123456789abcdef"
        fun randHex(len: Int) = buildString {
            repeat(len) { append(hex[Random.nextInt(16)]) }
        }

        // O `4` fixo no terceiro grupo e este caractere entre 8 e b são o que a norma
        // exige para marcar versão e variante.
        val variantChar = hex[8 + Random.nextInt(4)]
        return "${randHex(8)}-${randHex(4)}-4${randHex(3)}-$variantChar${randHex(3)}-${randHex(12)}"
    }
}
