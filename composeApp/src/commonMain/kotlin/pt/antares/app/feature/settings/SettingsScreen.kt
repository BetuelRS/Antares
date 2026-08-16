package pt.antares.app.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.larguraDeLeitura
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.ThemeMode
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.SectionHeader
import pt.antares.app.core.model.MealSlot
import pt.antares.app.core.model.mealSlotLabelDefault
import pt.antares.app.core.locale.AppLanguage
import pt.antares.app.core.locale.currentAppLanguage
import pt.antares.app.core.locale.rememberLanguageSetter
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAdminClick: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val adaptive by viewModel.adaptiveTargets.collectAsState()
    val mealNames by viewModel.mealNames.collectAsState()

    val selectedLanguage = currentAppLanguage()
    val setLanguage = rememberLanguageSetter()

    Scaffold(
        topBar = { AntaresTopBar(title = stringResource(Res.string.settings_general_title), onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .larguraDeLeitura()
                .padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            SectionHeader(title = stringResource(Res.string.settings_language))
            AntaresCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    LanguageRow(
                        label = stringResource(Res.string.settings_language_system),
                        selected = selectedLanguage == AppLanguage.SYSTEM,
                        onClick = { if (selectedLanguage != AppLanguage.SYSTEM) setLanguage(AppLanguage.SYSTEM) },
                    )
                    LanguageRow(
                        label = stringResource(Res.string.settings_language_pt),
                        selected = selectedLanguage == AppLanguage.PT,
                        onClick = { if (selectedLanguage != AppLanguage.PT) setLanguage(AppLanguage.PT) },
                    )
                    LanguageRow(
                        label = stringResource(Res.string.settings_language_en),
                        selected = selectedLanguage == AppLanguage.EN,
                        onClick = { if (selectedLanguage != AppLanguage.EN) setLanguage(AppLanguage.EN) },
                    )
                }
            }
            Text(
                stringResource(Res.string.settings_language_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.sm),
            )

            val themeMode by viewModel.themeMode.collectAsState()
            SectionHeader(title = stringResource(Res.string.settings_appearance))
            AntaresCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    LanguageRow(
                        label = stringResource(Res.string.settings_theme_system),
                        selected = themeMode == ThemeMode.SYSTEM,
                        onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                    )
                    LanguageRow(
                        label = stringResource(Res.string.settings_theme_light),
                        selected = themeMode == ThemeMode.LIGHT,
                        onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                    )
                    LanguageRow(
                        label = stringResource(Res.string.settings_theme_dark),
                        selected = themeMode == ThemeMode.DARK,
                        onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                    )
                }
            }

            SectionHeader(title = stringResource(Res.string.settings_general_behaviour))
            AntaresCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    ToggleRow(
                        title = stringResource(Res.string.settings_adaptive_title),
                        desc = stringResource(Res.string.settings_adaptive_desc),
                        checked = adaptive,
                        onChange = viewModel::setAdaptiveTargets,
                    )
                }
            }

            SectionHeader(title = stringResource(Res.string.settings_meal_names))
            AntaresCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        stringResource(Res.string.settings_meal_names_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = Spacing.sm),
                    )
                    MealSlot.entries.forEach { slot ->
                        val padrao = mealSlotLabelDefault(slot)
                        OutlinedTextField(
                            value = mealNames[slot].orEmpty(),
                            onValueChange = { viewModel.setMealName(slot, it) },

                            label = { Text(padrao) },
                            placeholder = { Text(padrao) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.xs),
                        )
                    }
                }
            }

            SectionHeader(title = stringResource(Res.string.admin_title))
            AntaresCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = false, onClick = onAdminClick)
                        .padding(vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(Res.string.admin_open),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = Spacing.sm))
    }
}

@Composable
private fun ToggleRow(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
