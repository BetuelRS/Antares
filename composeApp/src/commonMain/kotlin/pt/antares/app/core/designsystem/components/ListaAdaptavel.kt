package pt.antares.app.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import pt.antares.app.core.designsystem.LarguraDaJanela
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.larguraDaJanela

/**
 * Quantas colunas cabem numa lista. Uma lista de linhas curtas — um exercício, uma corrida,
 * uma pesagem — esticada por 1200 dp fica com um nome à esquerda, um número à direita e um
 * palmo de nada no meio; e obriga a percorrer três vezes mais para ver o mesmo.
 *
 * Três é o teto e não uma escala: acima disso cada coluna fica estreita de mais para o nome
 * de um alimento, que é o texto mais comprido que estas listas mostram.
 */
fun colunasDaLista(largura: LarguraDaJanela): Int = when (largura) {
    LarguraDaJanela.COMPACTA -> 1
    LarguraDaJanela.MEDIA -> 2
    LarguraDaJanela.LARGA -> 3
}

/**
 * Uma lista que ganha colunas com a janela. Numa janela compacta é exatamente uma
 * `LazyColumn` — uma coluna, o mesmo espaçamento — e por isso trocar uma pela outra não muda
 * nada no telemóvel de pé.
 *
 * O que estava em `item { }` e é um cabeçalho, um filtro ou um total passa a [linhaInteira]:
 * num grid, um `item` normal ocupa uma célula, e um título de secção ficaria encavalitado ao
 * lado do primeiro resultado.
 */
@Composable
fun ListaAdaptavel(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(Spacing.lg),
    espaco: Dp = Spacing.md,
    state: LazyGridState = rememberLazyGridState(),
    content: LazyGridScope.() -> Unit,
) {
    // Mede a caixa onde está, e não a janela. A diferença aparece dentro do
    // [PainelDeListaEDetalhe]: a lista fica com dois quintos do tablet, e a contar pela
    // janela desenhava três colunas em 682 dp — nomes partidos em três linhas ao lado de
    // miniaturas espremidas. Visto no emulador, não nos testes.
    //
    // O casco continua a decidir-se pela janela: onde vive a navegação é uma pergunta sobre
    // o ecrã todo, e o número de colunas de uma lista é uma pergunta sobre a lista.
    BoxWithConstraints(modifier = modifier) {
        val colunas = colunasDaLista(larguraDaJanela(maxWidth.value.toInt()))
        LazyVerticalGrid(
            columns = GridCells.Fixed(colunas),
            modifier = Modifier.fillMaxSize(),
            state = state,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(espaco),
            horizontalArrangement = Arrangement.spacedBy(espaco),
            content = content,
        )
    }
}

/**
 * Um item que atravessa a linha toda, seja qual for o número de colunas. O `maxLineSpan` é
 * lido no próprio grid, e não das colunas contadas à parte: são o mesmo número, mas contá-lo
 * duas vezes é ter dois sítios para o errar.
 */
fun LazyGridScope.linhaInteira(
    key: Any? = null,
    content: @Composable () -> Unit,
) = item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }
