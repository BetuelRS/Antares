package pt.antares.app.core.util

/**
 * O sítio onde as imagens vivem: em ficheiros, e nunca dentro da base.
 *
 * Há **duas** instâncias, em pastas diferentes, e a separação não é arrumação — é uma
 * salvaguarda. O [deleteAll] apaga a pasta inteira, e é o que apaga as fotos de progresso;
 * partilhar a pasta faria com que apagar as fotos de progresso levasse as dos pratos atrás.
 */
expect class LocalPhotoStore {

    suspend fun save(id: String, base64Jpeg: String): String?

    /** Os caminhos de tudo o que lá está. É por aqui que a varredura encontra os órfãos. */
    suspend fun listAll(): List<String>

    suspend fun delete(path: String)

    suspend fun deleteAll()

    suspend fun exists(path: String): Boolean

    suspend fun readBytes(path: String): ByteArray?

    suspend fun writeBytes(id: String, bytes: ByteArray): String?
}
