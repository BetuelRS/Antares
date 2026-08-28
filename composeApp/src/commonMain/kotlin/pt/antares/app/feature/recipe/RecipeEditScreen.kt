package pt.antares.app.feature.recipe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.larguraDeLeitura
import pt.antares.app.core.designsystem.fmtG
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.portionUnitLabel
import pt.antares.app.core.designsystem.rememberUnitSystem
import pt.antares.app.feature.fooddata.nomeDoMetodo
import pt.antares.app.feature.fooddata.paraCampo
import pt.antares.app.core.designsystem.macroInitials
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresScaffold
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.rememberApagarComDesfazer
import pt.antares.app.core.database.entities.FoodEntity
import pt.antares.app.core.database.entities.RecipeStepEntity
import pt.antares.app.core.designsystem.components.ConfirmDialog
import pt.antares.app.core.designsystem.components.LinhaDaLista
import pt.antares.app.core.designsystem.components.PrimaryButton
import pt.antares.app.core.designsystem.components.SectionHeader
import pt.antares.app.core.designsystem.components.SecondaryButton
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*
import kotlin.math.roundToInt

@Composable
fun RecipeEditScreen(
    recipeId: String?,
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: RecipeEditViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val procura by viewModel.procura.collectAsState()
    val passos by viewModel.passos.collectAsState()
    val passoEmEdicao by viewModel.passoEmEdicao.collectAsState()
    val unidades = rememberUnitSystem()
    var confirmarApagar by remember { mutableStateOf(false) }
    val apagar = rememberApagarComDesfazer()

    LaunchedEffect(recipeId) { viewModel.start(recipeId) }
    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    passoEmEdicao?.let { edicao ->
        FolhaDoPasso(
            edicao = edicao,
            onTexto = viewModel::escreverTextoDoPasso,
            onGravar = viewModel::gravarPasso,
            onFechar = viewModel::fecharEdicaoDePasso,
        )
    }

    procura?.let { p ->
        ProcuraDeIngredienteSheet(
            procura = p,
            onTexto = viewModel::procurar,
            onEscolher = viewModel::acrescentar,
            onFechar = viewModel::fecharProcura,
        )
    }

    AntaresScaffold(
        topBar = {
            AntaresTopBar(
                title = stringResource(if (recipeId == null) Res.string.recipe_new else Res.string.recipe_edit),
                onBack = onBack,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .larguraDeLeitura()
                .padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::setName,
                    label = { Text(stringResource(Res.string.recipe_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.md),
                )
            }
            item { PesoFinal(state, viewModel) }

            // A pergunta só aparece quando alguma família presente conhece métodos: uma
            // salada não tem nada a perguntar, e um campo que não faz nada é ruído.
            if (state.metodos.isNotEmpty()) {
                item { EscolhaDoMetodo(state, viewModel::escolherMetodo) }
            }

            item { Doses(state, viewModel) }

            item { CartaoPor100(state) }

            item {
                // Abre a folha por cima em vez de sair do ecrã. O ecrã de escolha que estava
                // aqui — uma pesquisa inteira, uma ida e uma volta por ingrediente — deixou
                // de ter quem lhe chamasse e saiu com esta versão, rota incluída.
                SecondaryButton(
                    text = stringResource(Res.string.recipe_add_ingredient),
                    onClick = viewModel::abrirProcura,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (state.rows.isEmpty()) {
                item {
                    Text(
                        stringResource(Res.string.recipe_empty_ingredients),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            items(state.rows, key = { it.ingredient.id }) { row ->
                LinhaDeIngrediente(row, unidades, viewModel, apagar)
            }

            secaoDosPassos(passos, viewModel, apagar)

            item {
                PrimaryButton(
                    text = stringResource(Res.string.common_save),
                    onClick = viewModel::save,
                    enabled = state.valid,
                    modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.md),
                )
            }

            if (recipeId != null) {
                item {
                    SecondaryButton(
                        text = stringResource(Res.string.recipe_delete),
                        onClick = { confirmarApagar = true },
                        modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.xl),
                    )
                }
            }
        }
    }

    if (confirmarApagar) {
        ConfirmDialog(
            title = stringResource(Res.string.recipe_delete_title),
            message = stringResource(Res.string.recipe_delete_body),
            confirmLabel = stringResource(Res.string.common_delete),
            dismissLabel = stringResource(Res.string.common_cancel),
            onConfirm = {
                confirmarApagar = false
                viewModel.delete()
            },
            onDismiss = { confirmarApagar = false },
        )
    }
}

/**
 * «Como se cozinhou?»
 *
 * A mesma pergunta que um alimento faz, feita ao prato inteiro. A diferença é o que se faz
 * com a resposta: no alimento ela muda o peso **e** os nutrientes, e aqui muda só os
 * nutrientes — o peso da receita é o que a pessoa escreveu ou a soma dos ingredientes, e não
 * se mexe nele por baixo de quem o escreveu.
 *
 * Cada ingrediente perde o que a tabela da **família dele** diz. Um cozido de carne com
 * legumes não perde a mesma vitamina C nos dois, e é por isso que a retenção não é um número
 * da receita.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EscolhaDoMetodo(state: RecipeEditState, onEscolher: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            stringResource(Res.string.recipe_metodo),
            style = MaterialTheme.typography.titleSmall,
        )

        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            for (m in state.metodos) {
                FilterChip(
                    selected = state.metodo == m.id,
                    onClick = { onEscolher(m.id) },
                    label = { Text(nomeDoMetodo(m.id, m.nome)) },
                )
            }
        }

        // Com método escolhido, diz-se o que mudou nas contas; sem ele, o que a pergunta
        // serve. As duas frases ocupam a mesma linha para o cartão não saltar de altura.
        Text(
            stringResource(
                if (state.metodo == null) Res.string.recipe_metodo_hint else Res.string.recipe_retencao_nota,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * A procura de ingredientes, por cima da receita.
 *
 * **Não se fecha ao escolher**, e é essa a razão de existir: quem faz uma receita
 * acrescenta ingredientes em série. Fechá-la a cada escolha repunha a viagem que esta
 * versão veio tirar — oito ingredientes eram oito idas a um ecrã de pesquisa e oito voltas.
 *
 * A conta do que já entrou fica à vista, porque a lista de ingredientes está tapada pela
 * folha: sem ela, não há sinal nenhum de que o toque fez alguma coisa.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProcuraDeIngredienteSheet(
    procura: ProcuraDeIngrediente,
    onTexto: (String) -> Unit,
    onEscolher: (FoodEntity) -> Unit,
    onFechar: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onFechar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(
                stringResource(Res.string.recipe_add_ingredient),
                style = MaterialTheme.typography.titleMedium,
            )

            OutlinedTextField(
                value = procura.texto,
                onValueChange = onTexto,
                placeholder = { Text(stringResource(Res.string.search_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            if (procura.acrescentados > 0) {
                Text(
                    pluralStringResource(
                        Res.plurals.recipe_ingredientes_acrescentados,
                        procura.acrescentados,
                        procura.acrescentados,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            if (procura.aProcurar) CircularProgressIndicator(modifier = Modifier)

            LazyColumn(
                modifier = Modifier.heightIn(max = PROCURA_ALTURA),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                items(procura.resultados, key = { it.id }) { food ->
                    LinhaDaLista(
                        titulo = food.namePt,
                        subtitulo = "${food.kcal} " + stringResource(Res.string.common_kcal) + " / 100 g",
                        onClick = { onEscolher(food) },
                        emCartao = false,
                    )
                }
            }
        }
    }
}

private val PROCURA_ALTURA = 420.dp

/**
 * O peso final do prato: o campo, a previsão para quem não pesou, e o aviso para quem
 * escreveu um número que a tabela não explica.
 *
 * Sai do ecrã porque ele passou o tecto das 120 linhas — e porque as três coisas são uma
 * conversa só sobre o mesmo número.
 */
@Composable
private fun PesoFinal(state: RecipeEditState, viewModel: RecipeEditViewModel) {
    // Em coluna, e não soltos: dentro de um `item` de uma lista os filhos são medidos
    // todos na mesma posição. Eram três — o campo, a sugestão e o aviso.
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            OutlinedTextField(
                value = state.yieldText,
                onValueChange = viewModel::setYield,
                label = { Text(stringResource(Res.string.recipe_yield)) },
                supportingText = { Text(stringResource(Res.string.recipe_yield_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            // A previsão das tabelas, e um botão que a escreve no campo. Nunca se
            // escreve sozinha: um peso final é uma medição do prato de quem cozinhou.
            state.pesoSugerido?.let { sugerido ->
                val gramas = sugerido.roundToInt()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(Res.string.recipe_peso_sugerido, gramas),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = viewModel::aceitarPesoSugerido) {
                        Text(stringResource(Res.string.recipe_peso_sugerido_usar, gramas))
                    }
                }
            }

            // E o contrário: um peso escrito que nenhum método de confeção explica.
            // Diz o intervalo em vez de dizer «parece errado» — e não corrige nada, que
            // é a diferença entre avisar e discutir com quem tem a balança à frente.
            state.pesoForaDoPrevisto?.let { previsto ->
                Text(
                    stringResource(
                        Res.string.recipe_peso_fora,
                        previsto.start.roundToInt(),
                        previsto.endInclusive.roundToInt(),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
            }
    }
}

/**
 * Um ingrediente na lista: o nome, as gramas e o apagar.
 *
 * Sai do ecrã pela mesma razão do [PesoFinal] — o tecto das 120 linhas —, e porque uma
 * linha de uma lista é a peça mais óbvia de se ler sozinha.
 */
@Composable
private fun LinhaDeIngrediente(
    row: IngredientRow,
    unidades: pt.antares.app.core.model.UnitSystem,
    viewModel: RecipeEditViewModel,
    apagar: (apagar: () -> Unit, desfazer: () -> Unit) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        val food = row.food
        Text(
            food?.let { it.namePt.ifBlank { it.nameEn } } ?: row.ingredient.foodId,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
        )
        // O ingrediente é sólido por definição — uma receita mede-se em massa —,
        // por isso vai sempre pela unidade de massa e nunca pela de volume.
        OutlinedTextField(
            value = paraCampo(row.ingredient.grams, unidades),
            onValueChange = { viewModel.updateGrams(row.ingredient, it, unidades) },
            label = { Text(stringResource(portionUnitLabel(unidades, liquid = false))) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(96.dp),
        )
        IconButton(
            onClick = {
                apagar(
                    { viewModel.removeIngredient(row.ingredient) },
                    { viewModel.restoreIngredient(row.ingredient.id) },
                )
            },
        ) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.common_delete))
        }
    }
}

/**
 * Em quantas doses o prato se divide, e quanto pesa uma.
 *
 * O peso de uma dose é o número que torna «uma dose» registável sem pesar o prato — e só
 * aparece quando as doses estão escritas, porque antes disso não há conta nenhuma.
 */
@Composable
private fun Doses(state: RecipeEditState, viewModel: RecipeEditViewModel) {
    OutlinedTextField(
        value = state.servingsText,
        onValueChange = viewModel::setServings,
        label = { Text(stringResource(Res.string.recipe_servings)) },
        supportingText = {
            val porDose = state.gramsPerServing
            Text(
                if (porDose == null) {
                    stringResource(Res.string.recipe_servings_hint)
                } else {
                    stringResource(Res.string.recipe_serving_weight, porDose.roundToInt())
                },
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Um passo na lista: o número, o texto, e os botões de mover e apagar.
 *
 * O número sai da posição na lista e não da coluna `posicao`, e é a mesma coisa por
 * construção — a renumeração no repositório garante-o. Mostrar o índice da lista é o que
 * faz o ecrã continuar certo no instante entre mover e a base responder.
 *
 * Mover é um passo de cada vez e não arrastar: arrastar dentro de uma coluna que rola
 * compete com o rolar, e uma receita tem passos que se contam pelos dedos.
 */
@Composable
private fun LinhaDePasso(
    passo: RecipeStepEntity,
    indice: Int,
    ultimo: Boolean,
    viewModel: RecipeEditViewModel,
    apagar: (apagar: () -> Unit, desfazer: () -> Unit) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            "${indice + 1}.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = Spacing.md),
        )
        Text(
            passo.texto,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1f)
                .padding(top = Spacing.md)
                .clickable(role = Role.Button) { viewModel.editarPasso(passo) },
        )
        // Nos extremos o botão desliga-se: um botão que não faz nada é pior do que um que
        // não está lá.
        IconButton(
            onClick = { viewModel.moverPasso(passo, indice - 1) },
            enabled = indice > 0,
        ) {
            Icon(
                Icons.Default.KeyboardArrowUp,
                contentDescription = stringResource(Res.string.recipe_passo_subir),
            )
        }
        IconButton(
            onClick = { viewModel.moverPasso(passo, indice + 1) },
            enabled = !ultimo,
        ) {
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = stringResource(Res.string.recipe_passo_descer),
            )
        }
        IconButton(
            onClick = {
                apagar({ viewModel.removerPasso(passo) }, { viewModel.devolverPasso(passo) })
            },
        ) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.common_delete))
        }
    }
}

/**
 * A folha onde se escreve um passo, nova ou a corrigir.
 *
 * É a mesma folha nos dois casos: a diferença está só em haver ou não uma linha para
 * reescrever, e duplicá-la para isso era ter duas que divergiam à primeira alteração.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolhaDoPasso(
    edicao: EdicaoDePasso,
    onTexto: (String) -> Unit,
    onGravar: () -> Unit,
    onFechar: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onFechar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(
                stringResource(
                    if (edicao.passo == null) Res.string.recipe_passo_novo else Res.string.recipe_passo_editar,
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedTextField(
                value = edicao.texto,
                onValueChange = onTexto,
                placeholder = { Text(stringResource(Res.string.recipe_passo_dica)) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryButton(
                text = stringResource(Res.string.common_save),
                onClick = onGravar,
                modifier = Modifier.fillMaxWidth(),
                // Um passo em branco não instrui nada, e ocupava uma linha numerada.
                enabled = edicao.podeGravar,
            )
        }
    }
}

/**
 * A secção da preparação dentro da lista da receita.
 *
 * É uma extensão do âmbito da lista e não um composable: os passos são `items` a sério, e
 * embrulhá-los num composable único fazia a lista medir a preparação inteira de uma vez.
 */
private fun androidx.compose.foundation.lazy.LazyListScope.secaoDosPassos(
    passos: List<RecipeStepEntity>,
    viewModel: RecipeEditViewModel,
    apagar: (apagar: () -> Unit, desfazer: () -> Unit) -> Unit,
) {
    // Os passos depois dos ingredientes, que é a ordem de quem cozinha: primeiro
    // junta-se o que é preciso, depois faz-se.
    item {
        SectionHeader(
            title = stringResource(Res.string.recipe_passos),
            modifier = Modifier.padding(top = Spacing.md),
        )
    }

    if (passos.isEmpty()) {
        item {
            Text(
                stringResource(Res.string.recipe_passos_vazio),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    itemsIndexed(passos, key = { _, passo -> passo.id }) { indice, passo ->
        LinhaDePasso(
            passo = passo,
            indice = indice,
            ultimo = indice == passos.lastIndex,
            viewModel = viewModel,
            apagar = apagar,
        )
    }

    item {
        SecondaryButton(
            text = stringResource(Res.string.recipe_passo_novo),
            onClick = viewModel::escreverPasso,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** O que a receita vale por 100 g, que é a unidade em que a app guarda tudo. */
@Composable
private fun CartaoPor100(state: RecipeEditState) {
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.recipe_per100), style = MaterialTheme.typography.titleSmall)
        val m = macroInitials()
        Text(
            "${state.nutrition.kcalPer100} ${stringResource(Res.string.common_kcal)} · " +
                "${m.p} ${fmtG(state.nutrition.proteinPer100)} · " +
                "${m.c} ${fmtG(state.nutrition.carbsPer100)} · " +
                "${m.f} ${fmtG(state.nutrition.fatPer100)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
