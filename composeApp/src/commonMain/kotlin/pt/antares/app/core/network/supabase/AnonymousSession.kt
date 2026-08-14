package pt.antares.app.core.network.supabase

import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * A conta. É anónima e cria-se sozinha: não há registo, palavra-passe nem e-mail. Existe
 * só para o servidor poder contar as utilizações de AI por dispositivo, e nenhum dado da
 * app lhe fica associado — o `NoSyncTest` garante que nada sobe.
 */
class AnonymousSession(
    private val container: SupabaseContainer,
    private val io: CoroutineDispatcher,
) {

    val isConfigured: Boolean get() = container.client != null

    // O identificador vai para o ecrã de administração e para o pedido de apagamento de
    // conta. É a única coisa que liga este telemóvel ao servidor.
    fun currentUid(): String? = container.client?.auth?.currentUserOrNull()?.id

    /** Sem chaves, não faz nada e não falha: a app tem de funcionar sem servidor. */
    suspend fun ensure() = withContext(io) {
        val auth = container.client?.auth ?: return@withContext
        if (auth.currentUserOrNull() == null) auth.signInAnonymously()
    }
}
