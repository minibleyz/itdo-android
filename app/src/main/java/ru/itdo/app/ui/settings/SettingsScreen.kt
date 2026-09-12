package ru.itdo.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import ru.itdo.app.core.AppContainer
import ru.itdo.app.data.model.Session
import ru.itdo.app.ui.biometric.isBiometricAvailable

/**
 * Настройки — полный набор из iOS SettingsView + AppearanceSettingsView.
 */
@Composable
fun SettingsScreen(
    container: AppContainer,
    onLoggedOut: () -> Unit,
    onOpenBookmarks: () -> Unit = {},
    onOpenHiddenAuthors: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var sessions by remember { mutableStateOf<List<Session>>(emptyList()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var biometricEnabled by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        sessions = runCatching { container.repository.sessions() }.getOrNull()?.sessions ?: emptyList()
        biometricEnabled = isBiometricAvailable(context)
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Настройки") },
            navigationIcon = {
                IconButton(onClick = { /* TODO: back */ }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                }
            }
        )
    }) { padding ->
        Column(
            Modifier.fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // Biometric
            if (biometricEnabled) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Безопасность", color = Color(0xFF0A0A0A), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(12.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = Color(0xFF0080FF))
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Отпечаток / Face Unlock", color = Color(0xFF0A0A0A), fontSize = 15.sp)
                                    Text("Быстрая авторизация", color = Color(0x8C8C8C), fontSize = 13.sp)
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0x8C8C8C))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Appearance
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Внешний вид", color = Color(0xFF0A0A0A), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    SettingRow(Icons.Default.DarkMode, "Тёмная тема", "Использовать тёмную тему")
                    Spacer(Modifier.height(8.dp))
                    SettingRow(Icons.Default.FontDownload, "Размер шрифта", "Настроить размер текста")
                }
            }

            Spacer(Modifier.height(16.dp))

            // Privacy
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Конфиденциальность", color = Color(0xFF0A0A0A), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    SettingRow(Icons.Default.Bookmarks, "Закладки", "Управление закладками") { onOpenBookmarks() }
                    Spacer(Modifier.height(8.dp))
                    SettingRow(Icons.Default.VisibilityOff, "Скрытые авторы", "Управление скрытыми авторами") { onOpenHiddenAuthors() }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Sessions
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("АКТИВНЫЕ СЕССИИ", color = Color(0x8C8C8C), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    sessions.forEach { s ->
                        ListItem(
                            headlineContent = { Text(s.userAgent ?: "Неизвестное устройство") },
                            supportingContent = { Text(listOfNotNull(s.ip, s.createdAt).joinToString(" · ")) },
                            trailingContent = {
                                if (s.isCurrent) Text("Текущая", color = Color(0xFF0080FF), fontSize = 12.sp)
                                else Text("Завершить", color = Color(0xFF00BA7C), fontSize = 12.sp)
                            },
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Account
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("АККАУНТ", color = Color(0x8C8C8C), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    ListItem(
                        headlineContent = { Text("Выйти") },
                        modifier = Modifier.clickable { showLogoutConfirm = true }
                    )
                    ListItem(
                        headlineContent = { Text("Удалить аккаунт", color = Color(0xFFF91880)) },
                        modifier = Modifier.clickable { showDeleteConfirm = true }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Version
            Text("ITDO v1.0.0", color = Color(0x8C8C8C), fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }

    // Logout confirm
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Выйти?") },
            text = { Text("Вы выйдете из своего аккаунта.") },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    scope.launch {
                        busy = true
                        container.repository.logout()
                        busy = false
                        showLogoutConfirm = false
                        onLoggedOut()
                    }
                }) { Text("Выйти", color = Color(0xFF0080FF)) }
            },
            dismissButton = { TextButton(onClick = { showLogoutConfirm = false }) { Text("Отмена") } }
        )
    }

    // Delete confirm
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Удалить аккаунт?") },
            text = { Text("Это действие необратимо — все данные будут удалены безвозвратно.") },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    scope.launch {
                        busy = true
                        val ok = container.repository.deleteAccount().error == null
                        busy = false
                        showDeleteConfirm = false
                        if (ok) onLoggedOut()
                    }
                }) { Text("Удалить", color = Color(0xFFF91880)) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick ?: {})
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF0080FF), modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color(0xFF0A0A0A), fontSize = 15.sp)
            Text(subtitle, color = Color(0x8C8C8C), fontSize = 13.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0x8C8C8C))
    }
}
