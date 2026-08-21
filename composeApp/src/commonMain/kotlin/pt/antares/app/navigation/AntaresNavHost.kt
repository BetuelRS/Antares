package pt.antares.app.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import pt.antares.app.core.designsystem.motion.MovimentoDasRotas
import pt.antares.app.core.designsystem.motion.Transicoes
import pt.antares.app.core.designsystem.motion.animacoesLigadas

/**
 * O grafo de navegação, por áreas.
 *
 * Os ecrãs não navegam: recebem funções e chamam-nas, e por isso nenhum deles conhece
 * outro. Quem quiser saber o que leva a onde lê estes ficheiros e mais nenhum.
 *
 * Eram 530 linhas num só. A regra de arrumação é a área do ecrã, e não o tipo de rota:
 * quem vai mexer no diário quer as rotas do diário à vista, e não espalhadas por uma lista
 * ordenada por outra coisa qualquer.
 *
 * **O movimento é decidido aqui e não em cada rota.** Um movimento descreve a relação entre
 * dois ecrãs, e uma relação não se lê olhando para um lado só — quem entra pergunta pelo
 * destino, quem sai pergunta pelo destino também, porque é o destino que define o degrau.
 */
@Composable
fun AntaresNavHost(
    navController: NavHostController,
    startDestination: Route,
) {
    // Quem desligou as animações no sistema não as recebe. Não é uma preferência da app: é a
    // do aparelho, e ignorá-la seria decidir por quem tem enjoo com movimento.
    val comMovimento = animacoesLigadas()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            if (!comMovimento) EnterTransition.None
            else Transicoes.entrada(MovimentoDasRotas.de(targetState.destination.route), this)
        },
        exitTransition = {
            if (!comMovimento) ExitTransition.None
            else Transicoes.saida(MovimentoDasRotas.de(targetState.destination.route), this)
        },
        popEnterTransition = {
            if (!comMovimento) EnterTransition.None
            else Transicoes.entradaAoVoltar(
                // Ao voltar, o degrau que se desfaz é o do ecrã que está a **sair**: é ele
                // que estava mais fundo. Perguntar pelo destino aqui daria o movimento do
                // ecrã para onde se regressa, que não é o que se está a desfazer.
                MovimentoDasRotas.de(initialState.destination.route),
                this,
            )
        },
        popExitTransition = {
            if (!comMovimento) ExitTransition.None
            else Transicoes.saidaAoVoltar(MovimentoDasRotas.de(initialState.destination.route), this)
        },
    ) {
        rotasDeComida(navController)
        rotasDeCodigoDeBarras(navController)
        rotasDeReceitas(navController)
        rotasDeTreino(navController)
        rotasDeCorrida(navController)
        rotasDePerfil(navController)
        rotasDaApp(navController)
    }
}
