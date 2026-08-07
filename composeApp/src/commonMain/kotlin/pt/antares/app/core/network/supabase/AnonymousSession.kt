package pt.antares.app.core.network.supabase

import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class AnonymousSession(
    private val container: SupabaseContainer,
    private val io: CoroutineDispatcher,
) {

    val isConfigured: Boolean get() = container.client != null

    fun currentUid(): String? = container.client?.auth?.currentUserOrNull()?.id

    suspend fun ensure() = withContext(io) {
        val auth = container.client?.auth ?: return@withContext
        if (auth.currentUserOrNull() == null) auth.signInAnonymously()
    }
}
