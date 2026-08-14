package pt.antares.app.core.network.supabase

import io.github.jan.supabase.SupabaseClient

/**
 * O cliente do Supabase, ou null quando a app foi construída sem chaves. Tudo o que
 * depende de rede tem de aguentar esse null: a app funciona inteira sem servidor, e só a
 * análise por AI é que não.
 */
class SupabaseContainer(val client: SupabaseClient?)
