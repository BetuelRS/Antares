package pt.antares.app.core.designsystem.motion

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.graphics.TransformOrigin

/**
 * As transições, escritas a partir do [Movimento].
 *
 * Duas regras atravessam tudo o que está aqui:
 *
 * **Voltar é o movimento ao contrário, e não outro movimento.** Se entrar desliza da direita,
 * sair desliza para a direita. Uma app onde o gesto de voltar faz uma coisa diferente da que
 * desfaz é uma app onde ninguém aprende o gesto.
 *
 * **Quem sai move-se menos do que quem entra.** Um terço, nos movimentos com direcção. É a
 * paralaxe, e é o que faz dois ecrãs lerem-se como folhas empilhadas em vez de dois
 * diapositivos a trocar de lugar. Sem ela o movimento existe e não significa nada.
 */
object Transicoes {

    /** Um terço. A folha de baixo acompanha, não desfila. */
    private const val PARALAXE = 3

    // Quem sai não desaparece: escurece o suficiente para o olho saber qual é o de cima, e
    // não tanto que pareça outra coisa. Um quarto de opacidade é onde isso acontece.
    private const val SOMBRA_DE_QUEM_SAI = 0.75f

    fun entrada(movimento: Movimento, scope: AnimatedContentTransitionScope<*>): EnterTransition =
        when (movimento) {
            Movimento.ENTRE_IRMAOS ->
                fadeIn(aparecer(Duracoes.RAPIDA)) +
                    slideInVertically(deslizar(Duracoes.RAPIDA)) { LEVANTAR }

            Movimento.MAIS_FUNDO ->
                scope.deslizaDe(Lado.DIREITA) + fadeIn(aparecer(Duracoes.RAPIDA))

            Movimento.DE_BAIXO ->
                slideInVertically(deslizar(Duracoes.NORMAL)) { alturaTotal -> alturaTotal } +
                    fadeIn(aparecer(Duracoes.RAPIDA)) +
                    scaleIn(aparecer(), initialScale = ESCALA_DE_FOLHA)

            Movimento.MERGULHO ->
                scaleIn(aparecer(Duracoes.CALMA), initialScale = ESCALA_DE_MERGULHO) +
                    fadeIn(aparecer(Duracoes.NORMAL))

            Movimento.RESULTADO ->
                slideInVertically(
                    deslizar(Duracoes.CALMA, Curvas.ASSENTA),
                ) { alturaTotal -> alturaTotal / 2 } +
                    fadeIn(aparecer(Duracoes.NORMAL)) +
                    scaleIn(
                        aparecer(Duracoes.CALMA),
                        initialScale = ESCALA_DE_RESULTADO,
                        transformOrigin = TransformOrigin(PIVO_X, PIVO_Y_DE_BAIXO),
                    )
        }

    fun saida(movimento: Movimento, scope: AnimatedContentTransitionScope<*>): ExitTransition =
        when (movimento) {
            Movimento.ENTRE_IRMAOS ->
                fadeOut(desaparecer()) + slideOutVertically(deslizar(Duracoes.RAPIDA)) { -LEVANTAR }

            Movimento.MAIS_FUNDO ->
                scope.deslizaPara(Lado.ESQUERDA, comParalaxe = true) +
                    fadeOut(desaparecer(Duracoes.NORMAL), targetAlpha = SOMBRA_DE_QUEM_SAI)

            Movimento.DE_BAIXO, Movimento.RESULTADO ->
                fadeOut(desaparecer(), targetAlpha = SOMBRA_DE_QUEM_SAI)

            // Recua para trás em vez de sair de lado: quem mergulha deixa o outro ecrã atrás
            // de si, e não ao lado.
            Movimento.MERGULHO ->
                scaleOut(desaparecer(Duracoes.NORMAL), targetScale = ESCALA_DE_QUEM_RECUA) +
                    fadeOut(desaparecer(Duracoes.NORMAL))
        }

