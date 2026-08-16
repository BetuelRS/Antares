package pt.antares.app.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.koinInject
import pt.antares.app.core.model.UnitSystem
import pt.antares.app.feature.profile.data.ProfileRepository

/**
 * O sistema de unidades escolhido, para quem precisa dele só para apresentar um número.
 *
 * Vem daqui e não do ViewModel do ecrã de propósito: o treino, a corrida e as porções são
 * quatro ecrãs de treino, cinco de corrida e três de comida, e passar a preferência por doze
 * ViewModels que não a usam para mais nada era o custo que a mantinha por fazer. Quem já a
 * tem no estado — o Hoje, o Progresso, o peso — continua a usar a sua.
 *
 * Enquanto o perfil não chega vale o métrico. É o que a app já mostrava, e um número em
 * libras que passa a quilos ao fim de um instante lê-se pior do que um sempre em quilos.
 */
@Composable
fun rememberUnitSystem(): UnitSystem {
    val repository: ProfileRepository = koinInject()
    val profile by repository.observeProfile().collectAsState(initial = null)
    return profile?.unitSystem ?: UnitSystem.METRIC
}
