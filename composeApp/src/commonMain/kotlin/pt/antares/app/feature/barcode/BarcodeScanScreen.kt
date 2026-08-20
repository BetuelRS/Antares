package pt.antares.app.feature.barcode

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.PrimaryButton
import pt.antares.app.core.designsystem.components.SecondaryButton
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*

@Composable
fun BarcodeScanScreen(
    onDetected: (String) -> Unit,
    onBack: () -> Unit,
    networkError: Boolean = false,
    pesquisaDesligada: Boolean = false,
    onRetry: () -> Unit = {},

    continuous: Boolean = false,
    onToggleContinuous: () -> Unit = {},

    logged: List<String> = emptyList(),
    notFoundCodes: List<String> = emptyList(),
    onCreateMissing: ((String) -> Unit)? = null,
) {
    val permission = rememberCameraPermissionState()
    var torch by remember { mutableStateOf(false) }
    var manualCode by remember { mutableStateOf("") }
    var handled by remember { mutableStateOf(false) }

    var lastCode by remember { mutableStateOf("") }

    fun emit(code: String) {
        if (code.isBlank()) return
        if (continuous) {
            if (code == lastCode) return
            lastCode = code
            onDetected(code)
            return
        }
        if (!handled) {
            handled = true
            onDetected(code)
        }
    }

    // As duas falhas que param a leitura. Fora do corpo do ecrã porque são o mesmo desenho
    // com palavras diferentes, e juntas dizem melhor o que as separa.
    if (networkError || pesquisaDesligada) {
        LaunchedEffect(Unit) { handled = false }
        DialogoDeFalha(desligada = pesquisaDesligada, onFechar = onRetry)
    }

    LaunchedEffect(Unit) { if (!permission.granted) permission.request() }

    Scaffold(
        topBar = { AntaresTopBar(title = stringResource(Res.string.scan_title), onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black, RoundedCornerShape(Spacing.md)),
                contentAlignment = Alignment.Center,
            ) {
                if (permission.granted) {
                    BarcodeCameraPreview(
                        torchEnabled = torch,
                        onBarcode = ::emit,
                        modifier = Modifier.fillMaxSize(),
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .fillMaxHeight(0.35f)
                            .border(2.dp, Color.White, RoundedCornerShape(Spacing.sm)),
                    )
                    IconButton(
                        onClick = { torch = !torch },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(Spacing.md),
                    ) {
                        Icon(
                            if (torch) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                            contentDescription = stringResource(Res.string.scan_torch),
                            tint = Color.White,
                        )
                    }
                } else {

                    Column(
                        modifier = Modifier.padding(Spacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        Text(
                            stringResource(Res.string.scan_permission_rationale),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                        )
                        SecondaryButton(
                            text = stringResource(Res.string.scan_grant),
                            onClick = permission::request,
                        )
                    }
                }
            }

            FilterChip(
                selected = continuous,
                onClick = onToggleContinuous,
                label = { Text(stringResource(Res.string.scan_continuous)) },
            )
            if (continuous) {
                Text(
                    stringResource(Res.string.scan_continuous_note),
                    style = MaterialTheme.typography.bodySmall,
                )
                if (logged.isNotEmpty()) {
                    Text(
                        stringResource(Res.string.scan_continuous_logged, logged.size) +
                            ": " + logged.takeLast(4).joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                CodigosPorCriar(notFoundCodes, onCreateMissing)
            }

            Text(stringResource(Res.string.scan_manual_title), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = manualCode,
                onValueChange = { manualCode = it.filter(Char::isDigit).take(14) },
                label = { Text(stringResource(Res.string.scan_manual_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryButton(
                text = stringResource(Res.string.scan_confirm),
                onClick = { emit(manualCode) },
                enabled = manualCode.length in 8..14,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Os códigos que ficaram por resolver, um por linha e com o número à vista: sem ele não há
 * como saber qual dos produtos em cima da mesa é que falta ao catálogo.
 *
 * O botão fica de fora quando ninguém o soube ligar — a lista continua a valer por si.
 */

/**
 * A leitura parou, e por uma de duas razões que não se podem confundir: não houve rede, ou a
 * pessoa desligou a pesquisa em linha. A primeira convida a tentar outra vez; na segunda não
 * há nada a repetir enquanto o interruptor estiver onde está.
 */
@Composable
private fun DialogoDeFalha(desligada: Boolean, onFechar: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onFechar,
        title = {
            Text(
                stringResource(
                    if (desligada) Res.string.scan_off_disabled_title else Res.string.scan_network_error_title,
                ),
            )
        },
        text = {
            Text(
                stringResource(
                    if (desligada) Res.string.scan_off_disabled_body else Res.string.scan_network_error_body,
                ),
            )
        },
        confirmButton = {
            PrimaryButton(
                text = stringResource(
                    if (desligada) Res.string.common_ok else Res.string.scan_network_error_retry,
                ),
                onClick = onFechar,
            )
        },
    )
}

@Composable
private fun CodigosPorCriar(codigos: List<String>, onCreateMissing: ((String) -> Unit)?) {
    if (codigos.isEmpty()) return
    Text(
        stringResource(Res.string.scan_continuous_missed, codigos.size),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )
    codigos.forEach { codigo ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(codigo, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            onCreateMissing?.let { criar ->
                TextButton(onClick = { criar(codigo) }) {
                    Text(stringResource(Res.string.scan_continuous_create))
                }
            }
        }
    }
}
