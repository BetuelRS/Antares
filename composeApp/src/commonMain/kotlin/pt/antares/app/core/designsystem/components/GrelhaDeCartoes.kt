package pt.antares.app.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import pt.antares.app.core.designsystem.LarguraDaJanela
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.larguraDaJanela

/**
 * Quantas colunas de cartões cabem. Param nas duas mesmo numa janela larga, e não é o mesmo
 * critério das listas: um cartão do Hoje leva um anel, três macros e um botão, e a um terço
 * de 1280 dp a fila dos macros parte-se.
 */
fun colunasDeCartoes(largura: LarguraDaJanela): Int =
    if (largura == LarguraDaJanela.COMPACTA) 1 else 2

/**
 * Recolhe os cartões antes de os desenhar. É o que permite distribuí-los por colunas: numa
 * `Column` normal a ordem é a da chamada, e aqui é preciso saber quantos são para decidir
 * quem vai para que lado.
 */
class GrelhaDeCartoesScope internal constructor() {
    internal val cartoes = mutableListOf<@Composable () -> Unit>()

    fun cartao(content: @Composable () -> Unit) {
        cartoes += content
    }
}

/**
 * Cartões em coluna no telemóvel, em duas colunas quando a janela dá. Os cartões vão a
 * alternar — o primeiro à esquerda, o segundo à direita, o terceiro à esquerda — e por isso
 * a ordem de importância continua a ler-se de cima para baixo em cada lado.
 *
 * Duas colunas independentes, e não uma linha por par: os cartões têm alturas diferentes, e
 * emparelhá-los deixava um buraco por baixo do mais baixo de cada par.
 *
 * O [cabecalho] fica sempre por cima e a toda a largura. É onde vive o que não é um cartão —
 * a barra de registo rápido, que é a primeira coisa que a pessoa toca.
 */
@Composable
fun GrelhaDeCartoes(
    modifier: Modifier = Modifier,
    espaco: Dp = Spacing.lg,
    cabecalho: (@Composable () -> Unit)? = null,
    conteudo: GrelhaDeCartoesScope.() -> Unit,
) {
    val scope = GrelhaDeCartoesScope().apply(conteudo)

    // Mede a caixa e não a janela, como na [ListaAdaptavel]: se estes cartões forem um dia
    // para dentro de um painel, a conta tem de ser sobre o espaço que eles têm mesmo.
    BoxWithConstraints(modifier = modifier) {
        val colunas = colunasDeCartoes(larguraDaJanela(maxWidth.value.toInt()))

        Column(verticalArrangement = Arrangement.spacedBy(espaco)) {
            cabecalho?.invoke()

            if (colunas <= 1) {
                scope.cartoes.forEach { it() }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(espaco)) {
                    for (coluna in 0 until colunas) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(espaco),
                        ) {
                            scope.cartoes
                                .filterIndexed { indice, _ -> indice % colunas == coluna }
                                .forEach { it() }
                        }
                    }
                }
            }
        }
    }
}
