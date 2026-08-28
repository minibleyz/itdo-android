package ru.itdo.app.ui.auth

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Реальный виджет hCaptcha через WebView (см. https://docs.hcaptcha.com/).
 * Бэкенд требует токен hCaptcha при каждом входе по паролю без TOTP и при
 * каждой регистрации (см. api/auth/login.php -> requireLoginCaptcha,
 * api/auth/register.php, api/lib/security.php -> hcaptchaVerify).
 *
 * Загружает мини-HTML-страницу с оффициальным JS-виджетом hCaptcha и
 * передаёт полученный токен обратно в Compose через JavascriptInterface —
 * без ручного ввода токена пользователем.
 *
 * baseUrl указывает на реальный домен бэкенда, т.к. hCaptcha привязывает
 * ключ к зарегистрированным доменам.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HCaptchaWidget(
    sitekey: String,
    modifier: Modifier = Modifier,
    onToken: (String) -> Unit,
    onExpired: () -> Unit = {}
) {
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    AndroidView(
        modifier = modifier.fillMaxWidth().height(90.dp),
        factory = {
            WebView(context).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = WebViewClient()

                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onToken(token: String) {
                        mainHandler.post { onToken(token) }
                    }

                    @JavascriptInterface
                    fun onExpired() {
                        mainHandler.post { onExpired() }
                    }
                }, "AndroidHCaptcha")

                val theme = if (darkTheme) "dark" else "light"
                val html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <script src="https://js.hcaptcha.com/1/api.js" async defer></script>
                        <style>
                            html, body {
                                margin: 0; padding: 0; background: transparent;
                                display: flex; align-items: center; justify-content: center;
                            }
                        </style>
                    </head>
                    <body>
                        <div class="h-captcha"
                             data-sitekey="$sitekey"
                             data-theme="$theme"
                             data-callback="onHCaptchaSuccess"
                             data-expired-callback="onHCaptchaExpired"
                             data-error-callback="onHCaptchaExpired"></div>
                        <script>
                            function onHCaptchaSuccess(token) { AndroidHCaptcha.onToken(token); }
                            function onHCaptchaExpired() { AndroidHCaptcha.onExpired(); }
                        </script>
                    </body>
                    </html>
                """.trimIndent()

                loadDataWithBaseURL(
                    "https://itdo.bleyzos.ru/",
                    html,
                    "text/html",
                    "utf-8",
                    null
                )
            }
        }
    )
}
