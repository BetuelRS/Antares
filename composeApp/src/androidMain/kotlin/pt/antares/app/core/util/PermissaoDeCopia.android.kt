package pt.antares.app.core.util

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

@Composable
actual fun rememberPedidoDePastaDeCopias(
    onResult: (concedida: Boolean) -> Unit,
): (() -> Unit)? {
    val resposta = rememberUpdatedState(onResult)
    // O `launcher` regista-se sempre, mesmo quando não vai ser usado: registar um contrato
    // dentro de um `if` faz o Compose recusar-se a compor quando a condição muda.
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { concedida -> resposta.value(concedida) }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return null
    return remember(launcher) {
        { launcher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) }
    }
}
