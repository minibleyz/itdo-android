package ru.itdo.app.ui.profile

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
import ru.itdo.app.data.model.Post
import ru.itdo.app.data.model.User
import ru.itdo.app.ui.components.PinBadges

/**
 * UserProfile screen — shows another user's profile.
 * 1:1 with iOS UserProfileView.
 */
@Composable
fun UserProfileScreen(
    container: AppContainer,
    userId: Int,
    onNavigateBack: () -> Unit = {},
    onOpenAuthor: (Int) -> Unit = {}
) {
    var user by remember { mutableStateOf<User?>(null) }
    var userPosts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var selectedTab by remember { mutableStateOf(0) } // 0=posts, 1=likes, 2=bookmarks
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Profile tabs
    val tabs = listOf("Посты", "Лайки", "Закладки")

    suspend fun loadProfile() {
        loading = true
        runCatching { container.repository.getUser(userId) }
            .onSuccess {
                user = it.user
                userPosts = it.posts
                error = it.error
            }
            .onFailure { error = it.message }
        loading = false
    }

    LaunchedEffect(Unit) { loadProfile() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Профиль") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        if (user == null && loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onBackground)
            }
        } else if (user == null && error != null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(error ?: "Ошибка", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else if (user != null) {
            UserProfileContent(
                user = user!!,
                posts = userPosts,
                selectedTab = selectedTab,
                onTabChange = { selectedTab = it },
                onNavigateBack = onNavigateBack,
                onOpenAuthor = onOpenAuthor,
                onFollow = {
                    scope.launch {
                        val isFollowing = user!!.isFollowing
                        runCatching {
                            if (isFollowing) container.repository.unfollowUser(userId)
                            else container.repository.followUser(userId)
                        }
                        loadProfile()
                    }
                },
                onRefresh = { scope.launch { loadProfile() } }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserProfileContent(
    user: User,
    posts: List<Post>,
    selectedTab: Int,
    onTabChange: (Int) -> Unit,
    onNavigateBack: () -> Unit,
    onOpenAuthor: (Int) -> Unit,
    onFollow: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        // Banner
        Box(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(Color(0xFF222222))
        ) {
            if (user.banner != null) {
                AsyncImage(
                    model = user.banner,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Column(Modifier.padding(top = -36.dp, horizontal = 20.dp)) {
            // Avatar
            Box(
                Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF222222))
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (user.avatar != null) {
                    AsyncImage(model = user.avatar, contentDescription = null, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color(0x8C8C8C))
                }
            }
            Spacer(Modifier.height(10.dp))

            // Name and badges
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    user.name?.takeIf { it.isNotBlank() } ?: user.username,
                    color = Color(0xFF0A0A0A),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                PinBadges(
                    isVerified = user.isVerified,
                    isNuksta = user.isNuksta,
                    isBanned = user.isBanned,
                    pinChoice = user.pinChoice
                )
            }

            Text("@${user.username}", color = Color(0x8C8C8C), fontSize = 15.sp)

            user.bio?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, color = Color(0xFF0A0A0A), fontSize = 15.sp, lineHeight = 20.sp)
            }

            // Action buttons
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Follow button
                Button(
                    onClick = onFollow,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    shape = RoundedCornerShape(20),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        if (user.isFollowing) "Отписаться" else "Подписаться",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Share button
                OutlinedButton(
                    onClick = { /* TODO: share profile */ },
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    shape = RoundedCornerShape(20),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Поделиться", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Stats
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                StatItem("${user.postsCount}", "Посты")
                StatItem("${user.followersCount}", "Подписчики")
                StatItem("${user.followingCount}", "Подписки")
            }

            // Tabs
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                tabs.forEachIndexed { index, title ->
                    TabSegment(title, selected = selectedTab == index) { onTabChange(index) }
                }
            }
            Divider(color = Color(0x1F000000))

            // Tab content
            when (selectedTab) {
                0 -> {
                    if (posts.isEmpty()) {
                        Text("Постов нет", color = Color(0x8C8C8C), modifier = Modifier.padding(top = 60.dp).align(Alignment.CenterHorizontally))
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, bottom = 40.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(posts, key = { it.id }) { post ->
                                PostCardCompact(post, onOpenAuthor = onOpenAuthor)
                            }
                        }
                    }
                }
                1 -> {
                    Text("Лайков нет", color = Color(0x8C8C8C), modifier = Modifier.padding(top = 60.dp).align(Alignment.CenterHorizontally))
                }
                2 -> {
                    Text("Закладок нет", color = Color(0x8C8C8C), modifier = Modifier.padding(top = 60.dp).align(Alignment.CenterHorizontally))
                }
            }
        }
    }
}

@Composable
private fun StatItem(value: String, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(value, color = Color(0xFF0A0A0A), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(5.dp))
        Text(title, color = Color(0x8C8C8C), fontSize = 14.sp)
    }
}

@Composable
private fun TabSegment(title: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .weight(1f)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            title,
            color = if (selected) Color(0xFF0A0A0A) else Color(0x8C8C8C),
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
        if (selected) {
            Spacer(
                Modifier
                    .fillMaxWidth(0.6f)
                    .height(2.dp)
                    .align(Alignment.BottomCenter)
                    .background(Color(0xFF0080FF))
            )
        }
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = post.author?.avatar,
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        post.author?.displayName ?: "",
                        color = Color(0xFF0A0A0A),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { post.author?.id?.let(onOpenAuthor) }
                    )
                    if (post.author?.isVerified == true) {
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(14.dp))
                    }
                }
                Text("@${post.author?.username ?: ""}", color = Color(0x8C8C8C), fontSize = 14.sp)
            }
        }
        post.text?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = Color(0xFF0A0A0A), fontSize = 15.sp, lineHeight = 20.sp)
        }
    }
}
