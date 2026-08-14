package pt.antares.app.core.privacy

/**
 * A estrutura do arquivo de cópia de segurança: o JSON dos dados e uma pasta de fotos. Os
 * nomes são o formato — mudá-los faz a app deixar de reconhecer cópias já criadas.
 */
object BackupFiles {

    const val DATA = "antares-dados.json"

    const val PHOTO_DIR = "fotos/"

    /**
     * O identificador da foto tirado do caminho dentro do arquivo. Devolve null a tudo o
     * resto: um ZIP vindo de fora pode trazer entradas quaisquer, e só as que seguem
     * exatamente este formato viram ficheiros no telemóvel.
     */
    fun photoIdOf(entrada: String): String? =
        entrada.takeIf { it.startsWith(PHOTO_DIR) && it.endsWith(".jpg") }
            ?.removePrefix(PHOTO_DIR)
            ?.removeSuffix(".jpg")
            ?.takeIf { it.isNotBlank() }
}
