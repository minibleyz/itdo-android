package ru.itdo.app.ui.feed

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import ru.itdo.app.core.AppContainer
import ru.itdo.app.data.model.Post
import ru.itdo.app.data.model.PostMedia
import ru.itdo.app.data.model.PostPoll
import ru.itdo.app.data.model.PostTrack
import ru.itdo.app.data.model.User
import ru.itdo.app.ui.theme.IosDesignTokens as T

/**
 * Лента, свёрстанная 1:1 по ITDOApp/Views/Feed/FeedView.swift +
 * Views/Components/DesignTokens.swift (см. чат) — тёмный фон, табы
 * "Для вас"/"Подписки" с нижней чертой, карточки постов с тем же порядком
 * блоков (пин-лейбл → шапка автора → текст → опрос → трек → медиа →
 * действия) и той же чёрно-синей палитрой.
 */
@Composable
fun FeedScreen(container: AppContainer) {
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableStateOf("for_you") }
    var showComposer by remember { mutableStateOf(false) }
    var composerText by remember { mutableStateOf("") }
    var posting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        loading = true
        runCatching { container.repository.feed(1, tab) }
            .onSuccess { posts = it.posts; error = it.error }
            .onFailure { error = it.message }
        loading = false
    }

    LaunchedEffect(tab) { load() }

    Box(
        Modifier
            .fillMaxSize()
            .background(T.background)
    ) {
        Column(Modifier.fillMaxSize()) {
            TabSwitcher(tab) { tab = it }

            ComposeBox(onClick = { showComposer = true })

            when {
                loading && posts.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = T.textPrimary)
                }
                error != null && posts.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(error!!, color = T.textPrimary.copy(alpha = 0.7f))
                }
                posts.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Пока нет постов", color = T.textSecondary)
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(posts, key = { it.id }) { post ->
                        PostCard(
                            post = post,
                            onLike = {
                                scope.launch {
                                    runCatching {
                                        if (post.liked) container.repository.unlike(post.id)
                                        else container.repository.like(post.id)
                                    }
                                    load()
                                }
                            },
                            onBookmark = {
                                scope.launch {
                                    runCatching { container.repository.toggleBookmark(post.id) }
                                    load()
                                }
                            },
                            onComment = { /* TODO: экран комментариев */ },
                            onRepost = {
                                scope.launch {
                                    runCatching { container.repository.repost(post.id) }
                                    load()
                                }
                            }
                        )
                    }
                }
            }
        }

        if (showComposer) {
            ComposerSheet(
                text = composerText,
                posting = posting,
                onTextChange = { composerText = it },
                onDismiss = { showComposer = false },
                onSubmit = {
                    scope.launch {
                        posting = true
                        runCatching { container.repository.createPost(composerText) }
                        posting = false
                        composerText = ""
                        showComposer = false
                        load()
                    }
                }
            )
        }
    }
}

@Composable
private fun TabSwitcher(tab: String, onTabChange: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .border(width = 1.dp, color = Color.Transparent) // якорь для нижней черты ниже
    ) {
        TabSegment("Для вас", selected = tab == "for_you", modifier = Modifier.weight(1f)) { onTabChange("for_you") }
        TabSegment("Подписки", selected = tab == "following", modifier = Modifier.weight(1f)) { onTabChange("following") }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(T.border)
    )
}

@Composable
private fun TabSegment(title: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clickable(onClick = onClick)
            .background(if (selected) T.accentPrimary.copy(alpha = 0.08f) else Color.Transparent)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            title,
            color = if (selected) T.textPrimary else T.textSecondary,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .height(2.dp)
                .fillMaxWidth(0.6f)
                .background(if (selected) T.accentPrimary else Color.Transparent)
        )
    }
}

@Composable
private fun ComposeBox(onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(T.backgroundBlock)
            .border(1.dp, T.borderSubtle, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(url = null, size = 40.dp)
        Spacer(Modifier.width(12.dp))
        Text("Что происходит?", color = T.textSecondary, fontSize = 17.sp)
    }
}

@Composable
private fun Avatar(url: String?, size: androidx.compose.ui.unit.Dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(T.backgroundSecondary)
            .border(1.dp, T.textPrimary.copy(alpha = 0.25f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (url != null) {
            AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(Icons.Filled.Person, contentDescription = null, tint = T.textSecondary)
        }
    }
}

@Composable
private fun PostCard(
    post: Post,
    onLike: () -> Unit,
    onBookmark: () -> Unit,
    onComment: () -> Unit,
    onRepost: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(T.backgroundBlock)
            .border(1.dp, T.borderSubtle, RoundedCornerShape(20.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        if (post.adminPinned || post.isPinned) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.PushPin, contentDescription = null, tint = T.accentSecondary, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(5.dp))
                Text(
                    if (post.adminPinned) "Админ закреп" else "Закреплено",
                    color = T.accentSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(6.dp))
        }

        PostHeader(post.author)

        post.text?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, color = T.textPrimary, fontSize = 15.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(10.dp))
        }

        post.poll?.let {
            PollView(it)
            Spacer(Modifier.height(10.dp))
        }

        post.track?.let {
            TrackPlayer(it)
            Spacer(Modifier.height(10.dp))
        }

        post.media?.takeIf { it.isNotEmpty() }?.let {
            MediaGallery(it)
            Spacer(Modifier.height(10.dp))
        }

        ActionsRow(post, onLike, onComment, onRepost, onBookmark)
    }
}

