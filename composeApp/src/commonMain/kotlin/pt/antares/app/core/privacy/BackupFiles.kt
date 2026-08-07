package pt.antares.app.core.privacy

object BackupFiles {

    const val DATA = "antares-dados.json"

    const val PHOTO_DIR = "fotos/"

    fun photoIdOf(entrada: String): String? =
        entrada.takeIf { it.startsWith(PHOTO_DIR) && it.endsWith(".jpg") }
            ?.removePrefix(PHOTO_DIR)
            ?.removeSuffix(".jpg")
            ?.takeIf { it.isNotBlank() }
}
