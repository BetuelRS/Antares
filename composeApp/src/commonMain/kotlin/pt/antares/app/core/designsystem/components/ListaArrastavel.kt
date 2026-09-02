package pt.antares.app.core.designsystem.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import kotlin.math.abs

/**
 * Uma lista que se reordena a arrastar.
 *
 * **Porquê aqui e não uma biblioteca:** a app não traz nenhuma dependência para isto, e o
 * caso é pequeno — uma rotina tem meia dúzia de exercícios. Mover o sexto para primeiro
 * custava **cinco toques** nas setas ▲▼, com a lista a saltar debaixo do dedo em cada um.
 *
 * **Alturas medidas e não assumidas.** Os cartões não têm todos a mesma altura — um nome de
 * exercício pode ocupar duas linhas —, e por isso cada um diz a sua ao ser desenhado. A troca
 * acontece quando o dedo passa **metade** do vizinho na direção em que vai, que é a regra que
 * faz a lista parecer que segue a mão em vez de saltar.
 *
 * **Grava-se uma vez, no fim.** Enquanto o dedo anda, a ordem só muda em memória; a base leva
 * uma escrita quando ele levanta. Arrastar por cinco posições eram cinco trocas gravadas.
 *
 * @param chaves a identidade de cada linha, pela ordem atual.
 * @param aoLargar recebe a ordem nova. Só é chamado se ela for diferente da que entrou.
 */
@Composable
fun <T> ListaArrastavel(
    itens: List<T>,
    chave: (T) -> String,
    espaco: Dp,
    aoLargar: (List<String>) -> Unit,
    linha: @Composable (item: T, aArrastar: Boolean) -> Unit,
) {
    // A ordem que o dedo está a desenhar. Fora de um arrasto é sempre a que veio de fora —
    // senão uma alteração vinda da base (apagar um exercício noutro sítio) ficava por ver.
    var ordemLocal by remember(itens.map(chave)) { mutableStateOf(itens) }
    var indiceArrastado by remember { mutableStateOf<Int?>(null) }
    var deslocamento by remember { mutableStateOf(0f) }
    val alturas = remember { mutableStateMapOf<String, Int>() }
    val espacoPx = with(LocalDensity.current) { espaco.toPx() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(espaco),
    ) {
        ordemLocal.forEachIndexed { indice, item ->
            val id = chave(item)
            val aArrastar = indiceArrastado == indice
            Column(
                modifier = Modifier
                    .onSizeChanged { alturas[id] = it.height }
                    // Só o que está a ser arrastado sobe: sem isto, o cartão passava por
                    // baixo dos vizinhos e desaparecia a meio do gesto.
                    .zIndex(if (aArrastar) 1f else 0f)
                    .graphicsLayer {
                        translationY = if (aArrastar) deslocamento else 0f
                        // O que está na mão cresce um cabelo e fica um pouco translucido: sem
                        // um sinal qualquer, arrastar não se distingue de percorrer a lista.
                        val escala = if (aArrastar) LEVANTADO else 1f
                        scaleX = escala
                        scaleY = escala
                        alpha = if (aArrastar) OPACIDADE_A_ARRASTAR else 1f
                    }
                    .pointerInput(id, ordemLocal.size) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                indiceArrastado = ordemLocal.indexOfFirst { chave(it) == id }
                                deslocamento = 0f
                            },
                            onDragEnd = {
                                indiceArrastado = null
                                deslocamento = 0f
                                val nova = ordemLocal.map(chave)
                                if (nova != itens.map(chave)) aoLargar(nova)
                            },
                            onDragCancel = {
                                indiceArrastado = null
                                deslocamento = 0f
                                ordemLocal = itens
                            },
                            onDrag = { mudanca, delta ->
                                mudanca.consume()
                                deslocamento += delta.y
                                val de = indiceArrastado ?: return@detectDragGesturesAfterLongPress
                                val passou = trocaPossivel(ordemLocal, chave, alturas, de, deslocamento, espacoPx)
                                if (passou != null) {
                                    val (para, alturaVizinho) = passou
                                    ordemLocal = ordemLocal.toMutableList().also {
                                        it.add(para, it.removeAt(de))
                                    }
                                    indiceArrastado = para
                                    // O deslocamento perde a altura do vizinho por onde
                                    // passou: sem isto, o cartão ficava a acumular e voava.
                                    deslocamento -= alturaVizinho
                                }
                            },
                        )
                    },
            ) {
                linha(item, aArrastar)
            }
        }
    }
}

private const val LEVANTADO = 1.02f
private const val OPACIDADE_A_ARRASTAR = 0.92f

/**
 * O vizinho por onde o dedo já passou, se passou.
 *
 * Devolve o índice novo e a altura que o deslocamento tem de perder — com o espaço entre
 * cartões incluído, porque ele também se percorre.
 */
private fun <T> trocaPossivel(
    ordem: List<T>,
    chave: (T) -> String,
    alturas: Map<String, Int>,
    de: Int,
    deslocamento: Float,
    espacoPx: Float,
): Pair<Int, Float>? {
    val direcao = if (deslocamento > 0) 1 else -1
    val vizinho = de + direcao
    if (vizinho !in ordem.indices) return null
    // O espaço entre cartões percorre-se também: sem ele, a troca acontecia um pouco
    // antes do que o olho vê.
    val altura = (alturas[chave(ordem[vizinho])]?.toFloat() ?: return null) + espacoPx
    // Metade do vizinho: é aí que o olho já o considera ultrapassado.
    if (abs(deslocamento) < altura / 2) return null
    return vizinho to altura * direcao
}
