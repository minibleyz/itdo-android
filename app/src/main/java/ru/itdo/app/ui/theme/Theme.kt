package ru.itdo.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Material 3 Expressive: MaterialExpressiveTheme вместо MaterialTheme +
// MotionScheme.expressive() — «пружинящие» анимации вместо фиксированных
// easing/duration старого M3. ColorScheme — та же фирменная палитра сайта
// (см. Color.kt), конструктор схем (lightColorScheme/darkColorScheme) не
// изменился между обычным и Expressive M3.
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ItdoTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialExpressiveTheme(
        colorScheme = colors,
        motionScheme = MotionScheme.expressive(),
        content = content
    )
}
