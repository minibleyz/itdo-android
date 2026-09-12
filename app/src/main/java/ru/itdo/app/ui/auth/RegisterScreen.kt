@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package ru.itdo.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.itdo.app.BuildConfig
import ru.itdo.app.core.AppContainer

/**
 * hCaptcha обязательна для каждой регистрации (см. api/auth/register.php).
 * Sitekey подтягивается с сервера (auth/registration_status.php), фоллбэк —
 * BuildConfig.HCAPTCHA_SITE_KEY.
 */
@Composable
fun RegisterScreen(
    container: AppContainer,
    onRegistered: () -> Unit,
    onBack: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var sitekey by remember { mutableStateOf(BuildConfig.HCAPTCHA_SITE_KEY) }
    var captchaToken by remember { mutableStateOf<String?>(null) }
    var captchaKey by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        runCatching { container.repository.registrationStatus() }
            .onSuccess { it.hcaptchaSitekey?.let { key -> if (key.isNotBlank()) sitekey = key } }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Регистрация", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(username, { username = it }, label = { Text("Логин") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(password, { password = it }, label = { Text("Пароль") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())

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
            onClick = {
                error = null; loading = true
                scope.launch {
                    val resp = runCatching {
                        container.repository.register(username.trim(), email.trim(), password, captchaToken)
                    }
                    loading = false
                    resp.onSuccess {
                        if (it.accessToken != null) {
                            onRegistered()
                        } else {
                            error = it.error ?: "Ошибка регистрации"
                            if ((it.error ?: "").contains("hCaptcha", ignoreCase = true)) {
                                captchaToken = null
                                captchaKey++
                            }
                        }
                    }.onFailure { error = it.message ?: "Ошибка сети" }
                }
            },
            enabled = !loading && username.isNotBlank() && email.isNotBlank() &&
                password.isNotBlank() && captchaToken != null,
            modifier = Modifier.fillMaxWidth().heightIn(ButtonDefaults.LargeContainerHeight)
        ) {
            if (loading) LoadingIndicator(modifier = Modifier.size(24.dp))
            else Text("Зарегистрироваться", style = ButtonDefaults.textStyleFor(ButtonDefaults.LargeContainerHeight))
        }

        TextButton(onClick = onBack) { Text("Назад ко входу") }
    }
}
