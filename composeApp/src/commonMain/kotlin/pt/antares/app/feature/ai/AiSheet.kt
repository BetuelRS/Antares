package pt.antares.app.feature.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.ai.AiFoodItem
import pt.antares.app.core.ai.AiWarnings
import pt.antares.app.core.database.entities.FoodEntity
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.macroInitials
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.LinhaDaLista
import pt.antares.app.core.designsystem.components.PrimaryButton
import pt.antares.app.core.designsystem.components.SecondaryButton
import pt.antares.app.core.model.MealSlot
import pt.antares.app.core.model.mealSlotLabel
import pt.antares.app.core.util.AppError
import pt.antares.app.core.util.rememberImagePicker
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.ai_add_item
import pt.antares.app.generated.resources.ai_add_title
import pt.antares.app.generated.resources.ai_analyze
import pt.antares.app.generated.resources.ai_items_empty
import pt.antares.app.generated.resources.ai_analyzing
import pt.antares.app.generated.resources.ai_camera
import pt.antares.app.generated.resources.ai_cancel
import pt.antares.app.generated.resources.ai_check
import pt.antares.app.generated.resources.ai_confirm_no
import pt.antares.app.generated.resources.ai_disclaimer
import pt.antares.app.generated.resources.ai_error_generic
import pt.antares.app.generated.resources.ai_error_offline
import pt.antares.app.generated.resources.ai_gallery
import pt.antares.app.generated.resources.ai_menos_dez
import pt.antares.app.generated.resources.ai_mais_dez
import pt.antares.app.generated.resources.ai_grams
import pt.antares.app.generated.resources.ai_hint
import pt.antares.app.generated.resources.ai_meal_name_hint
import pt.antares.app.generated.resources.ai_not_food
import pt.antares.app.generated.resources.ai_per_100g
import pt.antares.app.generated.resources.ai_quota_banner
import pt.antares.app.generated.resources.ai_paused
import pt.antares.app.generated.resources.ai_quota_over
import pt.antares.app.generated.resources.ai_remove
import pt.antares.app.generated.resources.ai_review_hint
import pt.antares.app.generated.resources.ai_review_title
import pt.antares.app.generated.resources.ai_save
import pt.antares.app.generated.resources.ai_save_as_meal
import pt.antares.app.generated.resources.ai_saved_as_meal
import pt.antares.app.generated.resources.ai_source_estimated
import pt.antares.app.generated.resources.ai_swap
import pt.antares.app.generated.resources.ai_swap_title
import pt.antares.app.generated.resources.ai_title_photo
import pt.antares.app.generated.resources.ai_title_text
import pt.antares.app.generated.resources.ai_total
import pt.antares.app.generated.resources.ai_trial_banner
import pt.antares.app.generated.resources.ai_trial_over
import pt.antares.app.generated.resources.ai_too_short
import pt.antares.app.generated.resources.ai_unclear_image
import pt.antares.app.generated.resources.ai_vague_item
import pt.antares.app.generated.resources.search_hint
import kotlin.math.roundToInt

enum class AiMode { TEXT, PHOTO }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiFoodSheet(
    mode: AiMode,
    mealSlot: MealSlot,
    epochDay: Long,
    onDismiss: () -> Unit,

    // O que se ditou na barra de registo rápido. Chega escrito e não analisado: quem ditou
    // lê antes de gastar uma utilização da quota numa frase que o telemóvel ouviu mal.
    initialText: String = "",
    viewModel: AiViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(initialText) { viewModel.prefill(initialText) }

    val picker = rememberImagePicker { image ->
        viewModel.analyzePhoto(image.base64, image.mime)
    }

    LaunchedEffect(state.saved) {
        if (state.saved) {
            viewModel.reset()
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.fecharGuardandoOTexto()
            onDismiss()
        },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            state.usage?.let { QuotaBanner(it.remaining, it.limit, it.trial) }

            when (state.phase) {
                AiPhase.INPUT -> InputStep(
                    mode = mode,
                    text = state.text,
                    onTextChange = viewModel::onTextChange,
                    onAnalyze = viewModel::analyzeText,
                    onCamera = picker::takePhoto,
                    onGallery = picker::pickFromGallery,
                )

                AiPhase.ANALYZING -> AnalyzingStep(onCancel = viewModel::cancel)

                AiPhase.REVIEW -> ReviewStep(
                    state = state,
                    refeicao = mealSlot,
                    accoes = AccoesDaRevisao(
                        onGramsTexto = viewModel::onGramsText,
                        onRemove = viewModel::removeItem,
                        onTrocar = viewModel::abrirTroca,
                        onAcrescentar = viewModel::abrirAcrescento,
                        onGuardarRefeicao = { nome -> viewModel.guardarComoRefeicao(nome, mealSlot) },
                        onConfirm = { viewModel.confirm(mealSlot, epochDay) },
                    ),
                )

                AiPhase.ERROR -> ErrorStep(
                    error = state.error,
                    inputError = state.inputError,

                    trial = state.usage?.trial ?: true,
                    onRetry = viewModel::reset,
                )
            }

        }
    }

    state.procura?.let { procura ->
        ProcuraDeItemSheet(
            procura = procura,
            onTexto = viewModel::procurar,
            onEscolher = viewModel::escolher,
            onFechar = viewModel::fecharProcura,
        )
    }
}

