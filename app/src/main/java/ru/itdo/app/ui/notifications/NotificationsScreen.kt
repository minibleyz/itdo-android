package ru.itdo.app.ui.notifications

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
import kotlinx.coroutines.launch
import ru.itdo.app.core.AppContainer
import ru.itdo.app.data.model.AppNotification

/**
 * Notifications screen — 1:1 with iOS NotificationsView.
 */
@Composable
fun NotificationsScreen(
    container: AppContainer,
    onNavigateBack: () -> Unit = {}
) {
    var notifications by remember { mutableStateOf<List<AppNotification>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        loading = true
        runCatching { container.repository.notifications() }
            .onSuccess { notifications = it.notifications; error = it.error }
            .onFailure { error = it.message }
        loading = false
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Уведомления") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        scope.launch {
                            runCatching { container.repository.markNotificationsRead() }
                            load()
                        }
                    }) {
                        Text("Прочитать всё", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                loading && notifications.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                notifications.isEmpty() -> {
                    EmptyState()
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        items(notifications, key = { it.id }) { notification ->
                            NotificationRow(notification)
                            Divider(
                                color = Color(0x1F000000),
                                modifier = Modifier.padding(start = 72.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(notification: AppNotification) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = !notification.isRead) { /* TODO: navigate to target */ }
            .background(
                if (!notification.isRead) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else Color.Transparent
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Icon circle
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                getNotificationIcon(notification.type),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(15.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Text
        Column(Modifier.weight(1f)) {
            val actorName = notification.fromUser?.displayName ?: "Кто-то"
            val actionText = notification.text ?: ""
            Text(
                "$actorName $actionText",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 14.sp,
                lineHeight = 18.sp
            )
            notification.createdAt?.let {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    formatNotificationTime(it),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }

        // Unread dot
        if (!notification.isRead) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
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
            Icons.Default.NotificationsOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Пока нет уведомлений",
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontSize = 15.sp
        )
    }
}

private fun getNotificationIcon(type: String?): androidx.compose.ui.graphics.vector.ImageVector {
    return when (type) {
        "like" -> Icons.Default.Favorite
        "comment" -> Icons.Default.ChatBubble
        "repost" -> Icons.Default.Repeat
        "follow" -> Icons.Default.PersonAdd
        // В Material Icons нет иконок 'At'/'Bell' — используем существующие аналоги.
        "mention" -> Icons.Default.AlternateEmail
        "gift" -> Icons.Default.CardGiftcard
        "stream_like" -> Icons.Default.Favorite
        "donate" -> Icons.Default.MonetizationOn
        else -> Icons.Default.Notifications
    }
}

private fun formatNotificationTime(dateStr: String): String {
    // Simple time formatting — in production use a proper date formatter
    return dateStr
}
