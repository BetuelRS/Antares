package pt.antares.app.feature.fooddata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.antares.app.core.ai.AiRepository
import pt.antares.app.core.ai.AiWarnings
import pt.antares.app.core.database.entities.FoodEntity
import pt.antares.app.core.util.AppError
import pt.antares.app.core.util.AppResult
import kotlin.math.abs
import kotlin.math.roundToInt

data class FoodEditState(
    val editingId: String? = null,
    val name: String = "",
    val kcal: String = "",
    val protein: String = "",
    val carbs: String = "",
    val fat: String = "",
    val sugars: String = "",
    val satFat: String = "",
    val fiber: String = "",
    val sodium: String = "",
    val servingName: String = "",
    val servingGrams: String = "",
    val saved: Boolean = false,

    val readingLabel: Boolean = false,
    val labelError: AppError? = null,

    val labelIncomplete: Boolean = false,

    val labelNeedsCheck: Boolean = false,

    // Alimentos que já existem com um nome parecido. Avisam, não bloqueiam: há bacalhaus
    // diferentes, e quem escreve o nome é quem sabe se é o mesmo.
    val duplicados: List<FoodEntity> = emptyList(),
) {
    private fun num(s: String): Double? = s.replace(',', '.').toDoubleOrNull()

    val valid: Boolean
        get() = name.trim().length >= 2 &&
            kcal.toIntOrNull()?.let { it in 0..900 } == true &&
            num(protein) != null && num(carbs) != null && num(fat) != null

    val kcalMismatch: Boolean
        get() {
            val k = kcal.toIntOrNull() ?: return false
            val p = num(protein) ?: return false
            val c = num(carbs) ?: return false
            val f = num(fat) ?: return false
            val computed = p * 4 + c * 4 + f * 9
            if (k == 0 && computed == 0.0) return false
            return abs(computed - k) > (k.coerceAtLeast(1)) * 0.10 + 5
        }
}

