package pt.antares.app.feature.profile

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.feature.profile.ui.EscolhaDaSegundaPesagem
import pt.antares.app.feature.profile.ui.WeightViewModel
import pt.antares.app.testing.Fabricas
import pt.antares.app.testing.ViewModelHarness
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Quem se pesa, acha o valor estranho e volta a subir à balança ficava com a média dos dois —
 * em silêncio, sem saber que houve média, e sem poder dizer qual das medições valia.
 *
 * A média continua a ser uma resposta possível. Deixou é de ser a única, e de ser tomada por
 * quem não perguntou.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SegundaPesagemPerguntaTest : ViewModelHarness() {

    private val hoje = 20_000L

    private fun viewModel() = vivo(WeightViewModel(Fabricas.profileRepository(db, dispatcher)))

    private fun pesar(vm: WeightViewModel, kg: Double) {
        vm.submit(epochDay = hoje, weightKg = kg, note = null)
        dispatcher.scheduler.advanceUntilIdle()
    }

    private suspend fun pesoGuardado(): Double? =
        db.weightLogDao().byDayForWrite(hoje)?.takeIf { !it.deleted }?.weightKg

    @Test
    fun `a primeira pesagem do dia grava sem perguntar nada`() = runTest(dispatcher) {
        val vm = viewModel()

        pesar(vm, 80.0)

        assertNull(vm.segundaPesagem.value, "não há nada a perguntar quando não há conflito")
        assertEquals(80.0, pesoGuardado())
    }

    @Test
    fun `a segunda pesagem para e pergunta, com os dois valores`() = runTest(dispatcher) {
        val vm = viewModel()
        pesar(vm, 80.0)

        pesar(vm, 81.0)

        val p = assertNotNull(vm.segundaPesagem.value)
        assertEquals(80.0, p.anteriorKg)
        assertEquals(81.0, p.novoKg)
        assertEquals(80.0, pesoGuardado(), "nada é escrito antes de haver resposta")
    }

    @Test
    fun `substituir fica com o valor novo`() = runTest(dispatcher) {
        val vm = viewModel()
        pesar(vm, 80.0)
        pesar(vm, 81.0)

        vm.resolverSegundaPesagem(EscolhaDaSegundaPesagem.SUBSTITUIR)
        advanceUntilIdle()

        assertEquals(81.0, pesoGuardado())
        assertNull(vm.segundaPesagem.value)
    }

    @Test
    fun `a media continua a existir, mas escolhida`() = runTest(dispatcher) {
        val vm = viewModel()
        pesar(vm, 80.0)
        pesar(vm, 81.0)

        vm.resolverSegundaPesagem(EscolhaDaSegundaPesagem.MEDIA)
        advanceUntilIdle()

        assertEquals(80.5, pesoGuardado())
    }

    @Test
    fun `manter a anterior nao escreve nada`() = runTest(dispatcher) {
        val vm = viewModel()
        pesar(vm, 80.0)
        pesar(vm, 95.0)

        vm.resolverSegundaPesagem(EscolhaDaSegundaPesagem.MANTER_A_ANTERIOR)
        advanceUntilIdle()

        assertEquals(
            80.0,
            pesoGuardado(),
            "é a resposta a «subi mal à balança», e é a que a média silenciosa estragava",
        )
    }

    @Test
    fun `fechar a pergunta sem responder deixa tudo como estava`() = runTest(dispatcher) {
        val vm = viewModel()
        pesar(vm, 80.0)
        pesar(vm, 81.0)

        vm.dispensarSegundaPesagem()
        advanceUntilIdle()

        assertNull(vm.segundaPesagem.value)
        assertEquals(80.0, pesoGuardado())
    }

    @Test
    fun `a escolha nao se guarda para a proxima vez`() = runTest(dispatcher) {
        val vm = viewModel()
        pesar(vm, 80.0)
        pesar(vm, 81.0)
        vm.resolverSegundaPesagem(EscolhaDaSegundaPesagem.SUBSTITUIR)
        advanceUntilIdle()

        pesar(vm, 82.0)

        assertNotNull(
            vm.segundaPesagem.value,
            "a razão de repetir a pesagem muda de dia para dia; não é uma preferência",
        )
    }
}
