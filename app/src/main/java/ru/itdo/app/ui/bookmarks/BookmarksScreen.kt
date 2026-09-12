package ru.itdo.app.ui.bookmarks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import ru.itdo.app.core.AppContainer
import ru.itdo.app.data.model.Post

/**
 * Bookmarks screen — 1:1 with iOS BookmarksView.
 */
@Composable
fun BookmarksScreen(
    container: AppContainer,
    onNavigateBack: () -> Unit = {},
    onOpenAuthor: (Int) -> Unit = {}
) {
    var bookmarks by remember { mutableStateOf<List<Post>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        loading = true
        runCatching { container.repository.bookmarks() }
            .onSuccess { bookmarks = it.posts; error = it.error }
            .onFailure { error = it.message }
        loading = false
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Закладки") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                loading && bookmarks.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.onBackground)
                }
                bookmarks.isEmpty() -> {
                    EmptyState()
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(bookmarks, key = { it.id }) { post ->
                            PostCardCompact(post, onOpenAuthor = onOpenAuthor)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.BookmarkBorder,
            contentDescription = null,
            tint = Color(0x668C8C8C),
            modifier = Modifier.size(36.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text("Закладок нет", color = Color(0x8C8C8C))
    }
}

@Composable
private fun PostCardCompact(post: Post, onOpenAuthor: (Int) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.dp, Color(0x1F000000), RoundedCornerShape(20.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        if (post.adminPinned || post.isPinned) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PushPin, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(5.dp))
                Text(
                    if (post.adminPinned) "Админ закреп" else "Закреплено",
                    color = Color(0xFF3B82F6),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(6.dp))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = post.author?.avatar,
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    post.author?.displayName ?: "",
                    color = Color(0xFF0A0A0A),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { post.author?.id?.let(onOpenAuthor) }
                )
                Text("@${post.author?.username ?: ""}", color = Color(0x8C8C8C), fontSize = 14.sp)
            }
        }

        post.text?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = Color(0xFF0A0A0A), fontSize = 15.sp, lineHeight = 20.sp)
        }

        post.media?.takeIf { it.isNotEmpty() }?.let { media ->
            Spacer(Modifier.height(10.dp))
            if (media.size == 1) {
                AsyncImage(
                    model = media[0].url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(12.dp))
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    media.take(4).forEach { item ->
                        AsyncImage(
                            model = item.url,
                            contentDescription = null,
                            modifier = Modifier.weight(1f).height(140.dp).clip(RoundedCornerShape(12.dp))
                        )
                    }
                }
            }
        }
    }
}
