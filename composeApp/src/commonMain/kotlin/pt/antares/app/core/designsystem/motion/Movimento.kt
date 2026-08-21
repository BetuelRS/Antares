package pt.antares.app.core.designsystem.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset

/**
 * O vocabulário de movimento da app.
 *
 * Antes da 2.3.0 a app trocava de ecrã sem animação nenhuma: corte seco. Não é neutro — é a
 * app a não dizer para onde se foi, e quem não sabe para onde foi não sabe como voltar.
 *
 * Cada movimento diz uma coisa diferente sobre a relação entre os dois ecrãs, e é por isso
 * que não há um só para tudo. Um separador não é «mais fundo» do que outro; um leitor de
 * código de barras não é um irmão do Diário.
 */
enum class Movimento {

    /**
     * Separadores. Não há hierarquia entre eles: ninguém está mais fundo do que ninguém, e
     * por isso não há direcção — desvanece com um levantar de oito pontos, e nada desliza.
     * Deslizar entre irmãos daria a ideia falsa de que se está a percorrer uma sequência.
     */
    ENTRE_IRMAOS,

    /**
     * Entrar num detalhe. O ecrã novo entra da direita a toda a velocidade e o antigo sai
     * para a esquerda a **um terço** dela, escurecendo — é a paralaxe que faz os dois
     * lerem-se como folhas empilhadas em vez de dois diapositivos a trocar. Voltar é
     * exactamente o mesmo ao contrário.
     */
    MAIS_FUNDO,

    /**
     * Um modo em que se entra e de que se sai: a câmara, o leitor de códigos. Sobe de baixo
     * com um toque de escala, como uma folha que se puxa por cima da mesa. A gramática de
     * quem vê isto já sabe que se fecha para baixo.
     */
    DE_BAIXO,

    /**
     * Mergulhar numa sessão — um treino a decorrer, uma corrida, um jejum. O ecrã novo cresce
     * de dentro e o antigo recua para trás dele em vez de sair de lado. Não se está a navegar:
     * está-se a entrar noutro estado da app, e o movimento tem de o dizer.
     */
    MERGULHO,

    /**
     * Chegar a um resultado: o resumo de um treino, o de uma corrida. Sobe de baixo e
     * **assenta** — a curva tem um travar no fim, para o número final aterrar em vez de
     * aparecer. É o único movimento com peso, e é de propósito: acontece uma vez por sessão.
     */
    RESULTADO,
}

/**
 * As curvas. São as do Material 3 escritas à mão porque a biblioteca não as expõe todas, e
 * porque uma curva com nome é uma decisão — `FastOutSlowIn` para tudo é a ausência de uma.
 */
object Curvas {

    /** Arranca depressa e trava muito no fim. É a curva de quem entra e fica. */
    val ACENTUADA: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** O contrário: sai a acelerar. Quem sai não precisa de ser acompanhado até ao fim. */
    val ACENTUADA_A_SAIR: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /** Simétrica e discreta, para o que só desvanece. */
    val PADRAO: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Passa do destino e volta. Só o [Movimento.RESULTADO] a usa, uma vez por sessão. */
    val ASSENTA: Easing = CubicBezierEasing(0.2f, 1.2f, 0.3f, 1f)
}

/**
 * As durações. Três, com nome, e nenhuma escrita à mão nos ecrãs.
 *
 * O limite de cima é 400 ms: acima disso deixa de se ler como resposta ao dedo e passa a
 * ler-se como espera. O de baixo é 150 ms, abaixo do qual o olho não chega a ver o
 * movimento e o efeito é o mesmo que não haver nenhum — com o custo de o haver.
 */
object Duracoes {
    const val RAPIDA = 180
    const val NORMAL = 280
    const val CALMA = 380
}

internal fun aparecer(duracao: Int = Duracoes.NORMAL, atraso: Int = 0) =
    tween<Float>(durationMillis = duracao, delayMillis = atraso, easing = Curvas.ACENTUADA)

internal fun desaparecer(duracao: Int = Duracoes.RAPIDA) =
    tween<Float>(durationMillis = duracao, easing = Curvas.ACENTUADA_A_SAIR)

internal fun deslizar(duracao: Int = Duracoes.NORMAL, curva: Easing = Curvas.ACENTUADA) =
    tween<IntOffset>(durationMillis = duracao, easing = curva)
