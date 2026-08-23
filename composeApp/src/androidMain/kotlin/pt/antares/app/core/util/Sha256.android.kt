package pt.antares.app.core.util

import java.security.MessageDigest

private const val MASCARA_DE_BYTE = 0xFF
private const val BASE_HEXADECIMAL = 16
private const val CASAS_DO_HEXADECIMAL = 2

actual fun sha256(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { b ->
            (b.toInt() and MASCARA_DE_BYTE).toString(BASE_HEXADECIMAL)
                .padStart(CASAS_DO_HEXADECIMAL, '0')
        }
