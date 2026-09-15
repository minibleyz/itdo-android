package ru.itdo.app.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import ru.itdo.app.core.AppContainer
import ru.itdo.app.ui.admin.AdminScreen
import ru.itdo.app.ui.agent.AgentScreen
import ru.itdo.app.ui.articles.ArticlesScreen
import ru.itdo.app.ui.auth.LoginScreen
import ru.itdo.app.ui.auth.RegisterScreen
import ru.itdo.app.ui.bookmarks.BookmarksScreen
import ru.itdo.app.ui.calls.CallsScreen
import ru.itdo.app.ui.chats.ChatScreen
import ru.itdo.app.ui.chats.ChatsListScreen
import ru.itdo.app.ui.clips.ClipsScreen
import ru.itdo.app.ui.components.ComingSoonScreen
import ru.itdo.app.ui.components.ItdoLoadingScreen
import ru.itdo.app.ui.explore.ExploreScreen
import ru.itdo.app.ui.feed.CommentsScreen
import ru.itdo.app.ui.feed.FeedScreen
import ru.itdo.app.ui.feed.PostDetailScreen
import ru.itdo.app.ui.hidden.HiddenAuthorsScreen
import ru.itdo.app.ui.leaderboard.LeaderboardScreen
import ru.itdo.app.ui.nuksta.NukstaScreen
import ru.itdo.app.ui.notifications.NotificationsScreen
import ru.itdo.app.ui.pixelbattle.PixelBattleScreen
import ru.itdo.app.ui.profile.EditProfileScreen
import ru.itdo.app.ui.profile.ProfileScreen
import ru.itdo.app.ui.profile.UserProfileScreen
import ru.itdo.app.ui.profile.VerificationScreen
import ru.itdo.app.ui.quest.QuestsScreen
import ru.itdo.app.ui.settings.SettingsScreen
import ru.itdo.app.ui.streams.StreamsScreen
import ru.itdo.app.ui.support.SupportScreen
import ru.itdo.app.ui.wallet.WalletScreen

private const val ROUTE_LOGIN = "login"
private const val ROUTE_REGISTER = "register"
private const val ROUTE_MAIN = "main"

// 5 табов — 1:1 с iOS (RootView.swift): Лента, Поиск, Увед., Сообщения, Ещё.
// "Профиль" отдельным табом больше не является — он переехал в "Ещё",
// как и в iOS-версии.
private sealed class MainTab(val route: String, val label: String, val icon: ImageVector) {
    object Feed : MainTab("feed", "Лента", Icons.Filled.Home)
    object Explore : MainTab("explore", "Поиск", Icons.Filled.Search)
    object Notifications : MainTab("notifications", "Увед.", Icons.Filled.Notifications)
    object Chats : MainTab("chats", "Сообщения", Icons.Filled.ChatBubble)
}
private const val ROUTE_MORE = "more"

@Composable
fun AppNav(container: AppContainer) {
    val rootNav = rememberNavController()
    var loggedIn by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) { loggedIn = container.repository.isLoggedIn() }

    when (loggedIn) {
        null -> ItdoLoadingScreen()
        false -> NavHost(rootNav, startDestination = ROUTE_LOGIN) {
            composable(ROUTE_LOGIN) {
                LoginScreen(
                    container = container,
                    onLoggedIn = { rootNav.navigate(ROUTE_MAIN) { popUpTo(ROUTE_LOGIN) { inclusive = true } } },
                    onGoRegister = { rootNav.navigate(ROUTE_REGISTER) }
                )
            }
            composable(ROUTE_REGISTER) {
                RegisterScreen(
                    container = container,
                    onRegistered = { rootNav.navigate(ROUTE_MAIN) { popUpTo(ROUTE_LOGIN) { inclusive = true } } },
                    onBack = { rootNav.popBackStack() }
                )
            }
            composable(ROUTE_MAIN) {
                MainTabs(container) {
                    rootNav.navigate(ROUTE_LOGIN) { popUpTo(0) }
                }
            }
        }
        true -> MainTabs(container) {
            loggedIn = false
        }
    }
}

