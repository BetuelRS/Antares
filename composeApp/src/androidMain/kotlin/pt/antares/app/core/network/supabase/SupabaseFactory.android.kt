package pt.antares.app.core.network.supabase

import com.antares.app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.functions.Functions
import kotlin.time.Duration.Companion.seconds

fun createAntaresSupabase(): SupabaseClient? {
    val url = BuildConfig.SUPABASE_URL
    val key = BuildConfig.SUPABASE_ANON_KEY
    if (url.isBlank() || key.isBlank()) return null
    return runCatching {
        createSupabaseClient(supabaseUrl = url, supabaseKey = key) {

            requestTimeout = 60.seconds

            install(Auth)
            install(Functions)
        }
    }.getOrNull()
}
