package pt.antares.app.core.locale

import androidx.compose.runtime.Composable

@Composable
expect fun currentAppLanguage(): AppLanguage

@Composable
expect fun rememberLanguageSetter(): (AppLanguage) -> Unit
