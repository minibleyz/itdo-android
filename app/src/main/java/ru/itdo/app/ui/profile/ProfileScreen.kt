package ru.itdo.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import ru.itdo.app.core.AppContainer
import ru.itdo.app.data.model.Clip
import ru.itdo.app.data.model.Post
import ru.itdo.app.data.model.User

private enum class ProfileTab { POSTS, LIKES, CLIPS, BOOKMARKS }

/**
 * Профиль — по образцу ITDOApp/Views/Profile (iOS) и loadProfile() из веба:
 * баннер + аватар внахлёст, статистика (посты/подписчики/подписки),
 * кнопка подписки (или Редактировать — для себя), вкладки контента.
 *
 * userId == null → свой профиль (как раньше). Иначе — чужой (без
 * кнопки логаута, но с кнопкой подписки) — используется, когда где-то
 * в приложении появится переход по автору поста/клипа.
 */
@Composable
fun ProfileScreen(container: AppContainer, userId: Int? = null, onLogout: () -> Unit) {
    val scope = rememberCoroutineScope()
    var user by remember { mutableStateOf<User?>(null) }
    var isMe by remember { mutableStateOf(userId == null) }
    var error by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableStateOf(ProfileTab.POSTS) }
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var clips by remember { mutableStateOf<List<Clip>>(emptyList()) }
    var contentLoading by remember { mutableStateOf(false) }
    var followBusy by remember { mutableStateOf(false) }

    suspend fun loadUser() {
        if (userId == null) {
            val resp = runCatching { container.repository.me() }.getOrNull()
            user = resp?.user
            isMe = true
            if (resp?.user == null) error = resp?.error ?: "Ошибка загрузки"
        } else {
            val u = runCatching { container.repository.user(userId.toString()) }.getOrNull()
            user = u
            // me() уже дороже — сверяем по id, чтобы не делать второй запрос,
            // если это на самом деле свой профиль, открытый по id.
            val myId = runCatching { container.repository.me() }.getOrNull()?.user?.id
            isMe = myId != null && myId == userId
            if (u == null) error = "Профиль не найден"
        }
    }

    suspend fun loadTabContent(u: User, t: ProfileTab) {
        contentLoading = true
        when (t) {
            ProfileTab.POSTS -> posts = runCatching { container.repository.fetchUserPosts(u.id) }.getOrNull()?.posts ?: emptyList()
            ProfileTab.LIKES -> posts = runCatching { container.repository.fetchLikedPosts() }.getOrNull()?.posts ?: emptyList()
            ProfileTab.BOOKMARKS -> posts = runCatching { container.repository.bookmarks() }.getOrNull()?.posts ?: emptyList()
            ProfileTab.CLIPS -> clips = runCatching { container.repository.clips(userId = u.id) }.getOrNull()?.clips ?: emptyList()
        }
        contentLoading = false
    }

    LaunchedEffect(userId) {
        loadUser()
        user?.let { loadTabContent(it, tab) }
    }
    LaunchedEffect(tab, user?.id) {
        user?.let { loadTabContent(it, tab) }
    }

    val u = user
    if (u == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (error != null) Text(error ?: "", color = MaterialTheme.colorScheme.error)
            else CircularProgressIndicator()
        }
        return
    }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            ProfileHeader(
                user = u,
                isMe = isMe,
                followBusy = followBusy,
                onFollowToggle = {
                    scope.launch {
                        followBusy = true
                        val ok = if (u.isFollowing) {
                            container.repository.unfollowUser(u.id).error == null
                        } else {
                            container.repository.followUser(u.id).error == null
                        }
                        if (ok) loadUser()
                        followBusy = false
                    }
                },
                onLogout = { scope.launch { container.repository.logout(); onLogout() } }
            )
            ProfileTabRow(tab = tab, isMe = isMe, onTabChange = { tab = it })
        }

        if (contentLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (tab == ProfileTab.CLIPS) {
            item {
                if (clips.isEmpty()) EmptyTabState("Клипов пока нет")
                else ClipsGrid(clips)
            }
        } else {
            if (posts.isEmpty()) {
                item { EmptyTabState(emptyMessageFor(tab)) }
            } else {
                items(posts) { post -> ProfilePostRow(post) }
            }
        }
    }
}

