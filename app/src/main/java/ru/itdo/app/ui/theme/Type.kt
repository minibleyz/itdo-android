package ru.itdo.app.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.GoogleFont
import ru.itdo.app.R

// Downloadable Fonts (Google Fonts через Google Play Services) — не тащим
// бинарные .ttf в репозиторий, шрифт подгружается на устройстве по запросу
// и кэшируется системой. Требует google-services на устройстве (обычный
// случай) и сертификаты в res/values/font_certs.xml.
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
