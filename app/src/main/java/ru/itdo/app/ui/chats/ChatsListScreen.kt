@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package ru.itdo.app.ui.chats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.itdo.app.core.AppContainer
import ru.itdo.app.data.model.Conversation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsListScreen(container: AppContainer, onOpenConversation: (Int) -> Unit) {
    var conversations by remember { mutableStateOf<List<Conversation>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        runCatching { container.repository.conversations() }
            .onSuccess { conversations = it.conversations; error = it.error }
            .onFailure { error = it.message }
        loading = false
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Сообщения") }) }) { padding ->
        Column(Modifier.padding(padding)) {
            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { LoadingIndicator() }
            } else if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            } else if (conversations.isEmpty()) {
                Text("Пока нет диалогов", modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn {
                    items(conversations, key = { it.id }) { conv ->
                        ListItem(
                            headlineContent = { Text(conv.peer?.displayName ?: conv.title ?: "Диалог #${conv.id}") },
                            supportingContent = { Text(conv.lastMessage?.text ?: "") },
                            trailingContent = {
                                if (conv.unreadCount > 0) Badge { Text("${conv.unreadCount}") }
                            },
                            modifier = Modifier.clickable { onOpenConversation(conv.id) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
