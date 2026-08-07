package pt.antares.app.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pt.antares.app.core.demo.DemoDataWriter
import pt.antares.app.core.demo.DemoResult
import pt.antares.app.core.util.todayEpochDay

enum class DemoMessage { NENHUMA, LIGOU, DESLIGOU, RECUSOU_DADOS_REAIS, ERRO }

enum class DemoAcao { LIGAR, DESLIGAR }

data class DemoState(
    val ligado: Boolean = false,
    val linhas: Int = 0,
    val aTrabalhar: Boolean = false,
    val confirmar: DemoAcao? = null,
    val mensagem: DemoMessage = DemoMessage.NENHUMA,

    val linhasReais: Int = 0,
)

class DemoViewModel(
    private val demo: DemoDataWriter,
) : ViewModel() {

    private val _state = MutableStateFlow(DemoState())
    val state: StateFlow<DemoState> = _state.asStateFlow()

    init {

        viewModelScope.launch { recontar() }
    }

    private suspend fun recontar() {
        val linhas = demo.quantasLinhas()
        _state.value = _state.value.copy(ligado = linhas > 0, linhas = linhas)
    }

    fun pedir(acao: DemoAcao) {
        if (_state.value.aTrabalhar) return
        _state.value = _state.value.copy(confirmar = acao, mensagem = DemoMessage.NENHUMA)
    }

    fun cancelar() {
        _state.value = _state.value.copy(confirmar = null)
    }

    fun confirmar() {

        val acao = _state.value.confirmar ?: return
        _state.value = _state.value.copy(confirmar = null, aTrabalhar = true)
        viewModelScope.launch {
            val r = when (acao) {
                DemoAcao.LIGAR -> demo.ligar(todayEpochDay())
                DemoAcao.DESLIGAR -> demo.desligar()
            }
            val mensagem = when (r) {
                is DemoResult.Ligado -> DemoMessage.LIGOU
                is DemoResult.Desligado -> DemoMessage.DESLIGOU
                is DemoResult.RecusadoPorDadosReais -> DemoMessage.RECUSOU_DADOS_REAIS
                is DemoResult.Falhou -> DemoMessage.ERRO
            }

            recontar()
            _state.value = _state.value.copy(
                aTrabalhar = false,
                mensagem = mensagem,
                linhasReais = (r as? DemoResult.RecusadoPorDadosReais)?.linhas ?: 0,
            )
        }
    }
}
