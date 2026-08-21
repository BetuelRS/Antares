package pt.antares.app.feature.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.larguraDeLeitura
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresScaffold
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.PrimaryButton
import pt.antares.app.core.designsystem.components.SecondaryButton
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.admin_code_label
import pt.antares.app.generated.resources.admin_disable
import pt.antares.app.generated.resources.admin_enable
import pt.antares.app.generated.resources.admin_hint
import pt.antares.app.generated.resources.admin_msg_bad_code
import pt.antares.app.generated.resources.admin_msg_disabled
import pt.antares.app.generated.resources.admin_msg_enabled
import pt.antares.app.generated.resources.admin_msg_error
import pt.antares.app.generated.resources.admin_msg_network
import pt.antares.app.generated.resources.admin_status_off
import pt.antares.app.generated.resources.admin_status_on
import pt.antares.app.generated.resources.admin_title
import pt.antares.app.generated.resources.common_cancel
import pt.antares.app.generated.resources.demo_confirm_off_body
import pt.antares.app.generated.resources.demo_confirm_off_title
import pt.antares.app.generated.resources.demo_confirm_on_body
import pt.antares.app.generated.resources.demo_confirm_on_title
import pt.antares.app.generated.resources.demo_disable
import pt.antares.app.generated.resources.demo_enable
import pt.antares.app.generated.resources.demo_explain
import pt.antares.app.generated.resources.demo_msg_error
import pt.antares.app.generated.resources.demo_msg_off
import pt.antares.app.generated.resources.demo_msg_on
import pt.antares.app.generated.resources.demo_msg_refused
import pt.antares.app.generated.resources.demo_status_off
import pt.antares.app.generated.resources.demo_status_on
import pt.antares.app.generated.resources.demo_title
import pt.antares.app.generated.resources.demo_working

@Composable
fun AdminScreen(
    onBack: () -> Unit,
    viewModel: AdminViewModel = koinViewModel(),
    demoViewModel: DemoViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val unlimited by viewModel.unlimited.collectAsState()
    val demo by demoViewModel.state.collectAsState()

    AntaresScaffold(
        topBar = { AntaresTopBar(title = stringResource(Res.string.admin_title), onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)

                // Com o acesso ilimitado ligado abre-se a secção de demonstração
                // por baixo desta, e sem scroll o botão dela fica fora do ecrã.
                .verticalScroll(rememberScrollState())
                .larguraDeLeitura()
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {

            AntaresCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(if (unlimited) Res.string.admin_status_on else Res.string.admin_status_off),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (unlimited) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                stringResource(Res.string.admin_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = state.code,
                onValueChange = viewModel::setCode,
                label = { Text(stringResource(Res.string.admin_code_label)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.fillMaxWidth()) {
                PrimaryButton(
                    text = stringResource(Res.string.admin_enable),
                    onClick = { viewModel.submit(enable = true) },
                    enabled = state.code.isNotBlank() && !state.loading,
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton(
                    text = stringResource(Res.string.admin_disable),
                    onClick = { viewModel.submit(enable = false) },
                    enabled = state.code.isNotBlank() && !state.loading,
                    modifier = Modifier.weight(1f),
                )
            }

            val message = when (state.message) {
                AdminMessage.ENABLED -> stringResource(Res.string.admin_msg_enabled)
                AdminMessage.DISABLED -> stringResource(Res.string.admin_msg_disabled)
                AdminMessage.BAD_CODE -> stringResource(Res.string.admin_msg_bad_code)
                AdminMessage.NETWORK -> stringResource(Res.string.admin_msg_network)
                AdminMessage.ERROR -> stringResource(Res.string.admin_msg_error)
                AdminMessage.NONE -> null
            }
            message?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.message == AdminMessage.BAD_CODE || state.message == AdminMessage.NETWORK || state.message == AdminMessage.ERROR) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.tertiary
                    },
                )
            }

            if (unlimited) {
                HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
                DemoSection(
                    state = demo,
                    onLigar = { demoViewModel.pedir(DemoAcao.LIGAR) },
                    onDesligar = { demoViewModel.pedir(DemoAcao.DESLIGAR) },
                )
            }
        }
    }

    demo.confirmar?.let { acao ->
        val ligar = acao == DemoAcao.LIGAR
        AlertDialog(
            onDismissRequest = demoViewModel::cancelar,
            title = {
                Text(
                    stringResource(
                        if (ligar) Res.string.demo_confirm_on_title else Res.string.demo_confirm_off_title,
                    ),
                )
            },
            text = {
                Text(
                    stringResource(
                        if (ligar) Res.string.demo_confirm_on_body else Res.string.demo_confirm_off_body,
                    ),
                )
            },
            confirmButton = {
                PrimaryButton(
                    text = stringResource(if (ligar) Res.string.demo_enable else Res.string.demo_disable),
                    onClick = demoViewModel::confirmar,
                )
            },
            dismissButton = {
                SecondaryButton(
                    text = stringResource(Res.string.common_cancel),
                    onClick = demoViewModel::cancelar,
                )
            },
        )
    }
}

@Composable
private fun DemoSection(
    state: DemoState,
    onLigar: () -> Unit,
    onDesligar: () -> Unit,
) {
    Text(stringResource(Res.string.demo_title), style = MaterialTheme.typography.titleMedium)
    Text(
        stringResource(Res.string.demo_explain),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Text(

            if (state.ligado) {
                stringResource(Res.string.demo_status_on, state.linhas.toString())
            } else {
                stringResource(Res.string.demo_status_off)
            },
            style = MaterialTheme.typography.titleMedium,
            color = if (state.ligado) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }

    if (state.ligado) {
        SecondaryButton(
            text = stringResource(Res.string.demo_disable),
            onClick = onDesligar,
            enabled = !state.aTrabalhar,
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        PrimaryButton(
            text = stringResource(
                if (state.aTrabalhar) Res.string.demo_working else Res.string.demo_enable,
            ),
            onClick = onLigar,
            enabled = !state.aTrabalhar,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    val demoMsg = when (state.mensagem) {
        DemoMessage.LIGOU -> stringResource(Res.string.demo_msg_on)
        DemoMessage.DESLIGOU -> stringResource(Res.string.demo_msg_off)
        DemoMessage.RECUSOU_DADOS_REAIS ->
            stringResource(Res.string.demo_msg_refused, state.linhasReais.toString())
        DemoMessage.ERRO -> stringResource(Res.string.demo_msg_error)
        DemoMessage.NENHUMA -> null
    }
    demoMsg?.let {
        Text(
            it,
            style = MaterialTheme.typography.bodyMedium,

            color = if (state.mensagem == DemoMessage.RECUSOU_DADOS_REAIS || state.mensagem == DemoMessage.ERRO) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.tertiary
            },
        )
    }
}