private fun emptyMessageFor(tab: ProfileTab) = when (tab) {
    ProfileTab.POSTS -> "Постов пока нет"
    ProfileTab.LIKES -> "Лайкнутых постов нет"
    ProfileTab.BOOKMARKS -> "Закладок пока нет"
    ProfileTab.CLIPS -> "Клипов пока нет"
}

@Composable
private fun ProfileHeader(
    user: User,
    isMe: Boolean,
    followBusy: Boolean,
    onFollowToggle: () -> Unit,
    onLogout: () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        // Баннер — как .profile-banner в веб-версии / bannerImage в iOS.
        Box(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    if (user.banner != null) Color.Transparent
                    else MaterialTheme.colorScheme.surfaceVariant
                )
        ) {
            if (user.banner != null) {
                AsyncImage(
                    model = user.banner,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            // Аватар внахлёст на баннер, как в iOS/web.
            Box(Modifier.offset(y = (-36).dp)) {
                Box(
                    Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (user.avatar != null) {
                        AsyncImage(model = user.avatar, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
                    } else {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Row(Modifier.fillMaxWidth().offset(y = (-24).dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(user.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        if (user.isVerified) {
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Filled.Verified, contentDescription = "Верифицирован", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                    Text("@${user.username}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }

                if (isMe) {
                    OutlinedButton(onClick = { /* TODO: экран редактирования профиля */ }) { Text("Редактировать") }
                } else {
                    Button(onClick = onFollowToggle, enabled = !followBusy) {
                        Text(if (user.isFollowing) "Отписаться" else "Подписаться")
                    }
                }
            }

            user.bio?.takeIf { it.isNotBlank() }?.let {
                Text(it, Modifier.offset(y = (-16).dp).padding(bottom = 4.dp), fontSize = 14.sp)
            }

            Row(Modifier.offset(y = (-8).dp).padding(bottom = 8.dp)) {
                StatItem(user.postsCount, "Постов")
                Spacer(Modifier.width(20.dp))
                StatItem(user.followersCount, "Подписчиков")
                Spacer(Modifier.width(20.dp))
                StatItem(user.followingCount, "Подписок")
            }

            if (isMe) {
                TextButton(onClick = onLogout, modifier = Modifier.padding(bottom = 8.dp)) {
                    Text("Выйти из аккаунта", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun StatItem(count: Int, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(count.toString(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.width(4.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
    }
}

@Composable
private fun ProfileTabRow(tab: ProfileTab, isMe: Boolean, onTabChange: (ProfileTab) -> Unit) {
    val tabs = buildList {
        add(ProfileTab.POSTS to "Посты")
        add(ProfileTab.LIKES to "Лайки")
        if (isMe) add(ProfileTab.BOOKMARKS to "Закладки")
        add(ProfileTab.CLIPS to "Клипы")
    }
    TabRow(selectedTabIndex = tabs.indexOfFirst { it.first == tab }.coerceAtLeast(0)) {
        tabs.forEach { (t, label) ->
            Tab(selected = tab == t, onClick = { onTabChange(t) }, text = { Text(label) })
        }
    }
}

@Composable
private fun EmptyTabState(text: String) {
    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ProfilePostRow(post: Post) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        post.text?.takeIf { it.isNotBlank() }?.let {
            Text(it, fontSize = 15.sp)
            Spacer(Modifier.height(6.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (post.liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = null,
                tint = if (post.liked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(post.likesCount.toString(), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(16.dp))
            Icon(Icons.Filled.ChatBubbleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(post.commentsCount.toString(), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun ClipsGrid(clips: List<Clip>) {
    // Простая обёртка без LazyVerticalGrid, т.к. уже внутри LazyColumn.
    val rows = clips.chunked(3)
    Column(Modifier.padding(horizontal = 8.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { clip ->
                    Box(
                        Modifier
                            .weight(1f)
                            .padding(4.dp)
                            .aspectRatio(9f / 16f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (clip.thumbnailUrl != null) {
                            AsyncImage(model = clip.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
