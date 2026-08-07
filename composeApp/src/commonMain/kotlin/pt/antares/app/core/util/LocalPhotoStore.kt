package pt.antares.app.core.util

expect class LocalPhotoStore {

    suspend fun save(id: String, base64Jpeg: String): String?

    suspend fun delete(path: String)

    suspend fun deleteAll()

    suspend fun exists(path: String): Boolean

    suspend fun readBytes(path: String): ByteArray?

    suspend fun writeBytes(id: String, bytes: ByteArray): String?
}
