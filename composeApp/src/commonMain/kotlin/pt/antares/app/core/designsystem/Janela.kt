package pt.antares.app.core.designsystem

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * O tamanho da janela, em classes e não em números. A app esteve trancada em retrato desde
 * sempre; ao destrancar, a mesma composição passa a ter de servir um telemóvel de 360 dp
 * deitado e um tablet de 1200 dp.
 *
 * As fronteiras são as do Material 3 — 600 dp e 840 dp de largura, 480 dp de altura — e não
 * medidas inventadas: são as mesmas que o Android usa para decidir que recursos carrega, e
 * afastar-se delas faria a app discordar do sistema no mesmo ecrã.
 *
 * Mede-se a janela e não o dispositivo. Em ecrã dividido, metade de um tablet é uma janela
 * compacta, e é a janela que a pessoa vê.
 */
enum class LarguraDaJanela { COMPACTA, MEDIA, LARGA }

/**
 * Só duas: o que muda com a altura é se cabe uma barra em baixo. Um telemóvel deitado tem
 * menos de 480 dp de alto, e aí cada linha de conteúdo perdida conta.
 */
enum class AlturaDaJanela { BAIXA, NORMAL }

/**
 * Os três modos do plano. Não são estilos à escolha: cada um responde a uma largura, e é o
 * ecrã que decide o que faz com o modo que recebe — uma lista ganha detalhe ao lado, um
 * formulário ganha só margem.
 */
enum class ModoDeEsquema { UMA_COLUNA, LISTA_E_DETALHE, DUAS_COLUNAS }

const val LARGURA_MEDIA_DP = 600
const val LARGURA_LARGA_DP = 840
const val ALTURA_BAIXA_DP = 480

/** A largura de leitura confortável. Acima disto o olho perde a linha ao voltar à esquerda. */
const val LARGURA_DE_LEITURA_DP = 640

fun larguraDaJanela(dp: Int): LarguraDaJanela = when {
    dp >= LARGURA_LARGA_DP -> LarguraDaJanela.LARGA
    dp >= LARGURA_MEDIA_DP -> LarguraDaJanela.MEDIA
    else -> LarguraDaJanela.COMPACTA
}

fun alturaDaJanela(dp: Int): AlturaDaJanela =
    if (dp < ALTURA_BAIXA_DP) AlturaDaJanela.BAIXA else AlturaDaJanela.NORMAL

fun modoDeEsquema(largura: LarguraDaJanela): ModoDeEsquema = when (largura) {
    LarguraDaJanela.COMPACTA -> ModoDeEsquema.UMA_COLUNA
    LarguraDaJanela.MEDIA -> ModoDeEsquema.LISTA_E_DETALHE
    LarguraDaJanela.LARGA -> ModoDeEsquema.DUAS_COLUNAS
}

/**
 * Onde vive a navegação entre separadores. Ao lado em dois casos diferentes, e por razões
 * diferentes: numa janela larga porque uma barra esticada por 1200 dp põe os alvos longe do
 * polegar, e numa janela baixa porque uma barra em baixo come a altura que já falta.
 */
fun navegacaoAoLado(largura: LarguraDaJanela, altura: AlturaDaJanela): Boolean =
    largura != LarguraDaJanela.COMPACTA || altura == AlturaDaJanela.BAIXA

/**
 * O valor por omissão é o comportamento de hoje. Um ecrã composto fora do [ProvedorDaJanela]
 * — num teste de um cartão sozinho, por exemplo — continua a desenhar-se como sempre se
 * desenhou, em vez de rebentar ou de adivinhar mal.
 */
val LocalLarguraDaJanela = staticCompositionLocalOf { LarguraDaJanela.COMPACTA }
val LocalAlturaDaJanela = staticCompositionLocalOf { AlturaDaJanela.NORMAL }

val LocalModoDeEsquema = staticCompositionLocalOf { ModoDeEsquema.UMA_COLUNA }

/**
 * Mede a janela uma vez, no arranque, e serve as três classes a toda a árvore. Sem isto
 * cada ecrã teria o seu `BoxWithConstraints`, e mediria a caixa onde está e não a janela —
 * uma lista dentro de uma coluna estreita concluiria «compacta» num tablet.
 */
@Composable
fun ProvedorDaJanela(content: @Composable () -> Unit) {
    BoxWithConstraints {
        val largura = larguraDaJanela(maxWidth.emDp())
        val altura = alturaDaJanela(maxHeight.emDp())
        CompositionLocalProvider(
            LocalLarguraDaJanela provides largura,
            LocalAlturaDaJanela provides altura,
            LocalModoDeEsquema provides modoDeEsquema(largura),
            content = content,
        )
    }
}

// `Dp.value` é `Float` e as fronteiras são inteiras; arredondar aqui evita que 599,7 dp
// caia de um lado nas contas e do outro no que o sistema decidiu.
private fun Dp.emDp(): Int = value.toInt()

/**
 * Trava a largura de um formulário ou de um texto corrido e centra-o. Num tablet, um campo
 * de texto com 1200 dp de largura é impossível de ler e ridículo de preencher; e o olho
 * perde a linha ao voltar à esquerda muito antes disso.
 *
 * O `wrapContentWidth` no meio é o que centra: dá ao conteúdo largura mínima zero dentro da
 * largura toda, e o `widthIn` a seguir é que decide onde ele para. Sem ele, o conteúdo ficava
 * encostado à esquerda com um vazio à direita.
 *
 * Num telemóvel não faz nada — 360 dp nunca chega ao teto.
 */
fun Modifier.larguraDeLeitura(): Modifier = this
    .fillMaxWidth()
    .wrapContentWidth()
    .widthIn(max = LARGURA_DE_LEITURA_DP.dp)
