package pt.antares.app.feature.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.larguraDeLeitura
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresScaffold
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.PrimaryButton
import pt.antares.app.core.designsystem.components.SecondaryButton
import pt.antares.app.core.privacy.BackupFiles
import pt.antares.app.core.privacy.ImportMode
import pt.antares.app.core.privacy.LeitorDeResumo
import pt.antares.app.core.privacy.PrivacyViewModel
import pt.antares.app.core.privacy.ResumoDaCopia
import pt.antares.app.core.util.rememberBackupPicker
import pt.antares.app.core.util.rememberZipSharer
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.backup_title
import pt.antares.app.generated.resources.backup_why
import pt.antares.app.generated.resources.copia_conta_body_measurement_log
import pt.antares.app.generated.resources.copia_conta_food_log
import pt.antares.app.generated.resources.copia_conta_foods
import pt.antares.app.generated.resources.copia_conta_linha
import pt.antares.app.generated.resources.copia_conta_outras
import pt.antares.app.generated.resources.copia_conta_progress_photo
import pt.antares.app.generated.resources.copia_conta_recipe
import pt.antares.app.generated.resources.copia_conta_run
import pt.antares.app.generated.resources.copia_conta_water_log
import pt.antares.app.generated.resources.copia_conta_weight_log
import pt.antares.app.generated.resources.copia_conta_workout_session
import pt.antares.app.generated.resources.copia_import_de
import pt.antares.app.generated.resources.copia_import_resumo
import pt.antares.app.generated.resources.copia_import_sem_data
import pt.antares.app.generated.resources.copia_inclui
import pt.antares.app.generated.resources.copia_inclui_total
import pt.antares.app.generated.resources.privacy_error
import pt.antares.app.generated.resources.privacy_export
import pt.antares.app.generated.resources.privacy_export_desc
import pt.antares.app.generated.resources.privacy_import
import pt.antares.app.generated.resources.privacy_import_desc
import pt.antares.app.generated.resources.privacy_import_done
import pt.antares.app.generated.resources.privacy_import_merge
import pt.antares.app.generated.resources.privacy_import_merge_desc
import pt.antares.app.generated.resources.privacy_import_replace
import pt.antares.app.generated.resources.privacy_import_replace_desc
import pt.antares.app.generated.resources.privacy_import_title

