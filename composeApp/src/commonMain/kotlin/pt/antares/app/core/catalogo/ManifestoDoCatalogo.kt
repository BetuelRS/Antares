package pt.antares.app.core.catalogo

import kotlinx.serialization.Serializable

/**
 * O que a app precisa de saber para decidir **sem** descarregar cinco megabytes.
 *
 * Vive numa release do GitHub, ao lado do catálogo que descreve. É pequeno de propósito: o
 * pedido do manifesto é a única coisa que sai daqui quando alguém carrega no botão e já está
 * tudo em dia, e esse é o caso comum.
 *
 * O [alimentos] e a [nota] não decidem nada — são para a pessoa ler antes de aceitar uma
 * descarga. O que decide é a [versao] e o [sha256].
 */
@Serializable
data class ManifestoDoCatalogo(
    val versao: Int,
    val url: String,
    val sha256: String,
    val alimentos: Int,
    val nota: String = "",
)
