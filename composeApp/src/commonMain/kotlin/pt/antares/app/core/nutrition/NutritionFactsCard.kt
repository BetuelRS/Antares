package pt.antares.app.core.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.fmtG
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*
import kotlin.math.roundToInt

/**
 * A ficha nutricional, usada no alimento, no registo e na receita. Mostra sempre alguma
 * coisa: sem micronutrientes aparece a explicação do [MicroGap] em vez de um cartão vazio.
 *
 * @param expandKey identifica este cartão para o estado de expandido sobreviver à rotação
 *   e não se confundir com o de outro cartão na mesma lista.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NutritionFactsCard(
    breakdown: NutritionBreakdown?,
    gap: MicroGap,

    expandKey: String,
    modifier: Modifier = Modifier,

    source: StringResource? = null,

    sourceLabel: StringResource = Res.string.nutrition_source_label,
) {
    val labels = breakdown?.labels.orEmpty()

    AntaresCard(modifier = modifier) {
        Column {
            Text(
                stringResource(Res.string.nutrition_facts_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(Spacing.sm))

            labels.forEach { MicroRow(it, showBar = false) }

            breakdown?.overLimits?.forEach { over ->
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    stringResource(
                        Res.string.nutrition_over_limit,
                        stringResource(microLabelRes(over.key)),
                        over.pctDv ?: 0,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (breakdown != null && !breakdown.isEmpty) {
                val highlights = breakdown.highlights
                if (highlights.isNotEmpty()) {
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        stringResource(Res.string.nutrition_rich_in),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        // Seis chega para duas linhas num telemóvel; mais e o destaque
                        // deixa de destacar.
                        highlights.take(6).forEach { HighlightChip(it) }
                    }
                }

                // Fechado por omissão: são umas trinta linhas, e abertas empurram para fora
                // do ecrã o que a pessoa veio ver.
                var expanded by rememberSaveable(expandKey) { mutableStateOf(false) }
                Spacer(Modifier.height(Spacing.sm))
                TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(0.dp)) {
                    Text(
                        if (expanded) stringResource(Res.string.nutrition_hide_all)
                        else stringResource(Res.string.nutrition_show_all, breakdown.all.size),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                if (expanded) {
                    if (breakdown.vitamins.isNotEmpty()) {
                        MicroSection(stringResource(Res.string.nutrition_vitamins), breakdown.vitamins)
                    }
                    if (breakdown.minerals.isNotEmpty()) {
                        MicroSection(stringResource(Res.string.nutrition_minerals), breakdown.minerals)
                    }
                    if (breakdown.others.isNotEmpty()) {
                        MicroSection(stringResource(Res.string.nutrition_others), breakdown.others)
                    }
                }
            }

            if (gap != MicroGap.NONE) {
                if (labels.isNotEmpty()) Spacer(Modifier.height(Spacing.sm))
                Text(
                    stringResource(
                        when (gap) {
                            MicroGap.PACKAGED_LABEL -> Res.string.nutrition_gap_label
                            MicroGap.AI_ESTIMATE -> Res.string.nutrition_gap_ai
                            MicroGap.USER_CREATED -> Res.string.nutrition_gap_custom
                            MicroGap.RECIPE_INGREDIENTS -> Res.string.nutrition_gap_recipe
                            else -> Res.string.nutrition_gap_not_measured
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            source?.let {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    "${stringResource(sourceLabel)}: ${stringResource(it)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MicroSection(title: String, values: List<MicroValue>) {
    Spacer(Modifier.height(Spacing.sm))
    Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    values.forEach { MicroRow(it) }
}

@Composable
private fun HighlightChip(value: MicroValue) {
    val high = (value.pctDv ?: 0) >= NutrientClaim.HIGH_IN
    val bg = if (high) MaterialTheme.colorScheme.tertiaryContainer
             else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (high) MaterialTheme.colorScheme.onTertiaryContainer
             else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = Spacing.sm, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(microLabelRes(value.key)), style = MaterialTheme.typography.labelMedium, color = fg)
        Spacer(Modifier.width(Spacing.xs))
        Text(
            "${value.pctDv}%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = fg,
        )
    }
}

@Composable
private fun MicroRow(value: MicroValue, showBar: Boolean = true) {
    // Acima de 10 as casas decimais não acrescentam nada; abaixo, são a diferença entre
    // 0,8 mg e nada.
    val amount = if (value.amount >= 10) value.amount.roundToInt().toString() else fmtG(value.amount)
    val pct = value.pctDv
    Column(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {

            Text(
                stringResource(microLabelRes(value.key)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f, fill = false).padding(end = Spacing.md),
            )

            val salt = if (value.key == Nutrients.SODIUM) {
                " · " + stringResource(Res.string.nutrition_salt, fmtG(value.amount * SALT_PER_SODIUM / 1000))
            } else {
                ""
            }

            // Dois nutrientes em que a percentagem alta é má; a cor tem de dizer o
            // contrário do que diz nos outros.
            val lessIsBetter = value.key == Nutrients.SODIUM || value.key == Nutrients.SAT_FAT
            Text(
                (pct?.let { "$amount ${value.unit} · $it%" } ?: "$amount ${value.unit}") + salt,
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    lessIsBetter && (pct ?: 0) >= NutritionBreakdown.OVER_LIMIT ->
                        MaterialTheme.colorScheme.error
                    !lessIsBetter && pct != null && pct >= NutrientClaim.HIGH_IN ->
                        MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
        }
        if (pct != null && showBar) {
            Spacer(Modifier.height(3.dp))
            LinearProgressIndicator(

                progress = { (pct / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                color = if (pct >= NutrientClaim.HIGH_IN) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                drawStopIndicator = {},
            )
        }
    }
}

fun provenanceRes(p: FoodProvenance, hasMicros: Boolean): StringResource? = when (p) {
    FoodProvenance.CURATED ->
        if (hasMicros) Res.string.nutrition_source_curated
        else Res.string.nutrition_source_curated_plain
    FoodProvenance.TCA -> Res.string.nutrition_source_tca
    FoodProvenance.CIQUAL -> Res.string.nutrition_source_ciqual
    FoodProvenance.USDA -> Res.string.nutrition_source_usda
    FoodProvenance.OFF -> Res.string.nutrition_source_off
    FoodProvenance.AI -> Res.string.nutrition_source_ai
    FoodProvenance.USER -> Res.string.nutrition_source_user
    FoodProvenance.UNKNOWN -> null
}

// O sal mostra-se ao lado do sódio porque é o que vem nos rótulos e o que as pessoas
// reconhecem. O fator é a razão entre as massas moleculares do cloreto de sódio e do sódio.
private const val SALT_PER_SODIUM = 2.5
