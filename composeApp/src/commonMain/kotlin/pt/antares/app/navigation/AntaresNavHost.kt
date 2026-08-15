package pt.antares.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost

/**
 * O grafo de navegação, por áreas.
 *
 * Os ecrãs não navegam: recebem funções e chamam-nas, e por isso nenhum deles conhece
 * outro. Quem quiser saber o que leva a onde lê estes ficheiros e mais nenhum.
 *
 * Eram 530 linhas num só. A regra de arrumação é a área do ecrã, e não o tipo de rota:
 * quem vai mexer no diário quer as rotas do diário à vista, e não espalhadas por uma lista
 * ordenada por outra coisa qualquer.
 */
@Composable
fun AntaresNavHost(
    navController: NavHostController,
    startDestination: Route,
) {
    NavHost(navController = navController, startDestination = startDestination) {
        rotasDeComida(navController)
        rotasDeCodigoDeBarras(navController)
        rotasDeReceitas(navController)
        rotasDeTreino(navController)
        rotasDeCorrida(navController)
        rotasDePerfil(navController)
        rotasDaApp(navController)
    }
}
