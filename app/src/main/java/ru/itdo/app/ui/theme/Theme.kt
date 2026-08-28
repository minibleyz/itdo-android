package ru.itdo.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Строго Material3 ColorScheme, но со значениями из фирменной палитры сайта
// (см. Color.kt) — чтобы клиент не выглядел как generic M3-демо со стоковым
// фиолетовым baseline-цветом, а был частью того же бренда, что и веб-версия.
private val LightColors = lightColorScheme(
    primary = ItdoLightPrimary,
    onPrimary = ItdoLightOnPrimary,
    secondary = ItdoLightSecondary,
    onSecondary = ItdoLightOnPrimary,
    background = ItdoLightBackground,
    onBackground = ItdoLightOnBackground,
    surface = ItdoLightSurface,
    onSurface = ItdoLightOnBackground,
    surfaceVariant = ItdoLightSurfaceVariant,
    onSurfaceVariant = ItdoLightOnSurfaceVariant,
    error = ItdoLightError,
    onError = ItdoLightOnPrimary,
    outline = ItdoLightOutline
)

private val DarkColors = darkColorScheme(
    primary = ItdoDarkPrimary,
    onPrimary = ItdoDarkOnPrimary,
    secondary = ItdoDarkSecondary,
    onSecondary = ItdoDarkOnPrimary,
    background = ItdoDarkBackground,
    onBackground = ItdoDarkOnBackground,
    surface = ItdoDarkSurface,
    onSurface = ItdoDarkOnBackground,
    surfaceVariant = ItdoDarkSurfaceVariant,
    onSurfaceVariant = ItdoDarkOnSurfaceVariant,
    error = ItdoDarkError,
    onError = ItdoDarkOnPrimary,
    outline = ItdoDarkOutline
)

@Composable
fun ItdoTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
