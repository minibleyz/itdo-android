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

/**
 * Палитра из iOS-клиента (ITDOApp/Views/Components/DesignTokens.swift).
 * Это ДРУГАЯ, чёрно-синяя тема — не совпадает с --bg-primary/--accent-primary
 * из веба (тёплая бумага выше), т.к. iOS-клиент со своей "Liquid Glass"
 * дизайн-системой разошёлся с брендом веба. Используется точечно на
 * экранах, где явно просили выглядеть "как в iOS" (сейчас — лента),
 * а не как замена ItdoTheme целиком.
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
