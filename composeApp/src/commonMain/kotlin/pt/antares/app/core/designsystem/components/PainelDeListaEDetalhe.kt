package pt.antares.app.core.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pt.antares.app.core.designsystem.LocalModoDeEsquema
import pt.antares.app.core.designsystem.ModoDeEsquema

/**
 * Se a lista e o detalhe cabem ao mesmo tempo. Numa janela compacta não cabem — 360 dp
 * partidos ao meio não servem nem para um nem para outro — e aí o detalhe continua a ser um
 * ecrã à parte, aberto por cima da lista.
 */
@Composable
fun cabeDetalheAoLado(): Boolean = LocalModoDeEsquema.current != ModoDeEsquema.UMA_COLUNA

/**
 * Lista à esquerda, detalhe à direita. É o terceiro modo do plano, e resolve a coisa mais
 * cansativa de uma janela larga: abrir um exercício, voltar atrás, abrir o seguinte, voltar
 * atrás — quando havia espaço para os ver a todos sem sair da lista.
 *
 * A lista fica com dois quintos e o detalhe com três: a lista mostra nomes, e o detalhe
 * mostra imagens e instruções. Metade para cada um apertava o lado que precisa de espaço.
 *
 * Enquanto não houver nada escolhido, o lado direito diz o que fazer em vez de ficar branco.
 */
@Composable
fun PainelDeListaEDetalhe(
    lista: @Composable () -> Unit,
    detalhe: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
    vazio: @Composable () -> Unit = {},
) {
    Row(modifier = modifier.fillMaxSize()) {
        // Numa janela larga a lista para de crescer. Nomes de exercícios não ficam mais
        // legíveis com 480 dp do que com 340, e o espaço que sobra vale mais ao detalhe, que
        // tem imagens e instruções. É esta a diferença entre os dois modos com detalhe ao
        // lado — antes da 2.3.0 não havia nenhuma, e o `LISTA_E_DETALHE` e o `DUAS_COLUNAS`
        // desenhavam exatamente o mesmo.
        val ladoDaLista = when (LocalModoDeEsquema.current) {
            ModoDeEsquema.DUAS_COLUNAS -> Modifier.width(LARGURA_FIXA_DA_LISTA)
            else -> Modifier.weight(LADO_DA_LISTA)
        }
        Box(modifier = ladoDaLista) { lista() }
        VerticalDivider()
        Box(modifier = Modifier.weight(LADO_DO_DETALHE)) {
            if (detalhe != null) detalhe() else vazio()
        }
    }
}

private const val LADO_DA_LISTA = 2f
private const val LADO_DO_DETALHE = 3f

// Cabe um nome de exercício com marca e músculo sem partir, e não mais do que isso.
private val LARGURA_FIXA_DA_LISTA = 340.dp
