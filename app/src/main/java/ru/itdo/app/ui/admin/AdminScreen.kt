package ru.itdo.app.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Разделы соответствуют реальным эндпоинтам в /api/admin/*.php сайта:
 * announcements, antibot_settings, automod, bot_conversations, coins,
 * device_bans, ip_bans, logs, mail, posts, posts_bulk_delete, и т.д.
 * Каждый раздел стоит выводить отдельным экраном по мере необходимости —
 * их слишком много, чтобы включать все сразу в первую версию.
 */
private val SECTIONS = listOf(
    "Логи" to "admin/logs.php",
    "Посты (модерация)" to "admin/posts.php",
    "Бан по IP" to "admin/ip_bans.php",
    "Бан устройств" to "admin/device_bans.php",
    "Монеты" to "admin/coins.php",
    "Анонсы" to "admin/announcements.php",
    "Автомодерация" to "admin/automod.php",
    "Настройки антибота" to "admin/antibot_settings.php",
    "Почта" to "admin/mail.php"
)

@Composable
fun AdminScreen() {
    Scaffold(topBar = { TopAppBar(title = { Text("Админка") }) }) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            items(SECTIONS) { (title, endpoint) ->
                ListItem(
                    headlineContent = { Text(title) },
                    supportingContent = { Text(endpoint) }
                )
                Divider()
            }
        }
    }
}
