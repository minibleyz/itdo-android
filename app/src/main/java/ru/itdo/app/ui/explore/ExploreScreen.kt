package ru.itdo.app.ui.explore

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import coil.compose.AsyncImage
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ru.itdo.app.core.AppContainer
import ru.itdo.app.data.model.Post
import ru.itdo.app.data.model.User
import ru.itdo.app.ui.components.PinBadges

/**
 * Explore screen — 1:1 with iOS ExploreView.
 * Tabs: "В тренде" / "Люди", search bar, search results.
 */
@Composable
fun ExploreScreen(
    container: AppContainer,
    onNavigateBack: () -> Unit = {},
    onOpenAuthor: (Int) -> Unit = {}
) {
    var query by remember { mutableStateOf(TextFieldValue("")) }
    var selectedTab by remember { mutableStateOf(0) } // 0 = trending, 1 = people
    var trendingPosts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var peopleList by remember { mutableStateOf<List<User>>(emptyList()) }
    var searchResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var trendingLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Debounce search
    var searchTask: Job? by remember { mutableStateOf(null) }

    fun performSearch(q: String) {
        searchTask?.cancel()
        if (q.trim().length < 2) {
            searchResults = emptyList()
            return
        }
        searchTask = scope.launch {
            loading = true
            runCatching { container.repository.search(q.trim()) }
                .onSuccess {
                    val results = mutableListOf<SearchResult>()
                    it.users.forEach { u -> results.add(SearchResult.UserResult(u)) }
                    it.posts.forEach { p -> results.add(SearchResult.PostResult(p)) }
                    searchResults = results
                }
                .onFailure { error = it.message }
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        // Load trending
        runCatching { container.repository.fetchTrending() }
            .onSuccess { trendingPosts = it.posts }
            .onFailure { error = it.message }
        trendingLoading = false
        // Load people suggestions
        // fetchSuggestions() возвращает List<UserSuggestion> (укороченная модель
        // без bio/isNuksta) — приводим к полноценному User для переиспользования PeopleRow.
        runCatching { container.repository.fetchSuggestions() }
            .onSuccess { response ->
                peopleList = response.users.map { u ->
                    User(
                        id = u.id,
                        username = u.username,
                        name = u.name,
                        avatar = u.avatar,
                        isVerified = u.isVerified
                    )
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Поиск") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Search bar
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color(0x8C8C8C), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        performSearch(it.text)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFFF0F0F2), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    textStyle = TextStyle(color = Color(0xFF0A0A0A), fontSize = 15.sp)
                )
                if (query.text.isNotEmpty()) {
                    IconButton(onClick = { query = TextFieldValue(""); searchResults = emptyList() }) {
                        Icon(Icons.Default.Close, contentDescription = "Очистить", tint = Color(0x8C8C8C))
                    }
                }
            }

            if (query.text.trim().length >= 2) {
                // Search results
                if (loading && searchResults.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 60.dp))
                } else if (searchResults.isEmpty()) {
                    Text("Ничего не найдено", color = Color(0x8C8C8C), modifier = Modifier.padding(top = 60.dp).align(Alignment.CenterHorizontally))
                } else {
                    LazyColumn(contentPadding = PaddingValues(bottom = 110.dp)) {
                        items(searchResults, key = { it.id }) { result ->
                            SearchResultRow(result, onOpenAuthor)
                            Divider(color = Color(0x1F000000), modifier = Modifier.padding(start = 72.dp))
                        }
                    }
                }
            } else {
                // Tabs
                Row(Modifier.fillMaxWidth().padding(horizontal = 0.dp)) {
                    TabSegment("В тренде", selected = selectedTab == 0) { selectedTab = 0 }
                    TabSegment("Люди", selected = selectedTab == 1) { selectedTab = 1 }
                }
                Divider(color = Color(0x1F000000))

                when (selectedTab) {
                    0 -> {
                        // Trending posts
                        if (trendingLoading && trendingPosts.isEmpty()) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 60.dp))
                        } else if (trendingPosts.isEmpty()) {
                            Text("Пока нечего показать", color = Color(0x8C8C8C), modifier = Modifier.padding(top = 60.dp).align(Alignment.CenterHorizontally))
                        } else {
                            LazyColumn(contentPadding = PaddingValues(top = 12.dp, bottom = 110.dp)) {
                                items(trendingPosts, key = { it.id }) { post ->
                                    PostCardCompact(
                                        post,
                                        onOpenAuthor = onOpenAuthor,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        // People list
                        if (peopleList.isEmpty()) {
                            Column(
                                Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.PersonOutline, contentDescription = null, tint = Color(0x668C8C8C), modifier = Modifier.size(36.dp))
                                Spacer(Modifier.height(12.dp))
                                Text("Пользователей не найдено", color = Color(0x8C8C8C))
                            }
                        } else {
                            LazyColumn(contentPadding = PaddingValues(top = 4.dp, bottom = 110.dp)) {
                                items(peopleList, key = { it.id }) { user ->
                                    PeopleRow(user, onTap = { onOpenAuthor(user.id) })
                                    Divider(color = Color(0x1F000000), modifier = Modifier.padding(start = 72.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// weight() — расширение RowScope, поэтому функция должна быть его extension,
// а не принимать обычный Modifier: иначе .weight(1f) не резолвится.
@Composable
private fun RowScope.TabSegment(title: String, selected: Boolean, onClick: () -> Unit) {
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
private fun PostCardCompact(post: Post, onOpenAuthor: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = post.author?.avatar,
                contentDescription = null,
                modifier = Modifier.size(28.dp).clip(CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    post.author?.displayName ?: "",
                    color = Color(0xFF0A0A0A),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { post.author?.id?.let(onOpenAuthor) }
                )
                Text("@${post.author?.username ?: ""}", color = Color(0x8C8C8C), fontSize = 11.sp)
            }
        }
        post.text?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, color = Color(0xFF0A0A0A), fontSize = 14.sp, maxLines = 3)
        }
    }
}

@Composable
private fun PeopleRow(user: User, onTap: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFFF5F5F7)),
            contentAlignment = Alignment.Center
        ) {
            if (user.avatar != null) {
                AsyncImage(model = user.avatar, contentDescription = null, modifier = Modifier.fillMaxSize())
            } else {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0x8C8C8C))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(user.displayName, color = Color(0xFF0A0A0A), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(4.dp))
                PinBadges(isVerified = user.isVerified, isNuksta = user.isNuksta)
            }
            Text("@${user.username}", color = Color(0x8C8C8C), fontSize = 13.sp)
            user.bio?.let {
                Text(it, color = Color(0x8C8C8C), fontSize = 11.sp, maxLines = 1)
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0x668C8C8C))
    }
}

private sealed interface SearchResult {
    val id: Int
    data class UserResult(val user: User) : SearchResult { override val id: Int get() = user.id }
    data class PostResult(val post: Post) : SearchResult { override val id: Int get() = post.id }
}

@Composable
private fun SearchResultRow(result: SearchResult, onOpenAuthor: (Int) -> Unit) {
    when (result) {
        is SearchResult.UserResult -> {
            PeopleRow(result.user) { onOpenAuthor(result.user.id) }
        }
        is SearchResult.PostResult -> {
            PostCardCompact(result.post, onOpenAuthor)
        }
    }
}
