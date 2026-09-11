package pt.antares.app.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
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
 *
 * Dois tamanhos desde a 2.32.1: o [cartao] ocupa a largura da coluna, e o [pequeno] metade.
 * Os pequenos **seguidos** vão de dois em dois na mesma linha; um pequeno sozinho fica com
 * metade e deixa a outra vazia, em vez de esticar e fingir que é grande.
 */
class GrelhaDeCartoesScope internal constructor() {
    internal val itens = mutableListOf<Item>()

    fun cartao(content: @Composable () -> Unit) {
        itens += Item(content, pequeno = false)
    }

    fun pequeno(content: @Composable () -> Unit) {
        itens += Item(content, pequeno = true)
    }

    internal class Item(val content: @Composable () -> Unit, val pequeno: Boolean)
}

/**
 * Os blocos que a grelha distribui: um cartão grande é um bloco, e cada par de pequenos
 * seguidos é outro. É sobre blocos, e não sobre cartões, que as colunas alternam — senão os
 * dois pequenos de um par iam cada um para seu lado numa janela larga.
 */
internal fun blocosDaGrelha(itens: List<GrelhaDeCartoesScope.Item>): List<List<GrelhaDeCartoesScope.Item>> {
    val blocos = mutableListOf<List<GrelhaDeCartoesScope.Item>>()
    var par = mutableListOf<GrelhaDeCartoesScope.Item>()
    for (item in itens) {
        if (item.pequeno) {
            par += item
            if (par.size == 2) { blocos += par; par = mutableListOf() }
        } else {
            if (par.isNotEmpty()) { blocos += par; par = mutableListOf() }
            blocos += listOf(item)
        }
    }
    if (par.isNotEmpty()) blocos += par
    return blocos
}

/**
 * Cartões em coluna no telemóvel, em duas colunas quando a janela dá. Os blocos vão a
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
    val blocos = blocosDaGrelha(GrelhaDeCartoesScope().apply(conteudo).itens)

    // Mede a caixa e não a janela, como na [ListaAdaptavel]: se estes cartões forem um dia
    // para dentro de um painel, a conta tem de ser sobre o espaço que eles têm mesmo.
    BoxWithConstraints(modifier = modifier) {
        val colunas = colunasDeCartoes(larguraDaJanela(maxWidth.value.toInt()))

        Column(verticalArrangement = Arrangement.spacedBy(espaco)) {
            cabecalho?.invoke()

            if (colunas <= 1) {
                blocos.forEach { Bloco(it, espaco) }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(espaco)) {
                    for (coluna in 0 until colunas) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(espaco),
                        ) {
                            blocos
                                .filterIndexed { indice, _ -> indice % colunas == coluna }
                                .forEach { Bloco(it, espaco) }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Um bloco: o cartão grande tal como vem, ou uma linha de dois pequenos com a mesma altura —
 * dois cartões lado a lado com alturas diferentes liam-se como um erro de alinhamento.
 */
@Composable
private fun Bloco(bloco: List<GrelhaDeCartoesScope.Item>, espaco: Dp) {
    val primeiro = bloco.first()
    if (!primeiro.pequeno) {
        primeiro.content()
        return
    }
    Row(
        modifier = Modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(espaco),
    ) {
        for (item in bloco) {
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) { item.content() }
        }
        if (bloco.size == 1) Spacer(Modifier.weight(1f))
    }
}
