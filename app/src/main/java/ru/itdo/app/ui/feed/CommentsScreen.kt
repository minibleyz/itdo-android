package ru.itdo.app.ui.feed

import androidx.compose.foundation.background
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
import ru.itdo.app.data.model.Comment
import ru.itdo.app.data.model.Post
import ru.itdo.app.ui.components.PinBadges

/**
 * Comments screen for posts — 1:1 with iOS CommentsView.
 */
@Composable
fun CommentsScreen(
    container: AppContainer,
    post: Post,
    onNavigateBack: () -> Unit = {}
) {
    var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var newComment by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        loading = true
        runCatching { container.repository.comments(post.id) }
            .onSuccess { comments = it.comments; error = it.error }
            .onFailure { error = it.message }
        loading = false
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Комментарии (${comments.size})") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        bottomBar = {
            CommentComposer(
                text = newComment,
                onTextChange = { newComment = it },
                onSend = {
                    if (newComment.isBlank() || sending) return@CommentComposer
                    scope.launch {
                        sending = true
                        runCatching { container.repository.addComment(post.id, newComment.trim()) }
                        newComment = ""
                        sending = false
                        load()
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                loading && comments.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.onBackground)
                }
                comments.isEmpty() -> {
                    EmptyState()
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(comments, key = { it.id }) { comment ->
                            CommentItem(comment)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentComposer(text: String, onTextChange: (String) -> Unit, onSend: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F7))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text("Написать комментарий...", color = Color(0x8C8C8C)) },
            modifier = Modifier.weight(1f).heightIn(min = 40.dp),
            shape = RoundedCornerShape(18.dp),
            maxLines = 4
        )
        Spacer(Modifier.width(8.dp))
        IconButton(
            onClick = onSend,
            enabled = text.isNotBlank()
        ) {
            Icon(
                Icons.Default.Send,
                contentDescription = "Отправить",
                tint = if (text.isNotBlank()) Color(0xFF0080FF) else Color(0x8C8C8C)
            )
        }
    }
}

@Composable
private fun CommentItem(comment: Comment) {
    Row(verticalAlignment = Alignment.Top) {
        AsyncImage(
            model = comment.author?.avatar,
            contentDescription = null,
            modifier = Modifier.size(36.dp).clip(CircleShape)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    comment.author?.displayName ?: "Аноним",
                    color = Color(0xFF0A0A0A),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (comment.author?.isVerified == true) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF0080FF), modifier = Modifier.size(14.dp))
                }
            }
            comment.text?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, color = Color(0xFF0A0A0A), fontSize = 14.sp, lineHeight = 18.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Icon(Icons.Default.FavoriteBorder, contentDescription = null, tint = Color(0x8C8C8C), modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text("${comment.likesCount}", color = Color(0x8C8C8C), fontSize = 11.sp)
                Spacer(Modifier.width(14.dp))
                Text(comment.createdAt ?: "", color = Color(0x8C8C8C), fontSize = 11.sp)
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
            Icons.Default.ChatBubbleOutline,
            contentDescription = null,
            tint = Color(0x668C8C8C),
            modifier = Modifier.size(36.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text("Пока нет комментариев", color = Color(0x8C8C8C))
    }
}
