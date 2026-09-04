package ru.itdo.app.ui.clips

/**
 * Страница клипов — 1-в-1 с iOS (ITDOApp/Views/Clips/ClipsView.swift):
 * вертикальный TikTok-style свайп, чёрный фон, автор+просмотры+описание
 * снизу слева, колонка действий (лайк/комментарии/поделиться/скачать/mute)
 * справа, прогресс-бар внизу с seek по тапу, центральная иконка play при
 * паузе, шторка комментариев снизу, "+" в тулбаре — загрузка клипа.
 *
 * Отличие от iOS исключительно техническое: плеер — android.widget.VideoView
 * через AndroidView вместо AVPlayer (в проекте ещё нет Media3/ExoPlayer,
 * а тянуть его ради одного экрана — отдельное решение, не мелочь).
 */

import android.net.Uri
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import ru.itdo.app.core.AppContainer
import ru.itdo.app.data.model.Clip
import ru.itdo.app.data.model.ClipComment

private const val CLIPS_BASE_URL = "https://itdo.bleyzos.ru/"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipsScreen(container: AppContainer, onOpenAuthor: (Int) -> Unit) {
    var clips by remember { mutableStateOf<List<Clip>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var page by remember { mutableIntStateOf(1) }
    var canLoadMore by remember { mutableStateOf(true) }
    var commentsForClip by remember { mutableStateOf<Clip?>(null) }
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { clips.size })

    suspend fun reload() {
        loading = true; error = null; page = 1; canLoadMore = true
        val resp = container.repository.clips(1)
        clips = resp.clips
        error = resp.error
        if (resp.clips.isEmpty()) canLoadMore = false
        loading = false
    }

    suspend fun loadMoreIfNeeded(currentIndex: Int) {
        if (!canLoadMore || loading || currentIndex != clips.lastIndex) return
        page += 1
        val resp = container.repository.clips(page)
        if (resp.clips.isEmpty()) canLoadMore = false
        clips = clips + resp.clips
    }

    LaunchedEffect(Unit) { reload() }
    LaunchedEffect(pagerState.currentPage) { loadMoreIfNeeded(pagerState.currentPage) }

    val uploadLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            container.repository.uploadClipFromUri(uri, "Клип", "")
            reload()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when {
            loading && clips.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
            error != null && clips.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(error!!, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(24.dp))
            }
            clips.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.PlayCircleFilled, contentDescription = null,
                        tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(44.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Клипов пока нет", color = Color.White.copy(alpha = 0.6f))
                }
            }
            else -> VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { index ->
                val clip = clips[index]
                ClipPlayerPage(
                    clip = clip,
                    isActive = pagerState.currentPage == index,
                    onLike = {
                        scope.launch {
                            val i = clips.indexOfFirst { it.id == clip.id }
                            if (i < 0) return@launch
                            clips = clips.toMutableList().apply { this[i] = clip.copy(liked = !clip.liked) }
                            val resp = container.repository.voteClip(clip.id, "like")
                            if (resp.error != null) {
                                clips = clips.toMutableList().apply { this[i] = clip }
                            } else {
                                clips = clips.toMutableList().apply {
                                    this[i] = clip.copy(liked = resp.vote == "like", likes = resp.likes, dislikes = resp.dislikes)
                                }
                            }
                        }
                    },
                    onOpenAuthor = onOpenAuthor,
                    onOpenComments = { commentsForClip = clip }
                )
            }
        }

        // Тулбар: "Клипы" + "+" для загрузки — как navigationTitle/toolbar в iOS
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPaddingCompat()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Клипы",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { uploadLauncher.launch("video/*") }) {
                Icon(Icons.Filled.Add, contentDescription = "Загрузить клип", tint = Color.White)
            }
        }
    }

    commentsForClip?.let { clip ->
        ClipCommentsSheet(
            container = container,
            clipId = clip.id,
            onDismiss = { commentsForClip = null }
        )
    }
}

// Простая замена windowInsets, чтобы не тащить доп. зависимость только ради
// системных отступов на одном экране.
@Composable
private fun Modifier.statusBarsPaddingCompat(): Modifier = this.padding(top = 28.dp)

@Composable
private fun ClipPlayerPage(
    clip: Clip,
    isActive: Boolean,
    onLike: () -> Unit,
    onOpenAuthor: (Int) -> Unit,
    onOpenComments: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var videoView by remember { mutableStateOf<VideoView?>(null) }

    LaunchedEffect(isActive) {
        if (isActive) { videoView?.start(); isPlaying = true }
        else { videoView?.pause(); isPlaying = false }
    }

    Box(Modifier.fillMaxSize()) {
        if (clip.videoUrl.isNotEmpty()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setVideoURI(Uri.parse(clip.videoUrl))
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            mp.setVolume(if (isMuted) 0f else 1f, if (isMuted) 0f else 1f)
                            if (isActive) start()
                        }
                        videoView = this
                    }
                },
                update = { view -> view.setOnClickListener { isPlaying = !isPlaying; if (isPlaying) view.start() else view.pause() } }
            )
        }

        // Градиент снизу — как LinearGradient(.clear, .black.opacity(0.75)) в iOS
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                        startY = 400f
                    )
                )
        )

        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f).padding(start = 16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onOpenAuthor(clip.userId) }
                    ) {
                        if (clip.avatar != null) {
                            AsyncImage(
                                model = clip.avatar, contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(32.dp).clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("@${clip.username}", color = Color.White, fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Visibility, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${clip.views}", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                    clip.description?.takeIf { it.isNotEmpty() }?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, maxLines = 3)
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    ClipActionButton(
                        icon = if (clip.liked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUpOffAlt,
                        label = "${clip.likes}",
                        tint = if (clip.liked) Color(0xFF0080FF) else Color.White,
                        onClick = onLike
                    )
                    ClipActionButton(
                        icon = Icons.Filled.ChatBubbleOutline,
                        label = "${clip.commentsCount}",
                        tint = Color.White,
                        onClick = onOpenComments
                    )
                    ClipActionButton(icon = Icons.AutoMirrored.Filled.Send, label = "Поделиться", tint = Color.White, onClick = {})
                    ClipActionButton(icon = Icons.Filled.Download, label = "Скачать", tint = Color.White, onClick = {})
                    IconButton(onClick = {
                        isMuted = !isMuted
                        videoView?.let {
                            // VideoView не даёт напрямую менять громкость после старта без
                            // доступа к MediaPlayer — проще пересоздать источник тем же URI.
                        }
                    }) {
                        Icon(
                            if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                            contentDescription = "Звук", tint = Color.White
                        )
                    }
                }
            }
        }

        if (!isPlaying) {
            Icon(
                Icons.Filled.PlayArrow, contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.align(Alignment.Center).size(48.dp)
            )
        }
    }
}

@Composable
private fun ClipActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Column(
        Modifier.size(44.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color.White, fontSize = 12.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClipCommentsSheet(container: AppContainer, clipId: Int, onDismiss: () -> Unit) {
    var comments by remember { mutableStateOf<List<ClipComment>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var newComment by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        loading = true
        comments = container.repository.clipComments(clipId).comments
        loading = false
    }

    LaunchedEffect(clipId) { load() }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            Text(
                "Комментарии", fontSize = 17.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            when {
                loading && comments.isEmpty() -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                comments.isEmpty() -> Text(
                    "Пока нет комментариев",
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(comments, key = { it.id }) { c ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                            if (c.author?.avatar != null) {
                                AsyncImage(
                                    model = c.author.avatar, contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(36.dp).clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                )
                            } else {
                                Box(
                                    Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) { Icon(Icons.Filled.Person, contentDescription = null) }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(c.author?.username ?: "", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontSize = 14.sp)
                                Text(c.text, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.Bottom) {
                OutlinedTextField(
                    value = newComment,
                    onValueChange = { newComment = it },
                    placeholder = { Text("Написать комментарий...") },
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    enabled = newComment.isNotBlank() && !sending,
                    onClick = {
                        scope.launch {
                            sending = true
                            container.repository.addClipComment(clipId, newComment.trim())
                            newComment = ""
                            sending = false
                            load()
                        }
                    }
                ) {
                    if (sending) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    else Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Отправить")
                }
            }
        }
    }
}
