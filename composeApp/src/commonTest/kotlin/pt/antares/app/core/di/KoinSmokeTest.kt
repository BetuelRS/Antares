package pt.antares.app.core.di

import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import kotlin.test.AfterTest
import kotlin.test.Test

class KoinSmokeTest : KoinTest {

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `modulos comuns carregam sem conflito de definicoes`() {
        startKoin {
            modules(coreModule, viewModelModule)
        }
    }
}
