package ru.itdo.app.ui.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import ru.itdo.app.core.AppContainer
import ru.itdo.app.data.model.Post

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(container: AppContainer) {
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var newPostText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        loading = true
        runCatching { container.repository.feed(1, "for_you") }
            .onSuccess { posts = it.posts; error = it.error }
            .onFailure { error = it.message }
        loading = false
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(topBar = { TopAppBar(title = { Text("Лента") }) }) { padding ->
        Column(Modifier.padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(8.dp)) {
                OutlinedTextField(
                    value = newPostText,
                    onValueChange = { newPostText = it },
                    placeholder = { Text("Что нового?") },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    val text = newPostText
                    if (text.isNotBlank()) {
                        scope.launch {
                            runCatching { container.repository.createPost(text) }
                            newPostText = ""
                            load()
                        }
                    }
                }) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Отправить") }
            }

            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn {
                    items(posts, key = { it.id }) { post ->
                        PostItem(post) { liked ->
                            scope.launch {
                                runCatching {
                                    if (liked) container.repository.like(post.id)
                                    else container.repository.unlike(post.id)
                                }
                                load()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PostItem(post: Post, onToggleLike: (Boolean) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                AsyncImage(
                    model = post.author?.avatar,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(post.author?.displayName ?: post.author?.username ?: "Пользователь", style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(8.dp))
            post.text?.let { Text(it) }
            post.media?.firstOrNull()?.let {
                Spacer(Modifier.height(8.dp))
                AsyncImage(model = it, contentDescription = null, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                IconButton(onClick = { onToggleLike(!post.likedByMe) }) {
                    Icon(
                        if (post.likedByMe) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Лайк"
                    )
                }
                Text("${post.likesCount}")
                Spacer(Modifier.width(16.dp))
                Text("💬 ${post.commentsCount}")
                Spacer(Modifier.width(16.dp))
                Text("🔁 ${post.repostsCount}")
            }
        }
    }
}