@Composable
private fun PostHeader(author: User?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Avatar(url = author?.avatar, size = 40.dp)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    author?.displayName ?: "—",
                    color = T.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (author?.isVerified == true) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Filled.Verified, contentDescription = null, tint = T.accentSecondary, modifier = Modifier.size(14.dp))
                }
            }
            Text("@${author?.username ?: ""}", color = T.textSecondary, fontSize = 14.sp)
        }
    }
}

@Composable
private fun PollView(poll: PostPoll) {
    val total = poll.totalVotes
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(T.backgroundSecondary)
            .padding(12.dp)
    ) {
        poll.options.forEach { option ->
            val pct = if (total > 0) option.votes.toFloat() / total else 0f
            Column(Modifier.padding(bottom = 6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(option.text, color = T.textPrimary, fontSize = 13.sp)
                    Text("${(pct * 100).toInt()}%", color = T.textSecondary, fontSize = 11.sp)
                }
                Spacer(Modifier.height(3.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(T.backgroundBlock)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(pct.coerceIn(0.02f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (option.id == poll.voted) T.accentPrimary
                                else T.accentPrimary.copy(alpha = 0.5f)
                            )
                    )
                }
            }
        }
        if (total > 0) {
            Text("$total голосов", color = T.textSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun TrackPlayer(track: PostTrack) {
    val context = LocalContext.current
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    DisposableEffect(track.url) {
        onDispose { player?.release(); player = null }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(T.backgroundSecondary)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(T.backgroundHover),
            contentAlignment = Alignment.Center
        ) {
            if (track.cover != null) {
                AsyncImage(model = track.cover, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = T.textSecondary)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title ?: "Трек", color = T.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            track.artist?.let { Text(it, color = T.textSecondary, fontSize = 11.sp, maxLines = 1) }
        }
        IconButton(onClick = {
            val url = track.url ?: return@IconButton
            if (player == null) {
                player = MediaPlayer().apply {
                    setDataSource(url)
                    setOnCompletionListener { isPlaying = false }
                    prepareAsync()
                    setOnPreparedListener { it.start(); isPlaying = true }
                }
            } else if (isPlaying) {
                player?.pause(); isPlaying = false
            } else {
                player?.start(); isPlaying = true
            }
        }) {
            Icon(
                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = T.accentPrimary
            )
        }
    }
}

@Composable
private fun MediaGallery(media: List<PostMedia>) {
    if (media.size == 1) {
        AsyncImage(
            model = media[0].url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(T.backgroundHover)
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.height(if (media.size > 2) 292.dp else 140.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(media.take(4)) { item ->
                AsyncImage(
                    model = item.url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(T.backgroundHover)
                )
            }
        }
    }
}

@Composable
private fun ActionsRow(
    post: Post,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onRepost: () -> Unit,
    onBookmark: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ActionButton(
            icon = if (post.liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            count = post.likesCount,
            tint = if (post.liked) T.accentLike else T.textSecondary,
            onClick = onLike
        )
        ActionButton(
            icon = Icons.Filled.ChatBubbleOutline,
            count = post.commentsCount,
            tint = T.textSecondary,
            onClick = onComment
        )
        ActionButton(
            icon = Icons.Filled.Repeat,
            count = post.repostsCount,
            tint = if (post.reposted) T.accentRepost else T.textSecondary,
            onClick = onRepost
        )
        Spacer(Modifier.weight(1f))
        ActionButton(
            icon = if (post.bookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
            count = 0,
            tint = if (post.bookmarked) T.accentPrimary else T.textSecondary,
            onClick = onBookmark
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Visibility, contentDescription = null, tint = T.textSecondary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("${post.viewsCount}", color = T.textSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        if (count > 0) {
            Spacer(Modifier.width(5.dp))
            Text("$count", color = tint, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ComposerSheet(
    text: String,
    posting: Boolean,
    onTextChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(T.background)
                .clickable(enabled = false) {}
                .padding(20.dp)
        ) {
            Text("Новый пост", color = T.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("Что происходит?", color = T.textSecondary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(T.accentPrimary)
                    .clickable(enabled = !posting && text.isNotBlank(), onClick = onSubmit)
                    .padding(vertical = 14.dp)
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (posting) {
                        CircularProgressIndicator(color = T.textPrimary, modifier = Modifier.size(20.dp))
                    } else {
                        Text("Опубликовать", color = T.textPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
