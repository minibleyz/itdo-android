package ru.itdo.app.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ru.itdo.app.core.AppContainer
import ru.itdo.app.data.model.User

@Composable
fun ProfileScreen(container: AppContainer, onLogout: () -> Unit) {
    var user by remember { mutableStateOf<User?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        runCatching { container.repository.me() }
            .onSuccess { user = it.user; if (it.user == null) error = it.error }
            .onFailure { error = it.message }
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        user?.let { u ->
            AsyncImage(model = u.avatar, contentDescription = null, modifier = Modifier.size(96.dp))
            Spacer(Modifier.height(12.dp))
            Text(u.displayName ?: u.username, style = MaterialTheme.typography.headlineSmall)
            Text("@${u.username}", style = MaterialTheme.typography.bodyMedium)
            u.bio?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }
            Spacer(Modifier.height(12.dp))
            Row {
                Text("Подписчики: ${u.followersCount}")
                Spacer(Modifier.width(16.dp))
                Text("Подписки: ${u.followingCount}")
            }
        } ?: error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(32.dp))
        Button(onClick = onLogout) { Text("Выйти") }
    }
}