@Composable
private fun MainTabs(container: AppContainer, onLoggedOut: () -> Unit) {
    val nav = rememberNavController()
    var isAdmin by remember { mutableStateOf(false) }
    var currentUser by remember { mutableStateOf<ru.itdo.app.data.model.User?>(null) }
    var coins by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        runCatching { container.repository.me() }.getOrNull()?.let { resp ->
            if (resp.user != null) {
                isAdmin = resp.user.isAdmin
                currentUser = resp.user
                coins = resp.user.coins
            } else if (resp.error != null) {
                container.repository.logout()
                onLoggedOut()
            }
        }
    }

    val tabs = listOf(MainTab.Feed, MainTab.Explore, MainTab.Notifications, MainTab.Chats)

    Scaffold(bottomBar = {
        val backStack by nav.currentBackStackEntryAsState()
        val current = backStack?.destination?.route
        // "Ещё" считается выбранной не только на самом экране "more", но и
        // на любом из вложенных в неё разделов (профиль, настройки, клипы,
        // кошелёк и т.д.) — иначе после перехода вглубь пилюля "Ещё"
        // выглядела бы невыбранной, хотя пользователь всё ещё в этой ветке.
        val moreRoutes = setOf(
            ROUTE_MORE, "profile", "editProfile", "verification", "settings",
            "calls", "support", "admin", "streams", "clips", "agent",
            "leaderboard", "quests", "playlists", "articles", "gifts",
            "wallet", "nuksta", "bookmarks", "hiddenAuthors", "pixel"
        )
        PillNavigationBar(
            tabs = tabs,
            current = current,
            onMoreSelected = current != null && current in moreRoutes,
            onTabClick = { tab -> nav.navigate(tab.route) { launchSingleTop = true; popUpTo(MainTab.Feed.route) } },
            onMoreClick = { nav.navigate(ROUTE_MORE) { launchSingleTop = true; popUpTo(MainTab.Feed.route) } }
        )
    }) { padding ->
        NavHost(nav, startDestination = MainTab.Feed.route, modifier = Modifier.padding(padding)) {
            // Main tabs
            composable(MainTab.Feed.route) { FeedScreen(container) }
            composable(MainTab.Explore.route) {
                ExploreScreen(
                    container = container,
                    onNavigateBack = {},
                    onOpenAuthor = { userId -> nav.navigate("userProfile/$userId") }
                )
            }
            composable(MainTab.Notifications.route) {
                NotificationsScreen(container = container, onNavigateBack = {})
            }
            composable(MainTab.Chats.route) {
                ChatsListScreen(container) { convId -> nav.navigate("chat/$convId") }
            }

            // More screen routes
            composable(ROUTE_MORE) {
                MoreScreen(isAdmin = isAdmin) { route -> nav.navigate(route) { launchSingleTop = true } }
            }
            composable("chat/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) { backStackEntry ->
                val id = backStackEntry.arguments?.getInt("id") ?: 0
                ChatScreen(container, id)
            }
            // Профиль — раньше был главным табом, теперь открывается только
            // из "Ещё" (как ProfileView в iOS MoreView), поведение то же.
            composable("profile") {
                ProfileScreen(container) {
                    scope.launch {
                        container.repository.logout()
                        onLoggedOut()
                    }
                }
            }
            composable("agent") { AgentScreen(container) }
            composable("pixel") { PixelBattleScreen(container) }
            composable("settings") {
                SettingsScreen(
                    container = container,
                    onLoggedOut = {
                        scope.launch {
                            container.repository.logout()
                            onLoggedOut()
                        }
                    },
                    onOpenBookmarks = { nav.navigate("bookmarks") },
                    onOpenHiddenAuthors = { nav.navigate("hiddenAuthors") }
                )
            }
            if (isAdmin) composable("admin") { AdminScreen() }

            // New screens from iOS
            composable("userProfile/{userId}", arguments = listOf(navArgument("userId") { type = NavType.IntType })) { backStackEntry ->
                val userId = backStackEntry.arguments?.getInt("userId") ?: 0
                UserProfileScreen(
                    container = container,
                    userId = userId,
                    onNavigateBack = { nav.popBackStack() },
                    onOpenAuthor = { uid -> nav.navigate("userProfile/$uid") }
                )
            }
            composable("bookmarks") {
                BookmarksScreen(
                    container = container,
                    onNavigateBack = { nav.popBackStack() },
                    onOpenAuthor = { uid -> nav.navigate("userProfile/$uid") }
                )
            }
            composable("comments/{postId}", arguments = listOf(navArgument("postId") { type = NavType.IntType })) { backStackEntry ->
                val postId = backStackEntry.arguments?.getInt("postId") ?: 0
                // Comments need a post object — pass via nav args or use a different approach
                // For now, use a simple post ID and fetch
                ru.itdo.app.ui.feed.CommentsScreen(
                    container = container,
                    post = ru.itdo.app.data.model.Post(
                        id = postId,
                        author = ru.itdo.app.data.model.User(0, ""),
                        text = "",
                        media = emptyList(),
                        track = null,
                        poll = null,
                        quote = null,
                        reactions = emptyList(),
                        likesCount = 0,
                        commentsCount = 0,
                        repostsCount = 0,
                        viewsCount = 0,
                        liked = false,
                        reposted = false,
                        bookmarked = false,
                        myReaction = null,
                        isPinned = false,
                        adminPinned = false,
                        createdAt = null
                    ),
                    onNavigateBack = { nav.popBackStack() }
                )
            }
            composable("postDetail/{postId}", arguments = listOf(navArgument("postId") { type = NavType.IntType })) { backStackEntry ->
                val postId = backStackEntry.arguments?.getInt("postId") ?: 0
                ru.itdo.app.ui.feed.PostDetailScreen(
                    post = ru.itdo.app.data.model.Post(
                        id = postId,
                        author = ru.itdo.app.data.model.User(0, ""),
                        text = "",
                        media = emptyList(),
                        track = null,
                        poll = null,
                        quote = null,
                        reactions = emptyList(),
                        likesCount = 0,
                        commentsCount = 0,
                        repostsCount = 0,
                        viewsCount = 0,
                        liked = false,
                        reposted = false,
                        bookmarked = false,
                        myReaction = null,
                        isPinned = false,
                        adminPinned = false,
                        createdAt = null
                    ),
                    onNavigateBack = { nav.popBackStack() },
                    onOpenComments = { nav.navigate("comments/$postId") },
                    onOpenAuthor = { uid -> nav.navigate("userProfile/$uid") }
                )
            }
            composable("editProfile") {
                currentUser?.let {
                    EditProfileScreen(
                        container = container,
                        currentUser = it,
                        onNavigateBack = { nav.popBackStack() }
                    )
                }
            }
            composable("verification") {
                VerificationScreen(
                    container = container,
                    isVerified = currentUser?.isVerified ?: false,
                    onNavigateBack = { nav.popBackStack() }
                )
            }
            composable("streams") {
                StreamsScreen(
                    onNavigateBack = { nav.popBackStack() },
                    onOpenStream = { streamId -> /* TODO: stream player */ }
                )
            }
            // Клипы — экран уже реализован (ClipsScreen), но раньше не был
            // подключён к навигации. Теперь открывается из "Ещё".
            composable("clips") {
                ClipsScreen(container) { uid -> nav.navigate("userProfile/$uid") }
            }
            composable("wallet") {
                WalletScreen(
                    balance = coins,
                    onNavigateBack = { nav.popBackStack() }
                )
            }
            composable("support") {
                SupportScreen(
                    container = container,
                    onNavigateBack = { nav.popBackStack() }
                )
            }
            composable("calls") {
                CallsScreen(
                    onNavigateBack = { nav.popBackStack() }
                )
            }
            composable("leaderboard") {
                LeaderboardScreen(
                    onNavigateBack = { nav.popBackStack() }
                )
            }
            composable("quests") {
                QuestsScreen(
                    container = container,
                    coins = coins,
                    onNavigateBack = { nav.popBackStack() }
                )
            }
            composable("articles") {
                ArticlesScreen(
                    onNavigateBack = { nav.popBackStack() },
                    onOpenArticle = { articleId -> /* TODO: article detail */ }
                )
            }
            composable("nuksta") {
                NukstaScreen(
                    container = container,
                    onNavigateBack = { nav.popBackStack() }
                )
            }
            composable("hiddenAuthors") {
                HiddenAuthorsScreen(
                    container = container,
                    onNavigateBack = { nav.popBackStack() }
                )
            }
            // Разделы, которых пока нет на Android (нет экрана вообще) —
            // вместо краша/белого экрана показываем заглушку.
            composable("playlists") {
                ComingSoonScreen(title = "Плейлисты", onNavigateBack = { nav.popBackStack() })
            }
            composable("gifts") {
                ComingSoonScreen(title = "Подарки", onNavigateBack = { nav.popBackStack() })
            }
        }
    }
}

@Composable
private fun PillNavigationBar(
    tabs: List<MainTab>,
    current: String?,
    onMoreSelected: Boolean,
    onTabClick: (MainTab) -> Unit,
    onMoreClick: () -> Unit
) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(vertical = 6.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEach { tab ->
                PillNavItem(
                    icon = tab.icon,
                    label = tab.label,
                    selected = current == tab.route,
                    onClick = { onTabClick(tab) }
                )
            }
            PillNavItem(
                icon = Icons.Filled.MoreHoriz,
                label = "Ещё",
                selected = onMoreSelected,
                onClick = onMoreClick
            )
        }
    }
}

@Composable
private fun PillNavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else androidx.compose.ui.graphics.Color.Transparent
    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = if (selected) 16.dp else 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        if (selected) {
            Spacer(Modifier.width(6.dp))
            Text(label, color = tint, style = MaterialTheme.typography.labelMedium)
        }
    }
}
