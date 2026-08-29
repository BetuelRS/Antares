package pt.antares.app.feature.fooddata

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.LinhaDaLista
import pt.antares.app.core.designsystem.components.PrimaryButton
import pt.antares.app.core.designsystem.components.rememberDesfazer
import pt.antares.app.core.designsystem.virgulaDecimal
import pt.antares.app.core.model.MealSlot
import pt.antares.app.core.model.mealSlotLabel
import pt.antares.app.feature.templates.PreVisualizacaoDeModelo
import pt.antares.app.feature.templates.rotuloDaEscala
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.common_cancel
import pt.antares.app.generated.resources.common_delete
import pt.antares.app.generated.resources.common_grams_short
import pt.antares.app.generated.resources.common_kcal
import pt.antares.app.generated.resources.common_save
import pt.antares.app.generated.resources.modelo_aplicado
import pt.antares.app.generated.resources.modelo_aplicar
import pt.antares.app.generated.resources.modelo_aplicar_no
import pt.antares.app.generated.resources.refeicao_item_removido
import pt.antares.app.generated.resources.refeicao_nome
import pt.antares.app.generated.resources.refeicao_renomear
import pt.antares.app.generated.resources.templates_delete
import pt.antares.app.generated.resources.undo_action
import kotlin.math.roundToInt

private val ALTURA_DA_LISTA = 320.dp

/**
 * Uma refeição guardada, aberta: ver, escalar, corrigir, aplicar.
 *
 * Saiu do `FoodSearchScreen` quando ganhou a edição. **Aqui vive tudo o que se pode fazer a
 * uma refeição guardada**, e é de propósito: a lista passou a ser uma só com as duas
 * origens misturadas, e pôr botões diferentes conforme a origem em cada linha era desenhar
 * outra vez a divisão que a lista acabou de apagar.
 *
 * As três coisas que a área 05 do estudo marcava como em falta estão as três nesta folha —
 * ver antes de aplicar, escalar, e editar.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun FolhaDaRefeicaoGuardada(
    pre: PreVisualizacaoDeModelo,
    viewModel: FoodSearchViewModel,
    slot: MealSlot?,
    epochDay: Long?,
) {
    val desfazer = rememberDesfazer()
    val aplicada = stringResource(Res.string.modelo_aplicado)
    var aRenomear by rememberSaveable { mutableStateOf(false) }

    if (aRenomear) {
        DialogoDeNome(
            nome = pre.modelo.name,
            onGuardar = { viewModel.renomearModelo(it); aRenomear = false },
            onFechar = { aRenomear = false },
        )
    }

    ModalBottomSheet(
        onDismissRequest = viewModel::fecharPreVisualizacao,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    pre.modelo.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { aRenomear = true }) {
                    Icon(Icons.Default.Edit, stringResource(Res.string.refeicao_renomear))
                }
                // Apagar mudou-se para aqui com a lista só. Na linha, apagava-se uma coisa
                // sem se ver o que lá estava dentro.
                IconButton(
                    onClick = {
                        val id = pre.modelo.id
                        viewModel.fecharPreVisualizacao()
                        viewModel.deleteTemplate(id)
                    },
                ) {
                    Icon(Icons.Default.Delete, stringResource(Res.string.templates_delete))
                }
            }

            EscalaEmChips(pre, viewModel::escolherEscala)

            LazyColumn(
                modifier = Modifier.heightIn(max = ALTURA_DA_LISTA),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                items(pre.itens, key = { it.id }) { item ->
                    LinhaDaLista(
                        titulo = item.nameSnapshot,
                        subtitulo = "${pre.gramasDe(item).roundToInt()} " +
                            stringResource(Res.string.common_grams_short) +
                            " · ${pre.kcalDe(item)} ${stringResource(Res.string.common_kcal)}",
                        emCartao = false,
                        aoLado = {
                            IconButton(onClick = { viewModel.removerItemDoModelo(item.id) }) {
                                Icon(Icons.Default.Close, stringResource(Res.string.common_delete))
                            }
                        },
                    )
                }
            }

            // O desfazer de tirar um item vive **aqui dentro**, e não no aviso ao fundo do
            // ecrã como o resto da app. A folha é uma janela por cima do andaime, e o aviso
            // desenha-se por baixo dela: no aparelho não aparecia sequer na árvore de
            // acessibilidade. Um desfazer que ninguém vê não é um desfazer.
            //
            // De caminho fica melhor do que o aviso: sem corrida contra os quatro segundos.
            pre.removido?.let { removido ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(Res.string.refeicao_item_removido, removido.nameSnapshot),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = viewModel::restaurarUltimoItem) {
                        Text(stringResource(Res.string.undo_action))
                    }
                }
            }

            // **O botão diz onde é que a refeição cai.** É o desenho do esboço 05 e o mesmo
            // princípio do cabeçalho da pesquisa. Sem refeição não há destino nenhum a
            // nomear — é quem abriu isto para escolher um ingrediente —, e aí fica a frase
            // antiga, que não promete um sítio.
            PrimaryButton(
                text = if (slot != null) {
                    stringResource(Res.string.modelo_aplicar_no, mealSlotLabel(slot).lowercase())
                } else {
                    stringResource(Res.string.modelo_aplicar)
                },
                onClick = {
                    // Sem dia e sem refeição não há para onde aplicar — é o caso de quem
                    // abriu a pesquisa para escolher um ingrediente, e não para registar.
                    if (slot != null && epochDay != null) {
                        viewModel.aplicarPreVisualizacao(slot, epochDay) { criados ->
                            desfazer(aplicada) { viewModel.desfazerAplicacao(criados) }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                // Sem itens não escreve nada, e sem refeição do dia não tem para onde
                // escrever. O botão não pode prometer nenhuma das duas.
                enabled = pre.itens.isNotEmpty() && slot != null && epochDay != null,
            )
        }
    }
}

/**
 * As quatro escalas do esboço, e as calorias que elas dão.
 *
 * O total ao lado dos chips e não por baixo da lista: é o número que decide qual chip se
 * escolhe, e vê-lo depois de escolher é vê-lo tarde.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EscalaEmChips(pre: PreVisualizacaoDeModelo, onEscala: (Double) -> Unit) {
    val virgula = virgulaDecimal()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FlowRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            PreVisualizacaoDeModelo.ESCALAS.forEach { escala ->
                FilterChip(
                    selected = pre.multiplicador == escala,
                    onClick = { onEscala(escala) },
                    label = { Text(rotuloDaEscala(escala, virgula)) },
                )
            }
        }
        Text(
            "${pre.kcal} ${stringResource(Res.string.common_kcal)}",
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

/** Mudar o nome. O campo abre com o nome que lá está, para se corrigir em vez de reescrever. */
@Composable
private fun DialogoDeNome(nome: String, onGuardar: (String) -> Unit, onFechar: () -> Unit) {
    var texto by remember(nome) { mutableStateOf(nome) }
    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text(stringResource(Res.string.refeicao_renomear)) },
        text = {
            OutlinedTextField(
                value = texto,
                onValueChange = { texto = it },
                label = { Text(stringResource(Res.string.refeicao_nome)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onGuardar(texto) },
                // Um nome em branco deixava a linha da lista sem nada escrito, que é pior
                // do que o nome que ela tinha.
                enabled = texto.isNotBlank(),
            ) { Text(stringResource(Res.string.common_save)) }
        },
        dismissButton = {
            TextButton(onClick = onFechar) { Text(stringResource(Res.string.common_cancel)) }
        },
    )
}
