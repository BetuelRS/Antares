package pt.antares.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.nav_diary
import pt.antares.app.generated.resources.nav_more
import pt.antares.app.generated.resources.nav_progress
import pt.antares.app.generated.resources.nav_today
import pt.antares.app.generated.resources.nav_workout

/**
 * Navegação entre separadores. As três opções resolvem o mesmo problema: sem elas, saltar
 * de separador em separador empilhava ecrãs sem fim, e o botão de voltar percorria toda a
 * história em vez de sair da app.
 *
 * `saveState` e `restoreState` são o que faz cada separador lembrar-se de onde ia — a
 * posição da lista, o dia aberto no diário.
 */
fun NavHostController.navigateToTab(route: Route) {
    navigate(route) {

        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private data class BottomBarItem(
    val route: Route,
    val icon: ImageVector,
    val label: StringResource,
)

private val bottomBarItems = listOf(
    BottomBarItem(Route.Today, Icons.Default.WbSunny, Res.string.nav_today),
    BottomBarItem(Route.Diary, Icons.Default.MenuBook, Res.string.nav_diary),
    BottomBarItem(Route.Workout, Icons.Default.FitnessCenter, Res.string.nav_workout),
    BottomBarItem(Route.Progresso, Icons.AutoMirrored.Filled.TrendingUp, Res.string.nav_progress),
    BottomBarItem(Route.Mais, Icons.Default.MoreHoriz, Res.string.nav_more),
)

@Composable
fun AntaresBottomBar(navController: NavHostController) {
    val selecionado = separadorSelecionado(navController)

    NavigationBar {
        bottomBarItems.forEach { item ->
            NavigationBarItem(
                selected = selecionado(item.route),
                onClick = { navController.navigateToTab(item.route) },
                // Decorativo: cada separador tem o seu nome por baixo do ícone.
                icon = { Icon(item.icon, contentDescription = null) },
                label = { Text(stringResource(item.label)) },
            )
        }
    }
}

/**
 * Os mesmos separadores, de pé ao lado do conteúdo. É o que o Material manda numa janela
 * larga — uma barra em baixo esticada por 1200 dp põe cinco alvos ao longo de todo o ecrã —
 * e também num telemóvel deitado, onde a barra roubava altura que já não sobrava.
 *
 * A lista é a mesma do [AntaresBottomBar] de propósito: dois sítios a decidir que
 * separadores existem davam duas apps diferentes conforme a rotação.
 */
@Composable
fun AntaresNavigationRail(navController: NavHostController) {
    val selecionado = separadorSelecionado(navController)

    NavigationRail {
        bottomBarItems.forEach { item ->
            NavigationRailItem(
                selected = selecionado(item.route),
                onClick = { navController.navigateToTab(item.route) },
                // Decorativo: o nome do separador está logo por baixo do ícone.
                icon = { Icon(item.icon, contentDescription = null) },
                label = { Text(stringResource(item.label)) },
            )
        }
    }
}

/**
 * Percorre a hierarquia e não só o destino: um ecrã aninhado dentro de um separador continua
 * a pertencer-lhe, e sem isto o separador apagava-se assim que se abrisse alguma coisa nele.
 */
@Composable
private fun separadorSelecionado(navController: NavHostController): (Route) -> Boolean {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destino = backStackEntry?.destination
    return { rota ->
        val nome = rota::class.qualifiedName
        destino?.hierarchy?.any { it.route == nome } == true
    }
}
