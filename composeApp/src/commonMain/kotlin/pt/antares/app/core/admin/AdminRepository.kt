package pt.antares.app.core.admin

import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import pt.antares.app.core.datastore.AppPreferences
import pt.antares.app.core.network.supabase.SupabaseContainer
import pt.antares.app.core.util.AppError
import pt.antares.app.core.util.AppResult

@Serializable
private data class AdminUnlockRequest(val code: String, val enable: Boolean)

@Serializable
private data class AdminUnlockResponse(val unlimited: Boolean)

class AdminRepository(
    private val container: SupabaseContainer,
    private val prefs: AppPreferences,

    private val ensureAccount: suspend () -> Unit,
    private val io: CoroutineDispatcher,
) {
    private val json = Json { ignoreUnknownKeys = true }

    val unlimited: Flow<Boolean> = prefs.adminUnlimited

    suspend fun setUnlimited(code: String, enable: Boolean): AppResult<Boolean> = withContext(io) {
        val client = container.client
            ?: return@withContext AppResult.Failure(AppError.Unauthorized)
        try {

            ensureAccount()
            val response: HttpResponse = client.functions(
                function = "admin-unlock",
                body = AdminUnlockRequest(code = code, enable = enable),
                headers = Headers.build { append(HttpHeaders.ContentType, "application/json") },
            )
            val result = json.decodeFromString<AdminUnlockResponse>(response.body<String>())
            prefs.setAdminUnlimited(result.unlimited)
            AppResult.Success(result.unlimited)
        } catch (e: Exception) {
            AppResult.Failure(e.toAdminError())
        }
    }
}

private fun Throwable.toAdminError(): AppError {
    val status = when (this) {
        is RestException -> statusCode
        is ResponseException -> response.status.value
        else -> null
    }
    return when (status) {

        403 -> AppError.Unauthorized

        401 -> AppError.Unknown("admin 401 no session")
        null -> AppError.Network
        else -> AppError.Unknown("admin $status")
    }
}
