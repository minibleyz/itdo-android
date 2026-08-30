package ru.itdo.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Material 3 Expressive: MaterialExpressiveTheme вместо MaterialTheme +
// MotionScheme.expressive() — «пружинящие» анимации вместо фиксированных
// easing/duration старого M3. ColorScheme — та же фирменная палитра сайта
// (см. Color.kt), конструктор схем (lightColorScheme/darkColorScheme) не
// изменился между обычным и Expressive M3.
//
// ВАЖНО: заполнена ПОЛНАЯ схема (все параметры lightColorScheme/
// darkColorScheme), а не только primary/secondary/background/surface/error/
// outline. Если оставить остальные токены (primaryContainer,
// secondaryContainer, tertiary*, errorContainer, outlineVariant, inverse*)
// незаданными, Material3 подставляет свою дефолтную фиолетовую baseline-
// палитру — она и вылезала фиолетовыми пятнами на нижней навигации, чипах,
// полях ввода и т.п. поверх нашей бежевой темы.
private val LightColors = lightColorScheme(
    primary = ItdoLightPrimary,
    onPrimary = ItdoLightOnPrimary,
    primaryContainer = ItdoLightPrimaryContainer,
    onPrimaryContainer = ItdoLightOnPrimaryContainer,
    secondary = ItdoLightSecondary,
    onSecondary = ItdoLightOnPrimary,
    secondaryContainer = ItdoLightSecondaryContainer,
    onSecondaryContainer = ItdoLightOnSecondaryContainer,
    tertiary = ItdoLightTertiary,
    onTertiary = ItdoLightOnTertiary,
    tertiaryContainer = ItdoLightTertiaryContainer,
    onTertiaryContainer = ItdoLightOnTertiaryContainer,
    background = ItdoLightBackground,
    onBackground = ItdoLightOnBackground,
    surface = ItdoLightSurface,
    onSurface = ItdoLightOnBackground,
    surfaceVariant = ItdoLightSurfaceVariant,
    onSurfaceVariant = ItdoLightOnSurfaceVariant,
    surfaceTint = ItdoLightPrimary,
    inverseSurface = ItdoLightInverseSurface,
    inverseOnSurface = ItdoLightInverseOnSurface,
    inversePrimary = ItdoDarkPrimary,
    error = ItdoLightError,
    onError = ItdoLightOnPrimary,
    errorContainer = ItdoLightErrorContainer,
    onErrorContainer = ItdoLightOnErrorContainer,
    outline = ItdoLightOutline,
    outlineVariant = ItdoLightOutlineVariant,
    scrim = Color.Black
)

private val DarkColors = darkColorScheme(
    primary = ItdoDarkPrimary,
    onPrimary = ItdoDarkOnPrimary,
    primaryContainer = ItdoDarkPrimaryContainer,
    onPrimaryContainer = ItdoDarkOnPrimaryContainer,
    secondary = ItdoDarkSecondary,
    onSecondary = ItdoDarkOnPrimary,
    secondaryContainer = ItdoDarkSecondaryContainer,
    onSecondaryContainer = ItdoDarkOnSecondaryContainer,
    tertiary = ItdoDarkTertiary,
    onTertiary = ItdoDarkOnTertiary,
    tertiaryContainer = ItdoDarkTertiaryContainer,
    onTertiaryContainer = ItdoDarkOnTertiaryContainer,
    background = ItdoDarkBackground,
    onBackground = ItdoDarkOnBackground,
    surface = ItdoDarkSurface,
    onSurface = ItdoDarkOnBackground,
    surfaceVariant = ItdoDarkSurfaceVariant,
    onSurfaceVariant = ItdoDarkOnSurfaceVariant,
    surfaceTint = ItdoDarkPrimary,
    inverseSurface = ItdoDarkInverseSurface,
    inverseOnSurface = ItdoDarkInverseOnSurface,
    inversePrimary = ItdoLightPrimary,
    error = ItdoDarkError,
    onError = ItdoDarkOnPrimary,
    errorContainer = ItdoDarkErrorContainer,
    onErrorContainer = ItdoDarkOnErrorContainer,
    outline = ItdoDarkOutline,
    outlineVariant = ItdoDarkOutlineVariant,
    scrim = Color.Black
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