@Composable
fun BackupScreen(
    onBack: () -> Unit,
    viewModel: PrivacyViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    AntaresScaffold(
        topBar = { AntaresTopBar(title = stringResource(Res.string.backup_title), onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .larguraDeLeitura()
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {

            AntaresCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(Res.string.backup_why),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            CartaoDaCopia()

            OQueVaiNaCopia()

            BackupActions(viewModel, state.busy, state.importDone)

            state.error?.let { message ->
                Text(
                    stringResource(Res.string.privacy_error, message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/**
 * As contagens do que a cópia leva.
 *
 * Uma cópia é um ficheiro opaco: dizer «cópia feita» e dizer «cópia feita com 487 refeições
 * e 92 treinos» custa o mesmo e só a segunda se pode conferir. Um ficheiro vazio, escrito
 * por um erro que ninguém viu, lê-se aqui de relance.
 */
@Composable
private fun OQueVaiNaCopia(viewModel: CopiaViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.carregarContagens() }
    if (state.contagens.isEmpty()) return

    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                stringResource(Res.string.copia_inclui),
                style = MaterialTheme.typography.titleMedium,
            )
            LinhasDeContagem(state.contagens)
            Text(
                stringResource(
                    Res.string.copia_inclui_total,
                    state.contagens.values.sum().toString(),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * As tabelas que uma pessoa reconhece pelo nome, e o resto somado numa linha.
 *
 * São vinte e seis tabelas na cópia. Listá-las todas dava um ecrã de nomes de base de dados
 * — `daily_target_override`, `routine_item` — que não diz nada a ninguém e esconde as nove
 * que dizem.
 */
@Composable
private fun LinhasDeContagem(contagens: Map<String, Int>) {
    val destacadas = DESTAQUES.mapNotNull { (chave, rotulo) ->
        contagens[chave]?.takeIf { it > 0 }?.let { rotulo to it }
    }
    val resto = contagens.filterKeys { it !in DESTAQUES.keys }.values.sum()

    destacadas.forEach { (rotulo, quantos) ->
        Text(
            stringResource(Res.string.copia_conta_linha, stringResource(rotulo), quantos.toString()),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    if (resto > 0) {
        Text(
            stringResource(
                Res.string.copia_conta_linha,
                stringResource(Res.string.copia_conta_outras),
                resto.toString(),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// A ordem é a de quem lê, não a da base: primeiro o que se registou hoje, no fim o que se
// mede uma vez por mês. As chaves são as dos `ExportSource` e mudá-las parte isto em
// silêncio — o que aparece é a linha «Outras tabelas» a engordar.
private val DESTAQUES: Map<String, StringResource> = linkedMapOf(
    "food_log" to Res.string.copia_conta_food_log,
    "water_log" to Res.string.copia_conta_water_log,
    "workout_session" to Res.string.copia_conta_workout_session,
    "run" to Res.string.copia_conta_run,
    "weight_log" to Res.string.copia_conta_weight_log,
    "body_measurement_log" to Res.string.copia_conta_body_measurement_log,
    "progress_photo" to Res.string.copia_conta_progress_photo,
    "foods" to Res.string.copia_conta_foods,
    "recipe" to Res.string.copia_conta_recipe,
)

@Composable
internal fun BackupActions(
    viewModel: PrivacyViewModel,
    busy: Boolean,
    importDone: Int?,
) {
    val shareZip = rememberZipSharer()
    var lidas by remember { mutableStateOf<Map<String, ByteArray>?>(null) }
    val escolher = rememberBackupPicker { entries ->

        if (entries.isNotEmpty()) lidas = entries
    }

    Text(
        stringResource(Res.string.privacy_export_desc),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    PrimaryButton(
        text = stringResource(Res.string.privacy_export),
        onClick = { viewModel.exportData { name, entries -> shareZip(name, entries) } },
        modifier = Modifier.fillMaxWidth(),
        enabled = !busy,
    )

    Text(
        stringResource(Res.string.privacy_import_desc),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Spacing.sm),
    )
    SecondaryButton(
        text = stringResource(Res.string.privacy_import),
        onClick = escolher,
        modifier = Modifier.fillMaxWidth(),
        enabled = !busy,
    )
    importDone?.let { quantos ->
        Text(
            stringResource(Res.string.privacy_import_done, quantos.toString()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.tertiary,
        )
    }

    lidas?.let { entries ->
        // O resumo lê-se aqui e não no ViewModel: são uns quilobytes de JSON já em memória,
        // e passar por uma corrotina punha o diálogo a abrir sem ele e a preenchê-lo depois
        // — que é a piscadela em que alguém carrega em «substituir» sem ter lido nada.
        val resumo = remember(entries) {
            entries[BackupFiles.DATA]?.let { LeitorDeResumo.ler(it.decodeToString()) }
        }
        AlertDialog(
            onDismissRequest = { lidas = null },
            title = { Text(stringResource(Res.string.privacy_import_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    ResumoDoFicheiro(resumo)
                    Text(stringResource(Res.string.privacy_import_merge_desc))

                    Text(
                        stringResource(Res.string.privacy_import_replace_desc),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            confirmButton = {
                SecondaryButton(
                    text = stringResource(Res.string.privacy_import_merge),
                    onClick = {
                        lidas = null
                        viewModel.importBackup(entries, ImportMode.MERGE)
                    },
                )
            },
            dismissButton = {
                SecondaryButton(
                    text = stringResource(Res.string.privacy_import_replace),
                    onClick = {
                        lidas = null
                        viewModel.importBackup(entries, ImportMode.REPLACE)
                    },
                )
            },
        )
    }
}

/**
 * O que o ficheiro escolhido traz, antes de se decidir o que fazer com ele. Substituir é
 * irreversível, e até aqui a pergunta era feita sobre um ficheiro de que a app não dizia
 * nada — nem a data, nem quantos registos, nem a versão que o escreveu.
 */
@Composable
private fun ResumoDoFicheiro(resumo: ResumoDaCopia?) {
    Text(
        stringResource(Res.string.copia_import_resumo),
        style = MaterialTheme.typography.titleSmall,
    )
    if (resumo?.exportadoEm == null) {
        Text(
            stringResource(Res.string.copia_import_sem_data),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    } else {
        Text(
            stringResource(
                Res.string.copia_import_de,
                resumo.exportadoEm.substringBefore('T'),
                resumo.versaoApp.orEmpty(),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    resumo?.let { LinhasDeContagem(it.contagens) }
}
