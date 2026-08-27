package ru.itdo.app.ui.chats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.itdo.app.core.AppContainer
import ru.itdo.app.data.model.Message

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(container: AppContainer, conversationId: Int) {
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var text by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        runCatching { container.repository.messages(conversationId) }
            .onSuccess { messages = it.messages }
    }

    LaunchedEffect(conversationId) { load() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Диалог") }) },
        bottomBar = {
            Row(Modifier.fillMaxWidth().padding(8.dp)) {
                OutlinedTextField(
                    value = text, onValueChange = { text = it },
                    placeholder = { Text("Сообщение...") },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    val t = text
                    if (t.isNotBlank()) {
                        scope.launch {
                            runCatching { container.repository.sendMessage(conversationId, t) }
                            text = ""
                            load()
                        }
                    }
                }) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Отправить") }
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(messages, key = { it.id }) { msg ->
                Text(msg.text ?: "", modifier = Modifier.padding(8.dp))
            }
        }
    }
}
