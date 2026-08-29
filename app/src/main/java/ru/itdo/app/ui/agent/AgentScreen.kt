package ru.itdo.app.ui.agent

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ru.itdo.app.BuildConfig
import ru.itdo.app.core.AppContainer
import ru.itdo.app.ui.components.ItdoLoadingScreen

/**
 * Страница ИИ-агента (ai-agent.html) — в отличие от остальных экранов, это
 * не нативная реализация, а полноценный веб-виджет: SSE-стриминг ответов,
 * markdown-рендер, tool-события агента и вся клиентская логика уже написаны
 * и обкатаны в вебе (assets частично инлайн внутри ai-agent.html). Дублировать
 * это нативно в Compose — большой отдельный объём работы, тогда как страница
 * сама по себе не завязана на Discord-подобный UI, а является отдельным
 * self-contained HTML-приложением, которое одинаково хорошо работает в
 * WebView.
 *
 * ВАЖНО про авторизацию: ai-agent.html аутентифицирует все свои fetch()
 * через `credentials: 'include'`, т.е. полагается на cookie, а НЕ на
 * заголовок Authorization (см. requireAuth() в api/config.php:
 * `bearerToken() ?: $_COOKIE['access_token']`). Мобильное приложение хранит
 * только Bearer JWT в TokenStore — сам по себе WebView его не знает и без
 * дополнительных действий страница показала бы юзера как неавторизованного.
 * Поэтому перед загрузкой страницы кладём тот же access_token в cookie
 * WebView'а через CookieManager — сервер одинаково принимает токен что из
 * заголовка (родное REST-API), что из cookie (веб-страницы), см. ту же
 * строку в config.php.
 */
@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentScreen(container: AppContainer) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var reloadTrigger by remember { mutableStateOf(0) }
    val agentUrl = remember { BuildConfig.SITE_BASE_URL.trimEnd('/') + "/ai-agent.html" }
    val siteHost = remember { agentUrl.toUri().host ?: "" }

    // WebView живёт вне Compose-рекомпозиций — держим ссылку, чтобы кнопка
    // "обновить" в топбаре могла дёрнуть reload() без пересоздания вьюхи.
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("ИИ-агент") },
            actions = {
                IconButton(onClick = { reloadTrigger++; webViewRef?.reload() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Обновить")
                }
            }
        )
    }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (loadError != null) {
                Text(
                    loadError ?: "",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp())
                )
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            webViewRef = this
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            // Страница читает/пишет localStorage (последняя открытая
                            // переписка, выбранная модель, флаг "показывать думание") —
                            // без domStorageEnabled это тихо не работало бы.
                            settings.databaseEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false

                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, true)

                            // Кладём тот же Bearer-токен, которым уже авторизован
                            // REST API приложения, как cookie для того же домена —
                            // см. подробное объяснение в комментарии к функции выше.
                            val token = runBlocking { container.tokenStore.accessTokenOrNull() }
                            if (!token.isNullOrEmpty()) {
                                val cookieAttrs = "; Path=/; Secure; SameSite=Lax"
                                cookieManager.setCookie(BuildConfig.SITE_BASE_URL, "access_token=$token$cookieAttrs")
                            }
                            cookieManager.flush()

                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoading = false
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: android.webkit.WebResourceError?
                                ) {
                                    // Ошибки на под-ресурсах (шрифты, favicon и т.п.) не должны
                                    // прятать всю страницу за экраном ошибки — реагируем только
                                    // на провал загрузки главного документа.
                                    if (request?.isForMainFrame == true) {
                                        isLoading = false
                                        loadError = "Не удалось загрузить страницу агента. Проверьте соединение и нажмите обновить."
                                    }
                                }

                                // Внешние ссылки (например, из markdown-ответа агента) открываем
                                // в системном браузере, а не внутри этого WebView — иначе
                                // пользователь мог бы случайно "уйти" со страницы агента без
                                // возможности вернуться кнопкой назад в приложении.
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val uri = request?.url ?: return false
                                    if (uri.host == siteHost) return false
                                    return try {
                                        context.startActivity(
                                            android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                        )
                                        true
                                    } catch (e: Exception) {
                                        false
                                    }
                                }
                            }

                            loadUrl(agentUrl)
                        }
                    },
                    update = { /* URL/токен выставляются один раз в factory — страница сама
                                  управляет своим состоянием (SSE, история) при recomposition. */ }
                )
                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }

    // Токен мог обновиться, пока экран уже открыт (refreshAccessToken в
    // AuthInterceptor работает независимо от WebView) — не отслеживаем это
    // построчно, страница агента живёт в рамках одной сессии открытия экрана,
    // а на следующее открытие AgentScreen соберёт WebView заново со свежим
    // токеном (composable пересоздаётся при уходе с таба, см. AppNav.kt:
    // popUpTo(Tab.Feed.route) держит только один экземпляр таба в стеке).
    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.apply {
                stopLoading()
                webViewRef = null
            }
        }
    }
}

private fun Int.dp() = androidx.compose.ui.unit.dp(this.toFloat())