@OptIn(FlowPreview::class)
class FoodEditViewModel(
    private val repository: FoodRepository,
    private val ai: AiRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(FoodEditState())
    val state: StateFlow<FoodEditState> = _state

    private var barcode: String? = null

    private val nomeEscrito = MutableStateFlow("")

    init {
        // O mesmo intervalo da pesquisa de alimentos. Menos do que isto consulta o índice a
        // cada tecla; mais e o aviso só chega depois de a pessoa ter escrito os macros.
        nomeEscrito
            .debounce(ESPERA_MS)
            .onEach { nome -> _state.update { it.copy(duplicados = procurarParecidos(nome)) } }
            .launchIn(viewModelScope)
    }

    /**
     * Procura no catálogo com o mesmo índice da pesquisa de alimentos, e não por igualdade
     * de texto: quem escreve «arroz cozido» tem de ser avisado do «Arroz, cozido» que já lá
     * está, e uma comparação exata nunca os juntava.
     *
     * Só ao criar. A editar, o alimento parecido é quase sempre o próprio.
     */
    private suspend fun procurarParecidos(nome: String): List<FoodEntity> {
        if (_state.value.editingId != null) return emptyList()
        if (nome.trim().length < MIN_LETRAS) return emptyList()
        return repository.search(nome).take(MAX_DUPLICADOS)
    }

    fun readLabel(imageBase64: String, mime: String = "image/jpeg") {
        if (_state.value.readingLabel) return
        _state.update { it.copy(readingLabel = true, labelError = null, labelIncomplete = false) }
        viewModelScope.launch {
            when (val r = ai.readLabel(imageBase64, mime)) {
                is AppResult.Failure ->
                    _state.update { it.copy(readingLabel = false, labelError = r.error) }

                is AppResult.Success -> {
                    val draft = r.value.draft
                    val p = draft.per100g
                    val incomplete = r.value.warnings.contains(AiWarnings.LABEL_INCOMPLETE) ||
                        r.value.warnings.contains(AiWarnings.NOT_LABEL) ||
                        r.value.warnings.contains(AiWarnings.LABEL_NO_SERVING_G) ||
                        p.kcal == null

                    val needsCheck = r.value.warnings.contains(AiWarnings.LABEL_CONVERTED) ||
                        r.value.warnings.contains(AiWarnings.LABEL_INCONSISTENT)
                    _state.update { s ->
                        s.copy(
                            name = s.name.ifBlank { draft.name.orEmpty() },
                            kcal = s.kcal.ifBlank { p.kcal.asField(decimals = false) },
                            protein = s.protein.ifBlank { p.protein.asField() },
                            carbs = s.carbs.ifBlank { p.carbs.asField() },
                            fat = s.fat.ifBlank { p.fat.asField() },

                            sugars = s.sugars.ifBlank { p.sugars.asField() },
                            satFat = s.satFat.ifBlank { p.satFat.asField() },
                            fiber = s.fiber.ifBlank { p.fiber.asField() },
                            sodium = s.sodium.ifBlank { p.sodiumMg.asField(decimals = false) },
                            servingGrams = s.servingGrams.ifBlank { draft.servingG.asField() },
                            readingLabel = false,
                            labelIncomplete = incomplete,
                            labelNeedsCheck = needsCheck,
                        )
                    }
                }
            }
        }
    }

    fun clearLabelError() = _state.update { it.copy(labelError = null) }

    private fun Double?.asField(decimals: Boolean = true): String = when {
        this == null -> ""
        decimals -> ((this * 10).roundToInt() / 10.0).toString().removeSuffix(".0")
        else -> roundToInt().toString()
    }

    fun start(foodId: String?, barcode: String?) {
        this.barcode = barcode
        load(foodId)
    }

    fun load(foodId: String?) {
        if (foodId == null) return
        viewModelScope.launch {
            val f = repository.byId(foodId) ?: return@launch
            _state.update {
                FoodEditState(
                    editingId = f.id,
                    name = f.namePt,
                    kcal = "${f.kcal}",
                    protein = "${f.proteinG}",
                    carbs = "${f.carbsG}",
                    fat = "${f.fatG}",
                    sugars = f.sugarsG?.toString().orEmpty(),
                    satFat = f.satFatG?.toString().orEmpty(),
                    fiber = f.fiberG?.toString().orEmpty(),
                    sodium = f.sodiumMg?.toString().orEmpty(),
                    servingName = f.servingName.orEmpty(),
                    servingGrams = f.servingGrams?.toString().orEmpty(),
                )
            }
        }
    }

    private fun digits(s: String, decimals: Boolean = true) =
        s.filter { it.isDigit() || (decimals && (it == '.' || it == ',')) }.take(7)

    fun setName(v: String) {
        val nome = v.take(80)
        _state.update { it.copy(name = nome) }
        nomeEscrito.value = nome
    }
    fun setKcal(v: String) = _state.update { it.copy(kcal = digits(v, decimals = false).take(4)) }
    fun setProtein(v: String) = _state.update { it.copy(protein = digits(v)) }
    fun setCarbs(v: String) = _state.update { it.copy(carbs = digits(v)) }
    fun setFat(v: String) = _state.update { it.copy(fat = digits(v)) }
    fun setSugars(v: String) = _state.update { it.copy(sugars = digits(v)) }
    fun setSatFat(v: String) = _state.update { it.copy(satFat = digits(v)) }
    fun setFiber(v: String) = _state.update { it.copy(fiber = digits(v)) }
    fun setSodium(v: String) = _state.update { it.copy(sodium = digits(v, decimals = false)) }
    fun setServingName(v: String) = _state.update { it.copy(servingName = v.take(40)) }
    fun setServingGrams(v: String) = _state.update { it.copy(servingGrams = digits(v)) }

    fun save() {
        val s = _state.value
        if (!s.valid) return
        fun num(str: String): Double? = str.replace(',', '.').toDoubleOrNull()
        viewModelScope.launch {
            repository.upsertCustom(
                existingId = s.editingId,
                namePt = s.name.trim(),
                kcal = s.kcal.toInt(),
                proteinG = num(s.protein)!!,
                carbsG = num(s.carbs)!!,
                fatG = num(s.fat)!!,
                sugarsG = num(s.sugars),
                satFatG = num(s.satFat),
                fiberG = num(s.fiber),
                sodiumMg = s.sodium.toIntOrNull(),
                servingName = s.servingName.trim().ifBlank { null },
                servingGrams = num(s.servingGrams),
                barcode = barcode,
            )
            _state.update { it.copy(saved = true) }
        }
    }

    private companion object {
        const val ESPERA_MS = 300L
        const val MIN_LETRAS = 3
        // Três chegam para reconhecer o alimento. Uma lista mais longa passava de aviso a
        // segunda pesquisa, e o ecrã não é para procurar.
        const val MAX_DUPLICADOS = 3
    }
}
