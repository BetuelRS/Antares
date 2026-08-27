package pt.antares.app.core.di

import org.koin.dsl.module
import pt.antares.app.core.ai.AiClient
import pt.antares.app.core.ai.AiRepository
import pt.antares.app.core.ai.SupabaseAiClient
import pt.antares.app.core.catalogo.ActualizadorDoCatalogo
import pt.antares.app.core.catalogo.ApiDoCatalogo
import pt.antares.app.core.database.daos.FoodLogDao
import pt.antares.app.core.database.daos.WeightLogDao
import pt.antares.app.core.datastore.AppPreferences
import pt.antares.app.core.network.supabase.AnonymousSession
import pt.antares.app.core.network.createAntaresHttpClient
import pt.antares.app.core.network.off.OffApi
import pt.antares.app.feature.about.AppChangelog
import pt.antares.app.feature.fooddata.OffRepository
import pt.antares.app.core.admin.AdminRepository

/**
 * Tudo o que sai do telemóvel: o cliente HTTP, a Open Food Facts, a sessão anónima e a IA.
 *
 * Um cliente para a app toda. Cada instância abre o seu conjunto de ligações, e várias
 * delas eram memória e sockets a mais para as duas ou três chamadas que a app faz.
 */
val networkModule = module {
    // Um cliente HTTP para a app toda: cada instância abre o seu conjunto de ligações, e
    // várias delas eram memória e sockets a mais para as duas ou três chamadas que a app faz.
    single { createAntaresHttpClient() }
    // A versão sai do `AppChangelog`, que o `AppChangelogTest` mantém colado ao
    // `versionName` do build. É o que impede o `User-Agent` de envelhecer sozinho.
    single { OffApi(get(), userAgent = "Antares/${AppChangelog.CURRENT} (${OffApi.CONTACT})") }
    single {
        val preferencias = get<pt.antares.app.core.datastore.AppPreferences>()
        // Lido a cada chamada e não uma vez: desligar o interruptor tem de valer já, e não
        // só depois de a app ser reaberta.
        OffRepository(get(), get(), get(IoDispatcher)) { preferencias.pesquisaEmLinhaOnce() }
    }

    // O mesmo `User-Agent` da Open Food Facts, pela mesma razão: um pedido sem nome nem
    // versão é um pedido que ninguém consegue atribuir quando corre mal.
    single { ApiDoCatalogo(get(), userAgent = "Antares/${AppChangelog.CURRENT}") }
    single { ActualizadorDoCatalogo(get(), get(), get(), get(IoDispatcher)) }

    single { AnonymousSession(get(), get(IoDispatcher)) }

    single<AiClient> { SupabaseAiClient(get(), get(IoDispatcher)) }
    single {
        val sessao: AnonymousSession = get()
        AdminRepository(
            container = get(),
            prefs = get(),
            ensureAccount = { sessao.ensure() },
            io = get(IoDispatcher),
        )
    }
    single {
        val sessao: AnonymousSession = get()
        val foodLogDao: FoodLogDao = get()
        val weightLogDao: WeightLogDao = get()
        val prefs: AppPreferences = get()
        val fotos: pt.antares.app.core.util.LocalPhotoStore = get(FotosDePrato)
        AiRepository(
            client = get(),

            // Funções em vez dos DAOs inteiros: a classe da AI fica sem acesso à base, e
            // o que ela pode escrever está aqui à vista.
            ensureAccount = { sessao.ensure() },
            saveFoodLog = { foodLogDao.upsert(it) },
            latestWeightKg = { weightLogDao.latest()?.weightKg },
            persistUsage = { usage, day ->
                prefs.setAiUsage(usage.used, usage.limit, usage.trial, day)
            },
            savePhoto = { id, base64 -> fotos.save(id, base64) },
            io = get(IoDispatcher),
        )
    }
}
