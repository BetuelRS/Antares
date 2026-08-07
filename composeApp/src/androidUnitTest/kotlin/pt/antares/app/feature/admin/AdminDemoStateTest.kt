package pt.antares.app.feature.admin

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.core.database.AntaresDb
import pt.antares.app.core.database.entities.WeightLogEntity
import pt.antares.app.core.demo.DemoDataWriter
import pt.antares.app.core.model.WeightSource
import pt.antares.app.core.util.todayEpochDay
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AdminDemoStateTest {

    private lateinit var db: AntaresDb
    private lateinit var vm: DemoViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AntaresDb::class.java)
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()
        vm = DemoViewModel(DemoDataWriter(db.demoDao(), Dispatchers.Default))
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private suspend fun esperar(condicao: (DemoState) -> Boolean): DemoState =
        vm.state.first { condicao(it) }

    private suspend fun ligar() {
        vm.pedir(DemoAcao.LIGAR)
        esperar { it.confirmar != null }
        vm.confirmar()
        esperar { it.mensagem != DemoMessage.NENHUMA && !it.aTrabalhar }
    }

    @Test
    fun `abre a dizer desligado`() = runTest {
        val s = esperar { !it.aTrabalhar }
        assertFalse(s.ligado)
        assertEquals(0, s.linhas)
        assertNull(s.confirmar)
        assertEquals(DemoMessage.NENHUMA, s.mensagem)
    }

    @Test
    fun `pedir abre a confirmacao e nao escreve nada`() = runTest {
        esperar { !it.aTrabalhar }
        vm.pedir(DemoAcao.LIGAR)

        val s = esperar { it.confirmar != null }
        assertEquals(DemoAcao.LIGAR, s.confirmar)

        assertEquals(0, db.demoDao().demoCount())
        assertFalse(s.ligado)
    }

    @Test
    fun `cancelar fecha a confirmacao e deixa a base como estava`() = runTest {
        esperar { !it.aTrabalhar }
        vm.pedir(DemoAcao.LIGAR)
        esperar { it.confirmar != null }

        vm.cancelar()

        assertNull(esperar { it.confirmar == null }.confirmar)
        assertEquals(0, db.demoDao().demoCount())
    }

    @Test
    fun `confirmar liga, e o estado diz quantas linhas estao la`() = runTest {
        esperar { !it.aTrabalhar }
        ligar()

        val s = esperar { it.mensagem == DemoMessage.LIGOU }
        assertTrue(s.ligado)
        assertFalse(s.aTrabalhar)
        assertTrue(s.linhas > 0)

        assertEquals(db.demoDao().demoCount(), s.linhas)
    }

    @Test
    fun `desligar devolve o ecra a zero`() = runTest {
        esperar { !it.aTrabalhar }
        ligar()

        vm.pedir(DemoAcao.DESLIGAR)
        esperar { it.confirmar != null }
        vm.confirmar()

        val s = esperar { it.mensagem == DemoMessage.DESLIGOU }
        assertFalse(s.ligado)
        assertEquals(0, s.linhas)
        assertEquals(0, db.demoDao().demoCount())
    }

    @Test
    fun `com dados reais o ecra recusa e diz quantos sao`() = runTest {
        db.weightLogDao().upsert(
            WeightLogEntity(
                id = "real-1",
                epochDay = todayEpochDay() - 2,
                weightKg = 80.0,
                note = null,
                source = WeightSource.MANUAL,
                sourceRef = null,
                updatedAt = 1L,
            ),
        )
        esperar { !it.aTrabalhar }
        ligar()

        val s = esperar { it.mensagem == DemoMessage.RECUSOU_DADOS_REAIS }

        assertEquals(1, s.linhasReais)
        assertFalse(s.ligado)
        assertEquals(0, db.demoDao().demoCount())
    }

    @Test
    fun `confirmar sem ter pedido nao faz nada`() = runTest {

        esperar { !it.aTrabalhar }
        vm.confirmar()

        assertEquals(0, db.demoDao().demoCount())
        val s = esperar { !it.aTrabalhar }
        assertFalse(s.ligado)
        assertEquals(DemoMessage.NENHUMA, s.mensagem)
    }
}
