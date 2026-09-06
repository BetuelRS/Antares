package pt.antares.app.core.designsystem

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

/**
 * O `BackHandler` do `activity-compose`, que intercepta tanto o botão como o gesto de deslizar
 * da margem. Com a lambda vazia: o recuo é consumido e não faz nada, que é o que travar quer
 * dizer — desactivá-lo por outra via deixaria a app a fechar-se em vez de ficar quieta.
 */
@Composable
actual fun TravarRecuo(activo: Boolean) {
    BackHandler(enabled = activo) {}
}
