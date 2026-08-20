package pt.antares.app.feature.barcode

import kotlinx.datetime.Clock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pt.antares.app.core.fooddata.Barcode
import pt.antares.app.core.model.MealSlot
import pt.antares.app.feature.diary.DiaryRepository
import pt.antares.app.feature.fooddata.FoodRepository
import pt.antares.app.feature.fooddata.OffFetch
import pt.antares.app.feature.fooddata.OffRepository

sealed interface BarcodeResult {
    data object Idle : BarcodeResult
    data object Resolving : BarcodeResult

    data class Found(val foodId: String) : BarcodeResult

    // Os dois estados de falha são distintos de propósito: não encontrado convida a criar
    // o alimento à mão, falha de rede convida a tentar outra vez.
    data class NotFound(val barcode: String) : BarcodeResult

    data class NetworkError(val barcode: String) : BarcodeResult

    // Terceira falha, e distinta das outras duas: a pesquisa em linha está desligada. Dizer
    // «sem rede» a quem desligou o interruptor é a app a culpar a ligação de uma escolha da
    // pessoa; dizer «não existe» é pior ainda, porque é falso.
    data class Desligada(val barcode: String) : BarcodeResult
}

class BarcodeResolveViewModel(
    private val foodRepository: FoodRepository,
    private val offRepository: OffRepository,
    private val diaryRepository: DiaryRepository,
) : ViewModel() {

    private val _result = MutableStateFlow<BarcodeResult>(BarcodeResult.Idle)
    val result: StateFlow<BarcodeResult> = _result

    fun reset() { _result.value = BarcodeResult.Idle }

    private fun now() = Clock.System.now().toEpochMilliseconds()

    private val _continuous = MutableStateFlow(false)
    val continuous: StateFlow<Boolean> = _continuous

    private val _logged = MutableStateFlow<List<String>>(emptyList())
    val logged: StateFlow<List<String>> = _logged

    /**
     * Os códigos que não estão em lado nenhum, e não quantos foram.
     *
     * Com um número, lêem-se cinco produtos, dois falham, e no fim sabe-se que dois
     * falharam — sem saber quais nem poder fazer nada. Com a lista dá para criar cada um
     * com o código já preenchido.
     */
    private val _notFound = MutableStateFlow<List<String>>(emptyList())
    val notFound: StateFlow<List<String>> = _notFound

    // O mesmo código volta a ser lido enquanto a câmara lhe estiver apontada. A lista é de
    // produtos por criar, não de leituras.
    fun forgetNotFound(barcode: String) {
        _notFound.value = _notFound.value - barcode
    }

    private var slot: MealSlot? = null
    private var epochDay: Long? = null

    fun configure(slot: MealSlot, epochDay: Long) {
        this.slot = slot
        this.epochDay = epochDay
    }

    fun toggleContinuous() {
        _continuous.value = !_continuous.value
    }

    /**
     * Resolve um código lido. Primeiro no catálogo local, só depois em linha: a leitura
     * repete-se muitas vezes por segundo com a câmara apontada, e ir à rede a cada uma
     * seria um pedido por fotograma.
     */
    fun resolve(barcode: String) {
        // A guarda contra chamadas concorrentes é o que torna a leitura contínua possível.
        if (_result.value is BarcodeResult.Resolving) return
        _result.value = BarcodeResult.Resolving
        viewModelScope.launch {

            // Tenta as variantes do código: o mesmo produto está guardado com ou sem o
            // zero à frente conforme a origem — ver [Barcode.searchVariants].
            val local = Barcode.searchVariants(barcode)
                .firstNotNullOfOrNull { foodRepository.byBarcode(it) }

            if (local != null) {
                val stale = foodRepository.isBarcodeCacheStale(local, now())
                if (!stale) {
                    publish(BarcodeResult.Found(local.id))
                    return@launch
                }
                val refreshed = offRepository.fetchAndCache(barcode) as? OffFetch.Found
                publish(BarcodeResult.Found(refreshed?.food?.id ?: local.id))
                return@launch
            }
            publish(
                when (val fetched = offRepository.fetchAndCache(barcode)) {
                    is OffFetch.Found -> BarcodeResult.Found(fetched.food.id)
                    is OffFetch.NotFound -> BarcodeResult.NotFound(barcode)
                    is OffFetch.NetworkError -> BarcodeResult.NetworkError(barcode)
                    is OffFetch.Desligada -> BarcodeResult.Desligada(barcode)
                },
            )
        }
    }

    private suspend fun publish(resultado: BarcodeResult) {
        if (!_continuous.value || resultado is BarcodeResult.NetworkError) {
            _result.value = resultado
            return
        }
        when (resultado) {
            is BarcodeResult.Found -> {
                val destino = slot
                val dia = epochDay
                val food = foodRepository.byId(resultado.foodId)
                if (food != null && destino != null && dia != null) {
                    val gramas = diaryRepository.defaultPortionFor(food)
                    diaryRepository.logFood(food, gramas, destino, dia)
                    foodRepository.touchLastUsed(food.id, amountG = gramas)
                    _logged.value = _logged.value + food.namePt.ifBlank { food.nameEn }
                }
            }
            is BarcodeResult.NotFound ->
                if (resultado.barcode !in _notFound.value) {
                    _notFound.value = _notFound.value + resultado.barcode
                }
            else -> Unit
        }
        _result.value = BarcodeResult.Idle
    }
}