    /** Voltar: a entrada é a saída ao contrário, e a saída é a entrada ao contrário. */
    fun entradaAoVoltar(
        movimento: Movimento,
        scope: AnimatedContentTransitionScope<*>,
    ): EnterTransition = when (movimento) {
        Movimento.MAIS_FUNDO ->
            scope.deslizaDe(Lado.ESQUERDA, comParalaxe = true) + fadeIn(aparecer(Duracoes.RAPIDA))

        Movimento.MERGULHO ->
            scaleIn(aparecer(Duracoes.NORMAL), initialScale = ESCALA_DE_QUEM_RECUA) +
                fadeIn(aparecer(Duracoes.NORMAL))

        Movimento.DE_BAIXO, Movimento.RESULTADO -> fadeIn(aparecer(Duracoes.NORMAL))
        Movimento.ENTRE_IRMAOS -> entrada(movimento, scope)
    }

    fun saidaAoVoltar(
        movimento: Movimento,
        scope: AnimatedContentTransitionScope<*>,
    ): ExitTransition = when (movimento) {
        Movimento.MAIS_FUNDO -> scope.deslizaPara(Lado.DIREITA) + fadeOut(desaparecer())

        Movimento.DE_BAIXO ->
            slideOutVertically(deslizar(Duracoes.NORMAL)) { alturaTotal -> alturaTotal } +
                fadeOut(desaparecer(Duracoes.NORMAL))

        Movimento.RESULTADO ->
            slideOutVertically(deslizar(Duracoes.NORMAL)) { alturaTotal -> alturaTotal / 2 } +
                fadeOut(desaparecer(Duracoes.NORMAL))

        Movimento.MERGULHO ->
            scaleOut(desaparecer(Duracoes.NORMAL), targetScale = ESCALA_DE_MERGULHO) +
                fadeOut(desaparecer(Duracoes.NORMAL))

        Movimento.ENTRE_IRMAOS -> saida(movimento, scope)
    }

    private enum class Lado { ESQUERDA, DIREITA }

    private fun AnimatedContentTransitionScope<*>.deslizaDe(
        lado: Lado,
        comParalaxe: Boolean = false,
    ): EnterTransition {
        val divisor = if (comParalaxe) PARALAXE else 1
        return slideIntoContainer(
            towards = if (lado == Lado.DIREITA) {
                AnimatedContentTransitionScope.SlideDirection.Left
            } else {
                AnimatedContentTransitionScope.SlideDirection.Right
            },
            animationSpec = deslizar(),
            initialOffset = { largura -> if (lado == Lado.DIREITA) largura else -largura / divisor },
        )
    }

    private fun AnimatedContentTransitionScope<*>.deslizaPara(
        lado: Lado,
        comParalaxe: Boolean = false,
    ): ExitTransition {
        val divisor = if (comParalaxe) PARALAXE else 1
        return slideOutOfContainer(
            towards = if (lado == Lado.ESQUERDA) {
                AnimatedContentTransitionScope.SlideDirection.Left
            } else {
                AnimatedContentTransitionScope.SlideDirection.Right
            },
            animationSpec = deslizar(Duracoes.NORMAL, Curvas.PADRAO),
            targetOffset = { largura -> if (lado == Lado.ESQUERDA) -largura / divisor else largura },
        )
    }

    // Oito pontos. Chega para o olho ver que houve movimento e não chega para parecer que o
    // ecrã veio de outro sítio — que é exactamente o que não se quer entre irmãos.
    private const val LEVANTAR = 8

    private const val ESCALA_DE_FOLHA = 0.96f
    private const val ESCALA_DE_MERGULHO = 0.92f
    private const val ESCALA_DE_QUEM_RECUA = 1.04f
    private const val ESCALA_DE_RESULTADO = 0.94f
    private const val PIVO_X = 0.5f
    private const val PIVO_Y_DE_BAIXO = 1f
}
