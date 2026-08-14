package pt.antares.app.core.network.supabase

import com.antares.app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.functions.Functions
import kotlin.time.Duration.Companion.seconds

/**
 * Devolve null quando a app foi construída sem chaves — o que é um cenário válido: só a
 * análise por AI depende do servidor, e tudo o resto funciona sem ele.
 */
fun createAntaresSupabase(): SupabaseClient? {
    val url = BuildConfig.SUPABASE_URL
    val key = BuildConfig.SUPABASE_ANON_KEY
    if (url.isBlank() || key.isBlank()) return null
    return runCatching {
        createSupabaseClient(supabaseUrl = url, supabaseKey = key) {

            // Um minuto: a análise de uma fotografia por um modelo de linguagem demora
            // mesmo dezenas de segundos, e o valor por omissão cortava-a a meio.
            requestTimeout = 60.seconds

            // Só autenticação e funções. Não se instala o `Postgrest` de propósito: sem ele
            // não há como a app escrever dados no servidor, nem por engano — é o que o
            // `NoSyncTest` verifica.
            install(Auth)
            install(Functions)
        }
    }.getOrNull()
}
