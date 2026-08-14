package pt.antares.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import pt.antares.app.generated.resources.nav_profile
import pt.antares.app.generated.resources.nav_run
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
    BottomBarItem(Route.Run, Icons.Default.DirectionsRun, Res.string.nav_run),

    BottomBarItem(Route.Me, Icons.Default.Person, Res.string.nav_profile),
)

@Composable
fun AntaresBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    NavigationBar {
        bottomBarItems.forEach { item ->
            val itemRouteName = item.route::class.qualifiedName
            val selected = currentDestination?.hierarchy?.any {
                it.route == itemRouteName
            } == true

            NavigationBarItem(
                selected = selected,
                onClick = { navController.navigateToTab(item.route) },
                icon = { Icon(item.icon, contentDescription = null) },
                label = { Text(stringResource(item.label)) },
            )
        }
    }
}
