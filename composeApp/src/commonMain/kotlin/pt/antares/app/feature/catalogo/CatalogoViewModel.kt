package pt.antares.app.feature.catalogo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.antares.app.core.catalogo.ActualizadorDoCatalogo
import pt.antares.app.core.catalogo.ManifestoDoCatalogo
import pt.antares.app.core.catalogo.ProcuraDeCatalogo
import pt.antares.app.core.catalogo.ResultadoDaActualizacao

/**
 * O que o cartão tem para dizer. Um recado de cada vez: quem procura outra vez apaga o
 * anterior, e não há histórico nenhum a guardar.
 */
data class EstadoDoCatalogo(
    val instalada: Int = 0,
    val ocupado: Boolean = false,
    val aDescarregar: Boolean = false,
    val novidade: ManifestoDoCatalogo? = null,
    val recado: RecadoDoCatalogo? = null,

    /**
     * Quantos alimentos entraram na última instalação. Guardado à parte do [novidade],
     * que desaparece assim que ela acontece — sem isto, a frase de sucesso dizia zero.
     */
    val alimentosInstalados: Int = 0,
)

/**
 * O que aconteceu, para o ecrã escolher a frase. É um tipo e não um texto porque as frases
 * mudam de idioma e o motivo não — e porque «erro» não é resposta para quem está a olhar.
 */
enum class RecadoDoCatalogo {
    EM_DIA,
    INSTALADO,
    SEM_RESPOSTA,
    RESUMO_NAO_BATE,
    FICHEIRO_ILEGIVEL,
    NAO_AVANCA,
    NAO_SE_GUARDOU,
}

/**
 * Duas ações, e nenhuma delas acontece sozinha: procurar — que só pede o manifesto — e
 * descarregar, que é quando os cinco megabytes saem da rede.
 *
 * A separação é a decisão de privacidade tomada nesta versão: a app nunca vai à rede por
 * causa do catálogo sem alguém lhe pedir, e a pessoa vê o tamanho do que vai buscar antes
 * de o mandar buscar.
 */
class CatalogoViewModel(
    private val actualizador: ActualizadorDoCatalogo,
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoDoCatalogo())
    val estado: StateFlow<EstadoDoCatalogo> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            _estado.update { it.copy(instalada = actualizador.versaoInstalada()) }
        }
    }

    fun procurar() {
        if (_estado.value.ocupado) return
        _estado.update { it.copy(ocupado = true, recado = null, novidade = null) }
        viewModelScope.launch {
            val procura = actualizador.procurar()
            _estado.update {
                when (procura) {
                    is ProcuraDeCatalogo.Ha ->
                        it.copy(instalada = procura.instalada, novidade = procura.manifesto)
                    is ProcuraDeCatalogo.EmDia ->
                        it.copy(instalada = procura.instalada, recado = RecadoDoCatalogo.EM_DIA)
                    ProcuraDeCatalogo.SemResposta ->
                        it.copy(recado = RecadoDoCatalogo.SEM_RESPOSTA)
                }.copy(ocupado = false)
            }
        }
    }

    fun descarregar() {
        val manifesto = _estado.value.novidade ?: return
        if (_estado.value.ocupado) return
        _estado.update { it.copy(ocupado = true, aDescarregar = true, recado = null) }
        viewModelScope.launch {
            val resultado = actualizador.instalar(manifesto)
            _estado.update {
                it.copy(
                    ocupado = false,
                    aDescarregar = false,
                    alimentosInstalados = (resultado as? ResultadoDaActualizacao.Instalado)
                        ?.alimentos ?: it.alimentosInstalados,
                    // A novidade só desaparece quando entrou. Se falhou, fica à vista para
                    // se poder tentar outra vez sem voltar a procurar.
                    novidade = if (resultado is ResultadoDaActualizacao.Instalado) null else it.novidade,
                    instalada = (resultado as? ResultadoDaActualizacao.Instalado)?.versao ?: it.instalada,
                    recado = recadoDe(resultado),
                )
            }
        }
    }

    private fun recadoDe(resultado: ResultadoDaActualizacao): RecadoDoCatalogo = when (resultado) {
        is ResultadoDaActualizacao.Instalado -> RecadoDoCatalogo.INSTALADO
        ResultadoDaActualizacao.SemResposta -> RecadoDoCatalogo.SEM_RESPOSTA
        ResultadoDaActualizacao.ResumoNaoBate -> RecadoDoCatalogo.RESUMO_NAO_BATE
        ResultadoDaActualizacao.FicheiroIlegivel -> RecadoDoCatalogo.FICHEIRO_ILEGIVEL
        ResultadoDaActualizacao.NaoAvanca -> RecadoDoCatalogo.NAO_AVANCA
        ResultadoDaActualizacao.NaoSeGuardou -> RecadoDoCatalogo.NAO_SE_GUARDOU
    }
}
