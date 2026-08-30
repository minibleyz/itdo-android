@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package ru.itdo.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.itdo.app.BuildConfig
import ru.itdo.app.core.AppContainer
import ru.itdo.app.ui.theme.UnboundedFontFamily

/**
 * Бэкенд требует hCaptcha для каждого входа по паролю без TOTP (см.
 * api/auth/login.php -> requireLoginCaptcha). Sitekey подтягивается с
 * сервера через auth/registration_status.php; если запрос не удался —
 * используется дефолт из BuildConfig.HCAPTCHA_SITE_KEY.
 */
@Composable
fun LoginScreen(
    container: AppContainer,
    onLoggedIn: () -> Unit,
    onGoRegister: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var sitekey by remember { mutableStateOf(BuildConfig.HCAPTCHA_SITE_KEY) }
    var captchaToken by remember { mutableStateOf<String?>(null) }
    var captchaKey by remember { mutableIntStateOf(0) } // для сброса виджета после истечения токена
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var showTwoFactorDialog by remember { mutableStateOf(false) }
    var twoFactorError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun doLogin(totpCode: String?) {
        error = null
        loading = true
        scope.launch {
            val resp = runCatching {
                container.repository.login(username.trim(), password, captchaToken, totpCode)
            }
            loading = false
            resp.onSuccess {
                when {
                    it.accessToken != null -> {
                        showTwoFactorDialog = false
                        onLoggedIn()
                    }
                    it.twoFactorRequired -> {
                        // Первая попытка (без totp_code) — сервер просит 2FA.
                        // Показываем модалку с полем кода, как на iOS, вместо
                        // постоянного поля на экране логина.
                        showTwoFactorDialog = true
                    }
                    it.banned -> error = "Аккаунт заблокирован"
                    else -> {
                        val msg = it.error ?: "Ошибка входа"
                        if (showTwoFactorDialog) twoFactorError = msg else error = msg
                        if (msg.contains("hCaptcha", ignoreCase = true)) {
                            captchaToken = null
                            captchaKey++ // пересоздать WebView с чистым виджетом
                        }
                        // При неверном коде 2FA (msg содержит "двухфакторной") модалка
                        // просто остаётся открытой с ошибкой — код уже установлен выше.
                    }
                }
            }.onFailure {
                val msg = it.message ?: "Ошибка сети"
                if (showTwoFactorDialog) twoFactorError = msg else error = msg
            }
        }
    }

    if (showTwoFactorDialog) {
        var code by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showTwoFactorDialog = false; twoFactorError = null },
            title = { Text("Двухфакторная аутентификация") },
            text = {
                Column {
                    Text("Введите код из приложения-аутентификатора или резервный код")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = code, onValueChange = { code = it },
                        label = { Text("Код 2FA") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    twoFactorError?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = code.isNotBlank() && !loading,
                    onClick = { twoFactorError = null; doLogin(code) }
                ) { Text("Подтвердить") }
            },
            dismissButton = {
                TextButton(onClick = { showTwoFactorDialog = false; twoFactorError = null }) { Text("Отмена") }
            }
        )
    }

    LaunchedEffect(Unit) {
        runCatching { container.repository.registrationStatus() }
            .onSuccess { it.hcaptchaSitekey?.let { key -> if (key.isNotBlank()) sitekey = key } }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Логотип-вордмарк — шрифт Unbounded (Google Fonts, Downloadable
        // Fonts, см. ui/theme/Type.kt), начертание Black (900), в семье
        // также зарегистрирован Bold (700) как более лёгкий вариант.
        Text(
            "ITDO",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = UnboundedFontFamily,
                fontWeight = FontWeight.Black
            )
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = username, onValueChange = { username = it },
            label = { Text("Логин или email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Пароль") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        // Поле кода 2FA больше не показывается на самом экране — как и на iOS,
        // оно всплывает отдельной модалкой, только когда сервер реально
        // ответил two_factor_required (401) на попытку логина без totp_code
        // (см. api/auth/login.php). До этого момента у пользователя может и
        // не быть 2FA вообще, так что поле было лишним шумом на экране.

        // hCaptcha обязателен только при входе без TOTP (см. requireLoginCaptcha
        // в api/auth/login.php). Пока идёт обычный логин (без 2FA-модалки),
        // totp всегда пуст на этом экране — виджет нужен постоянно здесь.
        Spacer(Modifier.height(8.dp))
        key(captchaKey) {
            HCaptchaWidget(
                sitekey = sitekey,
                onToken = { token -> captchaToken = token },
                onExpired = { captchaToken = null }
            )
        }

        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(16.dp))
        Button(
            contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.LargeContainerHeight),
            onClick = { doLogin(null) },
            enabled = !loading && username.isNotBlank() && password.isNotBlank() && captchaToken != null,
            modifier = Modifier.fillMaxWidth().heightIn(ButtonDefaults.LargeContainerHeight)
        ) {
            if (loading) LoadingIndicator(modifier = Modifier.size(24.dp))
            else Text("Войти", style = ButtonDefaults.textStyleFor(ButtonDefaults.LargeContainerHeight))
        }

        TextButton(onClick = onGoRegister) {
            Text("Нет аккаунта? Зарегистрироваться")
        }
    }
}
