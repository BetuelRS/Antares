package pt.antares.app.feature.fooddata

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*

enum class AddMode { SEARCH, SCAN, PHOTO, DESCRIBE, QUICK }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntrySheet(
    onPick: (AddMode) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.xl)) {
            Text(
                stringResource(Res.string.add_entry_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            )
            AddOption(
                icon = Icons.Default.Search,
                tint = MaterialTheme.colorScheme.primary,
                title = stringResource(Res.string.add_entry_search),
                desc = stringResource(Res.string.add_entry_search_desc),
                onClick = { onPick(AddMode.SEARCH) },
            )
            AddOption(
                icon = Icons.Default.QrCodeScanner,
                tint = MaterialTheme.colorScheme.tertiary,
                title = stringResource(Res.string.add_entry_scan),
                desc = stringResource(Res.string.add_entry_scan_desc),
                onClick = { onPick(AddMode.SCAN) },
            )
            AddOption(
                icon = Icons.Default.PhotoCamera,
                tint = MaterialTheme.colorScheme.secondary,
                title = stringResource(Res.string.add_entry_photo),
                desc = stringResource(Res.string.add_entry_photo_desc),
                onClick = { onPick(AddMode.PHOTO) },
            )
            AddOption(
                icon = Icons.Default.AutoAwesome,
                tint = MaterialTheme.colorScheme.secondary,
                title = stringResource(Res.string.add_entry_describe),
                desc = stringResource(Res.string.add_entry_describe_desc),
                onClick = { onPick(AddMode.DESCRIBE) },
            )

            AddOption(
                icon = Icons.Default.Bolt,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                title = stringResource(Res.string.add_entry_quick),
                desc = stringResource(Res.string.add_entry_quick_desc),
                onClick = { onPick(AddMode.QUICK) },
            )
        }
    }
}

@Composable
private fun AddOption(icon: ImageVector, tint: androidx.compose.ui.graphics.Color, title: String, desc: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()

            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            // Decorativo: a linha tem o rótulo ao lado e é ela que se toca.
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .background(tint.copy(alpha = 0.14f), CircleShape)
                .padding(10.dp)
                .size(24.dp),
        )
        Spacer(Modifier.width(Spacing.md))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
