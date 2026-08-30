package ru.itdo.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Цвета взяты из фирменной «тёплой бумажной» палитры веб-версии
 * (assets/css/app.css, :root и [data-theme=dark]), чтобы Android-клиент
 * выглядел как продолжение того же бренда, а не generic Material3 демо.
 */

// ---- Светлая тема ("бумага") ----
val ItdoLightBackground = Color(0xFFFAF5EA) // --bg-primary
val ItdoLightSurface = Color(0xFFFFFAF0)    // --block-bg
val ItdoLightSurfaceVariant = Color(0xFFF1E6D2) // --bg-secondary
val ItdoLightOnBackground = Color(0xFF2C241C) // --text-primary
val ItdoLightOnSurfaceVariant = Color(0xFF8A7A63) // --text-secondary
val ItdoLightPrimary = Color(0xFFB0602F) // --accent-primary
val ItdoLightOnPrimary = Color(0xFFFBF6EC) // --text-inverse
val ItdoLightSecondary = Color(0xFF8F4A2A) // --accent-secondary
val ItdoLightError = Color(0xFFC1443A) // --accent-like
val ItdoLightOutline = Color(0xFFE2D3B8) // --border-color

// ---- Доп. поверхности для ПОЛНОГО ColorScheme. Без явного переопределения
// этих токенов Material3 подставляет свою дефолтную фиолетовую baseline-
// палитру (primaryContainer/secondaryContainer/tertiary/errorContainer и
// т.п.) — именно она вылезала фиолетовыми пятнами на нижней навигации,
// чипах, полях ввода и т.д. поверх нашей бежевой темы. ----
val ItdoLightPrimaryContainer = Color(0xFFEFDCC0)
val ItdoLightOnPrimaryContainer = Color(0xFF3D2413)
val ItdoLightSecondaryContainer = Color(0xFFF1E0CE)
val ItdoLightOnSecondaryContainer = Color(0xFF3D2413)
val ItdoLightTertiary = ItdoLightSecondary
val ItdoLightOnTertiary = ItdoLightOnPrimary
val ItdoLightTertiaryContainer = ItdoLightSecondaryContainer
val ItdoLightOnTertiaryContainer = ItdoLightOnSecondaryContainer
val ItdoLightErrorContainer = Color(0xFFF6D8D5)
val ItdoLightOnErrorContainer = Color(0xFF410E0B)
val ItdoLightOutlineVariant = Color(0xFFEDE0C8)
val ItdoLightInverseSurface = Color(0xFF3A2F22)
val ItdoLightInverseOnSurface = Color(0xFFFBF3E4)

// ---- Тёмная тема ("ночная бумага") ----
val ItdoDarkBackground = Color(0xFF22190F) // --bg-primary (dark)
val ItdoDarkSurface = Color(0xFF22190F)    // --block-bg (dark)
val ItdoDarkSurfaceVariant = Color(0xFF2B2015) // --bg-secondary (dark)
val ItdoDarkOnBackground = Color(0xFFF0E4D0) // --text-primary (dark)
val ItdoDarkOnSurfaceVariant = Color(0xFFAB9578) // --text-secondary (dark)
val ItdoDarkPrimary = Color(0xFFD68A4C) // --accent-primary (dark)
val ItdoDarkOnPrimary = Color(0xFF22190F) // --text-inverse (dark)
val ItdoDarkSecondary = Color(0xFFB0602F) // --accent-secondary (dark)
val ItdoDarkError = Color(0xFFE0685A) // --accent-like (dark)
val ItdoDarkOutline = Color(0xFF4A3E2E) // приближение к rgba(240,228,208,.10) поверх тёмного фона

// ---- Доп. поверхности для тёмной темы — см. комментарий у светлых выше. ----
val ItdoDarkPrimaryContainer = Color(0xFF4A3319)
val ItdoDarkOnPrimaryContainer = Color(0xFFF0DABF)
val ItdoDarkSecondaryContainer = Color(0xFF3E2A18)
val ItdoDarkOnSecondaryContainer = Color(0xFFF0DABF)
val ItdoDarkTertiary = ItdoDarkSecondary
val ItdoDarkOnTertiary = ItdoDarkOnPrimary
val ItdoDarkTertiaryContainer = ItdoDarkSecondaryContainer
val ItdoDarkOnTertiaryContainer = ItdoDarkOnSecondaryContainer
val ItdoDarkErrorContainer = Color(0xFF5C1A14)
val ItdoDarkOnErrorContainer = Color(0xFFF9D6D2)
val ItdoDarkOutlineVariant = Color(0xFF3A2E1E)
val ItdoDarkInverseSurface = Color(0xFFF0E4D0)
val ItdoDarkInverseOnSurface = Color(0xFF22190F)

/**
 * Палитра из iOS-клиента (ITDOApp/Views/Components/DesignTokens.swift).
 * Это ДРУГАЯ, чёрно-синяя тема — не совпадает с --bg-primary/--accent-primary
 * из веба (тёплая бумага выше), т.к. iOS-клиент со своей "Liquid Glass"
 * дизайн-системой разошёлся с брендом веба. Сейчас НЕ используется ни на
 * одном экране (лента переведена на ItdoTheme/MaterialTheme.colorScheme) —
 * оставлено на случай, если для чего-то явно попросят стиль "как в iOS".
 */
object IosDesignTokens {
    val background = Color(0xFF000000)
    val backgroundSecondary = Color(0xFF222222)
    val backgroundBlock = Color(0xFF1C1C1C)
    val backgroundHover = Color(0x14FFFFFF) // white 8%
    val backgroundActive = Color(0x1FFFFFFF) // white 12%

    val textPrimary = Color(0xFFF5F5F5)
    val textSecondary = Color(0x80FFFFFF) // white 50%

    val accentPrimary = Color(0xFF0080FF)
    val accentSecondary = Color(0xFF3B82F6)
    val accentLike = Color(0xFFF91880)
    val accentRepost = Color(0xFF00BA7C)

    val border = Color(0x26FFFFFF) // white 15%
    val borderSubtle = Color(0x0DFFFFFF) // white 5%

    val error = Color(0xFFEF4444)
}
