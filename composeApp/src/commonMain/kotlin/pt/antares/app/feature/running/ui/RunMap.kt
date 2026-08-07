package pt.antares.app.feature.running.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun RunMap(
    path: List<Pair<Double, Double>>,
    modifier: Modifier,
    follow: Boolean,
)
