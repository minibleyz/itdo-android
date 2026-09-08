package ru.itdo.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * ПЕРЕНЕСЕНО с приоритетом на iOS-клиент (minibleyz/itdo-ios,
 * ITDOApp/Views/Components/DesignTokens.swift) по прямому запросу —
 * вёрстка/цвета должны совпадать в первую очередь с iOS, а не с вебом
 * (там "тёплая бумага", это осознанно другая, более старая ветка бренда).
 * iOS: adaptive(dark:"#000000", light:"#FFFFFF") и т.д. — ниже те же
 * значения захардкожены отдельно на светлую/тёмную Compose-схему.
 */

// ---- Светлая тема (iOS light: DesignTokens.background = "#FFFFFF") ----
val ItdoLightBackground = Color(0xFFFFFFFF) // iOS background (light)
val ItdoLightSurface = Color(0xFFF5F5F7)    // iOS backgroundBlock (light)
val ItdoLightSurfaceVariant = Color(0xFFF0F0F2) // iOS backgroundSecondary (light)
val ItdoLightOnBackground = Color(0xFF0A0A0A) // iOS textPrimary (light)
val ItdoLightOnSurfaceVariant = Color(0xFF8C8C8C) // iOS textSecondary (light, ~45% black)
val ItdoLightPrimary = Color(0xFF0080FF) // iOS accentPrimary
val ItdoLightOnPrimary = Color(0xFFFFFFFF) // iOS textInverse (light)
val ItdoLightSecondary = Color(0xFF3B82F6) // iOS accentSecondary
val ItdoLightError = Color(0xFFF91880) // iOS accentLike
val ItdoLightOutline = Color(0x1F000000) // iOS border (light, black 12%)

// ---- Доп. поверхности для ПОЛНОГО ColorScheme. Без явного переопределения
// этих токенов Material3 подставляет свою дефолтную фиолетовую baseline-
// палитру (primaryContainer/secondaryContainer/tertiary/errorContainer и
// т.п.) — именно она вылезала фиолетовыми пятнами на нижней навигации,
// чипах, полях ввода и т.д. поверх нашей бежевой темы. ----
val ItdoLightPrimaryContainer = Color(0xFFD6EAFF)
val ItdoLightOnPrimaryContainer = Color(0xFF00325C)
val ItdoLightSecondaryContainer = Color(0xFFDCE8FF)
val ItdoLightOnSecondaryContainer = Color(0xFF00204A)
val ItdoLightTertiary = ItdoLightSecondary
val ItdoLightOnTertiary = ItdoLightOnPrimary
val ItdoLightTertiaryContainer = ItdoLightSecondaryContainer
val ItdoLightOnTertiaryContainer = ItdoLightOnSecondaryContainer
val ItdoLightErrorContainer = Color(0xFFFFD9E8)
val ItdoLightOnErrorContainer = Color(0xFF5C0028)
val ItdoLightOutlineVariant = Color(0x14000000)
val ItdoLightInverseSurface = Color(0xFF1C1C1C)
val ItdoLightInverseOnSurface = Color(0xFFF5F5F5)

// ---- Тёмная тема (iOS dark: DesignTokens.background = "#000000") ----
val ItdoDarkBackground = Color(0xFF000000) // iOS background (dark)
val ItdoDarkSurface = Color(0xFF1C1C1C)    // iOS backgroundBlock (dark)
val ItdoDarkSurfaceVariant = Color(0xFF222222) // iOS backgroundSecondary (dark)
val ItdoDarkOnBackground = Color(0xFFF5F5F5) // iOS textPrimary (dark)
val ItdoDarkOnSurfaceVariant = Color(0x80FFFFFF) // iOS textSecondary (dark, white 50%)
val ItdoDarkPrimary = Color(0xFF0080FF) // iOS accentPrimary (совпадает в обеих темах)
val ItdoDarkOnPrimary = Color(0xFF000000) // iOS textInverse (dark)
val ItdoDarkSecondary = Color(0xFF3B82F6) // iOS accentSecondary
val ItdoDarkError = Color(0xFFF91880) // iOS accentLike
val ItdoDarkOutline = Color(0x26FFFFFF) // iOS border (dark, white 15%)
val ItdoAccentRepost = Color(0xFF00BA7C) // iOS accentRepost — для реакции "репост"

// ---- Доп. поверхности для тёмной темы — см. комментарий у светлых выше. ----
val ItdoDarkPrimaryContainer = Color(0xFF00477E)
val ItdoDarkOnPrimaryContainer = Color(0xFFD6EAFF)
val ItdoDarkSecondaryContainer = Color(0xFF1D3A66)
val ItdoDarkOnSecondaryContainer = Color(0xFFDCE8FF)
val ItdoDarkTertiary = ItdoDarkSecondary
val ItdoDarkOnTertiary = ItdoDarkOnPrimary
val ItdoDarkTertiaryContainer = ItdoDarkSecondaryContainer
val ItdoDarkOnTertiaryContainer = ItdoDarkOnSecondaryContainer
val ItdoDarkErrorContainer = Color(0xFF7A0038)
val ItdoDarkOnErrorContainer = Color(0xFFFFD9E8)
val ItdoDarkOutlineVariant = Color(0x1AFFFFFF)
val ItdoDarkInverseSurface = Color(0xFFF5F5F5)
val ItdoDarkInverseOnSurface = Color(0xFF1C1C1C)

// backgroundHover/backgroundActive/borderSubtle из iOS DesignTokens — нужны
// экранам, которые раньше ссылались на удалённый object IosDesignTokens
// (сама палитра теперь и есть ItdoLight*/ItdoDark* выше, дублировать незачем).
val ItdoLightHover = Color(0x0F000000) // black 6%
val ItdoLightActive = Color(0x1A000000) // black 10%
val ItdoLightBorderSubtle = Color(0x12000000) // black 7%
val ItdoDarkHover = Color(0x14FFFFFF) // white 8%
val ItdoDarkActive = Color(0x1FFFFFFF) // white 12%
val ItdoDarkBorderSubtle = Color(0x0DFFFFFF) // white 5%
