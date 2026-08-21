package pt.antares.app.core.designsystem.motion

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Lê o `ANIMATOR_DURATION_SCALE`, que é o que as Opções de Programador e as definições de
 * acessibilidade mexem. Zero quer dizer «sem animações».
 *
 * Lido uma vez e guardado: mudar esta definição reinicia a interface do sistema de qualquer
 * maneira, e consultá-la a cada composição seria um acesso às definições por cada transição.
 */
@Composable
actual fun animacoesLigadas(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        val escala = runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            )
        }.getOrDefault(1f)
        escala > 0f
    }
}
