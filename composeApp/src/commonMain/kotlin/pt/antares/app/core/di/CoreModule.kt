package pt.antares.app.core.di

import kotlinx.coroutines.Dispatchers
import org.koin.core.qualifier.named
import org.koin.dsl.module

// Qualificador do dispatcher de entrada e saída. Passa-se explicitamente a tudo o que
// toca disco ou rede, em vez de cada classe o escolher: é o que permite aos testes
// substituí-lo por um dispatcher determinista.
val IoDispatcher = named("io")

/**
 * O armazém das fotografias dos pratos, separado do das fotos de progresso.
 *
 * A separação é uma salvaguarda e não arrumação: o `deleteAll` apaga a pasta inteira, e
 * limpar as fotos de progresso levaria as dos pratos atrás.
 */
val FotosDePrato = named("fotosDePrato")

/**
 * Tudo o que vive enquanto a app viver, junto num nome só.
 *
 * Era um ficheiro de 511 linhas com todas as ligações da app lá dentro. Passou a cinco, por
 * área, e este junta-os — quem regista os módulos não tem de saber que eles são cinco, e um
 * repositório novo entra no ficheiro da sua área em vez de no fundo de uma lista.
 *
 * **Continua a ser um só módulo Gradle.** Separar em módulos de compilação era outra decisão,
 * com outro custo, e não é esta.
 */
val coreModule = module {
    single(IoDispatcher) { Dispatchers.IO }

    includes(
        daoModule,
        repositoryModule,
        networkModule,
        healthModule,
        privacyModule,
    )
}
