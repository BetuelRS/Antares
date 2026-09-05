package pt.antares.app.core.designsystem.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import pt.antares.app.core.util.rememberImageSharer

/**
 * Um cartão que se pode partilhar como imagem: o modificador que o grava, e a acção que o
 * envia.
 *
 * O [modifier] põe-se no cartão a partilhar; a [partilhar] chama-se do botão.
 */
class CartaoPartilhavel(
    val modifier: Modifier,
    val partilhar: () -> Unit,
)

/**
 * Grava o cartão **só quando alguém o quer partilhar**, e não a cada desenho.
 *
 * O cartão de partilha do Progresso fazia `camada.record { }` dentro do `drawWithContent`
 * sem condição nenhuma: cada composição do ecrã — cada rolagem, cada mudança de período —
 * gravava a camada inteira para uma acção que se faz uma vez por mês. É o defeito concreto 3
 * da `estudo/areas/14-progresso.md`, e a razão de esta função existir é não haver duas
 * maneiras de partilhar um cartão na mesma app: agora há uma, e a correcção está num sítio só.
 *
 * **As duas esperas por moldura não são cerimónia.** Pedir a imagem no mesmo instante em que
 * se liga a gravação devolve uma camada vazia — a bandeira muda no fim da composição, o
 * desenho vem depois dela, e é o desenho que enche a camada. A primeira espera devolve antes
 * dessa passagem de desenho; a segunda já é depois.
 */
@Composable
fun rememberCartaoPartilhavel(ficheiro: String): CartaoPartilhavel {
    val camada = rememberGraphicsLayer()
    val partilhador = rememberImageSharer()
    var aGravar by remember { mutableStateOf(false) }

    LaunchedEffect(aGravar) {
        if (!aGravar) return@LaunchedEffect

        // O `finally` desliga a gravação **haja o que houver**. Sem ele, uma partilha que
        // rebente — ou o ecrã que saia a meio — deixava a bandeira levantada, e o cartão
        // voltava a gravar a camada a cada desenho: o defeito que este ficheiro corrige.
        try {
            withFrameNanos { }
            withFrameNanos { }
            partilhador(ficheiro, camada.toImageBitmap())
        } finally {
            aGravar = false
        }
    }

    return CartaoPartilhavel(
        modifier = Modifier.drawWithContent {
            // A leitura da bandeira acontece aqui dentro, na fase de desenho: mudá-la manda
            // redesenhar este cartão e mais nada.
            if (aGravar) {
                camada.record { this@drawWithContent.drawContent() }
                drawLayer(camada)
            } else {
                drawContent()
            }
        },
        partilhar = { aGravar = true },
    )
}
