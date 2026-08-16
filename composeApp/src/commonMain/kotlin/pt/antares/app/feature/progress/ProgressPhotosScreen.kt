package pt.antares.app.feature.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.database.entities.ProgressPhotoEntity
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.bodyWeightWithUnit
import pt.antares.app.core.designsystem.rememberUnitSystem
import pt.antares.app.core.designsystem.fmtG
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.ConfirmDialog
import pt.antares.app.core.designsystem.components.EmptyState
import pt.antares.app.core.designsystem.components.PrimaryButton
import pt.antares.app.core.designsystem.components.SecondaryButton
import pt.antares.app.core.designsystem.components.SplitRow
import pt.antares.app.core.util.dayShortDated
import pt.antares.app.core.util.rememberImagePicker
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*

@Composable
fun ProgressPhotosScreen(
    onBack: () -> Unit,
    viewModel: ProgressPhotosViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var apagando by remember { mutableStateOf<ProgressPhotoEntity?>(null) }

    val picker = rememberImagePicker { imagem -> viewModel.add(imagem.base64) }

    Scaffold(
        topBar = {
            AntaresTopBar(title = stringResource(Res.string.photos_title), onBack = onBack)
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = Spacing.lg),
        ) {

            Text(
                stringResource(Res.string.photos_privacy),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = Spacing.sm),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                PrimaryButton(
                    text = stringResource(Res.string.photos_take),
                    onClick = picker::takePhoto,
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton(
                    text = stringResource(Res.string.photos_pick),
                    onClick = picker::pickFromGallery,
                    modifier = Modifier.weight(1f),
                )
            }

            if (state.photos.isEmpty()) {
                EmptyState(
                    title = stringResource(Res.string.photos_empty_title),
                    subtitle = stringResource(Res.string.photos_empty_subtitle),
                )
                return@Column
            }

            if (state.canCompare) {
                ComparisonCard(state.photos.first(), state.photos.last())
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(state.photos.reversed(), key = { it.id }) { foto ->
                    PhotoCard(foto, onDelete = { apagando = foto })
                }
            }
        }
    }

    apagando?.let { foto ->
        ConfirmDialog(
            title = stringResource(Res.string.photos_delete_title),
            message = stringResource(Res.string.photos_delete_body),
            confirmLabel = stringResource(Res.string.common_delete),
            dismissLabel = stringResource(Res.string.common_cancel),
            onConfirm = {
                viewModel.remove(foto.id)
                apagando = null
            },
            onDismiss = { apagando = null },
        )
    }
}

@Composable
private fun ComparisonCard(antes: ProgressPhotoEntity, depois: ProgressPhotoEntity) {
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(Res.string.photos_compare_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            ComparisonSide(antes, Modifier.weight(1f))
            ComparisonSide(depois, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ComparisonSide(foto: ProgressPhotoEntity, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        PhotoImage(foto)
        Text(
            dayShortDated(foto.epochDay),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        foto.weightKgSnapshot?.let {
            Text(bodyWeightWithUnit(it, rememberUnitSystem()), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PhotoCard(foto: ProgressPhotoEntity, onDelete: () -> Unit) {
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        SplitRow(
            leading = {
                Column {
                    Text(dayShortDated(foto.epochDay), style = MaterialTheme.typography.bodyLarge)
                    foto.weightKgSnapshot?.let {
                        Text(
                            bodyWeightWithUnit(it, rememberUnitSystem()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            trailing = {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(Res.string.common_delete),
                    )
                }
            },
        )
        PhotoImage(foto, Modifier.padding(top = Spacing.sm))
        foto.note?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
internal fun PhotoImage(foto: ProgressPhotoEntity, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(PHOTO_ASPECT)
            .clip(RoundedCornerShape(Spacing.sm))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        AsyncImage(
            model = foto.localPath,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

internal const val PHOTO_ASPECT = 0.75f
