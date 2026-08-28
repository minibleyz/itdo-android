package ru.itdo.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.itdo.app.BuildConfig
import ru.itdo.app.core.AppContainer

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
    var totp by remember { mutableStateOf("") }
    var sitekey by remember { mutableStateOf(BuildConfig.HCAPTCHA_SITE_KEY) }
    var captchaToken by remember { mutableStateOf<String?>(null) }
    var captchaKey by remember { mutableIntStateOf(0) } // для сброса виджета после истечения токена
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
        Text("ITDO", style = MaterialTheme.typography.headlineMedium)
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
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = totp, onValueChange = { totp = it },
            label = { Text("Код 2FA (если включён)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        // hCaptcha обязателен только при входе без TOTP (см. requireLoginCaptcha
        // в api/auth/login.php) — если введён код 2FA, виджет не нужен.
        if (totp.isBlank()) {
            Spacer(Modifier.height(8.dp))
            key(captchaKey) {
                HCaptchaWidget(
                    sitekey = sitekey,
                    onToken = { token -> captchaToken = token },
                    onExpired = { captchaToken = null }
                )
            }
        }

        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                error = null
                loading = true
                scope.launch {
                    val resp = runCatching {
                        container.repository.login(
                            username.trim(),
                            password,
                            captchaToken,
                            totp.ifBlank { null }
                        )
                    }
                    loading = false
                    resp.onSuccess {
                        when {
                            it.accessToken != null -> onLoggedIn()
                            it.twoFactorRequired -> error = "Введите код двухфакторной аутентификации"
                            it.banned -> error = "Аккаунт заблокирован"
                            else -> {
                                error = it.error ?: "Ошибка входа"
                                if ((it.error ?: "").contains("hCaptcha", ignoreCase = true)) {
                                    captchaToken = null
                                    captchaKey++ // пересоздать WebView с чистым виджетом
                                }
                            }
                        }
                    }.onFailure {
                        error = it.message ?: "Ошибка сети"
                    }
                }
            },
            enabled = !loading && username.isNotBlank() && password.isNotBlank() &&
                (totp.isNotBlank() || captchaToken != null),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loading) CircularProgressIndicator(modifier = Modifier.size(18.dp))
            else Text("Войти")
        }

        TextButton(onClick = onGoRegister) {
            Text("Нет аккаунта? Зарегистрироваться")
        }
    }
}