@Composable
private fun QuotaBanner(remaining: Int, limit: Int, trial: Boolean) {
    Text(
        text = if (trial) {
            stringResource(Res.string.ai_trial_banner, remaining, limit)
        } else {
            stringResource(Res.string.ai_quota_banner, remaining)
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun InputStep(
    mode: AiMode,
    text: String,
    onTextChange: (String) -> Unit,
    onAnalyze: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
) {
    Text(
        stringResource(if (mode == AiMode.PHOTO) Res.string.ai_title_photo else Res.string.ai_title_text),
        style = MaterialTheme.typography.titleMedium,
    )

    if (mode == AiMode.TEXT) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(Res.string.ai_hint)) },
            minLines = 2,
        )
        PrimaryButton(
            text = stringResource(Res.string.ai_analyze),
            onClick = onAnalyze,
            modifier = Modifier.fillMaxWidth(),
            enabled = text.isNotBlank(),
        )
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            PrimaryButton(
                text = stringResource(Res.string.ai_camera),
                onClick = onCamera,
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(
                text = stringResource(Res.string.ai_gallery),
                onClick = onGallery,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AnalyzingStep(onCancel: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier)
        Text(stringResource(Res.string.ai_analyzing), style = MaterialTheme.typography.bodyLarge)
    }

    TextButton(onClick = onCancel) { Text(stringResource(Res.string.ai_cancel)) }
}

/**
 * O que se pode fazer na revisão, num sítio só.
 *
 * São seis, e passadas soltas davam uma lista de parâmetros que o detekt recusa — e que,
 * pior, se trocam sem o compilador dizer nada: três delas são `(Int) -> Unit`.
 */
private data class AccoesDaRevisao(
    val onGramsTexto: (Int, String) -> Unit,
    val onRemove: (Int) -> Unit,
    val onTrocar: (Int) -> Unit,
    val onAcrescentar: () -> Unit,
    val onGuardarRefeicao: (String) -> Unit,
    val onConfirm: () -> Unit,
)

@Composable
private fun ReviewStep(state: AiState, refeicao: MealSlot, accoes: AccoesDaRevisao) {
    if (state.notFood) {
        Text(stringResource(Res.string.ai_not_food), style = MaterialTheme.typography.bodyLarge)
        return
    }

    Text(stringResource(Res.string.ai_review_title), style = MaterialTheme.typography.titleMedium)
    Text(
        stringResource(Res.string.ai_review_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    AvisosDaRevisao(state)

    LazyColumn(
        modifier = Modifier.heightIn(max = LISTA_ALTURA),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        itemsIndexed(state.items, key = { i, item -> "$i-${item.name}" }) { index, item ->
            ItemRow(
                item = item,
                gramasTexto = state.gramasTexto.getOrNull(index).orEmpty(),
                onGramsTexto = { accoes.onGramsTexto(index, it) },
                onTrocar = { accoes.onTrocar(index) },
                onRemove = { accoes.onRemove(index) },
            )
        }
    }

    // Acrescentar existe porque o modelo **omite** com a mesma facilidade com que erra: o
    // arroz que ficou tapado pela carne na fotografia não aparece em lista nenhuma, e sem
    // isto a única saída era desistir da revisão e registar tudo à mão.
    SecondaryButton(
        text = stringResource(Res.string.ai_add_item),
        onClick = accoes.onAcrescentar,
        modifier = Modifier.fillMaxWidth(),
    )

    Text(
        stringResource(Res.string.ai_total, state.totalKcal),
        style = MaterialTheme.typography.titleMedium,
    )

    GuardarComoRefeicao(
        podeGuardar = state.items.isNotEmpty(),
        guardada = state.guardadaComoRefeicao,
        onGuardar = accoes.onGuardarRefeicao,
    )

    // Vive aqui e não no corpo da folha, e é uma correção desta versão: estava fora do
    // `when` e aparecia nas quatro fases, erro incluído. Um aviso de que os números são
    // estimados, por cima de um ecrã que ainda não tem números nenhuns, não avisa de nada —
    // gasta-se, e deixa de se ler onde é preciso. É preciso aqui, em frente à lista que se
    // vai gravar, e o `DisclaimerNaRevisaoTest` prende-o a este sítio.
    Text(
        stringResource(Res.string.ai_disclaimer),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    // A razão de o botão estar cinzento fica **ao lado dele**, e não no topo do ecrã com a
    // lista pelo meio. É o padrão que o resto da app segue, e a área 04 do estudo apanhou
    // esta como a excepção: um controlo desativado sem explicação encostada é um beco.
    if (!state.canConfirm) {
        Text(
            stringResource(Res.string.ai_items_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    // **O botão diz onde é que isto cai** — «Registar no almoço». É o mesmo princípio do
    // cabeçalho da pesquisa, e o esboço 03 desenha-o assim: quem chega aqui por um atalho da
    // barra rápida não escolheu a refeição em ecrã nenhum, e o botão é o último sítio onde
    // ela ainda se vê antes de sete registos entrarem no dia.
    PrimaryButton(
        text = stringResource(Res.string.ai_confirm_no, mealSlotLabel(refeicao).lowercase()),
        onClick = accoes.onConfirm,
        modifier = Modifier.fillMaxWidth(),
        enabled = state.canConfirm,
    )
}

@Composable
private fun AvisosDaRevisao(state: AiState) {
    if (state.imagemPoucoClara) {
        Text(
            stringResource(Res.string.ai_unclear_image),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }

    if (state.vague) {
        Text(
            stringResource(Res.string.ai_vague_item),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

/**
 * Guardar o que se acabou de rever como refeição, para repetir sem voltar a pedir à AI.
 *
 * Guarda **estes itens**, e não a refeição do dia: quem já tivesse registado o pão às oito
 * ficava com ele dentro de um modelo chamado «Almoço» sem dar por isso.
 *
 * O campo do nome só aparece depois de se carregar. Guardar um modelo é raro ao pé de
 * confirmar um registo, e um campo sempre à vista empurrava o botão de confirmar para fora
 * do ecrã nos telemóveis pequenos.
 */
@Composable
private fun GuardarComoRefeicao(
    podeGuardar: Boolean,
    guardada: String?,
    onGuardar: (String) -> Unit,
) {
    if (!podeGuardar) return

    if (guardada != null) {
        Text(
            stringResource(Res.string.ai_saved_as_meal, guardada),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
        return
    }

    var aberto by rememberSaveable { mutableStateOf(false) }
    var nome by rememberSaveable { mutableStateOf("") }

    if (!aberto) {
        TextButton(onClick = { aberto = true }) {
            Text(stringResource(Res.string.ai_save_as_meal))
        }
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = nome,
            onValueChange = { nome = it.take(MAX_NOME_DE_REFEICAO) },
            placeholder = { Text(stringResource(Res.string.ai_meal_name_hint)) },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        TextButton(
            onClick = { onGuardar(nome) },
            // Um modelo sem nome fica na lista como uma linha em branco, e depois não se
            // sabe o que é sem o abrir.
            enabled = nome.isNotBlank(),
        ) {
            Text(stringResource(Res.string.ai_save))
        }
    }
}

/**
 * Os dois botões de dez em dez, ao lado do campo.
 *
 * Trabalham sobre o **texto** e não sobre o número gravado: é o campo que manda, e um botão
 * que escrevesse noutro sítio deixava os dois a discordar enquanto alguém escrevia. Um campo
 * vazio ou com letras trata-se como zero — o `+10` de nada é dez, e é o que a pessoa espera
 * de um botão que só sabe somar.
 *
 * Nunca desce abaixo de zero: um alimento com gramas negativas não é uma correcção, é um
 * defeito à espera de ir para o diário.
 */
@Composable
private fun AjusteDeDez(gramasTexto: String, onGramsTexto: (String) -> Unit) {
    fun mover(passo: Int) {
        val actual = gramasTexto.replace(',', '.').toDoubleOrNull() ?: 0.0
        val novo = (actual + passo).coerceAtLeast(0.0)
        onGramsTexto(if (novo % 1.0 == 0.0) novo.toInt().toString() else novo.toString())
    }

    TextButton(onClick = { mover(-PASSO_G) }, contentPadding = PaddingValues(horizontal = Spacing.sm)) {
        Text(stringResource(Res.string.ai_menos_dez))
    }
    TextButton(onClick = { mover(PASSO_G) }, contentPadding = PaddingValues(horizontal = Spacing.sm)) {
        Text(stringResource(Res.string.ai_mais_dez))
    }
}

private const val PASSO_G = 10

@Composable
private fun ItemRow(
    item: AiFoodItem,
    gramasTexto: String,
    onGramsTexto: (String) -> Unit,
    onTrocar: () -> Unit,
    onRemove: () -> Unit,
) {
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(item.name, style = MaterialTheme.typography.bodyLarge)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Escrevível, e é metade da versão: com `−10` e `+10` só, ir de 30 g para
                // 180 g eram quinze toques — e ninguém os dá, aceita-se o número errado.
                OutlinedTextField(
                    value = gramasTexto,
                    onValueChange = onGramsTexto,
                    label = { Text(stringResource(Res.string.ai_grams)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    // Fixa porque o campo é uma coluna de uma lista: com largura livre cada
                    // linha media o seu número e as gramas ficavam em degraus, uma por linha.
                    // Cabe a 200 % — quatro algarismos é tudo o que aqui entra.
                    modifier = Modifier.width(GRAMAS_LARGURA),
                )

                // **A outra metade, e faltava.** O esboço 03 escreve «campo de gramas
                // escrevível, com os ±10 **como acessório**» — o campo substitui a régua para
                // saltos grandes, e os dois botões continuam a servir o ajuste de uma mão, que
                // é como esta folha se usa. Tirá-los foi longe de mais na correcção da 2.17.0,
                // e só se viu ao pôr o desenho ao lado do ecrã.
                AjusteDeDez(gramasTexto, onGramsTexto)

                Text(
                    "${item.kcal} kcal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val m = macroInitials()
            Text(
                "${m.p} ${item.protein.roundToInt()} g · " +
                    "${m.c} ${item.carbs.roundToInt()} g · " +
                    "${m.f} ${item.fat.roundToInt()} g",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            item.assumption?.let { note ->
                Text(
                    note,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }

            // Um item trocado por um alimento do catálogo deixa de aparecer aqui, e é de
            // propósito: vem com confiança 1 e sem estimativa, porque já foi revisto por
            // quem o trocou.
            if (item.needsReview) {
                Text(
                    text = if (item.estimated) {
                        stringResource(Res.string.ai_source_estimated)
                    } else {
                        stringResource(Res.string.ai_check)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                TextButton(onClick = onTrocar) { Text(stringResource(Res.string.ai_swap)) }
                TextButton(onClick = onRemove) { Text(stringResource(Res.string.ai_remove)) }
            }
        }
    }
}

/**
 * A procura que troca ou acrescenta um item, por cima da revisão.
 *
 * É a mesma folha nos dois casos, e a diferença está no título: trocar substitui o item
 * apontado, acrescentar junta um ao fim. Escolher aqui é o que liga o registo ao catálogo —
 * até aqui, tudo o que a AI grava é um retrato solto, sem `foodId`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProcuraDeItemSheet(
    procura: ProcuraDeItem,
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
                stringResource(
                    if (procura.aTrocar) Res.string.ai_swap_title else Res.string.ai_add_title,
                ),
                style = MaterialTheme.typography.titleMedium,
            )

            OutlinedTextField(
                value = procura.texto,
                onValueChange = onTexto,
                placeholder = { Text(stringResource(Res.string.search_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            if (procura.aProcurar) CircularProgressIndicator(modifier = Modifier)

            LazyColumn(
                modifier = Modifier.heightIn(max = PROCURA_ALTURA),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                items(procura.resultados, key = { it.id }) { food ->
                    LinhaDaLista(
                        titulo = food.namePt,
                        subtitulo = stringResource(Res.string.ai_per_100g, food.kcal),
                        onClick = { onEscolher(food) },

                        // Sem cartão: isto já está dentro da folha, e aninhar cartões dá
                        // duas sombras e uma margem que ninguém pediu.
                        emCartao = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorStep(error: AppError?, inputError: Boolean = false, trial: Boolean, onRetry: () -> Unit) {
    val message = when {

        inputError -> stringResource(Res.string.ai_too_short)
        error == AppError.Network -> stringResource(Res.string.ai_error_offline)

        error == AppError.QuotaExceeded -> if (trial) {
            stringResource(Res.string.ai_trial_over)
        } else {
            stringResource(Res.string.ai_quota_over)
        }

        error == AppError.AiPaused -> stringResource(Res.string.ai_paused)
        else -> stringResource(Res.string.ai_error_generic)
    }
    Text(message, style = MaterialTheme.typography.bodyLarge)
    SecondaryButton(
        text = stringResource(Res.string.ai_cancel),
        onClick = onRetry,
        modifier = Modifier.fillMaxWidth(),
    )
}

// Chega para «1234,5» e deixa o rótulo caber. Fixa e não proporcional: um campo que
// encolhesse com o nome do alimento ficava com dois dígitos nos telemóveis pequenos.
private val GRAMAS_LARGURA = 132.dp

private val LISTA_ALTURA = 380.dp

private val PROCURA_ALTURA = 420.dp

private const val MAX_NOME_DE_REFEICAO = 40
