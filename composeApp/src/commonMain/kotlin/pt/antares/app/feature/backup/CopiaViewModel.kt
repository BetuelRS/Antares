package pt.antares.app.feature.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.antares.app.core.privacy.AutoBackup
import pt.antares.app.core.privacy.DataExporter
import pt.antares.app.core.privacy.EstadoDaCopia

data class CopiaUiState(
    val copia: EstadoDaCopia? = null,

    // Vazio enquanto não for pedido: contar as vinte e seis tabelas é trabalho de base de
    // dados, e o cartão do Hoje não precisa das contagens para dizer «há 34 dias».
    val contagens: Map<String, Int> = emptyMap(),
    val aCopiar: Boolean = false,
    val falhou: Boolean = false,
)

/**
 * O estado da cópia de segurança, para os ecrãs que o mostram. Separado do
 * `PrivacyViewModel` de propósito: aquele observa as pesquisas falhadas em permanência, e
 * o cartão do Hoje não tem nada que abrir essa torneira só para dizer há quantos dias foi
 * a última cópia.
 */
class CopiaViewModel(
    private val auto: AutoBackup,
    private val exporter: DataExporter,
) : ViewModel() {

    private val _state = MutableStateFlow(CopiaUiState())
    val state: StateFlow<CopiaUiState> = _state.asStateFlow()

    init {
        recarregar()
    }

    fun recarregar() {
        viewModelScope.launch {
            val estado = auto.estado()
            _state.update { it.copy(copia = estado) }
        }
    }

    /** As contagens são pedidas por quem as mostra, e não no arranque. */
    fun carregarContagens() {
        if (_state.value.contagens.isNotEmpty()) return
        viewModelScope.launch {
            val contagens = exporter.counts()
            _state.update { it.copy(contagens = contagens) }
        }
    }

    fun copiarAgora() {
        if (_state.value.aCopiar) return
        viewModelScope.launch {
            _state.update { it.copy(aCopiar = true, falhou = false) }
            val feito = auto.correrAgora()
            _state.update { it.copy(aCopiar = false, falhou = !feito, copia = auto.estado()) }
        }
    }
}
