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
import ru.itdo.app.core.AppContainer

/**
 * ВНИМАНИЕ: бэкенд требует hCaptcha для каждого входа по паролю (см.
 * api/auth/login.php -> requireLoginCaptcha), кроме случая, когда сразу
 * передаётся totp_code. Для реального прода сюда нужно встроить
 * hCaptcha-виджет через WebView (см. https://docs.hcaptcha.com/) и
 * передать полученный токен в поле captcha. Ниже — поле для ручного
 * ввода токена как временное решение/заглушка на время интеграции.
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
    var captchaToken by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("itdo", style = MaterialTheme.typography.headlineMedium)
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
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = captchaToken, onValueChange = { captchaToken = it },
            label = { Text("hCaptcha токен (временно вручную)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

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
                            captchaToken.ifBlank { null },
                            totp.ifBlank { null }
                        )
                    }
                    loading = false
                    resp.onSuccess {
                        when {
                            it.accessToken != null -> onLoggedIn()
                            it.twoFactorRequired -> error = "Введите код двухфакторной аутентификации"
                            it.banned -> error = "Аккаунт заблокирован"
                            else -> error = it.error ?: "Ошибка входа"
                        }
                    }.onFailure {
                        error = it.message ?: "Ошибка сети"
                    }
                }
            },
            enabled = !loading && username.isNotBlank() && password.isNotBlank(),
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
