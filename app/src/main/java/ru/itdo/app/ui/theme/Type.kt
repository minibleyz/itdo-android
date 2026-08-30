package ru.itdo.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.GoogleFont
import ru.itdo.app.R
import ru.itdo.app.core.DeviceServices

// Downloadable Fonts (Google Fonts через Google Play Services) — не тащим
// бинарные .ttf в репозиторий, шрифт подгружается на устройстве по запросу
// и кэшируется системой. Требует GMS (см. hasGmsFonts ниже) и сертификаты
// в res/values/font_certs.xml.
private val googleFontsProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val unbounded = GoogleFont("Unbounded")

/** Unbounded, только нужные начертания — Bold (700) и Black (900). */
val UnboundedFontFamily = FontFamily(
    Font(googleFont = unbounded, fontProvider = googleFontsProvider, weight = FontWeight.Bold),
    Font(googleFont = unbounded, fontProvider = googleFontsProvider, weight = FontWeight.Black)
)

/**
 * Downloadable Fonts работают только через Google Play Services — на
 * Huawei/Honor без GMS (обычное дело для EMUI/MagicOS-прошивок, см.
 * core/DeviceServices.kt) запрос к com.google.android.gms.fonts просто
 * не может отработать. Явно проверяем это и на таких устройствах отдаём
 * системный шрифт вместо того, чтобы дать Compose тихо (и не сразу)
 * откатиться самому — тут ItdoTheme применяет собственную типографику,
 * так что откат ощутим только полужирностью, не критично.
 *
 * HMS (Huawei Mobile Services) у Huawei НЕ имеет прямого аналога Google
 * Fonts provider — там нет отдельного "HMS Fonts Kit", поэтому для
 * Huawei/Honor без GMS используем системный шрифт, а не пытаемся
 * подключить несуществующий HMS-эквивалент. DeviceServices.detect()
 * при этом всё равно вызывается и логирует MobileServices.HMS для
 * будущих функций (пуш и т.п.), которым HMS-аналог уже нужен по-настоящему.
 */
@Composable
fun rememberUnboundedFontFamily(): FontFamily {
    val context = LocalContext.current
    val hasGmsFonts = remember(context) { DeviceServices.hasGms(context) }
    return if (hasGmsFonts) UnboundedFontFamily else FontFamily.Default
}
