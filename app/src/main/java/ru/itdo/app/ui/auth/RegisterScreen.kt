package ru.itdo.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.itdo.app.core.AppContainer

@Composable
fun RegisterScreen(
    container: AppContainer,
    onRegistered: () -> Unit,
    onBack: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var captchaToken by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
        OutlinedTextField(captchaToken, { captchaToken = it }, label = { Text("hCaptcha токен") }, singleLine = true, modifier = Modifier.fillMaxWidth())

        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                error = null; loading = true
                scope.launch {
                    val resp = runCatching {
                        container.repository.register(username.trim(), email.trim(), password, captchaToken.ifBlank { null })
                    }
                    loading = false
                    resp.onSuccess {
                        if (it.accessToken != null) onRegistered() else error = it.error ?: "Ошибка регистрации"
                    }.onFailure { error = it.message ?: "Ошибка сети" }
                }
            },
            enabled = !loading && username.isNotBlank() && email.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loading) CircularProgressIndicator(modifier = Modifier.size(18.dp)) else Text("Зарегистрироваться")
        }

        TextButton(onClick = onBack) { Text("Назад ко входу") }
    }
}
