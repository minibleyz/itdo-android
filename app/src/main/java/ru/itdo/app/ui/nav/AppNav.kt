package ru.itdo.app.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import ru.itdo.app.core.AppContainer
import ru.itdo.app.data.model.User
import ru.itdo.app.ui.admin.AdminScreen
import ru.itdo.app.ui.auth.LoginScreen
import ru.itdo.app.ui.auth.RegisterScreen
import ru.itdo.app.ui.chats.ChatScreen
import ru.itdo.app.ui.chats.ChatsListScreen
import ru.itdo.app.ui.feed.FeedScreen
import ru.itdo.app.ui.pixelbattle.PixelBattleScreen
import ru.itdo.app.ui.profile.ProfileScreen

private const val ROUTE_LOGIN = "login"
private const val ROUTE_REGISTER = "register"
private const val ROUTE_MAIN = "main"

private sealed class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Feed : Tab("feed", "Лента", Icons.Filled.Home)
    object Chats : Tab("chats", "Чаты", Icons.Filled.ChatBubble)
    object Pixel : Tab("pixel", "Pixel", Icons.Filled.GridOn)
    object Profile : Tab("profile", "Профиль", Icons.Filled.Person)
    object Admin : Tab("admin", "Админ", Icons.Filled.AdminPanelSettings)
}

@Composable
fun AppNav(container: AppContainer) {
    val rootNav = rememberNavController()
    var loggedIn by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) { loggedIn = container.repository.isLoggedIn() }

    when (loggedIn) {
        null -> Unit // сплэш/загрузка
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

    LaunchedEffect(Unit) {
        runCatching { container.repository.me() }.onSuccess { isAdmin = it.user?.isAdmin == true }
    }

    val tabs = buildList {
        add(Tab.Feed); add(Tab.Chats); add(Tab.Pixel); add(Tab.Profile)
        if (isAdmin) add(Tab.Admin)
    }
    val scope = rememberCoroutineScope()

    Scaffold(bottomBar = {
        NavigationBar {
            val backStack by nav.currentBackStackEntryAsState()
            val current = backStack?.destination?.route
            tabs.forEach { tab ->
                NavigationBarItem(
                    selected = current == tab.route,
                    onClick = { nav.navigate(tab.route) { launchSingleTop = true; popUpTo(Tab.Feed.route) } },
                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                    label = { Text(tab.label) }
                )
            }
        }
    }) { padding ->
        NavHost(nav, startDestination = Tab.Feed.route, modifier = Modifier.padding(padding)) {
            composable(Tab.Feed.route) { FeedScreen(container) }
            composable(Tab.Chats.route) {
                ChatsListScreen(container) { convId -> nav.navigate("chat/$convId") }
            }
            composable(
                "chat/{id}",
                arguments = listOf(navArgument("id") { type = NavType.IntType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getInt("id") ?: 0
                ChatScreen(container, id)
            }
            composable(Tab.Pixel.route) { PixelBattleScreen(container) }
            composable(Tab.Profile.route) {
                ProfileScreen(container) {
                    scope.launch {
                        container.repository.logout()
                        onLoggedOut()
                    }
                }
            }
            if (isAdmin) composable(Tab.Admin.route) { AdminScreen() }
        }
    }
}
