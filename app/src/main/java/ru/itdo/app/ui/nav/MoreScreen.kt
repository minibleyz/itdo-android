package ru.itdo.app.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class MoreItem(val title: String, val icon: ImageVector, val route: String)
private data class MoreSection(val title: String, val items: List<MoreItem>)

/**
 * "Ещё" — 1:1 с iOS MoreView (RootView.swift): сгруппированный список
 * всех второстепенных разделов.
 */
@Composable
fun MoreScreen(isAdmin: Boolean, onOpen: (String) -> Unit) {
    val sections = buildList {
        add(
            MoreSection(
                "Общее",
                buildList {
                    add(MoreItem("Профиль", Icons.Filled.Person, "profile"))
                    add(MoreItem("Верификация", Icons.Filled.Verified, "verification"))
                    add(MoreItem("Настройки", Icons.Filled.Settings, "settings"))
                    add(MoreItem("Звонки", Icons.Filled.Phone, "calls"))
                    add(MoreItem("Поддержка", Icons.Filled.QuestionMark, "support"))
                    if (isAdmin) add(MoreItem("Админка", Icons.Filled.AdminPanelSettings, "admin"))
                }
            )
        )
        add(
            MoreSection(
                "Развлечения",
                buildList {
                    add(MoreItem("Эфиры", Icons.Filled.Radio, "streams"))
                    add(MoreItem("Клипы", Icons.Filled.PlayCircle, "clips"))
                    add(MoreItem("ITDO Agent", Icons.Filled.AutoAwesome, "agent"))
                    add(MoreItem("Топ", Icons.Filled.Leaderboard, "leaderboard"))
                    add(MoreItem("Квесты", Icons.Filled.SportsEsports, "quests"))
                    add(MoreItem("Плейлисты", Icons.Filled.QueueMusic, "playlists"))
                    add(MoreItem("Статьи", Icons.Filled.Article, "articles"))
                    add(MoreItem("Подарки", Icons.Filled.CardGiftcard, "gifts"))
                }
            )
        )
        add(
            MoreSection(
                "Финансы",
                buildList {
                    add(MoreItem("Кошелёк", Icons.Filled.AccountBalanceWallet, "wallet"))
                    add(MoreItem("ITDO PRO", Icons.Filled.Star, "nuksta"))
                }
            )
        )
    }

    Scaffold(topBar = {
        CenterAlignedTopAppBar(title = { Text("Ещё") })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            sections.forEach { section ->
                item {
                    Text(
                        section.title.uppercase(),
                        Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(section.items) { item -> MoreRow(item) { onOpen(item.route) } }
            }
        }
    }
}

@Composable
private fun MoreRow(item: MoreItem, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(item.title, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
