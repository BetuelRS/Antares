package pt.antares.app.core.util

import kotlin.random.Random

object Ids {
    fun newUuid(): String {
        val hex = "0123456789abcdef"
        fun randHex(len: Int) = buildString {
            repeat(len) { append(hex[Random.nextInt(16)]) }
        }

        val variantChar = hex[8 + Random.nextInt(4)]
        return "${randHex(8)}-${randHex(4)}-4${randHex(3)}-$variantChar${randHex(3)}-${randHex(12)}"
    }
}
